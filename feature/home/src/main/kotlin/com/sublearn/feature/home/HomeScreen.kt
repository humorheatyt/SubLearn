package com.sublearn.feature.home

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowOutward
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.sublearn.design.SubLearnAlpha
import com.sublearn.design.SubLearnColors
import com.sublearn.design.SubLearnRadii
import com.sublearn.design.SubLearnSpacing
import com.sublearn.domain.AppSettings
import com.sublearn.domain.RecentMedia
import java.util.Locale

@Composable
fun HomeScreen(
    recentMedia: List<RecentMedia>,
    settings: AppSettings,
    onChooseVideo: () -> Unit,
    onChooseFolder: () -> Unit,
    onOpenMedia: (RecentMedia) -> Unit,
    onOpenUrl: (String) -> Unit,
    onRemoveRecent: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    var showUrlDialog by remember { mutableStateOf(false) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = SubLearnSpacing.screen, vertical = SubLearnSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(SubLearnSpacing.lg),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.home_eyebrow), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                IconButton(onClick = onOpenSettings, modifier = Modifier.size(SubLearnSpacing.target)) {
                    Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.home_settings))
                }
            }
        }
        item {
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(SubLearnRadii.large))
                    .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.surfaceVariant)))
                    .padding(SubLearnSpacing.xl),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(SubLearnSpacing.md)) {
                    Text(stringResource(R.string.home_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.home_subtitle), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(SubLearnSpacing.sm)) {
                        Button(onClick = onChooseVideo, modifier = Modifier.height(SubLearnSpacing.target), shape = RoundedCornerShape(SubLearnRadii.pill)) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                            Spacer(Modifier.width(SubLearnSpacing.xs))
                            Text(stringResource(R.string.home_open_video))
                        }
                        OutlinedButton(onClick = { showUrlDialog = true }, modifier = Modifier.height(SubLearnSpacing.target), shape = RoundedCornerShape(SubLearnRadii.pill)) {
                            Icon(Icons.Rounded.ArrowOutward, contentDescription = null)
                            Spacer(Modifier.width(SubLearnSpacing.xs))
                            Text(stringResource(R.string.home_open_url))
                        }
                    }
                    OutlinedButton(onClick = onChooseFolder, modifier = Modifier.height(SubLearnSpacing.target), shape = RoundedCornerShape(SubLearnRadii.pill)) {
                        Icon(Icons.Rounded.FolderOpen, contentDescription = null)
                        Spacer(Modifier.width(SubLearnSpacing.xs))
                        Text(stringResource(R.string.home_open_folder))
                    }
                }
            }
        }
        if (recentMedia.isEmpty()) {
            item {
                EmptyLibraryCard(onChooseVideo = onChooseVideo)
            }
        } else {
            val continuing = recentMedia.firstOrNull { it.lastPositionMs > 0 && it.durationMs > it.lastPositionMs }
            if (continuing != null) {
                item {
                    Text(stringResource(R.string.home_continue), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    ContinueCard(media = continuing, onClick = { onOpenMedia(continuing) })
                }
            }
            item { Text(stringResource(R.string.home_recent), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }
            items(recentMedia, key = { it.uri }) { media ->
                RecentMediaCard(media, onClick = { onOpenMedia(media) }, onRemove = { onRemoveRecent(media.uri) })
            }
        }
    }

    if (showUrlDialog) {
        DirectUrlDialog(
            onDismiss = { showUrlDialog = false },
            onOpen = { url -> showUrlDialog = false; onOpenUrl(url) },
        )
    }
}

@Composable
private fun EmptyLibraryCard(onChooseVideo: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(SubLearnRadii.large),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = SubLearnAlpha.emptyCard)),
    ) {
        Row(Modifier.padding(SubLearnSpacing.xl), horizontalArrangement = Arrangement.spacedBy(SubLearnSpacing.lg), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(SubLearnSpacing.homeHeroIcon)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Movie, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(SubLearnSpacing.xs)) {
                Text(stringResource(R.string.home_empty_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.home_empty_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Button(onClick = onChooseVideo, modifier = Modifier.fillMaxWidth().padding(horizontal = SubLearnSpacing.lg, vertical = SubLearnSpacing.md)) {
            Text(stringResource(R.string.home_open_video))
        }
    }
}

@Composable
private fun ContinueCard(media: RecentMedia, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(SubLearnRadii.medium),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Row(Modifier.fillMaxWidth().padding(SubLearnSpacing.lg), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SubLearnSpacing.md)) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(SubLearnSpacing.continueIcon)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary) }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(SubLearnSpacing.xs)) {
                Text(media.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(formatDuration(media.lastPositionMs), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                androidx.compose.material3.LinearProgressIndicator(progress = { media.lastPositionMs.toFloat() / media.durationMs.coerceAtLeast(1) }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun RecentMediaCard(media: RecentMedia, onClick: () -> Unit, onRemove: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(SubLearnRadii.medium),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(Modifier.fillMaxWidth().padding(start = SubLearnSpacing.lg, top = SubLearnSpacing.md, bottom = SubLearnSpacing.md, end = SubLearnSpacing.xs), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(SubLearnRadii.small), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(SubLearnSpacing.mediaThumbnail)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Movie, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            }
            Spacer(Modifier.width(SubLearnSpacing.md))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(SubLearnSpacing.xxs)) {
                Text(media.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(formatDuration(media.lastPositionMs), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(SubLearnSpacing.target)) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = stringResource(R.string.home_remove), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DirectUrlDialog(onDismiss: () -> Unit, onOpen: (String) -> Unit) {
    var value by remember { mutableStateOf("") }
    var invalid by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.home_url_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(SubLearnSpacing.sm)) {
                OutlinedTextField(value = value, onValueChange = { value = it; invalid = false }, label = { Text(stringResource(R.string.home_url_hint)) }, singleLine = true)
                AnimatedVisibility(visible = invalid) { Text(stringResource(R.string.home_url_invalid), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val normalized = value.trim()
                val uri = runCatching { Uri.parse(normalized) }.getOrNull()
                if (uri?.scheme in setOf("http", "https") && !uri.host.isNullOrBlank()) onOpen(normalized) else invalid = true
            }) { Text(stringResource(R.string.home_url_open)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.home_url_cancel)) } },
    )
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0) / 1_000)
    val hours = totalSeconds / 3_600
    val minutes = totalSeconds / 60 % 60
    val seconds = totalSeconds % 60
    return if (hours > 0) String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    else String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
}
