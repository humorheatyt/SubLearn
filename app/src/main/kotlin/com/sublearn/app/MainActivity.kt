package com.sublearn.app

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sublearn.domain.AiProviderFactory
import com.sublearn.domain.AppSettings
import com.sublearn.domain.AppSettingsRepository
import com.sublearn.domain.MediaRequest
import com.sublearn.domain.RecentMediaRepository
import com.sublearn.domain.SavedWordRepository
import com.sublearn.domain.SecretStore
import com.sublearn.domain.SubtitleFileLoader
import com.sublearn.domain.SubtitleRepository
import com.sublearn.domain.TranslationProvider
import com.sublearn.design.SubLearnTheme
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.core.context.GlobalContext

class MainActivity : ComponentActivity() {
    private val settingsRepository: AppSettingsRepository by inject()
    private val recentMediaRepository: RecentMediaRepository by inject()
    private val savedWordRepository: SavedWordRepository by inject()
    private val subtitleRepository: SubtitleRepository by inject()
    private val subtitleFileLoader: SubtitleFileLoader by inject()
    private val translationProvider: TranslationProvider by inject()
    private val aiProviderFactory: AiProviderFactory by inject()
    private val secretStore: SecretStore by inject()

    private var incomingMedia by mutableStateOf<MediaRequest?>(null)
    private var incomingEventId by mutableIntStateOf(0)
    private var incomingPdfEventId by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) window.isNavigationBarContrastEnforced = false
        consumeIntent(intent)

        setContent {
            val context = LocalContext.current
            val persistedSettings by settingsRepository.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
            val scope = rememberCoroutineScope()
            var appSettings by remember { mutableStateOf(persistedSettings.normalized()) }
            var pendingSettingsWrite by remember { mutableStateOf(false) }
            var settingsWriteVersion by remember { mutableIntStateOf(0) }

            LaunchedEffect(persistedSettings, pendingSettingsWrite) {
                if (!pendingSettingsWrite) appSettings = persistedSettings.normalized()
            }
            LaunchedEffect(settingsWriteVersion) {
                if (!pendingSettingsWrite) return@LaunchedEffect
                delay(180)
                val version = settingsWriteVersion
                val value = appSettings.normalized()
                try {
                    settingsRepository.replace(value)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Throwable) {
                    Toast.makeText(this@MainActivity, R.string.app_settings_save_error, Toast.LENGTH_SHORT).show()
                }
                if (version == settingsWriteVersion) pendingSettingsWrite = false
            }
            val updateSettings: (AppSettings) -> Unit = { next ->
                appSettings = next.normalized()
                pendingSettingsWrite = true
                settingsWriteVersion++
            }

            val localeContext = remember(context, appSettings.uiLanguageTag) {
                val locale = Locale.forLanguageTag(appSettings.uiLanguageTag)
                val configuration = Configuration(context.resources.configuration).apply {
                    setLocale(locale)
                    setLayoutDirection(locale)
                }
                context.createConfigurationContext(configuration)
            }
            val rtl = appSettings.uiLanguageTag.substringBefore('-').equals("fa", ignoreCase = true)

            val systemDark = isSystemInDarkTheme()
            val lightSystemBars = appSettings.themeMode == com.sublearn.domain.ThemeMode.LIGHT || (appSettings.themeMode == com.sublearn.domain.ThemeMode.SYSTEM && !systemDark)
            SideEffect {
                WindowInsetsControllerCompat(window, window.decorView).apply {
                    isAppearanceLightStatusBars = lightSystemBars
                    isAppearanceLightNavigationBars = lightSystemBars
                }
            }

            CompositionLocalProvider(
                LocalContext provides localeContext,
                LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
            ) {
                SubLearnTheme(mode = appSettings.themeMode, dynamicColor = appSettings.dynamicColor) {
                    SubLearnApp(
                        settings = appSettings,
                        settingsRepository = settingsRepository,
                        recentMediaRepository = recentMediaRepository,
                        savedWordRepository = savedWordRepository,
                        subtitleRepository = subtitleRepository,
                        subtitleFileLoader = subtitleFileLoader,
                        translationProvider = translationProvider,
                        aiProviderFactory = aiProviderFactory,
                        secretStore = secretStore,
                        onSettingsChange = updateSettings,
                        incomingMedia = incomingMedia,
                        incomingEventId = incomingEventId,
                        onIncomingConsumed = { eventId -> if (eventId == incomingEventId) incomingMedia = null },
                        incomingPdfEventId = incomingPdfEventId,
                        onIncomingPdfConsumed = { eventId -> if (eventId == incomingPdfEventId) incomingPdfEventId = 0 },
                        playerControllerFactory = { GlobalContext.get().get() },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeIntent(intent)
    }

    private fun consumeIntent(intent: Intent?) {
        val request = intent?.toMediaRequest()
        if (intent?.isPdfLearningRequest() == true) {
            incomingPdfEventId++
        } else if (request != null) {
            incomingMedia = request
            incomingEventId++
        } else if (intent?.action != Intent.ACTION_MAIN) {
            Toast.makeText(this, com.sublearn.app.R.string.app_external_error, Toast.LENGTH_SHORT).show()
        }
    }

    private fun Intent.toMediaRequest(): MediaRequest? {
        val sharedText = if (action == Intent.ACTION_SEND && type == "text/plain") getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()?.trim() else null
        val uri = data ?: sharedUri() ?: sharedText?.let(::parseWebUri) ?: return null
        val scheme = uri.scheme?.lowercase(Locale.ROOT)
        if (scheme !in setOf("content", "file", "http", "https")) return null
        if (scheme == "content") runCatching { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        val remote = scheme == "http" || scheme == "https"
        val displayName = if (remote) null else queryDisplayName(uri)
        val title = displayName?.takeIf(String::isNotBlank)
            ?: uri.lastPathSegment?.substringAfterLast('/')?.takeIf(String::isNotBlank)
            ?: uri.host
            ?: getString(com.sublearn.app.R.string.app_name)
        val mime = if (remote) type?.takeIf { it.startsWith("video/") } else runCatching { contentResolver.getType(uri) }.getOrNull() ?: type
        val knownVideoExtension = title.substringAfterLast('.', "").lowercase(Locale.ROOT) in setOf("3gp", "avi", "flv", "m4v", "mkv", "mov", "mp4", "mpeg", "mpg", "mts", "ts", "webm", "wmv")
        if (!remote && mime?.startsWith("video/") != true && !knownVideoExtension) return null
        return MediaRequest(uri.toString(), title, mime)
    }

    @Suppress("DEPRECATION")
    private fun Intent.sharedUri(): Uri? {
        if (action != Intent.ACTION_SEND && action != Intent.ACTION_SEND_MULTIPLE) return null
        if (Build.VERSION.SDK_INT >= 33) {
            getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)?.let { return it }
            return getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)?.firstOrNull()
        }
        @Suppress("DEPRECATION")
        val single = getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        if (single != null) return single
        @Suppress("DEPRECATION")
        return getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)?.firstOrNull()
    }

    @Suppress("DEPRECATION")
    private fun Intent.isPdfLearningRequest(): Boolean {
        if (action != Intent.ACTION_VIEW && action != Intent.ACTION_SEND && action != Intent.ACTION_SEND_MULTIPLE) return false
        val candidate = data ?: sharedUri() ?: getStringExtra(Intent.EXTRA_TEXT)?.let { Uri.parse(it.trim()) }
        val mime = type ?: candidate?.let { runCatching { contentResolver.getType(it) }.getOrNull() }
        return mime.equals("application/pdf", ignoreCase = true) ||
            candidate?.lastPathSegment?.substringBefore('?')?.endsWith(".pdf", ignoreCase = true) == true
    }

    private fun parseWebUri(value: String): Uri? {
        val raw = value.trim().substringBefore(' ')
        val normalized = if (raw.startsWith("http://", true) || raw.startsWith("https://", true)) raw else "https://$raw"
        return runCatching { Uri.parse(normalized) }.getOrNull()?.takeIf { it.host?.contains('.') == true }
    }

    private fun queryDisplayName(uri: Uri): String? = runCatching {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)) else null
        }
    }.getOrNull()
}
