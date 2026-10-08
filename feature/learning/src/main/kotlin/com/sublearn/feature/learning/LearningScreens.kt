package com.sublearn.feature.learning

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.animation.core.tween
import com.sublearn.design.SubLearnAlpha
import com.sublearn.design.SubLearnMotion
import com.sublearn.design.SubLearnElevation
import com.sublearn.design.SubLearnRadii
import com.sublearn.design.SubLearnSpacing
import com.sublearn.domain.AppSettings
import com.sublearn.domain.SavedWord
import com.sublearn.domain.SurfaceFontSettings
import com.sublearn.domain.TextDirection as DomainTextDirection
import com.sublearn.design.withSurfaceFont

@Composable
fun MyWordsScreen(
    words: List<SavedWord>,
    settings: AppSettings,
    query: String,
    onQueryChange: (String) -> Unit,
    onMarkKnown: (SavedWord, Boolean) -> Unit,
    onRemove: (Long) -> Unit,
) {
    Column(Modifier.padding(horizontal = SubLearnSpacing.screen, vertical = SubLearnSpacing.lg), verticalArrangement = Arrangement.spacedBy(SubLearnSpacing.lg)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(verticalArrangement = Arrangement.spacedBy(SubLearnSpacing.xs)) {
                Text(stringResource(R.string.words_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.words_word_count, words.size), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Rounded.Bookmark, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.words_search)) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(SubLearnRadii.medium),
        )
        if (words.isEmpty()) {
            Column(Modifier.fillMaxWidth().padding(top = SubLearnSpacing.xxl), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(SubLearnSpacing.sm)) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(SubLearnSpacing.iconHero)) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Bookmark, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                }
                Text(stringResource(R.string.words_empty_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.words_empty_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(SubLearnSpacing.md)) {
                items(words, key = { it.id }) { word ->
                    SavedWordCard(word, settings, onMarkKnown = { onMarkKnown(word, it) }, onRemove = { onRemove(word.id) })
                }
            }
        }
    }
}

@Composable
fun WordTranslationCard(
    word: String,
    translation: String,
    context: String,
    sourceLanguageTag: String,
    targetLanguageTag: String,
    settings: AppSettings,
    isSaved: Boolean,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth().shadow(SubLearnElevation.wordCard, RoundedCornerShape(SubLearnRadii.large)),
        shape = RoundedCornerShape(SubLearnRadii.large),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            Modifier.background(Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.primaryContainer.copy(alpha = SubLearnAlpha.cardAccent), MaterialTheme.colorScheme.surface)))
                .padding(SubLearnSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(SubLearnSpacing.sm),
        ) {
            val sourceFont = wordCardFont(settings, sourceLanguageTag)
            val targetFont = wordCardFont(settings, targetLanguageTag)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                SurfaceLanguageText(
                    word,
                    MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.primary),
                    sourceFont,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onSave, enabled = !isSaved, modifier = Modifier.size(SubLearnSpacing.target)) {
                    Icon(Icons.Rounded.Bookmark, contentDescription = stringResource(if (isSaved) R.string.words_saved else R.string.words_bookmark), tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            SurfaceLanguageText(translation, MaterialTheme.typography.titleMedium, targetFont)
            if (context.isNotBlank()) SurfaceLanguageText(context, MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant), sourceFont, maxLines = 2, overflow = TextOverflow.Ellipsis)
            androidx.compose.material3.TextButton(onClick = {}, enabled = false) { Text(stringResource(R.string.words_full_details_coming)) }
        }
    }
}

@Composable
fun LearningPopupStack(words: List<Pair<String, String>>, settings: AppSettings, modifier: Modifier = Modifier) {
    val learningFont = settings.surfaceFonts["popup.learning"] ?: SurfaceFontSettings()
    val nativeFont = settings.surfaceFonts["popup.native"] ?: SurfaceFontSettings()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(SubLearnSpacing.sm)) {
        words.forEach { pair ->
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(animationSpec = tween(SubLearnMotion.popupEnterMs)) { it / 2 } + fadeIn(tween(SubLearnMotion.popupEnterMs)),
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth().shadow(SubLearnElevation.popup, RoundedCornerShape(SubLearnRadii.medium)),
                    shape = RoundedCornerShape(SubLearnRadii.medium),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = settings.learningPopupOpacity)),
                ) {
                    Column(Modifier.padding(horizontal = SubLearnSpacing.md, vertical = SubLearnSpacing.sm), verticalArrangement = Arrangement.spacedBy(SubLearnSpacing.xxs)) {
                        SurfaceLanguageText(pair.first, MaterialTheme.typography.titleSmall.copy(color = MaterialTheme.colorScheme.primary), learningFont)
                        SurfaceLanguageText(pair.second, MaterialTheme.typography.bodySmall, nativeFont)
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedWordCard(word: SavedWord, settings: AppSettings, onMarkKnown: (Boolean) -> Unit, onRemove: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(SubLearnRadii.large),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(SubLearnSpacing.lg), verticalArrangement = Arrangement.spacedBy(SubLearnSpacing.sm)) {
            val sourceFont = wordCardFont(settings, word.languageTag)
            val targetFont = wordCardFont(settings, word.nativeLanguageTag)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(SubLearnSpacing.xs)) {
                    SurfaceLanguageText(word.text, MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.primary), sourceFont)
                    SurfaceLanguageText(word.translation, MaterialTheme.typography.titleMedium, targetFont)
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(SubLearnSpacing.target)) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = stringResource(R.string.words_remove), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (word.context.isNotBlank()) {
                SurfaceLanguageText(word.context, MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant), sourceFont, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Text(word.mediaTitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.words_known), style = MaterialTheme.typography.bodyMedium)
                Switch(checked = word.isKnown, onCheckedChange = onMarkKnown)
            }
        }
    }
}

private fun wordCardFont(settings: AppSettings, languageTag: String): SurfaceFontSettings {
    val source = languageTag.substringBefore('-').lowercase()
    val learning = settings.learningLanguageTag.substringBefore('-').lowercase()
    val role = if (source == learning) "learning" else "native"
    return settings.surfaceFonts["word-card.$role"] ?: SurfaceFontSettings()
}

@Composable
private fun SurfaceLanguageText(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    font: SurfaceFontSettings,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    val layoutDirection = when (font.direction) {
        DomainTextDirection.RTL -> LayoutDirection.Rtl
        DomainTextDirection.LTR -> LayoutDirection.Ltr
        DomainTextDirection.AUTO -> LocalLayoutDirection.current
    }
    androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Text(
            text = text,
            modifier = modifier,
            style = style.withSurfaceFont(font),
            maxLines = maxLines,
            overflow = overflow,
        )
    }
}
