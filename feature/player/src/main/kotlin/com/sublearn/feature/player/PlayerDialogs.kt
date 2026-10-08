package com.sublearn.feature.player

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.sublearn.design.SubLearnRadii
import com.sublearn.design.SubLearnSpacing
import com.sublearn.design.withSurfaceFont
import com.sublearn.domain.SurfaceFontSettings
import com.sublearn.domain.AppSettings
import com.sublearn.domain.Cue
import com.sublearn.domain.DecoderMode
import com.sublearn.domain.MediaRequest
import com.sublearn.domain.SubtitleLayer
import com.sublearn.domain.SubtitleTrack
import java.util.Locale

internal data class LoadedTrack(
    val track: SubtitleTrack,
    val layer: SubtitleLayer,
    val cues: List<Cue>,
)

@Composable
internal fun SubtitleTracksDialog(
    layer: SubtitleLayer,
    learningTracks: List<LoadedTrack>,
    nativeTracks: List<LoadedTrack>,
    embeddedTracks: List<SubtitleTrack>,
    selectedLearningId: String?,
    selectedNativeId: String?,
    selectedEmbeddedId: String?,
    onDismiss: () -> Unit,
    onLayerChange: (SubtitleLayer) -> Unit,
    onAdd: (SubtitleLayer) -> Unit,
    onSelectExternal: (SubtitleLayer, String?) -> Unit,
    onSelectEmbedded: (String?) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.player_subtitles)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(SubLearnSpacing.md)) {
                Row(horizontalArrangement = Arrangement.spacedBy(SubLearnSpacing.xs)) {
                    FilterChip(selected = layer == SubtitleLayer.LEARNING, onClick = { onLayerChange(SubtitleLayer.LEARNING) }, label = { Text(stringResource(R.string.player_layer_learning)) })
                    FilterChip(selected = layer == SubtitleLayer.NATIVE, onClick = { onLayerChange(SubtitleLayer.NATIVE) }, label = { Text(stringResource(R.string.player_layer_native)) })
                }
                val activeTracks = if (layer == SubtitleLayer.LEARNING) learningTracks else nativeTracks
                val selectedId = if (layer == SubtitleLayer.LEARNING) selectedLearningId else selectedNativeId
                Text(stringResource(R.string.player_external), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                if (activeTracks.isEmpty()) Text(stringResource(R.string.player_no_tracks), style = MaterialTheme.typography.bodySmall)
                activeTracks.forEach { loaded ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = loaded.track.id == selectedId, onClick = { onSelectExternal(layer, loaded.track.id) })
                        Text(loaded.track.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selectedId == null, onClick = { onSelectExternal(layer, null) })
                    Text(stringResource(R.string.player_no_tracks))
                }
                Button(onClick = { onAdd(layer) }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (layer == SubtitleLayer.LEARNING) stringResource(R.string.player_add_learning) else stringResource(R.string.player_add_native))
                }
                if (embeddedTracks.isNotEmpty()) {
                    Text(stringResource(R.string.player_embedded), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    embeddedTracks.forEach { track ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = track.id == selectedEmbeddedId, onClick = { onSelectEmbedded(track.id) })
                            Text(track.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedEmbeddedId == null, onClick = { onSelectEmbedded(null) })
                        Text(stringResource(R.string.player_no_tracks))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.player_done)) } },
    )
}

@Composable
internal fun AudioTracksDialog(tracks: List<SubtitleTrack>, selectedId: String?, onSelect: (String?) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.player_audio)) },
        text = {
            Column {
                tracks.forEach { track ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = track.id == selectedId, onClick = { onSelect(track.id) })
                        Text(track.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selectedId == null, onClick = { onSelect(null) })
                    Text(stringResource(R.string.player_decoder_auto))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.player_done)) } },
    )
}

@Composable
internal fun MoreOptionsSheet(
    decoderMode: DecoderMode,
    supportedDecoderModes: Set<DecoderMode>,
    speed: Float,
    speedSteps: List<Float>,
    aspectRatioMode: Int,
    onDecoderMode: (DecoderMode) -> Unit,
    onSpeed: (Float) -> Unit,
    onAspectRatio: (Int) -> Unit,
    onPip: () -> Unit,
    onPlaylist: () -> Unit,
    onTools: () -> Unit,
    onSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = SubLearnSpacing.lg), verticalArrangement = Arrangement.spacedBy(SubLearnSpacing.xs)) {
            Text(stringResource(R.string.player_more), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.player_decoder), style = MaterialTheme.typography.titleMedium)
            DecoderMode.entries.filter { it in supportedDecoderModes }.forEach { mode ->
                ListItem(
                    headlineContent = { Text(decoderLabel(mode)) },
                    leadingContent = { RadioButton(selected = decoderMode == mode, onClick = { onDecoderMode(mode) }) },
                    modifier = Modifier.combinedClickable(onClick = { onDecoderMode(mode) }),
                )
            }
            Text(stringResource(R.string.player_speed), style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(SubLearnSpacing.xs)) {
                speedSteps.forEach { value ->
                    FilterChip(selected = speed == value, onClick = { onSpeed(value) }, label = { Text(speedText(value)) })
                }
            }
            Text(stringResource(R.string.player_aspect), style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(SubLearnSpacing.xs)) {
                listOf(0 to stringResource(R.string.aspect_fit), 1 to stringResource(R.string.aspect_fill), 2 to stringResource(R.string.aspect_zoom)).forEach { (mode, label) ->
                    FilterChip(selected = aspectRatioMode == mode, onClick = { onAspectRatio(mode) }, label = { Text(label) })
                }
            }
            ListItem(headlineContent = { Text(stringResource(R.string.player_pip)) }, modifier = Modifier.combinedClickable(onClick = onPip))
            ListItem(headlineContent = { Text(stringResource(R.string.player_playlist)) }, modifier = Modifier.combinedClickable(onClick = onPlaylist))
            ListItem(headlineContent = { Text(stringResource(R.string.player_subtitle_manage)) }, modifier = Modifier.combinedClickable(onClick = onTools))
            ListItem(headlineContent = { Text(stringResource(R.string.player_settings)) }, modifier = Modifier.combinedClickable(onClick = onSettings))
        }
    }
}

@Composable
internal fun PlaylistDialog(media: List<com.sublearn.domain.RecentMedia>, currentUri: String, onOpen: (MediaRequest) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.player_playlist)) },
        text = {
            Column {
                if (media.isEmpty()) Text(stringResource(R.string.player_no_tracks))
                media.forEach { recent ->
                    ListItem(
                        headlineContent = { Text(recent.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        supportingContent = { Text(formatTime(recent.lastPositionMs)) },
                        leadingContent = { RadioButton(selected = currentUri == recent.uri, onClick = { onOpen(MediaRequest(recent.uri, recent.title, recent.mimeType, recent.lastPositionMs)) }) },
                        modifier = Modifier.combinedClickable(onClick = { onOpen(MediaRequest(recent.uri, recent.title, recent.mimeType, recent.lastPositionMs)) }),
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.player_close)) } },
    )
}

@Composable
internal fun AiAnswerDialog(answer: String, loading: Boolean, error: String?, settings: AppSettings, onCancel: () -> Unit, onResume: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = { if (!loading) onDismiss() },
        title = { Text(stringResource(R.string.player_ai_answer)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(SubLearnSpacing.md)) {
                if (loading) androidx.compose.material3.CircularProgressIndicator()
                if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
                if (answer.isNotBlank()) Text(
                    answer,
                    style = MaterialTheme.typography.bodyLarge.withSurfaceFont(settings.surfaceFonts["ai.answer"] ?: SurfaceFontSettings()),
                )
                if (loading) TextButton(onClick = onCancel) { Text(stringResource(R.string.player_ai_cancel)) }
            }
        },
        confirmButton = {
            if (!loading) Button(onClick = onResume) { Text(stringResource(R.string.player_ai_resume)) }
            else TextButton(onClick = onDismiss) { Text(stringResource(R.string.player_close)) }
        },
    )
}

@Composable
internal fun PromptEditorDialog(prompt: String, onPromptChange: (String) -> Unit, onSave: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.player_ai_prompt_title)) },
        text = { OutlinedTextField(value = prompt, onValueChange = onPromptChange, modifier = Modifier.fillMaxWidth(), minLines = 5, maxLines = 10) },
        confirmButton = { Button(onClick = onSave) { Text(stringResource(R.string.player_done)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.player_close)) } },
    )
}

@Composable
internal fun SubtitleToolsDialog(
    flatten: Boolean,
    split: Boolean,
    maxCharacters: Int,
    onFlatten: (Boolean) -> Unit,
    onSplit: (Boolean) -> Unit,
    onApply: () -> Unit,
    onExport: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.player_tools_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(SubLearnSpacing.sm)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = flatten, onCheckedChange = onFlatten)
                    Text(stringResource(R.string.player_tools_flatten))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = split, onCheckedChange = onSplit)
                    Text(stringResource(R.string.player_tools_split, maxCharacters))
                }
            }
        },
        confirmButton = { Button(onClick = onApply) { Text(stringResource(R.string.player_tools_apply)) } },
        dismissButton = { TextButton(onClick = onExport) { Text(stringResource(R.string.player_tools_export)) } },
    )
}

@Composable
internal fun SubtitleListPanel(
    cues: List<Cue>,
    currentCue: Cue?,
    noSpoiler: Boolean,
    onNoSpoiler: () -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onClose: () -> Unit = {},
) {
    var query by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val currentIndex = cues.indexOfFirst { it.id == currentCue?.id }.takeIf { it >= 0 } ?: 0
    val visible = cues.withIndex().filter { (index, cue) ->
        (!noSpoiler || currentCue == null || index <= currentIndex) && (query.isBlank() || cue.text.contains(query, ignoreCase = true))
    }
    LaunchedEffect(currentCue?.id, query, noSpoiler) {
        val currentVisible = visible.indexOfFirst { it.value.id == currentCue?.id }
        if (currentVisible >= 0) listState.animateScrollToItem(currentVisible)
    }
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(SubLearnRadii.large),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.fillMaxSize().padding(SubLearnSpacing.md), verticalArrangement = Arrangement.spacedBy(SubLearnSpacing.sm)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.player_subtitle_list), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                FilterChip(selected = noSpoiler, onClick = onNoSpoiler, label = { Text(stringResource(R.string.player_list_no_spoiler)) })
                IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.player_close)) }
            }
            OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.player_list_search)) }, singleLine = true)
            LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(SubLearnSpacing.xs)) {
                itemsIndexed(visible, key = { _, item -> "${item.value.trackId}-${item.value.id}" }) { _, indexed ->
                    val cue = indexed.value
                    val active = cue.id == currentCue?.id
                    Card(
                        onClick = { onSeek(cue.startMs) },
                        colors = CardDefaults.cardColors(containerColor = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)),
                        shape = RoundedCornerShape(SubLearnRadii.small),
                    ) {
                        Column(Modifier.fillMaxWidth().padding(SubLearnSpacing.md), verticalArrangement = Arrangement.spacedBy(SubLearnSpacing.xs)) {
                            Text(formatTime(cue.startMs), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Text(cue.text, style = MaterialTheme.typography.bodyLarge, maxLines = 4)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun decoderLabel(mode: DecoderMode): String = when (mode) {
    DecoderMode.AUTO -> stringResource(R.string.player_decoder_auto)
    DecoderMode.HARDWARE -> stringResource(R.string.player_decoder_hw)
    DecoderMode.SOFTWARE -> stringResource(R.string.player_decoder_sw)
    DecoderMode.HARDWARE_PLUS -> stringResource(R.string.player_decoder_hw_plus)
}

private fun speedText(speed: Float) = if (speed % 1f == 0f) speed.toInt().toString() else speed.toString()

private fun formatTime(ms: Long): String {
    val seconds = (ms.coerceAtLeast(0) / 1_000)
    return if (seconds >= 3_600) String.format(Locale.ROOT, "%d:%02d:%02d", seconds / 3_600, seconds / 60 % 60, seconds % 60)
    else String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60)
}
