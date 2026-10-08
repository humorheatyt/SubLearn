package com.sublearn.feature.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.util.Rational
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.sublearn.design.SubLearnAlpha
import com.sublearn.design.SubLearnMotion
import com.sublearn.design.SubLearnRadii
import com.sublearn.design.SubLearnSpacing
import com.sublearn.domain.AiContextBuilder
import com.sublearn.domain.AiProviderFactory
import com.sublearn.domain.AiRequest
import com.sublearn.domain.AppSettings
import com.sublearn.domain.Cue
import com.sublearn.domain.GestureAction
import com.sublearn.domain.LearningMode
import com.sublearn.domain.MediaRequest
import com.sublearn.domain.PlayerController
import com.sublearn.domain.PlayerSnapshot
import com.sublearn.domain.RecentMediaRepository
import com.sublearn.domain.RepeatPlanner
import com.sublearn.domain.SavedWord
import com.sublearn.domain.SavedWordRepository
import com.sublearn.domain.SecretStore
import com.sublearn.domain.SubtitleFileLoader
import com.sublearn.domain.SubtitleLayer
import com.sublearn.domain.SubtitleRepository
import com.sublearn.domain.SubtitleTrack
import com.sublearn.domain.TranslationProvider
import com.sublearn.domain.UnknownWordLevelProvider
import com.sublearn.domain.WordLevelProvider
import com.sublearn.platform.ContentResolverSubtitleRepository
import com.sublearn.subtitles.CueTimelineIndex
import com.sublearn.subtitles.SubtitleNormalizer
import java.io.IOException
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Connects the platform player and repositories to the reusable, state-driven player canvas. */
@OptIn(FlowPreview::class)
@Composable
fun PlayerScreen(
    request: MediaRequest,
    controller: PlayerController,
    settings: AppSettings,
    recentMediaRepository: RecentMediaRepository,
    savedWordRepository: SavedWordRepository,
    subtitleRepository: SubtitleRepository,
    subtitleLoader: SubtitleFileLoader,
    translationProvider: TranslationProvider,
    aiProviderFactory: AiProviderFactory,
    secretStore: SecretStore,
    onSettingsChange: (AppSettings) -> Unit,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit,
    wordLevelProvider: WordLevelProvider = UnknownWordLevelProvider(),
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val snapshot by controller.snapshot.collectAsStateWithLifecycle(initialValue = PlayerSnapshot())
    val activeRequest = snapshot.request ?: request
    val isLandscape = LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val recentMedia by recentMediaRepository.observeRecent(100).collectAsStateWithLifecycle(initialValue = emptyList())
    val savedWords by savedWordRepository.observeWords().collectAsStateWithLifecycle(initialValue = emptyList())
    val learningWords = remember(savedWords, settings.learningLanguageTag) { savedTokenSet(savedWords, settings.learningLanguageTag) }
    val knownWords = remember(savedWords, settings.learningLanguageTag) { savedTokenSet(savedWords, settings.learningLanguageTag, knownOnly = true) }
    val nativeWords = remember(savedWords, settings.nativeLanguageTag) { savedTokenSet(savedWords, settings.nativeLanguageTag) }
    val nativeKnownWords = remember(savedWords, settings.nativeLanguageTag) { savedTokenSet(savedWords, settings.nativeLanguageTag, knownOnly = true) }
    val loadedTracks = remember { mutableStateListOf<LoadedTrack>() }

    var selectedLearningTrack by remember { mutableStateOf<String?>(null) }
    var selectedNativeTrack by remember { mutableStateOf<String?>(null) }
    var selectedLayer by remember { mutableStateOf(SubtitleLayer.LEARNING) }
    var embeddedLayer by remember { mutableStateOf(SubtitleLayer.LEARNING) }
    var pendingLayer by remember { mutableStateOf(SubtitleLayer.LEARNING) }
    var temporarySubtitleLayer by remember { mutableStateOf<SubtitleLayer?>(null) }
    var learningPopups by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var lookup by remember { mutableStateOf<LookupUiState?>(null) }
    var lookupJob by remember { mutableStateOf<Job?>(null) }
    var aiJob by remember { mutableStateOf<Job?>(null) }
    var aiLoading by remember { mutableStateOf(false) }
    var aiError by remember { mutableStateOf<String?>(null) }
    var aiAnswer by remember { mutableStateOf("") }
    var resumeAfterLookup by remember { mutableStateOf(false) }
    var showTrackDialog by remember { mutableStateOf(false) }
    var showAudioDialog by remember { mutableStateOf(false) }
    var showMoreSheet by remember { mutableStateOf(false) }
    var showPlaylistDialog by remember { mutableStateOf(false) }
    var showToolsDialog by remember { mutableStateOf(false) }
    var showPromptEditor by remember { mutableStateOf(false) }
    var showAiDialog by remember { mutableStateOf(false) }
    var showSubtitleList by remember { mutableStateOf(false) }
    var layoutMode by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }
    var controlsLocked by remember { mutableStateOf(false) }
    var interactionKey by remember { mutableStateOf(0) }
    var volumeGestureRemainder by remember { mutableStateOf(0f) }
    var promptEditorText by remember(settings.aiPrompt) { mutableStateOf(settings.aiPrompt) }
    var toolFlatten by remember { mutableStateOf(settings.flattenLineBreaks) }
    var toolSplit by remember { mutableStateOf(false) }
    var pendingExportText by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val learningTrack = loadedTracks.firstOrNull { it.track.id == selectedLearningTrack && it.layer == SubtitleLayer.LEARNING }
    val nativeTrack = loadedTracks.firstOrNull { it.track.id == selectedNativeTrack && it.layer == SubtitleLayer.NATIVE }
    val embeddedCues = listOfNotNull(snapshot.embeddedSubtitleCue)
    val learningCues = learningTrack?.cues ?: embeddedCues.takeIf { embeddedLayer == SubtitleLayer.LEARNING }.orEmpty()
    val nativeCues = nativeTrack?.cues ?: embeddedCues.takeIf { embeddedLayer == SubtitleLayer.NATIVE }.orEmpty()
    val learningIndex = remember(learningCues) { CueTimelineIndex(learningCues) }
    val nativeIndex = remember(nativeCues) { CueTimelineIndex(nativeCues) }
    val currentLearningCue = learningIndex.cueAt((snapshot.positionMs - settings.learningSubtitleDelayMs).coerceAtLeast(0))
    val currentNativeCue = nativeIndex.cueAt((snapshot.positionMs - settings.nativeSubtitleDelayMs).coerceAtLeast(0))
    val playbackCue = currentLearningCue ?: currentNativeCue
    val playbackCueDelayMs = when {
        currentLearningCue != null -> settings.learningSubtitleDelayMs
        currentNativeCue != null -> settings.nativeSubtitleDelayMs
        learningCues.isNotEmpty() -> settings.learningSubtitleDelayMs
        else -> settings.nativeSubtitleDelayMs
    }
    val playbackCues = when {
        currentLearningCue != null -> learningCues
        currentNativeCue != null -> nativeCues
        learningCues.isNotEmpty() -> learningCues
        else -> nativeCues
    }
    val learningTracks = loadedTracks.filter { it.layer == SubtitleLayer.LEARNING }
    val nativeTracks = loadedTracks.filter { it.layer == SubtitleLayer.NATIVE }

    val subtitlePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        val targetLayer = pendingLayer
        if (uris.isNotEmpty()) scope.launch {
            uris.forEach { uri ->
                runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                val id = "external:${targetLayer.name.lowercase(Locale.ROOT)}:${uri}"
                val name = (subtitleRepository as? ContentResolverSubtitleRepository)?.displayName(uri)
                    ?: uri.lastPathSegment?.substringAfterLast('/')
                    ?: uri.toString()
                val parsed = runCatching { subtitleLoader.load(uri.toString(), id) }
                val cues = parsed.getOrElse {
                    statusMessage = context.getString(R.string.player_translation_error)
                    emptyList()
                }
                if (cues.isNotEmpty()) {
                    loadedTracks.removeAll { it.track.id == id }
                    loadedTracks += LoadedTrack(
                        track = SubtitleTrack(id, name, languageFromName(name), uri.toString(), isEmbedded = false),
                        layer = targetLayer,
                        cues = cues,
                    )
                    if (targetLayer == SubtitleLayer.LEARNING) selectedLearningTrack = id else selectedNativeTrack = id
                    if (snapshot.selectedEmbeddedSubtitleTrackId != null && embeddedLayer == targetLayer) controller.selectEmbeddedSubtitleTrack(null)
                    recentMediaRepository.recordOpened(activeRequest, snapshot.positionMs, snapshot.durationMs, uri.toString(), targetLayer)
                }
            }
            showTrackDialog = false
        }
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/x-subrip")) { uri ->
        if (uri != null) scope.launch {
            val written = runCatching {
                context.contentResolver.openOutputStream(uri)?.use { stream -> stream.write(pendingExportText.toByteArray(Charsets.UTF_8)) }
                    ?: throw IOException("Could not create subtitle file")
            }
            statusMessage = if (written.isSuccess) context.getString(R.string.player_tools_success)
            else context.getString(R.string.player_translation_error)
        }
    }

    fun interact() {
        controlsVisible = true
        interactionKey++
    }

    fun dismissLookup() {
        lookupJob?.cancel()
        lookup = null
        if (resumeAfterLookup) controller.resume()
        resumeAfterLookup = false
        interact()
    }

    fun beginLookup(text: String, cue: Cue?, layer: SubtitleLayer) {
        val selected = text.trim()
        if (selected.isBlank()) return
        val contextCue = cue ?: currentLearningCue ?: currentNativeCue ?: return
        val sourceLanguage = if (layer == SubtitleLayer.LEARNING) settings.learningLanguageTag else settings.nativeLanguageTag
        val targetLanguage = if (layer == SubtitleLayer.LEARNING) settings.nativeLanguageTag else settings.learningLanguageTag
        resumeAfterLookup = snapshot.isPlaying
        controller.pause()
        lookupJob?.cancel()
        lookup = LookupUiState(
            selectedText = selected,
            contextCue = contextCue,
            sourceLanguageTag = sourceLanguage,
            targetLanguageTag = targetLanguage,
        )
        interact()
        lookupJob = scope.launch {
            lookup = lookup?.copy(loading = true, problem = null)
            try {
                if (!translationProvider.isModelReady(sourceLanguage, targetLanguage)) {
                    lookup = lookup?.copy(loading = false, problem = TranslationProblem.MODEL_MISSING)
                } else {
                    val translation = translationProvider.translate(selected, sourceLanguage, targetLanguage)
                    val existing = savedWordRepository.find(selected, sourceLanguage)
                    lookup = lookup?.copy(translation = translation, loading = false, saved = existing != null)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                lookup = lookup?.copy(loading = false, problem = TranslationProblem.FAILED)
            }
        }
    }

    fun askAi() {
        val cue = currentLearningCue ?: currentNativeCue ?: return
        interact()
        controller.pause()
        showAiDialog = true
        aiLoading = true
        aiError = null
        aiAnswer = ""
        aiJob?.cancel()
        aiJob = scope.launch {
            try {
                val key = secretStore.read(settings.aiProviderId)
                if (key.isNullOrBlank()) throw IllegalStateException(context.getString(R.string.player_ai_empty_key))
                val orderedCues = (learningCues + nativeCues).distinctBy { "${it.trackId}:${it.id}" }.sortedBy { it.startMs }
                val previous = orderedCues.filter { it.startMs < cue.startMs }.takeLast(settings.aiContextBlockCount)
                val baseRequest = AiRequest(
                    selectedText = lookup?.selectedText.orEmpty().ifBlank { cue.text },
                    currentBlock = cue.text,
                    previousBlocks = previous,
                    filmTitle = activeRequest.title.takeIf { settings.aiIncludeFilmTitle },
                    includeTimestamps = settings.aiIncludeTimestamps,
                    prompt = settings.aiPrompt,
                    languageTag = settings.uiLanguageTag,
                    model = settings.aiModel,
                )
                val enriched = baseRequest.copy(prompt = AiContextBuilder.build(settings, baseRequest))
                aiAnswer = aiProviderFactory.get(settings.aiProviderId).complete(enriched, key).text
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                aiError = context.getString(R.string.player_ai_error)
            } finally {
                aiLoading = false
            }
        }
    }

    fun cycleSpeed() {
        val steps = settings.speedSteps.ifEmpty { listOf(1f) }
        val next = (settings.speedStepIndex + 1).mod(steps.size)
        val speed = steps[next]
        controller.setPlaybackSpeed(speed)
        onSettingsChange(settings.copy(speedStepIndex = next))
        statusMessage = context.getString(R.string.player_speed_value, speed.toString())
        interact()
    }

    fun handleGesture(action: GestureAction, deltaX: Float, deltaY: Float, size: IntSize) {
        when (action) {
            GestureAction.SEEK_BACK, GestureAction.SEEK_FORWARD -> {
                val base = snapshot.durationMs.coerceAtLeast(60_000L)
                val magnitude = (kotlin.math.abs(deltaX) / size.width.coerceAtLeast(1) * base).toLong()
                val horizontalSign = if (deltaX >= 0f) 1 else -1
                val direction = if (action == GestureAction.SEEK_BACK) -horizontalSign else horizontalSign
                if (magnitude > 0) controller.seekBy(magnitude * direction.toLong())
                statusMessage = context.getString(if (action == GestureAction.SEEK_FORWARD) R.string.player_next_block else R.string.player_previous_block)
            }
            GestureAction.VOLUME -> {
                val manager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                if (manager != null) {
                    val max = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                    val change = volumeGestureRemainder - deltaY / size.height.coerceAtLeast(1) * max
                    val steps = change.toInt()
                    volumeGestureRemainder = change - steps
                    if (steps != 0) {
                        val updated = (manager.getStreamVolume(AudioManager.STREAM_MUSIC) + steps).coerceIn(0, max)
                        manager.setStreamVolume(AudioManager.STREAM_MUSIC, updated, 0)
                        statusMessage = context.getString(R.string.player_volume_value, updated * 100 / max)
                    }
                }
            }
            GestureAction.BRIGHTNESS -> {
                val window = activity?.window
                if (window != null) {
                    val params = window.attributes
                    val current = params.screenBrightness.takeIf { it in 0f..1f } ?: 0.5f
                    val updated = (current - deltaY / size.height.coerceAtLeast(1) * 1.6f).coerceIn(0.05f, 1f)
                    params.screenBrightness = updated
                    window.attributes = params
                    statusMessage = context.getString(R.string.player_brightness_value, (updated * 100).toInt())
                }
            }
            GestureAction.SPEED_CYCLE -> cycleSpeed()
            GestureAction.PAUSE_PLAY -> controller.playPause()
            GestureAction.TOGGLE_CONTROLS -> interact()
            GestureAction.NONE -> Unit
        }
        interact()
    }

    fun applySubtitleTools() {
        val selectedId = if (selectedLayer == SubtitleLayer.LEARNING) selectedLearningTrack else selectedNativeTrack
        val index = loadedTracks.indexOfFirst { it.track.id == selectedId }
        if (index < 0) {
            statusMessage = context.getString(R.string.player_no_subtitle)
            return
        }
        val source = loadedTracks[index]
        val maxCharacters = settings.subtitleMaxCharacters
        val edited = source.cues.flatMap { cue ->
            val text = if (toolFlatten) cue.text.replace(Regex("\\s*\\n+\\s*"), " ").trim() else cue.text
            if (toolSplit && text.length > maxCharacters) splitCue(cue.copy(text = text), maxCharacters) else listOf(cue.copy(text = text, tokens = SubtitleNormalizer.tokens(text)))
        }
        loadedTracks[index] = source.copy(cues = edited)
        showToolsDialog = false
        statusMessage = context.getString(R.string.player_tools_success)
    }

    fun exportSubtitle() {
        val cues = if (selectedLayer == SubtitleLayer.LEARNING) learningTrack?.cues else nativeTrack?.cues
        if (cues.isNullOrEmpty()) {
            statusMessage = context.getString(R.string.player_no_subtitle)
            return
        }
        pendingExportText = cues.sortedBy { it.startMs }.mapIndexed { index, cue ->
            "${index + 1}\n${formatSrtTimestamp(cue.startMs)} --> ${formatSrtTimestamp(cue.endMs)}\n${cue.text}\n"
        }.joinToString("\n")
        exportLauncher.launch("${activeRequest.title.substringBeforeLast('.', activeRequest.title)}.srt")
    }

    LaunchedEffect(controller, request.uri) {
        val saved = withContext(Dispatchers.IO) { recentMediaRepository.observeRecent(100).first().firstOrNull { it.uri == request.uri } }
        val effectiveRequest = request.copy(resumePositionMs = request.resumePositionMs.takeIf { it > 0 } ?: saved?.lastPositionMs ?: 0L)
        controller.open(effectiveRequest)
        recentMediaRepository.recordOpened(effectiveRequest, effectiveRequest.resumePositionMs, 0)
    }

    LaunchedEffect(snapshot.request?.uri, settings.speedSteps, settings.speedStepIndex, settings.playbackAspectRatioMode) {
        if (snapshot.request != null) {
            val speed = settings.speedSteps.getOrElse(settings.speedStepIndex) { 1f }
            controller.setPlaybackSpeed(speed)
            controller.setAspectRatioMode(settings.playbackAspectRatioMode)
        }
    }

    LaunchedEffect(controller) {
        controller.snapshot.sample(5_000).collect { current ->
            current.request?.let { currentRequest ->
                recentMediaRepository.saveProgress(currentRequest.uri, current.positionMs, current.durationMs)
            }
        }
    }

    LaunchedEffect(activeRequest.uri) {
        loadedTracks.clear()
        selectedLearningTrack = null
        selectedNativeTrack = null
        selectedLayer = SubtitleLayer.LEARNING
        val recentEntry = withContext(Dispatchers.IO) {
            recentMediaRepository.observeRecent(100).first().firstOrNull { it.uri == activeRequest.uri }
        }
        val sidecarUri = recentEntry?.lastSubtitleUri
            ?: runCatching { subtitleRepository.findExternalSidecar(activeRequest.uri, activeRequest.title) }.getOrNull()
        if (!sidecarUri.isNullOrBlank()) {
            val id = "sidecar:${activeRequest.uri}"
            val cues = runCatching { subtitleLoader.load(sidecarUri, id) }.getOrDefault(emptyList())
            if (cues.isNotEmpty()) {
                val uri = Uri.parse(sidecarUri)
                val name = (subtitleRepository as? ContentResolverSubtitleRepository)?.displayName(uri) ?: uri.lastPathSegment ?: context.getString(R.string.player_subtitle_default_name)
                val layer = recentEntry?.lastSubtitleLayer ?: subtitleLayerForName(name, settings)
                loadedTracks += LoadedTrack(SubtitleTrack(id, name, languageFromName(name), sidecarUri), layer, cues)
                if (layer == SubtitleLayer.LEARNING) selectedLearningTrack = id else selectedNativeTrack = id
                if (snapshot.selectedEmbeddedSubtitleTrackId != null && embeddedLayer == layer) controller.selectEmbeddedSubtitleTrack(null)
                recentMediaRepository.recordOpened(activeRequest, snapshot.positionMs, snapshot.durationMs, sidecarUri, layer)
            }
        }
    }

    LaunchedEffect(currentLearningCue?.id, knownWords, settings.learningMode, settings.manualLevel, settings.manualLevelPopupLimit, settings.learningLanguageTag, settings.nativeLanguageTag) {
        learningPopups = emptyList()
        val activeCue = currentLearningCue ?: return@LaunchedEffect
        if (settings.learningMode == LearningMode.ENTERTAINMENT) return@LaunchedEffect
        val unknown = runCatching { wordLevelProvider.wordsAboveLevel(listOf(activeCue), settings.manualLevel, knownWords) }
            .getOrDefault(emptyList()).take(settings.manualLevelPopupLimit)
        if (unknown.isEmpty() || !runCatching { translationProvider.isModelReady(settings.learningLanguageTag, settings.nativeLanguageTag) }.getOrDefault(false)) return@LaunchedEffect
        learningPopups = unknown.mapNotNull { word ->
            runCatching { word to translationProvider.translate(word, settings.learningLanguageTag, settings.nativeLanguageTag) }.getOrNull()
        }
    }

    LaunchedEffect(interactionKey, snapshot.isPlaying, settings.autoHideControlsMs) {
        if (!snapshot.isPlaying || controlsLocked) return@LaunchedEffect
        delay(settings.autoHideControlsMs)
        if (snapshot.isPlaying && !showMoreSheet && !showTrackDialog && !showAiDialog && lookup == null) controlsVisible = false
    }

    LaunchedEffect(statusMessage) {
        if (statusMessage != null) {
            delay(SubLearnMotion.feedbackMs)
            statusMessage = null
        }
    }

    val latestSnapshot by rememberUpdatedState(snapshot)
    DisposableEffect(lifecycleOwner, controller, activity) {
        var resumeOnReturn = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    if (activity?.isInPictureInPictureMode != true) {
                        resumeOnReturn = latestSnapshot.isPlaying
                        controller.pause()
                    }
                    lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                        latestSnapshot.request?.let { current ->
                            recentMediaRepository.saveProgress(current.uri, latestSnapshot.positionMs, latestSnapshot.durationMs)
                        }
                    }
                }
                Lifecycle.Event.ON_START -> {
                    if (resumeOnReturn && activity?.isInPictureInPictureMode != true) {
                        resumeOnReturn = false
                        controller.resume()
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                latestSnapshot.request?.let { current ->
                    recentMediaRepository.saveProgress(current.uri, latestSnapshot.positionMs, latestSnapshot.durationMs)
                }
            }
            controller.release()
        }
    }

    val originalOrientation = remember(activity) { activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    val originalBrightness = remember(activity) { activity?.window?.attributes?.screenBrightness }
    DisposableEffect(activity) {
        onDispose {
            activity?.requestedOrientation = originalOrientation
            val targetActivity = activity
            val brightness = originalBrightness
            if (targetActivity != null && brightness != null) {
                val params = targetActivity.window.attributes
                params.screenBrightness = brightness
                targetActivity.window.attributes = params
            }
        }
    }
    androidx.compose.runtime.SideEffect {
        activity?.requestedOrientation = if (settings.rotationLocked) ActivityInfo.SCREEN_ORIENTATION_LOCKED else originalOrientation
    }

    Box(Modifier.fillMaxSize()) {
        val playerCanvas: @Composable (Modifier) -> Unit = { playerModifier ->
            PlayerCanvas(
            controller = controller,
            snapshot = snapshot,
            requestTitle = activeRequest.title,
            settings = settings,
            learningCues = learningCues,
            nativeCues = nativeCues,
            currentLearningCue = currentLearningCue,
            currentNativeCue = currentNativeCue,
            playbackCueDelayMs = playbackCueDelayMs,
            knownWords = knownWords,
            myWords = learningWords,
            nativeKnownWords = nativeKnownWords,
            nativeWords = nativeWords,
            learningPopups = learningPopups,
            lookup = lookup,
            controlsVisible = controlsVisible,
            controlsLocked = controlsLocked,
            aiLoading = aiLoading,
            layoutMode = layoutMode,
            subtitleListVisible = showSubtitleList && isLandscape,
            temporarySubtitleLayer = temporarySubtitleLayer,
            onBack = onBack,
            onInteraction = ::interact,
            onToggleControls = { controlsVisible = !controlsVisible; interactionKey++ },
            onShowAudio = { showAudioDialog = true; interact() },
            onShowSubtitles = { showTrackDialog = true; interact() },
            onShowMore = { showMoreSheet = true; interact() },
            onAskAi = ::askAi,
            onEditAiPrompt = { promptEditorText = settings.aiPrompt; showPromptEditor = true; interact() },
            onToggleLock = { controlsLocked = !controlsLocked; controlsVisible = true },
            onShowPlaylist = { showPlaylistDialog = true; interact() },
            onShowList = { showSubtitleList = !showSubtitleList; interact() },
            onLongList = { onSettingsChange(settings.copy(noSpoilerList = !settings.noSpoilerList)) },
            onNoSpoiler = { onSettingsChange(settings.copy(noSpoilerList = !settings.noSpoilerList)) },
            onToggleLayout = { layoutMode = !layoutMode; interact() },
            onToggleLayer = { layer ->
                val updated = if (layer == SubtitleLayer.LEARNING) settings.copy(subtitlesLearningVisible = !settings.subtitlesLearningVisible)
                else settings.copy(subtitlesNativeVisible = !settings.subtitlesNativeVisible)
                onSettingsChange(updated)
                interact()
            },
            onHoldLayer = { layer, pressed -> temporarySubtitleLayer = if (pressed) layer else temporarySubtitleLayer.takeUnless { it == layer } },
            onRepeatOnce = {
                val cue = playbackCue
                if (cue != null) {
                    val pauseMs = RepeatPlanner.delayAfterBlock(cue.endMs - cue.startMs, settings.repeatPauseMs, settings.repeatPauseMultiplier)
                    val start = (cue.startMs + playbackCueDelayMs).coerceAtLeast(0)
                    val end = (cue.endMs + playbackCueDelayMs).coerceAtLeast(start + 1)
                    controller.repeatBlock(start, end, count = 2, pauseAfterMs = pauseMs)
                }
                interact()
            },
            onRepeatAuto = { automatic ->
                val cue = playbackCue
                if (!automatic) controller.stopRepeat()
                else if (cue != null) {
                    val pauseMs = RepeatPlanner.delayAfterBlock(cue.endMs - cue.startMs, settings.repeatPauseMs, settings.repeatPauseMultiplier)
                    val start = (cue.startMs + playbackCueDelayMs).coerceAtLeast(0)
                    val end = (cue.endMs + playbackCueDelayMs).coerceAtLeast(start + 1)
                    controller.repeatBlock(start, end, count = settings.repeatCount + 1, pauseAfterMs = pauseMs)
                }
                interact()
            },
            onToggleStopAtEnd = {
                val enabled = !settings.stopAtBlockEnd
                controller.setStopAtBlockEnd(enabled, playbackCue?.let { (it.endMs + playbackCueDelayMs).coerceAtLeast(0) })
                onSettingsChange(settings.copy(stopAtBlockEnd = enabled))
                interact()
            },
            onHoldStopAtEnd = { pressed -> controller.setStopAtBlockEnd(settings.stopAtBlockEnd, playbackCue?.let { (it.endMs + playbackCueDelayMs).coerceAtLeast(0) }, temporarilyInverted = pressed) },
            onPlayPause = controller::playPause,
            onSeek = controller::seekTo,
            onPreviousCue = { seekAdjacent(playbackCues, playbackCue, snapshot.positionMs, -1, playbackCueDelayMs, controller) },
            onNextCue = { seekAdjacent(playbackCues, playbackCue, snapshot.positionMs, 1, playbackCueDelayMs, controller) },
            onTranslate = { text, _, layer -> beginLookup(text, if (layer == SubtitleLayer.LEARNING) currentLearningCue else currentNativeCue, layer) },
            onDismissLookup = ::dismissLookup,
            onSaveLookup = {
                val value = lookup
                if (value != null && value.translation.isNotBlank()) scope.launch {
                    val savedId = savedWordRepository.save(
                        SavedWord(
                            text = value.selectedText,
                            translation = value.translation,
                            languageTag = value.sourceLanguageTag,
                            nativeLanguageTag = value.targetLanguageTag,
                            context = value.contextCue.text,
                            mediaTitle = activeRequest.title,
                            startMs = value.contextCue.startMs,
                        ),
                    )
                    lookup = value.copy(saved = savedId > 0 || savedWordRepository.find(value.selectedText, value.sourceLanguageTag) != null)
                }
            },
            onUpdateSettings = onSettingsChange,
            onGesture = ::handleGesture,
            onTwoFingerGesture = { action -> if (action == GestureAction.SPEED_CYCLE) cycleSpeed() },
            modifier = playerModifier,
            )
        }

        if (showSubtitleList && !isLandscape) {
            Column(Modifier.fillMaxSize()) {
                playerCanvas(Modifier.weight(0.58f).fillMaxWidth())
                SubtitleListPanel(
                    cues = playbackCues,
                    currentCue = playbackCue,
                    noSpoiler = settings.noSpoilerList,
                    onNoSpoiler = { onSettingsChange(settings.copy(noSpoilerList = !settings.noSpoilerList)) },
                    onSeek = { controller.seekTo((it + playbackCueDelayMs).coerceAtLeast(0)) },
                    modifier = Modifier.weight(0.42f).fillMaxWidth(),
                    onClose = { showSubtitleList = false },
                )
            }
        } else {
            playerCanvas(Modifier.fillMaxSize())
        }

        if (snapshot.request == null && snapshot.errorMessage.isNullOrBlank()) {
            Surface(
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.surface.copy(alpha = SubLearnAlpha.statusSurface),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(SubLearnRadii.medium),
            ) {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.padding(horizontal = SubLearnSpacing.lg, vertical = SubLearnSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(SubLearnSpacing.sm),
                ) {
                    CircularProgressIndicator(modifier = Modifier.width(SubLearnSpacing.md))
                    Text(androidx.compose.ui.res.stringResource(R.string.player_loading))
                }
            }
        }

        statusMessage?.let { message ->
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = SubLearnSpacing.xxl),
                color = MaterialTheme.colorScheme.errorContainer,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(SubLearnRadii.medium),
            ) {
                Text(message, modifier = Modifier.padding(SubLearnSpacing.md), color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    }

    if (showTrackDialog) {
        SubtitleTracksDialog(
            layer = selectedLayer,
            learningTracks = learningTracks,
            nativeTracks = nativeTracks,
            embeddedTracks = snapshot.embeddedSubtitleTracks,
            selectedLearningId = selectedLearningTrack,
            selectedNativeId = selectedNativeTrack,
            selectedEmbeddedId = snapshot.selectedEmbeddedSubtitleTrackId,
            onDismiss = { showTrackDialog = false },
            onLayerChange = { selectedLayer = it },
            onAdd = { layer -> pendingLayer = layer; subtitlePicker.launch(arrayOf("application/x-subrip", "text/vtt", "text/plain", "application/octet-stream")) },
            onSelectExternal = { layer, id ->
                if (layer == SubtitleLayer.LEARNING) selectedLearningTrack = id else selectedNativeTrack = id
                if (snapshot.selectedEmbeddedSubtitleTrackId != null && embeddedLayer == layer) controller.selectEmbeddedSubtitleTrack(null)
            },
            onSelectEmbedded = { id ->
                controller.selectEmbeddedSubtitleTrack(id)
                if (id != null) {
                    embeddedLayer = selectedLayer
                    if (selectedLayer == SubtitleLayer.LEARNING) selectedLearningTrack = null else selectedNativeTrack = null
                }
            },
        )
    }
    if (showAudioDialog) AudioTracksDialog(snapshot.audioTracks, snapshot.selectedAudioTrackId, controller::selectAudioTrack) { showAudioDialog = false }
    if (showMoreSheet) MoreOptionsSheet(
        decoderMode = snapshot.decoderMode,
        supportedDecoderModes = controller.supportedDecoderModes(),
        speed = snapshot.playbackSpeed,
        speedSteps = settings.speedSteps,
        aspectRatioMode = snapshot.aspectRatioMode,
        onDecoderMode = { mode ->
            if (!controller.setDecoderMode(mode)) statusMessage = context.getString(R.string.player_decoder_unavailable)
            showMoreSheet = false
        },
        onSpeed = { speed ->
            controller.setPlaybackSpeed(speed)
            val index = settings.speedSteps.indexOf(speed).takeIf { it >= 0 } ?: 0
            onSettingsChange(settings.copy(speedStepIndex = index))
            showMoreSheet = false
        },
        onAspectRatio = { ratio -> controller.setAspectRatioMode(ratio); onSettingsChange(settings.copy(playbackAspectRatioMode = ratio)); showMoreSheet = false },
        onPip = { enterPictureInPicture(activity); showMoreSheet = false },
        onPlaylist = { showMoreSheet = false; showPlaylistDialog = true },
        onTools = { showMoreSheet = false; showToolsDialog = true },
        onSettings = { showMoreSheet = false; onOpenSettings() },
        onDismiss = { showMoreSheet = false },
    )
    if (showPlaylistDialog) PlaylistDialog(recentMedia, activeRequest.uri, onOpen = { target ->
        scope.launch {
            val queue = recentMedia.filter { it.uri != target.uri }.map { item -> MediaRequest(item.uri, item.title, item.mimeType, 0) }
            controller.open(target, queue)
            showPlaylistDialog = false
        }
    }) { showPlaylistDialog = false }
    if (showToolsDialog) SubtitleToolsDialog(
        flatten = toolFlatten,
        split = toolSplit,
        maxCharacters = settings.subtitleMaxCharacters,
        onFlatten = { toolFlatten = it },
        onSplit = { toolSplit = it },
        onApply = ::applySubtitleTools,
        onExport = ::exportSubtitle,
        onDismiss = { showToolsDialog = false },
    )
    if (showPromptEditor) PromptEditorDialog(
        prompt = promptEditorText,
        onPromptChange = { promptEditorText = it },
        onSave = { onSettingsChange(settings.copy(aiPrompt = promptEditorText)); showPromptEditor = false },
        onDismiss = { showPromptEditor = false },
    )
    if (showAiDialog) AiAnswerDialog(
        answer = aiAnswer,
        loading = aiLoading,
        error = aiError,
        settings = settings,
        onCancel = { aiJob?.cancel(); aiLoading = false; showAiDialog = false },
        onResume = { showAiDialog = false; controller.resume() },
        onDismiss = { showAiDialog = false },
    )
}

private fun savedTokenSet(words: List<SavedWord>, languageTag: String, knownOnly: Boolean = false): Set<String> {
    val wanted = languageTag.substringBefore('-').lowercase(Locale.ROOT)
    return words.asSequence()
        .filter { it.languageTag.substringBefore('-').lowercase(Locale.ROOT) == wanted && (!knownOnly || it.isKnown) }
        .flatMap { SubtitleNormalizer.tokens(it.text).asSequence() }
        .map { it.text.lowercase(Locale.ROOT) }
        .filter(String::isNotBlank)
        .toSet()
}

private fun seekAdjacent(cues: List<Cue>, current: Cue?, time: Long, direction: Int, delayMs: Long, controller: PlayerController) {
    if (cues.isEmpty()) return
    val subtitleTime = (time - delayMs).coerceAtLeast(0)
    val currentIndex = current?.let { cue -> cues.indexOfFirst { it.id == cue.id } } ?: -1
    val target = if (currentIndex >= 0) (currentIndex + direction).coerceIn(0, cues.lastIndex) else {
        if (direction > 0) cues.indexOfFirst { it.startMs > subtitleTime }.takeIf { it >= 0 } ?: cues.lastIndex
        else cues.indexOfLast { it.startMs < subtitleTime }.coerceAtLeast(0)
    }
    controller.seekTo((cues[target].startMs + delayMs).coerceAtLeast(0))
}

private fun splitCue(cue: Cue, maxCharacters: Int): List<Cue> {
    val words = cue.text.split(Regex("\\s+")).filter(String::isNotBlank)
    if (words.isEmpty()) return listOf(cue)
    val chunks = mutableListOf<String>()
    var current = StringBuilder()
    words.forEach { word ->
        if (current.isNotEmpty() && current.length + 1 + word.length > maxCharacters) {
            chunks += current.toString()
            current = StringBuilder()
        }
        if (current.isNotEmpty()) current.append(' ')
        current.append(word)
    }
    if (current.isNotEmpty()) chunks += current.toString()
    val duration = (cue.endMs - cue.startMs).coerceAtLeast(chunks.size.toLong())
    return chunks.mapIndexed { index, text ->
        val start = cue.startMs + duration * index / chunks.size
        val end = cue.startMs + duration * (index + 1) / chunks.size
        Cue(
            id = (cue.id shl 16) + index,
            startMs = start,
            endMs = maxOf(start + 1, end),
            text = text,
            tokens = SubtitleNormalizer.tokens(text),
            trackId = cue.trackId,
        )
    }
}

private fun formatSrtTimestamp(timeMs: Long): String {
    val total = timeMs.coerceAtLeast(0)
    val hours = total / 3_600_000
    val minutes = (total / 60_000) % 60
    val seconds = (total / 1_000) % 60
    val millis = total % 1_000
    return String.format(Locale.ROOT, "%02d:%02d:%02d,%03d", hours, minutes, seconds, millis)
}

private fun languageFromName(name: String): String {
    val lower = name.lowercase(Locale.ROOT)
    return when {
        Regex("(^|[._-])(fa|fas|per)([._-]|$)").containsMatchIn(lower) -> "fa"
        Regex("(^|[._-])(en|eng)([._-]|$)").containsMatchIn(lower) -> "en"
        else -> "und"
    }
}

private fun subtitleLayerForName(name: String, settings: AppSettings): SubtitleLayer {
    val language = languageFromName(name).substringBefore('-')
    return when (language) {
        settings.nativeLanguageTag.substringBefore('-').lowercase(Locale.ROOT) -> SubtitleLayer.NATIVE
        else -> SubtitleLayer.LEARNING
    }
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun enterPictureInPicture(activity: Activity?) {
    if (activity == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    if (!activity.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_PICTURE_IN_PICTURE)) return
    runCatching {
        val params = PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9)).build()
        activity.enterPictureInPictureMode(params)
    }
}
