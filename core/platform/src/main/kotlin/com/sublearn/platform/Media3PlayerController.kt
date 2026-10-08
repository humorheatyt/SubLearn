package com.sublearn.platform

import android.content.Context
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.C
import androidx.media3.common.CueGroup
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.trackselection.TrackSelectionParameters
import com.sublearn.domain.Cue
import com.sublearn.domain.DecoderMode
import com.sublearn.domain.MediaRequest
import com.sublearn.domain.PlayerController
import com.sublearn.domain.PlayerSnapshot
import com.sublearn.domain.RepeatPlanner
import com.sublearn.domain.SubtitleToken
import com.sublearn.domain.SubtitleTrack
import com.sublearn.subtitles.SubtitleNormalizer
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Media3-backed player. A replacement is used only when a decoder preference changes. */
@UnstableApi
class Media3PlayerController(context: Context) : PlayerController {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutableSnapshot = MutableStateFlow(PlayerSnapshot())
    override val snapshot: StateFlow<PlayerSnapshot> = mutableSnapshot.asStateFlow()
    private val mutableRepeatActive = MutableStateFlow(false)
    override val repeatActive: StateFlow<Boolean> = mutableRepeatActive.asStateFlow()

    private var decoderMode = DecoderMode.AUTO
    private var player: ExoPlayer = createPlayer(decoderMode)
    private var queueItems: List<MediaItem> = emptyList()
    private var selectedIndex = 0
    private var repeatJob: Job? = null
    private var repeatSession = 0L
    private var stopAtEndMs: Long? = null
    private var stopAtEndEnabled = false
    private var stopAtEndTemporarilyInverted = false
    private val cueIds = AtomicLong(1L)
    private var released = false
    private val pollingJob = scope.launch {
        while (isActive) {
            publishSnapshot()
            checkBlockStop()
            delay(200)
        }
    }

    val media3Player: Player get() = player

    init {
        player.addListener(listener)
        player.setAudioAttributes(
            AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(),
            true,
        )
    }

    override suspend fun open(request: MediaRequest, queue: List<MediaRequest>) {
        check(!released) { "Player has been released" }
        stopRepeat()
        val requests = listOf(request) + queue.filterNot { it.uri == request.uri }
        queueItems = requests.map(::toMediaItem)
        selectedIndex = 0
        player.setMediaItems(queueItems, 0, request.resumePositionMs.coerceAtLeast(0))
        player.prepare()
        player.playWhenReady = true
        mutableSnapshot.update { it.copy(request = request, errorMessage = null, decoderMode = decoderMode) }
    }

    override fun playPause() {
        stopRepeat()
        if (player.isPlaying) player.pause() else player.play()
    }

    override fun pause() {
        stopRepeat()
        player.pause()
    }
    override fun resume() = player.play()
    override fun seekTo(positionMs: Long) {
        stopRepeat()
        player.seekTo(positionMs.coerceAtLeast(0))
    }
    override fun seekBy(deltaMs: Long) {
        stopRepeat()
        player.seekTo((player.currentPosition + deltaMs).coerceAtLeast(0))
    }
    override fun setPlaybackSpeed(speed: Float) {
        player.setPlaybackSpeed(speed.coerceIn(0.25f, 3f))
        publishSnapshot()
    }

    override fun selectAudioTrack(trackId: String?) = selectTrack(C.TRACK_TYPE_AUDIO, trackId)
    override fun selectEmbeddedSubtitleTrack(trackId: String?) = selectTrack(C.TRACK_TYPE_TEXT, trackId)

    override fun supportedDecoderModes(): Set<DecoderMode> {
        val mime = player.currentTracks.groups.asSequence()
            .filter { it.type == C.TRACK_TYPE_VIDEO }
            .flatMap { group -> (0 until group.length).asSequence().map { group.getTrackFormat(it).sampleMimeType } }
            .filterNotNull().firstOrNull() ?: return setOf(DecoderMode.AUTO)
        val decoders = runCatching { MediaCodecSelector.DEFAULT.getDecoderInfos(mime, false, false) }.getOrDefault(emptyList())
        return buildSet {
            add(DecoderMode.AUTO)
            if (decoders.any { it.hardwareAccelerated }) {
                add(DecoderMode.HARDWARE)
                add(DecoderMode.HARDWARE_PLUS)
            }
            if (decoders.any { it.softwareOnly }) add(DecoderMode.SOFTWARE)
        }
    }

    override fun setDecoderMode(mode: DecoderMode): Boolean {
        if (mode == decoderMode) return true
        if (mode !in supportedDecoderModes()) return false
        stopRepeat()
        val position = player.currentPosition
        val wasPlaying = player.playWhenReady
        val old = player
        old.removeListener(listener)
        decoderMode = mode
        player = createPlayer(mode)
        player.addListener(listener)
        player.setAudioAttributes(
            AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(),
            true,
        )
        old.release()
        player.setMediaItems(queueItems, selectedIndex.coerceIn(0, (queueItems.size - 1).coerceAtLeast(0)), position)
        player.prepare()
        player.playWhenReady = wasPlaying
        mutableSnapshot.update { it.copy(decoderMode = mode, errorMessage = null) }
        return true
    }

    override fun repeatBlock(startMs: Long, endMs: Long, count: Int, pauseAfterMs: Long) {
        if (endMs <= startMs) return
        stopRepeat()
        val session = ++repeatSession
        val repeatCount = count.coerceAtLeast(0)
        mutableRepeatActive.value = true
        repeatJob = scope.launch {
            var completed = 0
            try {
                player.seekTo(startMs.coerceAtLeast(0))
                player.play()
                while (isActive && (repeatCount == 0 || completed < repeatCount)) {
                    while (isActive && player.currentPosition < endMs && player.playbackState != Player.STATE_ENDED) delay(40)
                    if (!isActive) break
                    completed++
                    if (repeatCount > 0 && completed >= repeatCount) break
                    player.pause()
                    delay(pauseAfterMs.coerceAtLeast(0))
                    player.seekTo(startMs.coerceAtLeast(0))
                    player.play()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } finally {
                if (repeatSession == session) {
                    mutableRepeatActive.value = false
                    repeatJob = null
                }
            }
        }
    }

    override fun stopRepeat() {
        repeatSession++
        repeatJob?.cancel()
        repeatJob = null
        mutableRepeatActive.value = false
    }

    override fun setStopAtBlockEnd(enabled: Boolean, endMs: Long?, temporarilyInverted: Boolean) {
        stopAtEndEnabled = enabled
        stopAtEndMs = endMs
        stopAtEndTemporarilyInverted = temporarilyInverted
        checkBlockStop()
    }

    override fun setAspectRatioMode(mode: Int) {
        mutableSnapshot.update { it.copy(aspectRatioMode = mode.coerceIn(0, 2)) }
    }

    override fun skipToNext() {
        stopRepeat()
        if (player.hasNextMediaItem()) player.seekToNextMediaItem()
    }

    override fun skipToPrevious() {
        stopRepeat()
        if (player.currentPosition > 3_000) player.seekTo(0) else if (player.hasPreviousMediaItem()) player.seekToPreviousMediaItem()
    }

    override fun release() {
        if (released) return
        released = true
        pollingJob.cancel()
        stopRepeat()
        player.removeListener(listener)
        player.release()
        scope.coroutineContext[Job]?.cancel()
    }

    private fun createPlayer(mode: DecoderMode): ExoPlayer {
        val selector = MediaCodecSelector { mimeType, secure, tunneled ->
            val decoders = MediaCodecSelector.DEFAULT.getDecoderInfos(mimeType, secure, tunneled)
            when (mode) {
                DecoderMode.HARDWARE, DecoderMode.HARDWARE_PLUS -> decoders.sortedBy { it.softwareOnly }
                DecoderMode.SOFTWARE -> decoders.sortedBy { !it.softwareOnly }
                DecoderMode.AUTO -> decoders
            }
        }
        val renderers = DefaultRenderersFactory(appContext)
            .setMediaCodecSelector(selector)
            .setEnableDecoderFallback(mode == DecoderMode.HARDWARE_PLUS)
        return ExoPlayer.Builder(appContext).setRenderersFactory(renderers).setHandleAudioBecomingNoisy(true).build()
    }

    private fun selectTrack(type: Int, id: String?) {
        val builder = player.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(type, false)
            .clearOverridesOfType(type)
        if (id != null) {
            val parts = id.split(':')
            val groupIndex = parts.getOrNull(0)?.toIntOrNull() ?: return
            val trackIndex = parts.getOrNull(1)?.toIntOrNull() ?: return
            val group = player.currentTracks.groups.getOrNull(groupIndex) ?: return
            if (trackIndex !in 0 until group.length || group.type != type) return
            builder.addOverride(TrackSelectionOverride(group.mediaTrackGroup, listOf(trackIndex)))
        }
        player.trackSelectionParameters = builder.build()
    }

    private fun publishSnapshot() {
        val current = mutableSnapshot.value
        val duration = player.duration.takeIf { it > 0 && it != C.TIME_UNSET } ?: 0L
        val request = currentRequest()
        mutableSnapshot.value = current.copy(
            request = request ?: current.request,
            positionMs = player.currentPosition.coerceAtLeast(0),
            durationMs = duration,
            bufferedPositionMs = player.bufferedPosition.coerceAtLeast(0),
            isPlaying = player.isPlaying,
            isBuffering = player.playbackState == Player.STATE_BUFFERING,
            playbackSpeed = player.playbackParameters.speed,
            audioTracks = listTracks(C.TRACK_TYPE_AUDIO),
            embeddedSubtitleTracks = listTracks(C.TRACK_TYPE_TEXT),
            selectedAudioTrackId = selectedTrackId(C.TRACK_TYPE_AUDIO),
            selectedEmbeddedSubtitleTrackId = selectedTrackId(C.TRACK_TYPE_TEXT),
            decoderMode = decoderMode,
        )
    }

    private fun currentRequest(): MediaRequest? {
        val item = player.currentMediaItem ?: return null
        val uri = item.localConfiguration?.uri?.toString() ?: return null
        return MediaRequest(
            uri = uri,
            title = item.mediaMetadata.title?.toString()?.takeIf(String::isNotBlank) ?: uri.substringAfterLast('/'),
            mimeType = item.localConfiguration?.mimeType,
            resumePositionMs = player.currentPosition.coerceAtLeast(0),
        )
    }

    private fun listTracks(type: Int): List<SubtitleTrack> = buildList {
        player.currentTracks.groups.forEachIndexed { groupIndex, group ->
            if (group.type != type) return@forEachIndexed
            repeat(group.length) { trackIndex ->
                val format = group.getTrackFormat(trackIndex)
                add(
                    SubtitleTrack(
                        id = "$groupIndex:$trackIndex",
                        label = format.label?.takeIf(String::isNotBlank) ?: defaultTrackLabel(format, type, trackIndex),
                        languageTag = format.language ?: "und",
                        isEmbedded = true,
                    ),
                )
            }
        }
    }

    private fun selectedTrackId(type: Int): String? = player.currentTracks.groups
        .withIndex()
        .firstNotNullOfOrNull { (groupIndex, group) ->
            if (group.type != type) return@firstNotNullOfOrNull null
            (0 until group.length).firstOrNull(group::isTrackSelected)?.let { "$groupIndex:$it" }
        }

    private fun defaultTrackLabel(format: Format, type: Int, index: Int): String {
        val language = format.language?.takeIf { it != "und" }
        return if (language != null) {
            appContext.getString(if (type == C.TRACK_TYPE_AUDIO) R.string.media3_language_audio else R.string.media3_language_subtitle, language)
        } else {
            appContext.getString(if (type == C.TRACK_TYPE_AUDIO) R.string.media3_audio_track else R.string.media3_subtitle_track, index + 1)
        }
    }

    private fun toMediaItem(request: MediaRequest): MediaItem = MediaItem.Builder()
        .setMediaId(request.uri)
        .setUri(Uri.parse(request.uri))
        .setMediaMetadata(MediaMetadata.Builder().setTitle(request.title).build())
        .apply { request.mimeType?.let(::setMimeType) }
        .build()

    private fun checkBlockStop() {
        val end = stopAtEndMs ?: return
        val effective = stopAtEndEnabled xor stopAtEndTemporarilyInverted
        if (effective && player.currentPosition >= end) {
            player.pause()
            stopAtEndMs = null
            stopAtEndTemporarilyInverted = false
        }
    }

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) = publishSnapshot()
        override fun onPlaybackStateChanged(playbackState: Int) = publishSnapshot()
        override fun onTracksChanged(tracks: androidx.media3.common.Tracks) = publishSnapshot()

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            selectedIndex = player.currentMediaItemIndex.coerceAtLeast(0)
            publishSnapshot()
        }

        override fun onCues(cueGroup: CueGroup) {
            val text = cueGroup.cues.mapNotNull { it.text?.toString()?.takeIf(String::isNotBlank) }.joinToString(" ")
            val time = (cueGroup.presentationTimeUs / 1_000L).coerceAtLeast(0)
            val cue = text.takeIf(String::isNotBlank)?.let { value ->
                val normalized = SubtitleNormalizer.cleanText(value)
                Cue(cueIds.getAndIncrement(), time, time + 60_000, normalized, SubtitleNormalizer.tokens(normalized), "embedded")
            }
            mutableSnapshot.update { it.copy(embeddedSubtitleCue = cue) }
        }

        override fun onPlayerError(error: PlaybackException) {
            mutableSnapshot.update { it.copy(errorMessage = appContext.getString(R.string.media3_playback_error, error.errorCodeName)) }
        }
    }
}
