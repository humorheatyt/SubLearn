package com.sublearn.subtitles

import com.sublearn.domain.Cue
import com.sublearn.domain.SubtitleToken

object SubtitleNormalizer {
    private val htmlTag = Regex("<[^>]{0,256}>")
    private val assTag = Regex("\\{[^}]{0,512}}")
    private val spaces = Regex("[\\t\\x0B\\f ]+")
    private val wordPattern = Regex("[\\p{L}\\p{M}\\p{N}]+(?:['’][\\p{L}\\p{M}\\p{N}]+)*")
    private val sentenceEnd = Regex("[.!?…؟۔][\\\"'’”»)}\\]]*$")
    private val closingPunctuation = setOf('.', ',', '!', '?', ';', ':', '…', '؟', '۔', ')', ']', '}', '»', '”', '’')
    private val openingPunctuation = setOf('(', '[', '{', '«', '“', '‘')

    fun cleanText(raw: String): String {
        val flattened = raw
            .replace("\\N", " ").replace("\\n", " ").replace("\\h", " ")
            .replace("\r\n", " ").replace('\r', ' ').replace('\n', ' ')
        val noTags = assTag.replace(htmlTag.replace(flattened, ""), " ")
        return spaces.replace(decodeEntities(noTags), " ")
            .replace(Regex(" +([,.;:!?؟۔…])"), "$1")
            .trim()
    }

    fun tokens(text: String): List<SubtitleToken> = wordPattern.findAll(text)
        .map { match -> SubtitleToken(match.value, match.range.first, match.range.last + 1) }
        .toList()

    fun normalize(cues: List<Cue>, maxCharacters: Int = 120, mergeFragments: Boolean = true): List<Cue> {
        require(maxCharacters >= 1)
        val clean = cues.asSequence()
            .filter { it.endMs > it.startMs }
            .map { it.copy(text = cleanText(it.text)) }
            .filter { it.text.isNotBlank() }
            .sortedWith(compareBy<Cue> { it.startMs }.thenBy { it.endMs })
            .map { it.copy(tokens = tokens(it.text)) }
            .toList()
        val merged = if (mergeFragments) mergeShortFragments(clean, maxCharacters) else clean
        return splitLongCues(merged, maxCharacters)
            .mapIndexed { index, cue -> cue.copy(id = index.toLong(), tokens = tokens(cue.text)) }
    }

    /** Batch operation used by the subtitle tools. */
    fun removeLineBreaks(cues: List<Cue>): List<Cue> = cues.map { cue ->
        val text = cleanText(cue.text)
        cue.copy(text = text, tokens = tokens(text))
    }

    /** Splits a long cue at word boundaries, preferring punctuation near the configured limit. */
    fun splitByMaxCharacters(cues: List<Cue>, maxCharacters: Int): List<Cue> {
        require(maxCharacters >= 1)
        return splitLongCues(removeLineBreaks(cues), maxCharacters)
            .mapIndexed { index, cue -> cue.copy(id = index.toLong(), tokens = tokens(cue.text)) }
    }

    private fun mergeShortFragments(cues: List<Cue>, maxCharacters: Int): List<Cue> {
        if (cues.size < 2) return cues
        val output = mutableListOf<Cue>()
        var current = cues.first()
        for (next in cues.drop(1)) {
            val joined = join(current.text, next.text)
            val gap = next.startMs - current.endMs
            val duration = maxOf(current.endMs, next.endMs) - current.startMs
            val canMerge = current.trackId == next.trackId &&
                gap >= -250 && gap <= 1_000 &&
                duration <= 7_000 &&
                joined.length <= maxCharacters &&
                !sentenceEnd.containsMatchIn(current.text) &&
                !startsNewParagraph(next.text)
            if (canMerge) {
                current = current.copy(
                    endMs = maxOf(current.endMs, next.endMs),
                    text = joined,
                    tokens = tokens(joined),
                )
            } else {
                output += current
                current = next
            }
        }
        output += current
        return output
    }

    private fun splitLongCues(cues: List<Cue>, maxCharacters: Int): List<Cue> = buildList {
        cues.forEach { cue ->
            val text = cleanText(cue.text)
            if (text.length <= maxCharacters) {
                add(cue.copy(text = text, tokens = tokens(text)))
                return@forEach
            }
            val chunks = splitText(text, maxCharacters)
            if (chunks.size == 1) {
                add(cue.copy(text = chunks.single(), tokens = tokens(chunks.single())))
                return@forEach
            }
            val totalWeight = chunks.sumOf { it.codePointCount(0, it.length) }.coerceAtLeast(1)
            val duration = (cue.endMs - cue.startMs).coerceAtLeast(0)
            var consumed = 0
            chunks.forEachIndexed { index, chunk ->
                val weight = chunk.codePointCount(0, chunk.length)
                val start = cue.startMs + duration * consumed / totalWeight
                consumed += weight
                val end = if (index == chunks.lastIndex) cue.endMs else cue.startMs + duration * consumed / totalWeight
                add(cue.copy(startMs = start.coerceAtMost(end), endMs = end, text = chunk, tokens = tokens(chunk)))
            }
        }
    }

    private fun splitText(text: String, maxCharacters: Int): List<String> {
        val result = mutableListOf<String>()
        var remaining = text.trim()
        while (remaining.length > maxCharacters) {
            var cut = safeBoundary(remaining, maxCharacters)
            val minPreferred = (maxCharacters * 0.55f).toInt().coerceAtLeast(1)
            val punctuationCut = (minPreferred until cut.coerceAtMost(remaining.length))
                .lastOrNull { index -> remaining[index - 1] in setOf('.', ',', ';', ':', '!', '?', '…', '؟', '۔') }
            if (punctuationCut != null) cut = punctuationCut
            val space = remaining.lastIndexOf(' ', cut - 1)
            if (space >= minPreferred) cut = space
            if (cut <= 0) cut = safeBoundary(remaining, maxCharacters)
            val piece = remaining.substring(0, cut).trim()
            if (piece.isNotEmpty()) result += piece
            remaining = remaining.substring(cut).trimStart()
        }
        if (remaining.isNotEmpty()) result += remaining
        return result
    }

    private fun safeBoundary(text: String, maxCharacters: Int): Int {
        var boundary = maxCharacters.coerceAtMost(text.length)
        if (boundary > 0 && boundary < text.length && Character.isHighSurrogate(text[boundary - 1]) && Character.isLowSurrogate(text[boundary])) {
            boundary--
        }
        return boundary.coerceAtLeast(1)
    }

    private fun join(first: String, second: String): String {
        val left = first.lastOrNull()
        val right = second.firstOrNull()
        val separator = when {
            left == null || right == null -> ""
            right in closingPunctuation -> ""
            left in openingPunctuation -> ""
            else -> " "
        }
        return first.trimEnd() + separator + second.trimStart()
    }

    private fun startsNewParagraph(text: String): Boolean = text.startsWith("—") || text.startsWith("–")

    private fun decodeEntities(text: String): String = text
        .replace("&amp;", "&", ignoreCase = true)
        .replace("&lt;", "<", ignoreCase = true)
        .replace("&gt;", ">", ignoreCase = true)
        .replace("&quot;", "\"", ignoreCase = true)
        .replace("&#39;", "'", ignoreCase = true)
        .replace("&apos;", "'", ignoreCase = true)
}
