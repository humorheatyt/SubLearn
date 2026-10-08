package com.sublearn.feature.settings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.sublearn.design.SubLearnRadii
import com.sublearn.design.SubLearnSpacing
import com.sublearn.domain.AiProviderFactory
import com.sublearn.domain.AppSettings
import com.sublearn.domain.DockMode
import com.sublearn.domain.GestureAction
import com.sublearn.domain.LearningMode
import com.sublearn.domain.SecretStore
import com.sublearn.domain.SurfaceFontSettings
import com.sublearn.domain.TextDirection
import com.sublearn.domain.ThemeMode
import com.sublearn.domain.TranslationProvider
import com.sublearn.domain.WordMarkStyle
import com.sublearn.domain.WordStyleSettings
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    settings: AppSettings,
    secretStore: SecretStore,
    providerFactory: AiProviderFactory,
    translationProvider: TranslationProvider,
    onUpdate: (AppSettings) -> Unit,
    onExportJson: () -> Unit,
    onImportJson: () -> Unit,
    onBack: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = SubLearnSpacing.screen, vertical = SubLearnSpacing.md),
        verticalArrangement = Arrangement.spacedBy(SubLearnSpacing.md),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.padding(end = SubLearnSpacing.xs)) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = stringResource(R.string.settings_title))
                }
                Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = onExportJson) { Icon(Icons.Rounded.FileDownload, contentDescription = stringResource(R.string.settings_export)) }
                IconButton(onClick = onImportJson) { Icon(Icons.Rounded.FileUpload, contentDescription = stringResource(R.string.settings_import)) }
            }
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.settings_search)) },
                singleLine = true,
                shape = RoundedCornerShape(SubLearnRadii.medium),
            )
        }

        if (matches(query, "appearance", "theme", "language", "color", "dynamic")) item {
            SettingsSection(stringResource(R.string.category_appearance)) {
                ChoiceSetting(
                    title = stringResource(R.string.theme),
                    options = listOf(
                        ThemeMode.SYSTEM to stringResource(R.string.theme_system),
                        ThemeMode.LIGHT to stringResource(R.string.theme_light),
                        ThemeMode.DARK to stringResource(R.string.theme_dark),
                        ThemeMode.AMOLED to stringResource(R.string.theme_amoled),
                    ),
                    selected = settings.themeMode,
                    onSelect = { onUpdate(settings.copy(themeMode = it)) },
                )
                SwitchSetting(stringResource(R.string.dynamic_color), settings.dynamicColor) { onUpdate(settings.copy(dynamicColor = it)) }
                ChoiceSetting(
                    title = stringResource(R.string.app_language),
                    options = listOf("en" to stringResource(R.string.language_english), "fa" to stringResource(R.string.language_persian)),
                    selected = settings.uiLanguageTag,
                    onSelect = { onUpdate(settings.copy(uiLanguageTag = it)) },
                )
                Text(stringResource(R.string.english_default_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (matches(query, "player", "controls", "orientation", "speed", "aspect", "double tap")) item {
            SettingsSection(stringResource(R.string.category_player)) {
                SliderSetting(
                    stringResource(R.string.control_hide), settings.autoHideControlsMs / 1_000f, 1f..15f, 13,
                    valueText = "${settings.autoHideControlsMs / 1_000f} s",
                    onValueChange = { onUpdate(settings.copy(autoHideControlsMs = (it * 1_000).roundToInt().toLong())) },
                )
                SwitchSetting(stringResource(R.string.rotation_lock), settings.rotationLocked) { onUpdate(settings.copy(rotationLocked = it)) }
                ChoiceSetting(
                    stringResource(R.string.double_tap),
                    listOf(
                        GestureAction.PAUSE_PLAY to stringResource(R.string.gesture_action_pause),
                        GestureAction.SEEK_BACK to stringResource(R.string.gesture_action_back),
                        GestureAction.SEEK_FORWARD to stringResource(R.string.gesture_action_forward),
                    ),
                    settings.gestureBindings["double-tap-video"] ?: settings.doubleTapAction,
                ) {
                    onUpdate(settings.copy(doubleTapAction = it, gestureBindings = settings.gestureBindings + ("double-tap-video" to it)))
                }
                ChoiceSetting(
                    stringResource(R.string.aspect_ratio),
                    listOf(0 to stringResource(R.string.aspect_fit), 1 to stringResource(R.string.aspect_fill), 2 to stringResource(R.string.aspect_zoom)),
                    settings.playbackAspectRatioMode,
                ) { onUpdate(settings.copy(playbackAspectRatioMode = it)) }
            }
        }

        if (matches(query, "subtitle", "caption", "delay", "position", "quick", "dock", "spoiler", "characters", "break")) item {
            SettingsSection(stringResource(R.string.category_subtitles)) {
                SubtitleLayerSettings(
                    title = stringResource(R.string.learning_subtitles),
                    visible = settings.subtitlesLearningVisible,
                    size = settings.learningSubtitleSizeSp,
                    alpha = settings.learningSubtitleAlpha,
                    delay = settings.learningSubtitleDelayMs,
                    position = settings.learningSubtitlePosition,
                    buttonSize = settings.learningButtonSizeDp,
                    buttonAlpha = settings.learningButtonAlpha,
                    buttonPosition = settings.learningButtonPosition,
                    onVisible = { onUpdate(settings.copy(subtitlesLearningVisible = it)) },
                    onSize = { onUpdate(settings.copy(learningSubtitleSizeSp = it)) },
                    onAlpha = { onUpdate(settings.copy(learningSubtitleAlpha = it)) },
                    onDelay = { onUpdate(settings.copy(learningSubtitleDelayMs = it)) },
                    onPosition = { onUpdate(settings.copy(learningSubtitlePosition = it)) },
                    onButtonSize = { onUpdate(settings.copy(learningButtonSizeDp = it)) },
                    onButtonAlpha = { onUpdate(settings.copy(learningButtonAlpha = it)) },
                    onButtonPosition = { onUpdate(settings.copy(learningButtonPosition = it)) },
                )
                HorizontalDivider()
                SubtitleLayerSettings(
                    title = stringResource(R.string.translation_subtitles),
                    visible = settings.subtitlesNativeVisible,
                    size = settings.nativeSubtitleSizeSp,
                    alpha = settings.nativeSubtitleAlpha,
                    delay = settings.nativeSubtitleDelayMs,
                    position = settings.nativeSubtitlePosition,
                    buttonSize = settings.nativeButtonSizeDp,
                    buttonAlpha = settings.nativeButtonAlpha,
                    buttonPosition = settings.nativeButtonPosition,
                    onVisible = { onUpdate(settings.copy(subtitlesNativeVisible = it)) },
                    onSize = { onUpdate(settings.copy(nativeSubtitleSizeSp = it)) },
                    onAlpha = { onUpdate(settings.copy(nativeSubtitleAlpha = it)) },
                    onDelay = { onUpdate(settings.copy(nativeSubtitleDelayMs = it)) },
                    onPosition = { onUpdate(settings.copy(nativeSubtitlePosition = it)) },
                    onButtonSize = { onUpdate(settings.copy(nativeButtonSizeDp = it)) },
                    onButtonAlpha = { onUpdate(settings.copy(nativeButtonAlpha = it)) },
                    onButtonPosition = { onUpdate(settings.copy(nativeButtonPosition = it)) },
                )
                ChoiceSetting(
                    stringResource(R.string.quick_action_dock),
                    listOf(
                        DockMode.QUICK_COLUMN to stringResource(R.string.dock_column),
                        DockMode.BOTTOM_BAR to stringResource(R.string.dock_bar),
                        DockMode.FLOATING to stringResource(R.string.dock_float),
                        DockMode.HIDDEN to stringResource(R.string.dock_hidden),
                    ), settings.quickActionDockMode,
                ) { onUpdate(settings.copy(quickActionDockMode = it)) }
                SliderSetting(
                    stringResource(R.string.subtitle_max_chars), settings.subtitleMaxCharacters.toFloat(), 20f..240f, 43,
                    valueText = settings.subtitleMaxCharacters.toString(),
                    onValueChange = { onUpdate(settings.copy(subtitleMaxCharacters = it.roundToInt())) },
                )
                SwitchSetting(stringResource(R.string.flatten_line_breaks), settings.flattenLineBreaks) { onUpdate(settings.copy(flattenLineBreaks = it)) }
                SwitchSetting(stringResource(R.string.no_spoiler), settings.noSpoilerList) { onUpdate(settings.copy(noSpoilerList = it)) }
            }
        }

        if (matches(query, "font", "family", "size", "weight", "color", "direction", "surface", "learning language", "translation")) item {
            SettingsSection(stringResource(R.string.category_fonts)) {
                FontSurfaces(settings = settings, onUpdate = onUpdate)
            }
        }

        if (matches(query, "gesture", "swipe", "tap", "brightness", "volume", "speed")) item {
            SettingsSection(stringResource(R.string.category_gestures)) {
                GestureSetting("tap-video", stringResource(R.string.gesture_tap), settings, onUpdate)
                GestureSetting("double-tap-video", stringResource(R.string.gesture_double), settings, onUpdate)
                GestureSetting("swipe-video-horizontal", stringResource(R.string.gesture_horizontal), settings, onUpdate)
                GestureSetting("swipe-video-left-vertical", stringResource(R.string.gesture_left_vertical), settings, onUpdate)
                GestureSetting("swipe-video-right-vertical", stringResource(R.string.gesture_right_vertical), settings, onUpdate)
                GestureSetting("two-finger-up", stringResource(R.string.gesture_two_finger), settings, onUpdate)
            }
        }

        if (matches(query, "shadowing", "repeat", "pause", "block")) item {
            SettingsSection(stringResource(R.string.category_shadowing)) {
                SliderSetting(stringResource(R.string.repeat_count), settings.repeatCount.toFloat(), 1f..10f, 8, settings.repeatCount.toString()) {
                    onUpdate(settings.copy(repeatCount = it.roundToInt()))
                }
                SliderSetting(stringResource(R.string.repeat_pause), settings.repeatPauseMs.toFloat(), 0f..3_000f, 29, settings.repeatPauseMs.toString()) {
                    onUpdate(settings.copy(repeatPauseMs = it.roundToInt().toLong()))
                }
                SliderSetting(stringResource(R.string.repeat_multiplier), settings.repeatPauseMultiplier, 0f..1.5f, 14, "${(settings.repeatPauseMultiplier * 100).roundToInt()}%") {
                    onUpdate(settings.copy(repeatPauseMultiplier = it))
                }
                SwitchSetting(stringResource(R.string.stop_at_end), settings.stopAtBlockEnd) { onUpdate(settings.copy(stopAtBlockEnd = it)) }
            }
        }

        if (matches(query, "learning", "level", "entertainment", "known", "style", "word", "lookup")) item {
            SettingsSection(stringResource(R.string.category_learning)) {
                ChoiceSetting(
                    stringResource(R.string.learning_mode),
                    listOf(LearningMode.ENTERTAINMENT to stringResource(R.string.mode_entertainment), LearningMode.LEARNING to stringResource(R.string.mode_learning)),
                    settings.learningMode,
                ) { onUpdate(settings.copy(learningMode = it)) }
                ChoiceSetting(stringResource(R.string.manual_level), listOf("A1", "A2", "B1", "B2", "C1", "C2").map { it to it }, settings.manualLevel) {
                    onUpdate(settings.copy(manualLevel = it))
                }
                SliderSetting(stringResource(R.string.popup_limit), settings.manualLevelPopupLimit.toFloat(), 1f..8f, 6, settings.manualLevelPopupLimit.toString()) {
                    onUpdate(settings.copy(manualLevelPopupLimit = it.roundToInt()))
                }
                SliderSetting(stringResource(R.string.learning_popup_opacity), settings.learningPopupOpacity, 0.2f..1f, 15, "${(settings.learningPopupOpacity * 100).roundToInt()}%") {
                    onUpdate(settings.copy(learningPopupOpacity = it))
                }
                ChoiceSetting(
                    stringResource(R.string.default_lookup),
                    listOf("mlkit" to stringResource(R.string.lookup_translation)),
                    "mlkit",
                ) { onUpdate(settings.copy(defaultLookupTarget = it)) }
                Text(stringResource(R.string.lookup_dictionary), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TranslationModelSetting(translationProvider)
                WordStyleSetting(stringResource(R.string.my_words_style), settings.myWordsStyle, true) { onUpdate(settings.copy(myWordsStyle = it)) }
                WordStyleSetting(stringResource(R.string.known_word_style), settings.knownWordStyle, true) { onUpdate(settings.copy(knownWordStyle = it)) }
                WordStyleSetting(stringResource(R.string.pos_style), settings.partOfSpeechStyle, false) { onUpdate(settings.copy(partOfSpeechStyle = it)) }
                WordStyleSetting(stringResource(R.string.phrase_style), settings.phraseStyle, false) { onUpdate(settings.copy(phraseStyle = it)) }
            }
        }

        if (matches(query, "ai", "provider", "key", "prompt", "context", "gemini", "chatgpt", "claude")) item {
            SettingsSection(stringResource(R.string.category_ai)) {
                AiSettings(settings, secretStore, providerFactory, onUpdate)
            }
        }

        if (matches(query, "dictionary", "import", "sqlite", "offline")) item {
            SettingsSection(stringResource(R.string.category_dictionary)) {
                Text(stringResource(R.string.coming_soon), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.dictionary_note), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (matches(query, "about", "version", "license", "privacy", "export", "import")) item {
            SettingsSection(stringResource(R.string.category_about)) {
                Text(stringResource(R.string.about_release), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.about_languages), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.settings_info), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(SubLearnSpacing.sm)) {
                    Button(onClick = onExportJson) { Text(stringResource(R.string.settings_export)) }
                    Button(onClick = onImportJson) { Text(stringResource(R.string.settings_import)) }
                }
            }
        }
    }
}

@Composable
private fun SubtitleLayerSettings(
    title: String,
    visible: Boolean,
    size: Float,
    alpha: Float,
    delay: Long,
    position: Float,
    buttonSize: Float,
    buttonAlpha: Float,
    buttonPosition: Float,
    onVisible: (Boolean) -> Unit,
    onSize: (Float) -> Unit,
    onAlpha: (Float) -> Unit,
    onDelay: (Long) -> Unit,
    onPosition: (Float) -> Unit,
    onButtonSize: (Float) -> Unit,
    onButtonAlpha: (Float) -> Unit,
    onButtonPosition: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(SubLearnSpacing.xs)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        SwitchSetting(stringResource(R.string.visible), visible, onVisible)
        SliderSetting(stringResource(R.string.text_size), size, 14f..48f, 16, "${size.roundToInt()} sp", onSize)
        SliderSetting(stringResource(R.string.opacity), alpha, 0.2f..1f, 15, "${(alpha * 100).roundToInt()}%", onAlpha)
        SliderSetting(stringResource(R.string.subtitle_delay), delay.toFloat(), -5_000f..5_000f, 199, delay.toString()) { onDelay(it.roundToInt().toLong()) }
        SliderSetting(stringResource(R.string.subtitle_position), position, 0.08f..0.96f, 21, "${(position * 100).roundToInt()}%", onPosition)
        Text(stringResource(R.string.toggle_button_settings), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SliderSetting(stringResource(R.string.button_size), buttonSize, 36f..80f, 10, "${buttonSize.roundToInt()} dp", onButtonSize)
        SliderSetting(stringResource(R.string.opacity), buttonAlpha, 0.3f..1f, 13, "${(buttonAlpha * 100).roundToInt()}%", onButtonAlpha)
        SliderSetting(stringResource(R.string.subtitle_position), buttonPosition, 0.08f..0.92f, 21, "${(buttonPosition * 100).roundToInt()}%", onButtonPosition)
    }
}

@Composable
private fun FontSurfaces(settings: AppSettings, onUpdate: (AppSettings) -> Unit) {
    val surfaces = listOf(
        "menu.app" to stringResource(R.string.font_surface_menu),
        "subtitle.learning" to stringResource(R.string.font_surface_learning),
        "subtitle.native" to stringResource(R.string.font_surface_native),
        "popup.learning" to stringResource(R.string.font_surface_popup_learning),
        "popup.native" to stringResource(R.string.font_surface_popup_native),
        "word-card.learning" to stringResource(R.string.font_surface_word_learning),
        "word-card.native" to stringResource(R.string.font_surface_word_native),
        "ai.answer" to stringResource(R.string.font_surface_ai),
    )
    surfaces.forEach { (key, label) ->
        val font = settings.surfaceFonts[key] ?: SurfaceFontSettings()
        Column(verticalArrangement = Arrangement.spacedBy(SubLearnSpacing.xs)) {
            Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            ChoiceSetting(
                stringResource(R.string.font_family),
                listOf("system" to stringResource(R.string.font_system), "serif" to stringResource(R.string.font_serif), "monospace" to stringResource(R.string.font_monospace)),
                font.family,
            ) { value -> updateFont(settings, key, font.copy(family = value), onUpdate) }
            SliderSetting(stringResource(R.string.text_size), font.sizeSp, 10f..56f, 22, "${font.sizeSp.roundToInt()} sp") {
                updateFont(settings, key, font.copy(sizeSp = it), onUpdate)
            }
            SliderSetting(stringResource(R.string.font_weight), font.weight.toFloat(), 100f..900f, 7, font.weight.toString()) {
                updateFont(settings, key, font.copy(weight = it.roundToInt()), onUpdate)
            }
            ChoiceSetting(
                stringResource(R.string.font_direction),
                listOf(TextDirection.AUTO to stringResource(R.string.direction_auto), TextDirection.LTR to stringResource(R.string.direction_ltr), TextDirection.RTL to stringResource(R.string.direction_rtl)),
                font.direction,
            ) { updateFont(settings, key, font.copy(direction = it), onUpdate) }
            ColorChoice(font.colorArgb) { color -> updateFont(settings, key, font.copy(colorArgb = color), onUpdate) }
        }
        HorizontalDivider()
    }
}

private fun updateFont(settings: AppSettings, key: String, font: SurfaceFontSettings, onUpdate: (AppSettings) -> Unit) {
    onUpdate(settings.copy(surfaceFonts = settings.surfaceFonts + (key to font)))
}

@Composable
private fun ColorChoice(selected: Long?, onSelect: (Long?) -> Unit) {
    val palette = listOf(null, 0xFFFFC857, 0xFF69D0BE, 0xFF9FA8FF, 0xFFFFFFFF, 0xFF1D1D1D)
    Column(verticalArrangement = Arrangement.spacedBy(SubLearnSpacing.xxs)) {
        Text(stringResource(R.string.font_color), style = MaterialTheme.typography.labelLarge)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(SubLearnSpacing.xs)) {
            palette.forEach { color ->
                FilterChip(selected = selected == color, onClick = { onSelect(color) }, label = { Text(color?.let { "#%06X".format(it and 0xFFFFFF) } ?: stringResource(R.string.font_system)) })
            }
        }
    }
}

@Composable
private fun GestureSetting(key: String, title: String, settings: AppSettings, onUpdate: (AppSettings) -> Unit) {
    val actions = listOf(
        GestureAction.NONE to stringResource(R.string.gesture_action_none),
        GestureAction.TOGGLE_CONTROLS to stringResource(R.string.gesture_action_controls),
        GestureAction.PAUSE_PLAY to stringResource(R.string.gesture_action_pause),
        GestureAction.SEEK_BACK to stringResource(R.string.gesture_action_back),
        GestureAction.SEEK_FORWARD to stringResource(R.string.gesture_action_forward),
        GestureAction.VOLUME to stringResource(R.string.gesture_action_volume),
        GestureAction.BRIGHTNESS to stringResource(R.string.gesture_action_brightness),
        GestureAction.SPEED_CYCLE to stringResource(R.string.gesture_action_speed),
    )
    ChoiceSetting(title, actions, settings.gestureBindings[key] ?: GestureAction.NONE) { action ->
        onUpdate(settings.copy(gestureBindings = settings.gestureBindings + (key to action), doubleTapAction = if (key == "double-tap-video") action else settings.doubleTapAction))
    }
}

@Composable
private fun WordStyleSetting(title: String, value: WordStyleSettings, analyzerAvailable: Boolean, onChange: (WordStyleSettings) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(SubLearnSpacing.xs)) {
        if (analyzerAvailable) {
            SwitchSetting(title, value.enabled) { onChange(value.copy(enabled = it)) }
            ChoiceSetting(
                title = stringResource(R.string.word_style),
                options = listOf(
                    WordMarkStyle.UNDERLINE to stringResource(R.string.style_underline),
                    WordMarkStyle.DOTTED_UNDERLINE to stringResource(R.string.style_dotted),
                    WordMarkStyle.OUTLINE to stringResource(R.string.style_outline),
                    WordMarkStyle.BACKGROUND to stringResource(R.string.style_background),
                    WordMarkStyle.BOLD to stringResource(R.string.style_bold),
                    WordMarkStyle.COLOR to stringResource(R.string.style_color),
                ), selected = value.style,
            ) { onChange(value.copy(style = it)) }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.bodyMedium)
                Switch(checked = value.enabled, onCheckedChange = {}, enabled = false)
            }
            Text(stringResource(R.string.coming_soon), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AiSettings(settings: AppSettings, secretStore: SecretStore, providerFactory: AiProviderFactory, onUpdate: (AppSettings) -> Unit) {
    val scope = rememberCoroutineScope()
    val saveSuccess = stringResource(R.string.ai_key_save_success)
    val saveFailure = stringResource(R.string.ai_key_save_failure)
    val removeSuccess = stringResource(R.string.ai_key_remove_success)
    val removeFailure = stringResource(R.string.ai_key_remove_failure)
    var hasKey by remember(settings.aiProviderId) { mutableStateOf(false) }
    var keyValue by remember { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    LaunchedEffect(settings.aiProviderId) { hasKey = runCatching { secretStore.read(settings.aiProviderId) != null }.getOrDefault(false) }
    val providers = providerFactory.supportedProviders()
    ChoiceSetting(
        stringResource(R.string.ai_provider),
        providers.map { (id, _) -> id to providerLabel(id) },
        settings.aiProviderId,
    ) { id ->
        onUpdate(settings.copy(aiProviderId = id, aiModel = defaultModel(id)))
    }
    OutlinedTextField(
        value = settings.aiModel,
        onValueChange = { onUpdate(settings.copy(aiModel = it)) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.ai_model)) },
        singleLine = true,
    )
    OutlinedTextField(
        value = settings.aiPrompt,
        onValueChange = { onUpdate(settings.copy(aiPrompt = it)) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.ai_prompt)) },
        minLines = 4,
        maxLines = 10,
    )
    SliderSetting(stringResource(R.string.ai_context), settings.aiContextBlockCount.toFloat(), 0f..30f, 29, settings.aiContextBlockCount.toString()) {
        onUpdate(settings.copy(aiContextBlockCount = it.roundToInt()))
    }
    SwitchSetting(stringResource(R.string.ai_include_title), settings.aiIncludeFilmTitle) { onUpdate(settings.copy(aiIncludeFilmTitle = it)) }
    SwitchSetting(stringResource(R.string.ai_include_timestamps), settings.aiIncludeTimestamps) { onUpdate(settings.copy(aiIncludeTimestamps = it)) }
    OutlinedTextField(
        value = keyValue,
        onValueChange = { keyValue = it },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.ai_key_placeholder)) },
        visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        singleLine = true,
        trailingIcon = { TextButton(onClick = { showKey = !showKey }) { Text(stringResource(if (showKey) R.string.settings_hide else R.string.settings_show)) } },
    )
    Row(horizontalArrangement = Arrangement.spacedBy(SubLearnSpacing.sm)) {
        Button(onClick = {
            val secret = keyValue
            if (secret.isNotBlank()) scope.launch {
                val saved = runCatching { secretStore.write(settings.aiProviderId, secret) }.isSuccess
                keyValue = ""
                hasKey = saved
                message = if (saved) saveSuccess else saveFailure
            }
        }, enabled = keyValue.isNotBlank()) { Text(stringResource(R.string.ai_key_save)) }
        if (hasKey) TextButton(onClick = {
            scope.launch {
                val removed = runCatching { secretStore.delete(settings.aiProviderId) }.isSuccess
                hasKey = !removed
                message = if (removed) removeSuccess else removeFailure
            }
        }) { Text(stringResource(R.string.ai_key_remove)) }
    }
    if (hasKey) Text(stringResource(R.string.ai_key_stored), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
    if (message.isNotBlank()) Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(stringResource(R.string.ai_privacy), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun TranslationModelSetting(translationProvider: TranslationProvider) {
    val scope = rememberCoroutineScope()
    val readyText = stringResource(R.string.translation_model_ready)
    val missingText = stringResource(R.string.translation_model_missing)
    val downloadText = stringResource(R.string.translation_model_download)
    val downloadingText = stringResource(R.string.translation_model_downloading)
    val successText = stringResource(R.string.translation_model_success)
    val errorText = stringResource(R.string.translation_model_error)
    var ready by remember { mutableStateOf<Boolean?>(null) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    LaunchedEffect(translationProvider) {
        ready = runCatching { translationProvider.isModelReady("en", "fa") }.getOrDefault(false)
    }
    Column(verticalArrangement = Arrangement.spacedBy(SubLearnSpacing.xs)) {
        Text(stringResource(R.string.translation_model_status), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(status.ifBlank { if (ready == true) readyText else missingText }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (ready != true) Button(enabled = !busy, onClick = {
            busy = true
            status = downloadingText
            scope.launch {
                val result = runCatching { translationProvider.downloadModel("en", "fa") }
                ready = result.isSuccess && runCatching { translationProvider.isModelReady("en", "fa") }.getOrDefault(false)
                status = if (ready == true) successText else errorText
                busy = false
            }
        }) { Text(if (busy) downloadingText else downloadText) }
    }
}

@Composable
private fun providerLabel(id: String): String = when (id) {
    "gemini" -> stringResource(R.string.ai_gemini)
    "openai" -> stringResource(R.string.ai_openai)
    "anthropic" -> stringResource(R.string.ai_anthropic)
    else -> id
}

private fun defaultModel(id: String): String = when (id) {
    "gemini" -> "gemini-2.0-flash"
    "openai" -> "gpt-4o-mini"
    "anthropic" -> "claude-3-5-haiku-latest"
    else -> ""
}

@Composable
private fun SwitchSetting(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f).padding(end = SubLearnSpacing.md))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun <T> ChoiceSetting(title: String, options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(SubLearnSpacing.xxs)) {
        Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(SubLearnSpacing.xs)) {
            options.forEach { (value, label) ->
                FilterChip(selected = selected == value, onClick = { onSelect(value) }, label = { Text(label) })
            }
        }
    }
}

@Composable
private fun SliderSetting(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueText: String,
    onValueChange: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(SubLearnSpacing.xxs)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(valueText, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        Slider(value = value.coerceIn(range.start, range.endInclusive), onValueChange = onValueChange, valueRange = range, steps = steps)
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(SubLearnRadii.large),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(SubLearnSpacing.lg), verticalArrangement = Arrangement.spacedBy(SubLearnSpacing.md)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

private fun matches(query: String, vararg terms: String): Boolean = query.isBlank() || terms.any { it.contains(query, ignoreCase = true) }
