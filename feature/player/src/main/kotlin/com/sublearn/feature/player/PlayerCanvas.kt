package com.sublearn.feature.player

import android.view.View
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.OpenWith
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.sublearn.design.SubLearnAlpha
import com.sublearn.design.SubLearnColors
import com.sublearn.design.SubLearnElevation
import com.sublearn.design.SubLearnRadii
import com.sublearn.design.SubLearnSpacing
import com.sublearn.domain.AppSettings
import com.sublearn.domain.Cue
import com.sublearn.domain.DockMode
import com.sublearn.domain.GestureAction
import com.sublearn.domain.LearningMode
import com.sublearn.domain.PlayerController
import com.sublearn.domain.PlayerSnapshot
import com.sublearn.domain.RecentMedia
import com.sublearn.domain.SubtitleLayer
import com.sublearn.feature.learning.LearningPopupStack
import com.sublearn.feature.learning.WordTranslationCard
import com.sublearn.platform.Media3PlayerController
import java.util.Locale
import kotlin.math.roundToInt
import androidx.compose.foundation.gestures.detectTapGestures

@OptIn(UnstableApi::class)
@Composable
internal fun PlayerCanvas(
    controller: PlayerController,
    snapshot: PlayerSnapshot,
    requestTitle: String,
    settings: AppSettings,
    learningCues: List<Cue>,
    nativeCues: List<Cue>,
    currentLearningCue: Cue?,
    currentNativeCue: Cue?,
    playbackCueDelayMs: Long,
    knownWords: Set<String>,
    myWords: Set<String>,
    nativeKnownWords: Set<String>,
    nativeWords: Set<String>,
    learningPopups: List<Pair<String, String>>,
    lookup: LookupUiState?,
    controlsVisible: Boolean,
    controlsLocked: Boolean,
    aiLoading: Boolean,
    layoutMode: Boolean,
    subtitleListVisible: Boolean,
    temporarySubtitleLayer: SubtitleLayer?,
    onBack: () -> Unit,
    onInteraction: () -> Unit,
    onToggleControls: () -> Unit,
    onShowAudio: () -> Unit,
    onShowSubtitles: () -> Unit,
    onShowMore: () -> Unit,
    onAskAi: () -> Unit,
    onEditAiPrompt: () -> Unit,
    onToggleLock: () -> Unit,
    onShowPlaylist: () -> Unit,
    onShowList: () -> Unit,
    onLongList: () -> Unit,
    onNoSpoiler: () -> Unit,
    onToggleLayout: () -> Unit,
    onToggleLayer: (SubtitleLayer) -> Unit,
    onHoldLayer: (SubtitleLayer, Boolean) -> Unit,
    onRepeatOnce: () -> Unit,
    onRepeatAuto: (Boolean) -> Unit,
    onToggleStopAtEnd: () -> Unit,
    onHoldStopAtEnd: (Boolean) -> Unit,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onPreviousCue: () -> Unit,
    onNextCue: () -> Unit,
    onTranslate: (String, SubtitleTapKind, SubtitleLayer) -> Unit,
    onDismissLookup: () -> Unit,
    onSaveLookup: () -> Unit,
    onUpdateSettings: (AppSettings) -> Unit,
    onGesture: (GestureAction, Float, Float, androidx.compose.ui.unit.IntSize) -> Unit,
    onTwoFingerGesture: (GestureAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val media3 = controller as? Media3PlayerController
    val repeatActive by controller.repeatActive.collectAsState(initial = false)
    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
            .background(SubLearnColors.PlayerSurface)
            .videoTwoFingerUp(settings, onTwoFingerGesture)
            .videoDragGestures(settings, onGesture)
            .videoTapGestures(
                settings,
                onSingleTap = { if (lookup != null) onDismissLookup() else onInteraction() },
                onDoubleTap = { action ->
                    onInteraction()
                    when (action) {
                        GestureAction.PAUSE_PLAY -> onPlayPause()
                        GestureAction.SEEK_BACK -> controller.seekBy(-10_000)
                        GestureAction.SEEK_FORWARD -> controller.seekBy(10_000)
                        GestureAction.TOGGLE_CONTROLS -> onToggleControls()
                        GestureAction.SPEED_CYCLE -> onTwoFingerGesture(action)
                        else -> Unit
                    }
                },
            ),
    ) {
        val maxHeightPx = maxHeight
        val playerHeightPx = with(LocalDensity.current) { maxHeight.toPx().coerceAtLeast(1f) }
        val listCues = when {
            currentLearningCue != null -> learningCues
            currentNativeCue != null -> nativeCues
            learningCues.isNotEmpty() -> learningCues
            else -> nativeCues
        }
        val listCurrentCue = currentLearningCue ?: currentNativeCue
        if (media3 != null) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { viewContext ->
                    PlayerView(viewContext).apply {
                        player = media3.media3Player
                        useController = false
                        subtitleView?.visibility = View.GONE
                        resizeMode = resizeMode(snapshot.aspectRatioMode)
                        setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                    }
                },
                update = { view ->
                    view.player = media3.media3Player
                    view.resizeMode = resizeMode(snapshot.aspectRatioMode)
                    view.subtitleView?.visibility = View.GONE
                },
            )
        }
        if (media3 == null) Box(Modifier.fillMaxSize().background(SubLearnColors.PlayerSurface))

        // Soft edge scrims preserve contrast without adding a permanent panel to the video.
        AnimatedVisibility(visible = controlsVisible && !controlsLocked, enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxWidth().height(SubLearnSpacing.xxl * 3f).align(Alignment.TopCenter).background(Brush.verticalGradient(listOf(SubLearnColors.PlayerSurface.copy(alpha = SubLearnAlpha.playerTopScrim), SubLearnColors.Transparent))))
                Box(Modifier.fillMaxWidth().height(SubLearnSpacing.xxl * 5f).align(Alignment.BottomCenter).background(Brush.verticalGradient(listOf(SubLearnColors.Transparent, SubLearnColors.PlayerSurface.copy(alpha = SubLearnAlpha.playerBottomScrim)))))
            }
        }

        SubtitleOverlay(
            cue = currentLearningCue,
            layer = SubtitleLayer.LEARNING,
            settings = settings,
            knownWords = knownWords,
            myWords = myWords,
            visible = settings.subtitlesLearningVisible,
            temporarilyInverted = temporarySubtitleLayer == SubtitleLayer.LEARNING,
            layoutMode = layoutMode,
            onTranslate = { text, kind -> onTranslate(text, kind, SubtitleLayer.LEARNING) },
            onLayoutDrag = { onUpdateSettings(settings.copy(learningSubtitlePosition = settings.learningSubtitlePosition + it / playerHeightPx)) },
            modifier = Modifier.align(Alignment.TopCenter).offset(y = maxHeightPx * settings.learningSubtitlePosition.coerceIn(0.08f, 0.96f)),
        )
        SubtitleOverlay(
            cue = currentNativeCue,
            layer = SubtitleLayer.NATIVE,
            settings = settings,
            knownWords = nativeKnownWords,
            myWords = nativeWords,
            visible = settings.subtitlesNativeVisible,
            temporarilyInverted = temporarySubtitleLayer == SubtitleLayer.NATIVE,
            layoutMode = layoutMode,
            onTranslate = { text, kind -> onTranslate(text, kind, SubtitleLayer.NATIVE) },
            onLayoutDrag = { onUpdateSettings(settings.copy(nativeSubtitlePosition = settings.nativeSubtitlePosition + it / playerHeightPx)) },
            modifier = Modifier.align(Alignment.TopCenter).offset(y = maxHeightPx * settings.nativeSubtitlePosition.coerceIn(0.08f, 0.96f)),
        )

        if (settings.learningMode == LearningMode.LEARNING && learningPopups.isNotEmpty()) {
            LearningPopupStack(
                words = learningPopups,
                settings = settings,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = SubLearnSpacing.lg, bottom = SubLearnSpacing.playerControl * 2f).width(SubLearnSpacing.posterHeight * 1.45f),
            )
        }

        if (lookup != null) {
            Card(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = SubLearnSpacing.screen, vertical = SubLearnSpacing.playerControl * 2f),
                shape = RoundedCornerShape(SubLearnRadii.large),
                elevation = CardDefaults.cardElevation(defaultElevation = SubLearnElevation.wordCard),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(Modifier.padding(SubLearnSpacing.md), verticalArrangement = Arrangement.spacedBy(SubLearnSpacing.xs)) {
                    when {
                        lookup.loading -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SubLearnSpacing.sm)) {
                            CircularProgressIndicator(modifier = Modifier.size(SubLearnSpacing.target))
                            Text(stringResource(R.string.player_translating))
                        }
                        lookup.problem == TranslationProblem.MODEL_MISSING -> Text(stringResource(R.string.player_download_model), color = MaterialTheme.colorScheme.error)
                        lookup.problem == TranslationProblem.FAILED -> Text(stringResource(R.string.player_translation_error), color = MaterialTheme.colorScheme.error)
                        lookup.translation.isNotBlank() -> WordTranslationCard(
                            word = lookup.selectedText,
                            translation = lookup.translation,
                            context = lookup.contextCue.text,
                            sourceLanguageTag = lookup.sourceLanguageTag,
                            targetLanguageTag = lookup.targetLanguageTag,
                            settings = settings,
                            isSaved = lookup.saved,
                            onSave = onSaveLookup,
                        )
                    }
                    Text(stringResource(R.string.player_dismiss_translation), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (controlsVisible && !controlsLocked) {
            TopControls(
                title = requestTitle,
                onBack = onBack,
                onAudio = onShowAudio,
                onSubtitles = onShowSubtitles,
                onMore = onShowMore,
                onAskAi = onAskAi,
                onEditAiPrompt = onEditAiPrompt,
                aiLoading = aiLoading,
                onInteraction = onInteraction,
            )
            when (settings.quickActionDockMode) {
                DockMode.QUICK_COLUMN -> QuickActionColumn(
                    settings, onToggleLayer, onHoldLayer, onToggleLayout, onShowList, onLongList,
                    onToggleStopAtEnd, onHoldStopAtEnd, onInteraction,
                )
                DockMode.BOTTOM_BAR -> QuickActionBar(
                    settings, onToggleLayer, onHoldLayer, onToggleLayout, onShowList, onLongList, onInteraction,
                )
                DockMode.FLOATING -> FloatingSubtitleButtons(settings, onToggleLayer, onHoldLayer, onInteraction)
                DockMode.HIDDEN -> Unit
            }
            CenterControls(
                isPlaying = snapshot.isPlaying,
                isBuffering = snapshot.isBuffering,
                repeatActive = repeatActive,
                onPlayPause = { onInteraction(); onPlayPause() },
                onRepeatTap = { if (repeatActive) onRepeatAuto(false) else onRepeatOnce() },
                onRepeatHold = { onRepeatAuto(!repeatActive) },
            )
            BottomControls(
                snapshot = snapshot,
                onSeek = { onInteraction(); onSeek(it) },
                onPrevious = { onInteraction(); onPreviousCue() },
                onPlayPause = { onInteraction(); onPlayPause() },
                onNext = { onInteraction(); onNextCue() },
            )
            CornerControls(
                locked = controlsLocked,
                onLock = { onInteraction(); onToggleLock() },
                onPlaylist = { onInteraction(); onShowPlaylist() },
                onSettings = { onInteraction(); onShowMore() },
            )
        } else if (controlsLocked) {
            IconButton(
                onClick = onToggleLock,
                modifier = Modifier.align(Alignment.Center).size(SubLearnSpacing.playerControl),
                colors = IconButtonDefaults.iconButtonColors(containerColor = SubLearnColors.PlayerSubtitleScrim, contentColor = SubLearnColors.PlayerText),
            ) { Icon(Icons.Rounded.LockOpen, contentDescription = stringResource(R.string.player_unlock)) }
        }

        if (subtitleListVisible && maxWidth > maxHeight) {
            SubtitleListPanel(
                cues = listCues,
                currentCue = listCurrentCue,
                noSpoiler = settings.noSpoilerList,
                onNoSpoiler = onNoSpoiler,
                onSeek = { onSeek((it + playbackCueDelayMs).coerceAtLeast(0)) },
                onClose = onShowList,
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().width((maxWidth * 0.36f).coerceIn(SubLearnSpacing.subtitleListMinWidth, SubLearnSpacing.subtitleListMaxWidth)),
            )
        }

        if (!snapshot.errorMessage.isNullOrBlank()) {
            Surface(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = SubLearnSpacing.xxl),
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(SubLearnRadii.medium),
            ) {
                Text(snapshot.errorMessage.orEmpty(), modifier = Modifier.padding(SubLearnSpacing.md), color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    }
}

@Composable
private fun BoxScope.TopControls(title: String, onBack: () -> Unit, onAudio: () -> Unit, onSubtitles: () -> Unit, onMore: () -> Unit, onAskAi: () -> Unit, onEditAiPrompt: () -> Unit, aiLoading: Boolean, onInteraction: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter).windowInsetsPadding(WindowInsets.statusBars).padding(horizontal = SubLearnSpacing.sm, vertical = SubLearnSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SubLearnSpacing.xs),
    ) {
        IconButton(onClick = { onInteraction(); onBack() }, modifier = Modifier.size(SubLearnSpacing.target)) { Icon(Icons.Rounded.ArrowBack, contentDescription = stringResource(R.string.player_back), tint = SubLearnColors.PlayerText) }
        Text(title, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium, color = SubLearnColors.PlayerText, fontWeight = FontWeight.SemiBold)
        IconButton(onClick = { onInteraction(); onAudio() }, modifier = Modifier.size(SubLearnSpacing.target)) { Icon(Icons.Rounded.Audiotrack, contentDescription = stringResource(R.string.player_audio), tint = SubLearnColors.PlayerText) }
        IconButton(onClick = { onInteraction(); onSubtitles() }, modifier = Modifier.size(SubLearnSpacing.target)) { Icon(Icons.Rounded.ClosedCaption, contentDescription = stringResource(R.string.player_subtitles), tint = SubLearnColors.PlayerText) }
        HoldActionIconButton(
            contentDescription = stringResource(R.string.player_ai),
            onClick = { onInteraction(); onAskAi() },
            onLongPress = onEditAiPrompt,
            onRelease = {},
            modifier = Modifier.size(SubLearnSpacing.target),
            selected = aiLoading,
            icon = { if (aiLoading) CircularProgressIndicator(modifier = Modifier.size(SubLearnSpacing.md)) else Icon(Icons.Rounded.AutoAwesome, contentDescription = null) },
        )
        IconButton(onClick = { onInteraction(); onMore() }, modifier = Modifier.size(SubLearnSpacing.target)) { Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.player_more), tint = SubLearnColors.PlayerText) }
    }
}

@Composable
private fun BoxScope.QuickActionColumn(
    settings: AppSettings,
    onToggleLayer: (SubtitleLayer) -> Unit,
    onHoldLayer: (SubtitleLayer, Boolean) -> Unit,
    onToggleLayout: () -> Unit,
    onShowList: () -> Unit,
    onLongList: () -> Unit,
    onToggleStopAtEnd: () -> Unit,
    onHoldStopAtEnd: (Boolean) -> Unit,
    onInteraction: () -> Unit,
) {
    Column(
        modifier = Modifier.align(Alignment.TopStart).windowInsetsPadding(WindowInsets.statusBars).padding(start = SubLearnSpacing.sm, top = SubLearnSpacing.target + SubLearnSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(SubLearnSpacing.xs),
    ) {
        SubtitleToggleButton(SubtitleLayer.LEARNING, settings, onToggleLayer, onHoldLayer)
        SubtitleToggleButton(SubtitleLayer.NATIVE, settings, onToggleLayer, onHoldLayer)
        IconButton(onClick = { onInteraction(); onToggleLayout() }, modifier = Modifier.size(SubLearnSpacing.target), colors = playerIconColors()) {
            Icon(Icons.Rounded.OpenWith, contentDescription = stringResource(R.string.player_layout))
        }
        HoldActionIconButton(
            contentDescription = stringResource(R.string.player_subtitle_list),
            onClick = { onInteraction(); onShowList() },
            onLongPress = { onLongList() },
            onRelease = {},
            icon = { Icon(Icons.Rounded.FormatListBulleted, contentDescription = null) },
        )
        HoldActionIconButton(
            contentDescription = stringResource(R.string.player_stop_at_end),
            onClick = { onInteraction(); onToggleStopAtEnd() },
            onLongPress = { onHoldStopAtEnd(true) },
            onRelease = { onHoldStopAtEnd(false) },
            icon = { Icon(Icons.Rounded.StopCircle, contentDescription = null) },
        )
    }
}

@Composable
private fun BoxScope.QuickActionBar(
    settings: AppSettings,
    onToggleLayer: (SubtitleLayer) -> Unit,
    onHoldLayer: (SubtitleLayer, Boolean) -> Unit,
    onToggleLayout: () -> Unit,
    onShowList: () -> Unit,
    onLongList: () -> Unit,
    onInteraction: () -> Unit,
) {
    Row(
        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = SubLearnSpacing.playerControl * 2f),
        horizontalArrangement = Arrangement.spacedBy(SubLearnSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SubtitleToggleButton(SubtitleLayer.LEARNING, settings, onToggleLayer, onHoldLayer)
        SubtitleToggleButton(SubtitleLayer.NATIVE, settings, onToggleLayer, onHoldLayer)
        IconButton(onClick = { onInteraction(); onToggleLayout() }, modifier = Modifier.size(SubLearnSpacing.target), colors = playerIconColors()) { Icon(Icons.Rounded.OpenWith, contentDescription = stringResource(R.string.player_layout)) }
        HoldActionIconButton(stringResource(R.string.player_subtitle_list), { onInteraction(); onShowList() }, onLongList, {}, { Icon(Icons.Rounded.FormatListBulleted, contentDescription = null) })
    }
}

@Composable
private fun BoxScope.FloatingSubtitleButtons(
    settings: AppSettings,
    onToggleLayer: (SubtitleLayer) -> Unit,
    onHoldLayer: (SubtitleLayer, Boolean) -> Unit,
    onInteraction: () -> Unit,
) {
    SubtitleToggleButton(
        SubtitleLayer.LEARNING,
        settings,
        onToggleLayer,
        onHoldLayer,
        Modifier.align(Alignment.CenterStart).offset(y = (-settings.learningButtonPosition * SubLearnSpacing.floatingButtonSeparation.value).dp),
    )
    SubtitleToggleButton(
        SubtitleLayer.NATIVE,
        settings,
        onToggleLayer,
        onHoldLayer,
        Modifier.align(Alignment.CenterStart).offset(y = (settings.nativeButtonPosition * SubLearnSpacing.floatingButtonSeparation.value).dp),
    )
}

@Composable
private fun SubtitleToggleButton(
    layer: SubtitleLayer,
    settings: AppSettings,
    onClick: (SubtitleLayer) -> Unit,
    onHold: (SubtitleLayer, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val learning = layer == SubtitleLayer.LEARNING
    val visible = if (learning) settings.subtitlesLearningVisible else settings.subtitlesNativeVisible
    val size = if (learning) settings.learningButtonSizeDp.dp else settings.nativeButtonSizeDp.dp
    val alpha = if (learning) settings.learningButtonAlpha else settings.nativeButtonAlpha
    val description = stringResource(if (learning) R.string.player_layer_learning else R.string.player_layer_native)
    HoldActionIconButton(
        contentDescription = description,
        onClick = { onClick(layer) },
        onLongPress = { onHold(layer, true) },
        onRelease = { onHold(layer, false) },
        modifier = modifier.size(size).alpha(alpha),
        icon = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Rounded.ClosedCaption, contentDescription = null, modifier = Modifier.size(SubLearnSpacing.lg))
                Text((if (learning) settings.learningLanguageTag else settings.nativeLanguageTag).substringBefore('-').uppercase(Locale.ROOT), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        },
        selected = visible,
    )
}

@Composable
private fun HoldActionIconButton(
    contentDescription: String,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onRelease: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    var longPressed by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier.pointerInput(contentDescription, onClick, onLongPress, onRelease) {
            detectTapGestures(
                onTap = { if (!longPressed) onClick(); longPressed = false },
                onLongPress = { longPressed = true; onLongPress() },
                onPress = {
                    tryAwaitRelease()
                    if (longPressed) onRelease()
                    longPressed = false
                },
            )
        }.semantics {
            role = Role.Button
            this.contentDescription = contentDescription
            onClick { onClick(); true }
        },
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else SubLearnColors.PlayerSubtitleScrim,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else SubLearnColors.PlayerText,
        tonalElevation = if (selected) SubLearnElevation.card else SubLearnElevation.flat,
    ) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { icon() } }
}

@Composable
private fun BoxScope.CenterControls(
    isPlaying: Boolean,
    isBuffering: Boolean,
    repeatActive: Boolean,
    onPlayPause: () -> Unit,
    onRepeatTap: () -> Unit,
    onRepeatHold: () -> Unit,
) {
    Row(
        modifier = Modifier.align(Alignment.Center),
        horizontalArrangement = Arrangement.spacedBy(SubLearnSpacing.xl),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HoldActionIconButton(
            contentDescription = stringResource(if (repeatActive) R.string.player_repeat_auto else R.string.player_repeat),
            onClick = onRepeatTap,
            onLongPress = onRepeatHold,
            onRelease = {},
            icon = { Icon(Icons.Rounded.Replay, contentDescription = null, modifier = Modifier.size(SubLearnSpacing.xl)) },
            modifier = Modifier.size(SubLearnSpacing.playerControl),
            selected = repeatActive,
        )
        Surface(
            modifier = Modifier.size(SubLearnSpacing.playerControl + SubLearnSpacing.md),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = SubLearnAlpha.playerCenterControl),
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isBuffering) CircularProgressIndicator(modifier = Modifier.size(SubLearnSpacing.lg), color = MaterialTheme.colorScheme.onPrimary)
                else IconButton(onClick = onPlayPause, modifier = Modifier.fillMaxSize()) {
                    Icon(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = stringResource(if (isPlaying) R.string.player_pause else R.string.player_play), modifier = Modifier.size(SubLearnSpacing.xl))
                }
            }
        }
    }
}

@Composable
private fun BoxScope.BottomControls(
    snapshot: PlayerSnapshot,
    onSeek: (Long) -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
) {
    Column(
        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = SubLearnSpacing.screen, vertical = SubLearnSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(SubLearnSpacing.xs),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(snapshot.positionMs), style = MaterialTheme.typography.labelMedium, color = SubLearnColors.PlayerText)
            Text(formatTime(snapshot.durationMs), style = MaterialTheme.typography.labelMedium, color = SubLearnColors.PlayerText)
        }
        Box(Modifier.fillMaxWidth()) {
            LinearProgressIndicator(
                progress = { if (snapshot.durationMs > 0) snapshot.bufferedPositionMs.toFloat() / snapshot.durationMs else 0f },
                modifier = Modifier.fillMaxWidth().height(SubLearnSpacing.xs).align(Alignment.Center),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = SubLearnAlpha.playerBufferedTrack),
                trackColor = SubLearnColors.Transparent,
            )
            Slider(
                value = snapshot.progress,
                onValueChange = { if (snapshot.durationMs > 0) onSeek((snapshot.durationMs * it).toLong()) },
                enabled = snapshot.durationMs > 0,
                modifier = Modifier.fillMaxWidth().height(SubLearnSpacing.target),
                colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.secondary, activeTrackColor = MaterialTheme.colorScheme.secondary, inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = SubLearnAlpha.playerInactiveTrack)),
            )
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            IconButton(onClick = onPrevious, modifier = Modifier.size(SubLearnSpacing.playerControl)) { Icon(Icons.Rounded.Replay, contentDescription = stringResource(R.string.player_previous_block), tint = SubLearnColors.PlayerText) }
            IconButton(onClick = onPlayPause, modifier = Modifier.size(SubLearnSpacing.playerControl)) {
                Icon(if (snapshot.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = stringResource(if (snapshot.isPlaying) R.string.player_pause else R.string.player_play), tint = SubLearnColors.PlayerText)
            }
            IconButton(onClick = onNext, modifier = Modifier.size(SubLearnSpacing.playerControl)) { Icon(Icons.Rounded.PlayArrow, contentDescription = stringResource(R.string.player_next_block), tint = SubLearnColors.PlayerText) }
        }
    }
}

@Composable
private fun BoxScope.CornerControls(locked: Boolean, onLock: () -> Unit, onPlaylist: () -> Unit, onSettings: () -> Unit) {
    Row(
        modifier = Modifier.align(Alignment.BottomEnd).padding(end = SubLearnSpacing.sm, bottom = SubLearnSpacing.playerControl * 4f),
        horizontalArrangement = Arrangement.spacedBy(SubLearnSpacing.xs),
    ) {
        IconButton(onClick = onPlaylist, modifier = Modifier.size(SubLearnSpacing.target), colors = playerIconColors()) { Icon(Icons.Rounded.PlaylistPlay, contentDescription = stringResource(R.string.player_playlist)) }
        IconButton(onClick = onSettings, modifier = Modifier.size(SubLearnSpacing.target), colors = playerIconColors()) { Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.player_more)) }
        IconButton(onClick = onLock, modifier = Modifier.size(SubLearnSpacing.target), colors = playerIconColors()) { Icon(if (locked) Icons.Rounded.LockOpen else Icons.Rounded.Lock, contentDescription = stringResource(if (locked) R.string.player_unlock else R.string.player_lock)) }
    }
}

@Composable
private fun playerIconColors() = IconButtonDefaults.iconButtonColors(containerColor = SubLearnColors.PlayerSubtitleScrim, contentColor = SubLearnColors.PlayerText)

private fun resizeMode(mode: Int): Int = when (mode) {
    1 -> AspectRatioFrameLayout.RESIZE_MODE_FILL
    2 -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
    else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
}

private fun formatTime(ms: Long): String {
    val seconds = ms.coerceAtLeast(0) / 1_000
    return if (seconds >= 3_600) String.format(Locale.ROOT, "%d:%02d:%02d", seconds / 3_600, seconds / 60 % 60, seconds % 60)
    else String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60)
}
