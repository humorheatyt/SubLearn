package com.sublearn.subtitles

import com.sublearn.domain.Cue
import java.nio.charset.Charset
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SubtitleParsingTest {
    private val parser = DefaultSubtitleParser()

    @Test fun parsesSrtAndNormalizesMarkupAndLineBreaks() = runBlocking {
        val parsed = parser.parse(
            """1
00:00:01,200 --> 00:00:03,400
<i>Hello</i>,
world &amp; friends.
""".toByteArray(), "english", "movie.srt",
        )
        assertEquals(1, parsed.size)
        assertEquals(1_200L, parsed.single().startMs)
        assertEquals(3_400L, parsed.single().endMs)
        assertEquals("Hello, world & friends.", parsed.single().text)
        assertEquals("Hello", parsed.single().tokens.first().text)
    }

    @Test fun parsesWebVttWithCueIdSettingsAndShortTimestamp() = runBlocking {
        val parsed = parser.parse(
            """WEBVTT

intro
00:01.500 --> 00:03.250 align:start
<v Alice>Hello there.</v>

""".toByteArray(), "track-vtt", "sub.vtt",
        )
        assertEquals(1, parsed.size)
        assertEquals(1_500L, parsed.single().startMs)
        assertEquals(3_250L, parsed.single().endMs)
        assertEquals("Hello there.", parsed.single().text)
        assertEquals("track-vtt", parsed.single().trackId)
    }

    @Test fun parsesTextAssAndStripsOverrideTags() = runBlocking {
        val parsed = parser.parse(
            """[Script Info]
; Comment
[Events]
Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
Dialogue: 0,0:00:02.50,0:00:04.00,Default,,0,0,0,,{\i1}One\Ntwo{\i0}.
""".toByteArray(), "ass-track", "sub.ass",
        )
        assertEquals(1, parsed.size)
        assertEquals(2_500L, parsed.single().startMs)
        assertEquals("One two.", parsed.single().text)
    }

    @Test fun detectsUtf16AndWindows1256() {
        assertEquals("hello", SubtitleCharset.decode(byteArrayOf(0xFF.toByte(), 0xFE.toByte()) + "hello".toByteArray(Charsets.UTF_16LE)))
        val windows = Charset.forName("windows-1256")
        assertEquals("سلام", SubtitleCharset.decode("سلام".toByteArray(windows)))
    }

    @Test fun rejectsUnsupportedImageSubtitleAndEmptyInput() = runBlocking {
        assertTrue(runCatching { parser.parse(byteArrayOf(), "x", "x.pgs") }.isFailure)
        assertTrue(runCatching { parser.parse("image subtitle".toByteArray(), "x", "x.pgs") }.isFailure)
    }

    @Test fun normalizationMergesFragmentsAndKeepsPunctuationReadable() {
        val cues = listOf(
            Cue(9, 0, 900, "We are"),
            Cue(10, 1_000, 1_800, "learning."),
            Cue(11, 2_600, 3_000, "Really ?"),
        )
        val result = SubtitleNormalizer.normalize(cues, maxCharacters = 96)
        assertEquals(2, result.size)
        assertEquals("We are learning.", result.first().text)
        assertEquals("Really ?", result.last().text)
        assertEquals(0L, result.first().id)
    }

    @Test fun batchToolsFlattenAndSplitAtReadableBoundariesWithoutLosingDuration() {
        val source = listOf(Cue(1, 0, 9_000, "A sentence\nwith several words, and a useful ending."))
        val flattened = SubtitleNormalizer.removeLineBreaks(source).single()
        assertFalse(flattened.text.contains('\n'))
        val split = SubtitleNormalizer.splitByMaxCharacters(source, 20)
        assertTrue(split.size > 2)
        assertTrue(split.all { it.text.length <= 20 })
        assertEquals(0L, split.first().startMs)
        assertEquals(9_000L, split.last().endMs)
        assertEquals(split.size, split.map { it.id }.distinct().size)
    }

    @Test fun timelineLookupUsesHalfOpenIntervalsAndHandlesOverlap() {
        val index = CueTimelineIndex(
            listOf(
                Cue(1, 0, 5_000, "long"),
                Cue(2, 1_000, 2_000, "short"),
                Cue(3, 6_000, 8_000, "later"),
            ),
        )
        assertEquals("short", index.cueAt(1_500)?.text)
        assertEquals("long", index.cueAt(2_000)?.text)
        assertNull(index.cueAt(5_000))
        assertEquals("later", index.cueAt(6_000)?.text)
        assertNull(index.cueAt(-1))
    }

    @Test fun tokenOffsetsAreExactForMixedPersianAndEnglishText() {
        val source = "Learn کتاب today"
        val tokens = SubtitleNormalizer.tokens(source)
        assertEquals(listOf("Learn", "کتاب", "today"), tokens.map { it.text })
        tokens.forEach { token -> assertEquals(token.text, source.substring(token.startOffset, token.endOffset)) }
        assertNotNull(CueTimelineIndex(emptyList()).cues)
    }
}
