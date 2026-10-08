package com.sublearn.domain

import kotlinx.coroutines.flow.Flow

interface AppSettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun update(transform: (AppSettings) -> AppSettings)
    suspend fun replace(settings: AppSettings)
    suspend fun exportJson(): String
    suspend fun importJson(json: String)
}

interface RecentMediaRepository {
    fun observeRecent(limit: Int = 30): Flow<List<RecentMedia>>
    suspend fun recordOpened(request: MediaRequest, positionMs: Long = 0, durationMs: Long = 0, subtitleUri: String? = null, subtitleLayer: SubtitleLayer? = null)
    suspend fun saveProgress(uri: String, positionMs: Long, durationMs: Long)
    suspend fun remove(uri: String)
}

interface SavedWordRepository {
    fun observeWords(query: String = ""): Flow<List<SavedWord>>
    suspend fun find(text: String, languageTag: String = "en"): SavedWord?
    suspend fun save(word: SavedWord): Long
    suspend fun update(word: SavedWord)
    suspend fun remove(id: Long)
    suspend fun markKnown(text: String, languageTag: String = "en", known: Boolean)
    suspend fun knownWords(languageTag: String = "en"): Set<String>
}

interface SecretStore {
    suspend fun read(providerId: String): String?
    suspend fun write(providerId: String, secret: String)
    suspend fun delete(providerId: String)
}

interface PlayerController {
    val snapshot: Flow<PlayerSnapshot>
    val repeatActive: Flow<Boolean>
    suspend fun open(request: MediaRequest, queue: List<MediaRequest> = emptyList())
    fun playPause()
    fun pause()
    fun resume()
    fun seekTo(positionMs: Long)
    fun seekBy(deltaMs: Long)
    fun setPlaybackSpeed(speed: Float)
    fun selectAudioTrack(trackId: String?)
    fun selectEmbeddedSubtitleTrack(trackId: String?)
    fun supportedDecoderModes(): Set<DecoderMode>
    fun setDecoderMode(mode: DecoderMode): Boolean
    fun repeatBlock(startMs: Long, endMs: Long, count: Int = 1, pauseAfterMs: Long = 0)
    fun stopRepeat()
    fun setStopAtBlockEnd(enabled: Boolean, endMs: Long?, temporarilyInverted: Boolean = false)
    fun setAspectRatioMode(mode: Int)
    fun skipToNext()
    fun skipToPrevious()
    fun release()
}

interface TranslationProvider {
    val providerId: String
    suspend fun isModelReady(sourceLanguage: String, targetLanguage: String): Boolean
    suspend fun downloadModel(sourceLanguage: String, targetLanguage: String)
    suspend fun translate(text: String, sourceLanguage: String, targetLanguage: String): String
}

data class AiRequest(
    val selectedText: String,
    val currentBlock: String,
    val previousBlocks: List<Cue>,
    val filmTitle: String?,
    val includeTimestamps: Boolean,
    val prompt: String,
    val languageTag: String = "en",
    val model: String? = null,
)

data class AiResponse(val text: String, val providerId: String, val model: String)

interface AiProvider {
    val id: String
    val displayName: String
    suspend fun complete(request: AiRequest, apiKey: String): AiResponse
}

interface AiProviderFactory {
    fun get(providerId: String): AiProvider
    fun supportedProviders(): List<Pair<String, String>>
}

/** LATER model runner boundary; model files and native runtimes must never be committed. */
interface OnDeviceAiModelProvider {
    val id: String
    suspend fun isModelReady(): Boolean
    suspend fun downloadModel()
    suspend fun complete(request: AiRequest): AiResponse
}

data class QuizQuestion(val prompt: String, val answers: List<String>, val correctAnswer: String)

/** LATER quiz generator consumes only user-owned saved words. */
interface QuizProvider {
    suspend fun createQuiz(words: List<SavedWord>, questionCount: Int): List<QuizQuestion>
}

interface SubtitleRepository {
    suspend fun parse(bytes: ByteArray, trackId: String, formatHint: String? = null): List<Cue>
    fun cueAt(cues: List<Cue>, timeMs: Long): Cue?
    fun findExternalSidecar(videoUri: String, videoName: String): String?
}

interface SubtitleFileLoader {
    suspend fun load(uri: String, trackId: String): List<Cue>
}

data class WordAnalysis(
    val partOfSpeech: String? = null,
    val phrases: List<String> = emptyList(),
    val cefr: String? = null,
)

interface WordAnalyzer {
    suspend fun analyze(text: String, languageTag: String): WordAnalysis
}

interface WordLevelProvider {
    suspend fun wordsAboveLevel(cues: List<Cue>, manualLevel: String, knownWords: Set<String>): List<String>
}

data class DictionaryEntry(
    val headword: String,
    val meanings: List<String>,
    val examples: List<Pair<String, String>> = emptyList(),
    val synonyms: List<String> = emptyList(),
    val antonyms: List<String> = emptyList(),
    val phrasalVerbs: List<String> = emptyList(),
    val collocations: List<String> = emptyList(),
    val idioms: List<String> = emptyList(),
    val wordFamily: List<String> = emptyList(),
    val cefr: String? = null,
    val categories: List<String> = emptyList(),
)

interface DictionaryProvider {
    val isAvailable: Boolean
    suspend fun search(query: String): List<DictionaryEntry>
    suspend fun importUserDatabase(bytes: ByteArray)
}

interface SpeechToText {
    suspend fun generateSubtitles(mediaUri: String, languageTag: String): List<Cue>
}

interface UpdateChecker {
    suspend fun latestRelease(): String
}

interface YouTubeCatalog {
    suspend fun search(query: String): List<MediaRequest>
}

interface PdfLearningProvider {
    suspend fun openDocument(uri: String)
}

interface AutomaticLevelDetector {
    suspend fun detectLevel(words: Set<String>): String
}

interface SubtitleAiTools {
    suspend fun resegment(cues: List<Cue>, maxChars: Int): List<Cue>
    suspend fun markQuotes(cues: List<Cue>): List<Cue>
}

/** LATER defaults fail explicitly; a caller must keep the feature disabled until replaced. */
class NotImplementedDictionaryProvider : DictionaryProvider {
    override val isAvailable = false
    override suspend fun search(query: String): List<DictionaryEntry> = notImplemented("offline dictionary lookup")
    override suspend fun importUserDatabase(bytes: ByteArray): Unit = notImplemented("user dictionary import")
}

class NotImplementedWordAnalyzer : WordAnalyzer {
    override suspend fun analyze(text: String, languageTag: String): WordAnalysis = notImplemented("offline POS/phrase analysis")
}

class NotImplementedSpeechToText : SpeechToText {
    override suspend fun generateSubtitles(mediaUri: String, languageTag: String): List<Cue> = notImplemented("offline speech-to-text")
}

class NotImplementedUpdateChecker : UpdateChecker {
    override suspend fun latestRelease(): String = notImplemented("release update checker")
}

class NotImplementedQuizProvider : QuizProvider {
    override suspend fun createQuiz(words: List<SavedWord>, questionCount: Int): List<QuizQuestion> = notImplemented("My Words quiz")
}

class NotImplementedOnDeviceAiModelProvider : OnDeviceAiModelProvider {
    override val id: String = "on-device-ai"
    override suspend fun isModelReady(): Boolean = notImplemented("on-device AI model status")
    override suspend fun downloadModel(): Unit = notImplemented("on-device AI model download")
    override suspend fun complete(request: AiRequest): AiResponse = notImplemented("on-device AI generation")
}

class NotImplementedYouTubeCatalog : YouTubeCatalog {
    override suspend fun search(query: String): List<MediaRequest> = notImplemented("YouTube catalog")
}

class NotImplementedPdfLearningProvider : PdfLearningProvider {
    override suspend fun openDocument(uri: String): Unit = notImplemented("PDF learning")
}

class NotImplementedAutomaticLevelDetector : AutomaticLevelDetector {
    override suspend fun detectLevel(words: Set<String>): String = notImplemented("automatic learner-level detection")
}

class NotImplementedSubtitleAiTools : SubtitleAiTools {
    override suspend fun resegment(cues: List<Cue>, maxChars: Int): List<Cue> = notImplemented("AI subtitle re-segmentation")
    override suspend fun markQuotes(cues: List<Cue>): List<Cue> = notImplemented("AI quote marking")
}

private fun <T> notImplemented(capability: String): T =
    throw NotImplementedError("LATER capability is disabled: $capability")
