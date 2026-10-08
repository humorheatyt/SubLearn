package com.sublearn.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.runBlocking

class DomainLogicTest {
    @Test fun repeatPauseIncludesDurationMultiplier() {
        assertEquals(1_200L, RepeatPlanner.delayAfterBlock(2_000, 500, 0.35f))
        assertEquals(250L, RepeatPlanner.delayAfterBlock(0, 250, 0f))
    }

    @Test fun temporaryHoldInvertsStopAtEnd() {
        assertTrue(RepeatPlanner.shouldPauseAtBlockEnd(true, false))
        assertFalse(RepeatPlanner.shouldPauseAtBlockEnd(true, true))
        assertTrue(RepeatPlanner.shouldPauseAtBlockEnd(false, true))
    }

    @Test fun settingsRoundTripAndClampRanges() {
        val source = AppSettings(repeatCount = 999, learningSubtitleSizeSp = 100f, aiContextBlockCount = -2)
        val result = SettingsCodec.decode(SettingsCodec.encode(source))
        assertEquals(20, result.repeatCount)
        assertEquals(72f, result.learningSubtitleSizeSp)
        assertEquals(0, result.aiContextBlockCount)
        assertEquals("en", result.learningLanguageTag)
        assertEquals("fa", result.nativeLanguageTag)
    }

    @Test fun speedIndexIsClampedAfterInvalidAndDuplicateStepsAreRemoved() {
        val settings = AppSettings(
            speedSteps = listOf(0.5f, 0f, 1.25f, 0.5f),
            speedStepIndex = 50,
        ).normalized()
        assertEquals(listOf(0.5f, 1.25f), settings.speedSteps)
        assertEquals(1, settings.speedStepIndex)
        val fallback = AppSettings(speedSteps = listOf(Float.NaN, -1f)).normalized()
        assertEquals(listOf(1f), fallback.speedSteps)
        assertEquals(0, fallback.speedStepIndex)
    }

    @Test fun settingsCanMigrateAZeroVersionAndRejectFutureVersion() {
        assertEquals(1, SettingsCodec.decode("""{"schemaVersion":0,"repeatCount":4}""").schemaVersion)
        val error = runCatching { SettingsCodec.decode("""{"schemaVersion":99}""") }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
    }

    @Test fun contextUsesSelectedTextAndConfiguredPreviousCount() {
        val settings = AppSettings(aiContextBlockCount = 1, aiIncludeFilmTitle = true, aiIncludeTimestamps = false)
        val request = AiRequest(
            selectedText = "  hello  ", currentBlock = "whole line", filmTitle = "A Film",
            includeTimestamps = true,
            previousBlocks = listOf(Cue(1, 1_000, 2_000, "oldest"), Cue(2, 3_000, 4_000, "latest")),
            prompt = settings.aiPrompt,
        )
        val prompt = AiContextBuilder.build(settings, request)
        assertTrue(prompt.contains("Selected subtitle: hello"))
        assertTrue(prompt.contains("Film/video: A Film"))
        assertTrue(prompt.contains("latest"))
        assertFalse(prompt.contains("oldest"))
        assertFalse(prompt.contains("00:00:03"))
    }

    @Test fun blankSelectionFallsBackToCurrentBlock() {
        val settings = AppSettings()
        val prompt = AiContextBuilder.build(settings, AiRequest(" ", "full block", emptyList(), null, true, settings.aiPrompt))
        assertTrue(prompt.contains("Selected subtitle: full block"))
    }

    @Test fun unknownWordFallbackDoesNotInventLevelsAndExcludesKnownWords() = runBlocking {
        val words = UnknownWordLevelProvider().wordsAboveLevel(
            listOf(Cue(1, 0, 1_000, "Learning is learning with words.")),
            manualLevel = "A1",
            knownWords = setOf("learning"),
        )
        assertEquals(listOf("is", "with", "words"), words)
    }

    @Test fun nextRepeatStartAddsGapAfterBlockEnd() {
        assertEquals(3_250L, RepeatPlanner.nextStartMs(1_000, 3_000, 250))
        assertEquals(3_000L, RepeatPlanner.nextStartMs(1_000, 3_000, 0))
    }

    @Test fun settingsRoundTripPreservesSurfaceFontsAndGestureBindings() {
        val source = AppSettings(
            gestureBindings = defaultGestureBindings() + ("double-tap-video" to GestureAction.SEEK_FORWARD),
            surfaceFonts = defaultSurfaceFonts() + ("subtitle.learning" to SurfaceFontSettings(sizeSp = 33f, weight = 800)),
        )
        val decoded = SettingsCodec.decode(SettingsCodec.encode(source))
        assertEquals(GestureAction.SEEK_FORWARD, decoded.gestureBindings["double-tap-video"])
        assertEquals(GestureAction.VOLUME, decoded.gestureBindings["swipe-video-right-vertical"])
        assertEquals(33f, decoded.surfaceFonts["subtitle.learning"]?.sizeSp)
        assertEquals(800, decoded.surfaceFonts["subtitle.learning"]?.weight)
    }

    @Test fun contextAddsFormattedTimestampsWhenConfigured() {
        val settings = AppSettings(aiContextBlockCount = 2, aiIncludeFilmTitle = false, aiIncludeTimestamps = true)
        val prompt = AiContextBuilder.build(
            settings,
            AiRequest("word", "line", listOf(Cue(1, 3_723_004, 3_724_000, "far")), null, true, settings.aiPrompt),
        )
        assertTrue(prompt.contains("[01:02:03] far"))
        assertFalse(prompt.contains("Film/video:"))
    }
}
