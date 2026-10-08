package com.sublearn.domain

import kotlinx.serialization.Serializable

/** Canonical subtitle cue used by parsers, repositories and the Compose overlay. */
@Serializable
data class Cue(
    val id: Long,
    val startMs: Long,
    val endMs: Long,
    val text: String,
    val tokens: List<SubtitleToken> = emptyList(),
    val trackId: String = "external",
) {
    init {
        require(startMs >= 0) { "Cue start must be non-negative" }
        require(endMs >= startMs) { "Cue end must not precede start" }
    }
}

@Serializable
data class SubtitleToken(val text: String, val startOffset: Int, val endOffset: Int) {
    init {
        require(startOffset >= 0 && endOffset >= startOffset)
    }
}

@Serializable
data class SubtitleTrack(
    val id: String,
    val label: String,
    val languageTag: String = "und",
    val uri: String? = null,
    val isEmbedded: Boolean = false,
)

@Serializable
data class MediaRequest(
    val uri: String,
    val title: String,
    val mimeType: String? = null,
    val resumePositionMs: Long = 0L,
)

@Serializable
data class RecentMedia(
    val uri: String,
    val title: String,
    val mimeType: String?,
    val lastPositionMs: Long,
    val durationMs: Long,
    val lastOpenedEpochMs: Long,
    val lastSubtitleUri: String? = null,
    val lastSubtitleLayer: SubtitleLayer? = null,
)

@Serializable
enum class SubtitleLayer { LEARNING, NATIVE }

@Serializable
enum class DecoderMode { AUTO, HARDWARE, SOFTWARE, HARDWARE_PLUS }

@Serializable
enum class RepeatState { OFF, ONCE, AUTOMATIC }

@Serializable
data class PlayerSnapshot(
    val request: MediaRequest? = null,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val bufferedPositionMs: Long = 0,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val playbackSpeed: Float = 1f,
    val audioTracks: List<SubtitleTrack> = emptyList(),
    val embeddedSubtitleTracks: List<SubtitleTrack> = emptyList(),
    val selectedAudioTrackId: String? = null,
    val selectedEmbeddedSubtitleTrackId: String? = null,
    val embeddedSubtitleCue: Cue? = null,
    val decoderMode: DecoderMode = DecoderMode.AUTO,
    val aspectRatioMode: Int = 0,
    val errorMessage: String? = null,
) {
    val progress: Float
        get() = if (durationMs <= 0) 0f else (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
}

@Serializable
data class SavedWord(
    val id: Long = 0,
    val text: String,
    val translation: String,
    val languageTag: String = "en",
    val nativeLanguageTag: String = "fa",
    val context: String = "",
    val mediaTitle: String = "",
    val startMs: Long = 0,
    val createdEpochMs: Long = 0,
    val isKnown: Boolean = false,
)

@Serializable
enum class ThemeMode { SYSTEM, LIGHT, DARK, AMOLED }

@Serializable
enum class LearningMode { ENTERTAINMENT, LEARNING }

@Serializable
enum class DockMode { QUICK_COLUMN, BOTTOM_BAR, FLOATING, HIDDEN }

@Serializable
enum class GestureAction { NONE, TOGGLE_CONTROLS, PAUSE_PLAY, SEEK_BACK, SEEK_FORWARD, VOLUME, BRIGHTNESS, SPEED_CYCLE }

@Serializable
enum class TextDirection { AUTO, LTR, RTL }

@Serializable
enum class WordMarkStyle { UNDERLINE, DOTTED_UNDERLINE, OUTLINE, BACKGROUND, BOLD, COLOR }

@Serializable
data class SurfaceFontSettings(
    val family: String = "system",
    val sizeSp: Float = 20f,
    val colorArgb: Long? = null,
    val weight: Int = 500,
    val direction: TextDirection = TextDirection.AUTO,
)

@Serializable
data class WordStyleSettings(
    val style: WordMarkStyle = WordMarkStyle.BACKGROUND,
    val colorArgb: Long = 0xFFFFC857,
    val enabled: Boolean = true,
)

/** One typed, versioned preferences schema. The API-key material lives outside this model. */
@Serializable
data class AppSettings(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val uiLanguageTag: String = "en",
    val learningLanguageTag: String = "en",
    val nativeLanguageTag: String = "fa",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val learningMode: LearningMode = LearningMode.ENTERTAINMENT,
    val manualLevel: String = "A2",
    val repeatCount: Int = 3,
    val repeatPauseMs: Long = 450,
    val repeatPauseMultiplier: Float = 0.35f,
    val stopAtBlockEnd: Boolean = false,
    val autoHideControlsMs: Long = 3_000,
    val doubleTapAction: GestureAction = GestureAction.PAUSE_PLAY,
    val gestureBindings: Map<String, GestureAction> = defaultGestureBindings(),
    val speedSteps: List<Float> = listOf(0.75f, 1f, 1.25f, 1.5f, 2f),
    val speedStepIndex: Int = 1,
    val playbackAspectRatioMode: Int = 0,
    val rotationLocked: Boolean = false,
    val subtitlesLearningVisible: Boolean = true,
    val subtitlesNativeVisible: Boolean = true,
    val learningSubtitleDelayMs: Long = 0,
    val nativeSubtitleDelayMs: Long = 0,
    val learningSubtitleSizeSp: Float = 24f,
    val nativeSubtitleSizeSp: Float = 20f,
    val learningSubtitleAlpha: Float = 1f,
    val nativeSubtitleAlpha: Float = 0.95f,
    val learningSubtitlePosition: Float = 0.76f,
    val nativeSubtitlePosition: Float = 0.86f,
    val learningButtonPosition: Float = 0.35f,
    val nativeButtonPosition: Float = 0.45f,
    val learningButtonSizeDp: Float = 44f,
    val nativeButtonSizeDp: Float = 44f,
    val learningButtonAlpha: Float = 0.92f,
    val nativeButtonAlpha: Float = 0.92f,
    val quickActionDockMode: DockMode = DockMode.QUICK_COLUMN,
    val subtitleMaxCharacters: Int = 96,
    val flattenLineBreaks: Boolean = true,
    val noSpoilerList: Boolean = false,
    val subtitleSearchQuery: String = "",
    val manualLevelPopupLimit: Int = 3,
    val learningPopupOpacity: Float = 0.82f,
    val myWordsStyle: WordStyleSettings = WordStyleSettings(style = WordMarkStyle.UNDERLINE, colorArgb = 0xFF8C79E8, enabled = true),
    val knownWordStyle: WordStyleSettings = WordStyleSettings(),
    val partOfSpeechStyle: WordStyleSettings = WordStyleSettings(enabled = false),
    val phraseStyle: WordStyleSettings = WordStyleSettings(enabled = false),
    val surfaceFonts: Map<String, SurfaceFontSettings> = defaultSurfaceFonts(),
    val aiProviderId: String = "gemini",
    val aiModel: String = "gemini-2.0-flash",
    val aiPrompt: String = DEFAULT_AI_PROMPT,
    val aiContextBlockCount: Int = 10,
    val aiIncludeFilmTitle: Boolean = true,
    val aiIncludeTimestamps: Boolean = true,
    val defaultLookupTarget: String = "mlkit",
    val dictionaryFeatureEnabled: Boolean = false,
    val youtubeFeatureEnabled: Boolean = false,
    val pdfLearningFeatureEnabled: Boolean = false,
    val automaticLevelFeatureEnabled: Boolean = false,
    val quizFeatureEnabled: Boolean = false,
    val updateCheckerFeatureEnabled: Boolean = false,
    val aiResegmentationFeatureEnabled: Boolean = false,
    val aiQuoteMarkingFeatureEnabled: Boolean = false,
    val speechToTextFeatureEnabled: Boolean = false,
    val extraLanguageFeatureEnabled: Boolean = false,
) {
    fun normalized(): AppSettings {
        val validSpeedSteps = speedSteps.filter { it in 0.25f..3f }.distinct().ifEmpty { listOf(1f) }
        return copy(
        schemaVersion = CURRENT_SCHEMA_VERSION,
        repeatCount = repeatCount.coerceIn(1, 20),
        repeatPauseMs = repeatPauseMs.coerceIn(0, 10_000),
        repeatPauseMultiplier = repeatPauseMultiplier.coerceIn(0f, 3f),
        autoHideControlsMs = autoHideControlsMs.coerceIn(1_000, 15_000),
        playbackAspectRatioMode = playbackAspectRatioMode.coerceIn(0, 2),
        learningSubtitleSizeSp = learningSubtitleSizeSp.coerceIn(12f, 72f),
        nativeSubtitleSizeSp = nativeSubtitleSizeSp.coerceIn(12f, 72f),
        learningSubtitleAlpha = learningSubtitleAlpha.coerceIn(0.15f, 1f),
        nativeSubtitleAlpha = nativeSubtitleAlpha.coerceIn(0.15f, 1f),
        learningSubtitlePosition = learningSubtitlePosition.coerceIn(0.08f, 0.96f),
        nativeSubtitlePosition = nativeSubtitlePosition.coerceIn(0.08f, 0.96f),
        learningButtonPosition = learningButtonPosition.coerceIn(0.08f, 0.92f),
        nativeButtonPosition = nativeButtonPosition.coerceIn(0.08f, 0.92f),
        learningButtonSizeDp = learningButtonSizeDp.coerceIn(36f, 80f),
        nativeButtonSizeDp = nativeButtonSizeDp.coerceIn(36f, 80f),
        learningButtonAlpha = learningButtonAlpha.coerceIn(0.3f, 1f),
        nativeButtonAlpha = nativeButtonAlpha.coerceIn(0.3f, 1f),
        subtitleMaxCharacters = subtitleMaxCharacters.coerceIn(20, 240),
        manualLevelPopupLimit = manualLevelPopupLimit.coerceIn(1, 8),
        learningPopupOpacity = learningPopupOpacity.coerceIn(0.2f, 1f),
        aiContextBlockCount = aiContextBlockCount.coerceIn(0, 30),
        speedSteps = validSpeedSteps,
        speedStepIndex = speedStepIndex.coerceIn(0, (validSpeedSteps.size - 1).coerceAtLeast(0)),
        surfaceFonts = defaultSurfaceFonts() + surfaceFonts.mapValues { (_, value) ->
            value.copy(sizeSp = value.sizeSp.coerceIn(10f, 72f), weight = value.weight.coerceIn(100, 900))
        },
        )
    }

    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
        const val DEFAULT_AI_PROMPT = "Explain the tone of the selected subtitle, why it is used in this context, how it differs from close synonyms, and where else it is used. Be concise, accurate, and mention uncertainty."
    }
}

fun defaultGestureBindings(): Map<String, GestureAction> = mapOf(
    "tap-video" to GestureAction.TOGGLE_CONTROLS,
    "double-tap-video" to GestureAction.PAUSE_PLAY,
    "swipe-video-horizontal" to GestureAction.SEEK_FORWARD,
    "swipe-video-left-vertical" to GestureAction.BRIGHTNESS,
    "swipe-video-right-vertical" to GestureAction.VOLUME,
    "two-finger-up" to GestureAction.SPEED_CYCLE,
)

fun defaultSurfaceFonts(): Map<String, SurfaceFontSettings> = mapOf(
    "menu.app" to SurfaceFontSettings(sizeSp = 16f, direction = TextDirection.LTR),
    "subtitle.learning" to SurfaceFontSettings(sizeSp = 24f, direction = TextDirection.LTR),
    "subtitle.native" to SurfaceFontSettings(sizeSp = 20f, direction = TextDirection.RTL),
    "popup.learning" to SurfaceFontSettings(sizeSp = 18f, direction = TextDirection.LTR),
    "popup.native" to SurfaceFontSettings(sizeSp = 18f, direction = TextDirection.RTL),
    "word-card.learning" to SurfaceFontSettings(sizeSp = 22f, direction = TextDirection.LTR),
    "word-card.native" to SurfaceFontSettings(sizeSp = 18f, direction = TextDirection.RTL),
    "ai.answer" to SurfaceFontSettings(sizeSp = 16f, direction = TextDirection.AUTO),
)

const val DEFAULT_AI_PROMPT = AppSettings.DEFAULT_AI_PROMPT
