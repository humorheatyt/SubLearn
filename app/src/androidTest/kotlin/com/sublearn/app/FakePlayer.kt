package com.sublearn.app

import com.sublearn.domain.DecoderMode
import com.sublearn.domain.MediaRequest
import com.sublearn.domain.PlayerController
import com.sublearn.domain.PlayerSnapshot
import com.sublearn.domain.SubtitleTrack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Deterministic player implementation used only by instrumented Compose tests. */
internal class FakePlayer : PlayerController {
    private val mutableSnapshot = MutableStateFlow(PlayerSnapshot())
    override val snapshot: StateFlow<PlayerSnapshot> = mutableSnapshot.asStateFlow()
    private val mutableRepeatActive = MutableStateFlow(false)
    override val repeatActive: StateFlow<Boolean> = mutableRepeatActive.asStateFlow()
    private var released = false

    override suspend fun open(request: MediaRequest, queue: List<MediaRequest>) {
        check(!released)
        mutableSnapshot.value = PlayerSnapshot(
            request = request,
            durationMs = 120_000,
            isPlaying = true,
            audioTracks = listOf(SubtitleTrack("audio:0", "Default audio", "und")),
        )
    }

    override fun playPause() = mutableSnapshot.update { it.copy(isPlaying = !it.isPlaying) }
    override fun pause() { mutableSnapshot.update { it.copy(isPlaying = false) } }
    override fun resume() { mutableSnapshot.update { it.copy(isPlaying = true) } }
    override fun seekTo(positionMs: Long) { mutableSnapshot.update { it.copy(positionMs = positionMs.coerceIn(0, it.durationMs)) } }
    override fun seekBy(deltaMs: Long) = seekTo(mutableSnapshot.value.positionMs + deltaMs)
    override fun setPlaybackSpeed(speed: Float) { mutableSnapshot.update { it.copy(playbackSpeed = speed.coerceIn(0.25f, 3f)) } }
    override fun selectAudioTrack(trackId: String?) { mutableSnapshot.update { it.copy(selectedAudioTrackId = trackId) } }
    override fun selectEmbeddedSubtitleTrack(trackId: String?) { mutableSnapshot.update { it.copy(selectedEmbeddedSubtitleTrackId = trackId) } }
    override fun supportedDecoderModes(): Set<DecoderMode> = DecoderMode.entries.toSet()
    override fun setDecoderMode(mode: DecoderMode): Boolean {
        mutableSnapshot.update { it.copy(decoderMode = mode) }
        return true
    }
    override fun repeatBlock(startMs: Long, endMs: Long, count: Int, pauseAfterMs: Long) {
        if (endMs > startMs) {
            mutableRepeatActive.value = true
            seekTo(startMs)
        }
    }
    override fun stopRepeat() { mutableRepeatActive.value = false }
    override fun setStopAtBlockEnd(enabled: Boolean, endMs: Long?, temporarilyInverted: Boolean) = Unit
    override fun setAspectRatioMode(mode: Int) { mutableSnapshot.update { it.copy(aspectRatioMode = mode.coerceIn(0, 2)) } }
    override fun skipToNext() = seekTo(0)
    override fun skipToPrevious() = seekTo(0)
    override fun release() { released = true; mutableRepeatActive.value = false }
}
