package com.sublearn.subtitles

import com.sublearn.domain.Cue
import com.sublearn.domain.SubtitleRepository
import com.sublearn.domain.SubtitleToken
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.util.Locale

/** Text-only SRT, WebVTT and ASS/SSA parser. Image-based subtitle formats are intentionally rejected. */
class DefaultSubtitleParser : SubtitleRepository {
    override suspend fun parse(bytes: ByteArray, trackId: String, formatHint: String?): List<Cue> {
        val text = SubtitleCharset.decode(bytes)
        val format = SubtitleFormat.from(formatHint, text)
        val parsed = when (format) {
            SubtitleFormat.SRT -> parseSrt(text, trackId)
            SubtitleFormat.WEBVTT -> parseWebVtt(text, trackId)
            SubtitleFormat.ASS -> parseAss(text, trackId)
        }
        return SubtitleNormalizer.normalize(parsed)
    }

    override fun cueAt(cues: List<Cue>, timeMs: Long): Cue? = CueTimelineIndex(cues).cueAt(timeMs)

    /** The Android repository replaces this with SAF/MediaStore lookup when permission permits. */
    override fun findExternalSidecar(videoUri: String, videoName: String): String? = null
}

enum class SubtitleFormat {
    SRT, WEBVTT, ASS;

    companion object {
        fun from(hint: String?, text: String): SubtitleFormat {
            val extension = hint?.substringAfterLast('.')?.substringBefore('?')?.lowercase(Locale.ROOT)
            return when {
                text.trimStart().startsWith("WEBVTT", ignoreCase = true) || extension in setOf("vtt", "webvtt") -> WEBVTT
                extension in setOf("ass", "ssa") || text.contains("[Events]", ignoreCase = true) -> ASS
                extension in setOf("srt", "sub") || text.contains("-->" ) -> SRT
                else -> throw IllegalArgumentException("Unsupported subtitle format (use SRT, WebVTT or text-based ASS/SSA)")
            }
        }
    }
}

object SubtitleCharset {
    private val windows1256 = Charset.forName("windows-1256")

    fun decode(bytes: ByteArray): String {
        require(bytes.isNotEmpty()) { "Subtitle file is empty" }
        if (bytes.startsWith(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))) {
            return bytes.copyOfRange(3, bytes.size).toString(Charsets.UTF_8)
        }
        if (bytes.startsWith(byteArrayOf(0xFF.toByte(), 0xFE.toByte()))) {
            return bytes.copyOfRange(2, bytes.size).toString(Charsets.UTF_16LE)
        }
        if (bytes.startsWith(byteArrayOf(0xFE.toByte(), 0xFF.toByte()))) {
            return bytes.copyOfRange(2, bytes.size).toString(Charsets.UTF_16BE)
        }
        if (looksUtf16(bytes)) {
            val littleEndian = bytes.indices.step(2).count { bytes[it] == 0.toByte() } <
                (1 until bytes.size step 2).count { bytes[it] == 0.toByte() }
            return bytes.toString(if (littleEndian) Charsets.UTF_16LE else Charsets.UTF_16BE)
        }
        val utf8 = runCatching {
            Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes)).toString()
        }.getOrNull()
        if (utf8 != null) return utf8.removePrefix("\uFEFF")
        return windows1256.newDecoder()
            .onMalformedInput(CodingErrorAction.REPLACE)
            .onUnmappableCharacter(CodingErrorAction.REPLACE)
            .decode(ByteBuffer.wrap(bytes)).toString()
    }

    private fun looksUtf16(bytes: ByteArray): Boolean {
        if (bytes.size < 4) return false
        val evenNulls = bytes.indices.step(2).count { bytes[it] == 0.toByte() }
        val oddNulls = (1 until bytes.size step 2).count { bytes[it] == 0.toByte() }
        val pairs = bytes.size / 2
        return evenNulls * 3 > pairs || oddNulls * 3 > pairs
    }

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean =
        size >= prefix.size && prefix.indices.all { this[it] == prefix[it] }
}

private val SRT_TIMESTAMP = Regex("(?m)^(\\d{1,2}:\\d{2}:\\d{2}[,.]\\d{1,3})\\s*-->\\s*(\\d{1,2}:\\d{2}:\\d{2}[,.]\\d{1,3})(?:\\s+.*)?$")
private val VTT_TIMESTAMP = Regex("(?m)^(?:(\\d{1,2}:)?\\d{2}:\\d{2}\\.\\d{1,3})\\s*-->\\s*(?:(?:\\d{1,2}:)?\\d{2}:\\d{2}\\.\\d{1,3})(?:\\s+.*)?$")
private val ASS_DIALOGUE = Regex("(?im)^Dialogue:\\s*(.+)$")

fun parseSrt(text: String, trackId: String = "external"): List<Cue> = parseTimedBlocks(text, SRT_TIMESTAMP, trackId, ::parseSrtTime)

fun parseWebVtt(text: String, trackId: String = "external"): List<Cue> {
    val body = text.removePrefix("\uFEFF").lineSequence().dropWhile { it.trim().startsWith("WEBVTT", true) || it.isBlank() }.joinToString("\n")
    val filtered = body.lineSequence().joinToString("\n")
    return parseTimedBlocks(filtered, VTT_TIMESTAMP, trackId, ::parseVttTime)
}

fun parseAss(text: String, trackId: String = "external"): List<Cue> {
    val result = mutableListOf<Cue>()
    ASS_DIALOGUE.findAll(text).forEach { match ->
        val fields = match.groupValues[1].split(',', limit = 10)
        if (fields.size < 10) return@forEach
        val start = parseAssTime(fields[1]) ?: return@forEach
        val end = parseAssTime(fields[2]) ?: return@forEach
        if (end <= start) return@forEach
        val dialogue = fields[9]
            .replace(Regex("\\{[^}]*}"), "")
            .replace(Regex("\\\\[Nn]"), "\n")
            .replace("\\h", " ")
        val clean = SubtitleNormalizer.cleanText(dialogue)
        if (clean.isNotBlank()) result += cue(result.size, start, end, clean, trackId)
    }
    return result
}

private fun parseTimedBlocks(
    text: String,
    timestampRegex: Regex,
    trackId: String,
    parseTime: (String) -> Long?,
): List<Cue> {
    val lines = text.replace("\r\n", "\n").replace('\r', '\n').lines()
    val result = mutableListOf<Cue>()
    var index = 0
    while (index < lines.size) {
        val line = lines[index].trim()
        if (!timestampRegex.matches(line)) {
            index++
            continue
        }
        if (timestampRegex.matchEntire(line) == null) {
            index++
            continue
        }
        val arrow = line.indexOf("-->")
        val times = line.substring(0, arrow).trim() to line.substring(arrow + 3).trim().substringBefore(' ')
        val start = parseTime(times.first)
        val end = parseTime(times.second)
        index++
        val payload = mutableListOf<String>()
        while (index < lines.size && lines[index].isNotBlank() && !timestampRegex.matches(lines[index].trim())) {
            val content = lines[index].trim()
            if (!content.startsWith("NOTE ", true) && !content.startsWith("STYLE", true) && !content.startsWith("REGION", true)) {
                payload += content
            }
            index++
        }
        if (start != null && end != null && end > start) {
            val content = SubtitleNormalizer.cleanText(payload.joinToString(" "))
            if (content.isNotBlank()) result += cue(result.size, start, end, content, trackId)
        }
        while (index < lines.size && lines[index].isBlank()) index++
    }
    return result
}

private fun cue(id: Int, start: Long, end: Long, text: String, trackId: String): Cue =
    Cue(id = id.toLong(), startMs = start, endMs = end, text = text, tokens = SubtitleNormalizer.tokens(text), trackId = trackId)

private fun parseSrtTime(value: String): Long? = parseClock(value.replace(',', '.'))
private fun parseVttTime(value: String): Long? = parseClock(value)

private fun parseAssTime(value: String): Long? {
    val parts = value.trim().split(':')
    if (parts.size != 3) return null
    val secondsParts = parts[2].split('.', limit = 2)
    if (secondsParts.size != 2) return null
    val hours = parts[0].toLongOrNull() ?: return null
    val minutes = parts[1].toLongOrNull() ?: return null
    val seconds = secondsParts[0].toLongOrNull() ?: return null
    val centiseconds = secondsParts[1].padEnd(2, '0').take(2).toLongOrNull() ?: return null
    if (minutes > 59 || seconds > 59) return null
    return ((hours * 3_600 + minutes * 60 + seconds) * 1_000) + centiseconds * 10
}

private fun parseClock(value: String): Long? {
    val parts = value.trim().split(':')
    if (parts.size !in 2..3) return null
    val secondsPart = parts.last().split('.', limit = 2)
    val wholeSeconds = secondsPart[0].toLongOrNull() ?: return null
    val milliseconds = secondsPart.getOrNull(1)?.padEnd(3, '0')?.take(3)?.toLongOrNull() ?: 0L
    val minutes = parts[parts.size - 2].toLongOrNull() ?: return null
    val hours = if (parts.size == 3) parts[0].toLongOrNull() ?: return null else 0L
    if (minutes > 59 || wholeSeconds > 59) return null
    return (hours * 3_600 + minutes * 60 + wholeSeconds) * 1_000 + milliseconds
}

private fun ByteArray.toString(charset: Charset): String = charset.decode(ByteBuffer.wrap(this)).toString()
