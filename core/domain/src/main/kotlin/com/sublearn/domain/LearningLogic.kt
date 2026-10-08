package com.sublearn.domain

import kotlin.math.roundToLong
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Repeats are time-based so player implementations can test the loop without a UI or codec. */
object RepeatPlanner {
    fun delayAfterBlock(blockDurationMs: Long, basePauseMs: Long, durationMultiplier: Float): Long {
        require(blockDurationMs >= 0)
        require(basePauseMs >= 0)
        require(durationMultiplier >= 0f)
        return (basePauseMs + blockDurationMs * durationMultiplier).roundToLong()
    }

    fun nextStartMs(startMs: Long, endMs: Long, gapMs: Long): Long {
        require(startMs >= 0 && endMs >= startMs && gapMs >= 0)
        return endMs + gapMs
    }

    fun shouldPauseAtBlockEnd(stopAtEnd: Boolean, temporarilyInverted: Boolean): Boolean =
        stopAtEnd xor temporarilyInverted
}

object SettingsCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    fun encode(settings: AppSettings): String = json.encodeToString(settings.normalized())

    fun decode(raw: String): AppSettings {
        val decoded = json.decodeFromString<AppSettings>(raw)
        return when (decoded.schemaVersion) {
            0 -> decoded.copy(schemaVersion = AppSettings.CURRENT_SCHEMA_VERSION).normalized()
            1 -> decoded.normalized()
            else -> throw IllegalArgumentException("Settings schema ${decoded.schemaVersion} is newer than this app")
        }
    }
}

object AiContextBuilder {
    fun build(settings: AppSettings, request: AiRequest): String {
        val selected = request.selectedText.trim().ifBlank { request.currentBlock.trim() }
        val prior = request.previousBlocks.takeLast(settings.aiContextBlockCount)
        return buildString {
            appendLine(settings.aiPrompt.ifBlank { AppSettings.DEFAULT_AI_PROMPT })
            appendLine()
            if (settings.aiIncludeFilmTitle && !request.filmTitle.isNullOrBlank()) {
                appendLine("Film/video: ${request.filmTitle.trim()}")
            }
            appendLine("Selected subtitle: $selected")
            if (prior.isNotEmpty()) {
                appendLine("Previous subtitle context:")
                prior.forEach { cue ->
                    val time = if (settings.aiIncludeTimestamps) "[${formatTimestamp(cue.startMs)}] " else ""
                    appendLine("$time${cue.text.trim()}")
                }
            }
            appendLine()
            append("Answer in the user's language. Do not invent plot facts not supported by the context.")
        }
    }

    private fun formatTimestamp(ms: Long): String {
        val seconds = (ms.coerceAtLeast(0) / 1_000)
        return "%02d:%02d:%02d".format(seconds / 3_600, (seconds / 60) % 60, seconds % 60)
    }
}

/** No licensed level list is bundled. Until one is audited, show only unmarked vocabulary. */
class UnknownWordLevelProvider : WordLevelProvider {
    override suspend fun wordsAboveLevel(
        cues: List<Cue>,
        manualLevel: String,
        knownWords: Set<String>,
    ): List<String> {
        val known = knownWords.mapTo(mutableSetOf()) { it.lowercase() }
        val seen = mutableSetOf<String>()
        return cues.asSequence()
            .flatMap { ENGLISH_WORD.findAll(it.text).asSequence().map { match -> match.value } }
            .filter { it.length > 1 }
            .filter { seen.add(it.lowercase()) && it.lowercase() !in known }
            .take(8)
            .toList()
    }

    private companion object {
        val ENGLISH_WORD = Regex("[\\p{L}\\p{M}]+(?:['’][\\p{L}\\p{M}]+)?")
    }
}
