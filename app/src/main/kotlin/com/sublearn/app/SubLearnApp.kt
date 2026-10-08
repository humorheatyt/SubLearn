package com.sublearn.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sublearn.design.SubLearnRadii
import com.sublearn.design.SubLearnSpacing
import com.sublearn.design.withSurfaceFont
import com.sublearn.domain.AiProviderFactory
import com.sublearn.domain.AppSettings
import com.sublearn.domain.AppSettingsRepository
import com.sublearn.domain.FeatureFlags
import com.sublearn.domain.LearningMode
import com.sublearn.domain.MediaRequest
import com.sublearn.domain.PlayerController
import com.sublearn.domain.RecentMedia
import com.sublearn.domain.RecentMediaRepository
import com.sublearn.domain.SavedWordRepository
import com.sublearn.domain.SurfaceFontSettings
import com.sublearn.domain.SecretStore
import com.sublearn.domain.SubtitleFileLoader
import com.sublearn.domain.SubtitleRepository
import com.sublearn.domain.TranslationProvider
import com.sublearn.feature.home.HomeScreen
import com.sublearn.feature.learning.MyWordsScreen
import com.sublearn.feature.player.PlayerScreen
import com.sublearn.feature.settings.SettingsScreen
import java.nio.charset.StandardCharsets
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val HOME_ROUTE = "home"
private const val LEARN_ROUTE = "learn"
private const val WORDS_ROUTE = "words"
private const val SETTINGS_ROUTE = "settings"
private const val LEVEL_ROUTE = "level"
private const val FOLDER_ROUTE = "folder"
private const val PLAYER_ROUTE = "player/{payload}"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubLearnApp(
    settings: AppSettings,
    settingsRepository: AppSettingsRepository,
    recentMediaRepository: RecentMediaRepository,
    savedWordRepository: SavedWordRepository,
    subtitleRepository: SubtitleRepository,
    subtitleFileLoader: SubtitleFileLoader,
    translationProvider: TranslationProvider,
    aiProviderFactory: AiProviderFactory,
    secretStore: SecretStore,
    onSettingsChange: (AppSettings) -> Unit,
    incomingMedia: MediaRequest?,
    incomingEventId: Int,
    onIncomingConsumed: (Int) -> Unit,
    incomingPdfEventId: Int,
    onIncomingPdfConsumed: (Int) -> Unit,
    playerControllerFactory: () -> PlayerController,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isPlayerRoute = currentRoute == PLAYER_ROUTE
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val snackbarHost = remember { SnackbarHostState() }
    val recentMedia by recentMediaRepository.observeRecent(30).collectAsStateWithLifecycle(initialValue = emptyList())
    var wordQuery by rememberSaveable { mutableStateOf("") }
    val words by savedWordRepository.observeWords(wordQuery).collectAsStateWithLifecycle(initialValue = emptyList())
    var folderVideos by remember { mutableStateOf<List<MediaRequest>>(emptyList()) }
    val menuFont = settings.surfaceFonts["menu.app"] ?: SurfaceFontSettings()

    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            val request = uri.toMediaRequest(context)
            navController.navigate(playerRoute(request)) { launchSingleTop = true }
        }
    }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { treeUri ->
        if (treeUri != null) scope.launch {
            runCatching { context.contentResolver.takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            folderVideos = withContext(kotlinx.coroutines.Dispatchers.IO) { listVideosInTree(context, treeUri) }
            navController.navigate(FOLDER_ROUTE) { launchSingleTop = true }
        }
    }
    val exportSettings = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            try {
                val json = settingsRepository.exportJson()
                context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray(StandardCharsets.UTF_8)) }
                    ?: throw IllegalStateException("Could not open the selected export destination")
                snackbarHost.showSnackbar(context.getString(R.string.app_export_success))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                snackbarHost.showSnackbar(context.getString(R.string.app_export_error))
            }
        }
    }
    val importSettings = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            try {
                val json = context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(StandardCharsets.UTF_8) }
                    ?: throw IllegalStateException("Could not read the selected settings file")
                settingsRepository.importJson(json)
                snackbarHost.showSnackbar(context.getString(R.string.app_import_success))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                snackbarHost.showSnackbar(context.getString(R.string.app_import_error))
            }
        }
    }

    LaunchedEffect(incomingEventId, incomingMedia) {
        val request = incomingMedia ?: return@LaunchedEffect
        navController.navigate(playerRoute(request)) { launchSingleTop = true }
        onIncomingConsumed(incomingEventId)
    }

    fun navigate(route: String) {
        scope.launch {
            drawerState.close()
            if (currentRoute != route) navController.navigate(route) {
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    LaunchedEffect(incomingPdfEventId) {
        if (incomingPdfEventId > 0) {
            navController.navigate(LEARN_ROUTE) { launchSingleTop = true }
            snackbarHost.showSnackbar(context.getString(R.string.app_pdf_opened))
            onIncomingPdfConsumed(incomingPdfEventId)
        }
    }

    CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.bodyMedium.withSurfaceFont(menuFont)) {
    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !isPlayerRoute,
        drawerContent = {
            if (!isPlayerRoute) ModalDrawerSheet {
                Column(Modifier.fillMaxWidth().padding(horizontal = SubLearnSpacing.lg, vertical = SubLearnSpacing.lg), verticalArrangement = Arrangement.spacedBy(SubLearnSpacing.xs)) {
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.app_learn_body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DrawerItem(stringResource(R.string.app_home), Icons.Rounded.Home, currentRoute == HOME_ROUTE, true) { navigate(HOME_ROUTE) }
                DrawerItem(stringResource(R.string.app_learn), Icons.Rounded.School, currentRoute == LEARN_ROUTE, true) { navigate(LEARN_ROUTE) }
                DrawerItem(stringResource(R.string.app_level), Icons.Rounded.Tune, currentRoute == LEVEL_ROUTE, true) { navigate(LEVEL_ROUTE) }
                DrawerItem(stringResource(R.string.app_my_words), Icons.Rounded.Bookmark, currentRoute == WORDS_ROUTE, true) { navigate(WORDS_ROUTE) }
                DrawerItem(stringResource(R.string.app_youtube), Icons.Rounded.PlayArrow, false, FeatureFlags.YOUTUBE, null)
                DrawerItem(stringResource(R.string.app_dictionary), Icons.Rounded.MenuBook, false, FeatureFlags.OFFLINE_DICTIONARY, null)
                DrawerItem(stringResource(R.string.app_quiz), Icons.Rounded.Refresh, false, FeatureFlags.QUIZ, null)
                DrawerItem(stringResource(R.string.app_updates), Icons.Rounded.Refresh, false, FeatureFlags.UPDATE_CHECKER, null)
                DrawerItem(stringResource(R.string.app_settings), Icons.Rounded.Settings, currentRoute == SETTINGS_ROUTE, true) { navigate(SETTINGS_ROUTE) }
            }
        },
    ) {
        Scaffold(
            contentWindowInsets = if (isPlayerRoute) WindowInsets(0, 0, 0, 0) else WindowInsets.safeDrawing,
            topBar = {
                if (!isPlayerRoute && currentRoute != SETTINGS_ROUTE) {
                    TopAppBar(
                        title = { Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold).withSurfaceFont(menuFont)) },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Rounded.Menu, contentDescription = stringResource(R.string.app_menu))
                            }
                        },
                        actions = {
                            if (currentRoute != SETTINGS_ROUTE) IconButton(onClick = { navigate(SETTINGS_ROUTE) }) {
                                Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.app_settings_action))
                            }
                        },
                    )
                }
            },
            bottomBar = {
                if (!isPlayerRoute) {
                    NavigationBar {
                        NavigationBarItem(
                            selected = currentRoute == HOME_ROUTE,
                            onClick = { navigate(HOME_ROUTE) },
                            icon = { Icon(Icons.Rounded.Home, contentDescription = null) },
                            label = { Text(stringResource(R.string.app_home)) },
                        )
                        NavigationBarItem(
                            selected = currentRoute == LEARN_ROUTE,
                            onClick = { navigate(LEARN_ROUTE) },
                            icon = { Icon(Icons.Rounded.School, contentDescription = null) },
                            label = { Text(stringResource(R.string.app_learn)) },
                        )
                        NavigationBarItem(
                            selected = false,
                            onClick = {},
                            enabled = FeatureFlags.YOUTUBE,
                            icon = { Icon(Icons.Rounded.PlayArrow, contentDescription = null) },
                            label = { Text("${stringResource(R.string.app_youtube)} · ${stringResource(R.string.app_coming_soon)}") },
                        )
                        NavigationBarItem(
                            selected = false,
                            onClick = {},
                            enabled = FeatureFlags.OFFLINE_DICTIONARY,
                            icon = { Icon(Icons.Rounded.MenuBook, contentDescription = null) },
                            label = { Text("${stringResource(R.string.app_dictionary)} · ${stringResource(R.string.app_coming_soon)}") },
                        )
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHost) },
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = HOME_ROUTE,
                modifier = Modifier.fillMaxSize().padding(if (isPlayerRoute) PaddingValues(0.dp) else padding),
            ) {
                composable(HOME_ROUTE) {
                    HomeScreen(
                        recentMedia = recentMedia,
                        settings = settings,
                        onChooseVideo = { videoPicker.launch(arrayOf("video/*")) },
                        onChooseFolder = { folderPicker.launch(null) },
                        onOpenMedia = { media -> navController.navigate(playerRoute(media.toRequest())) { launchSingleTop = true } },
                        onOpenUrl = { raw -> navController.navigate(playerRoute(raw.toMediaRequest())) { launchSingleTop = true } },
                        onRemoveRecent = { uri -> scope.launch { recentMediaRepository.remove(uri) } },
                        onOpenSettings = { navigate(SETTINGS_ROUTE) },
                    )
                }
                composable(LEARN_ROUTE) {
                    LearnDashboard(
                        settings = settings,
                        recentMedia = recentMedia,
                        onModeChange = { onSettingsChange(settings.copy(learningMode = it)) },
                        onSetLevel = { navigate(LEVEL_ROUTE) },
                        onOpenWords = { navigate(WORDS_ROUTE) },
                        onOpenRecent = { media -> navController.navigate(playerRoute(media.toRequest())) { launchSingleTop = true } },
                        onOpenHome = { navigate(HOME_ROUTE) },
                    )
                }
                composable(WORDS_ROUTE) {
                    MyWordsScreen(
                        words = words,
                        settings = settings,
                        query = wordQuery,
                        onQueryChange = { wordQuery = it },
                        onMarkKnown = { word, known -> scope.launch { savedWordRepository.markKnown(word.text, word.languageTag, known) } },
                        onRemove = { id -> scope.launch { savedWordRepository.remove(id) } },
                    )
                }
                composable(SETTINGS_ROUTE) {
                    SettingsScreen(
                        settings = settings,
                        secretStore = secretStore,
                        providerFactory = aiProviderFactory,
                        translationProvider = translationProvider,
                        onUpdate = onSettingsChange,
                        onExportJson = { exportSettings.launch("sublearn-settings.json") },
                        onImportJson = { importSettings.launch(arrayOf("application/json", "text/json")) },
                        onBack = { if (!navController.popBackStack()) navigate(HOME_ROUTE) },
                    )
                }
                composable(LEVEL_ROUTE) {
                    ManualLevelScreen(settings, onUpdate = onSettingsChange)
                }
                composable(FOLDER_ROUTE) {
                    FolderVideosScreen(
                        videos = folderVideos,
                        onBack = { navController.popBackStack() },
                        onOpen = { request -> navController.navigate(playerRoute(request)) { launchSingleTop = true } },
                    )
                }
                composable(PLAYER_ROUTE, arguments = listOf(navArgument("payload") { type = NavType.StringType })) { entry ->
                    val payload = entry.arguments?.getString("payload").orEmpty()
                    val request = remember(payload) { decodeMediaRequest(payload) }
                    val controller = remember(payload) { playerControllerFactory() }
                    PlayerScreen(
                        request = request,
                        controller = controller,
                        settings = settings,
                        recentMediaRepository = recentMediaRepository,
                        savedWordRepository = savedWordRepository,
                        subtitleRepository = subtitleRepository,
                        subtitleLoader = subtitleFileLoader,
                        translationProvider = translationProvider,
                        aiProviderFactory = aiProviderFactory,
                        secretStore = secretStore,
                        onSettingsChange = onSettingsChange,
                        onOpenSettings = { navigate(SETTINGS_ROUTE) },
                        onBack = { if (!navController.popBackStack()) navigate(HOME_ROUTE) },
                    )
                }
            }
        }
    }
    }
}

@Composable
private fun DrawerItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, enabled: Boolean, onClick: (() -> Unit)?) {
    val comingSoon = !enabled
    NavigationDrawerItem(
        label = {
            Column(verticalArrangement = Arrangement.spacedBy(SubLearnSpacing.xxs)) {
                Text(label)
                if (comingSoon) Text(stringResource(R.string.app_coming_soon), style = MaterialTheme.typography.labelSmall)
            }
        },
        icon = { Icon(icon, contentDescription = null) },
        selected = selected,
        onClick = { if (enabled) onClick?.invoke() },
        modifier = Modifier
            .padding(NavigationDrawerItemDefaults.ItemPadding)
            .alpha(if (enabled) 1f else 0.38f)
            .semantics { if (!enabled) disabled() },
    )
}

@Composable
private fun LearnDashboard(
    settings: AppSettings,
    recentMedia: List<RecentMedia>,
    onModeChange: (LearningMode) -> Unit,
    onSetLevel: () -> Unit,
    onOpenWords: () -> Unit,
    onOpenRecent: (RecentMedia) -> Unit,
    onOpenHome: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(SubLearnSpacing.screen),
        verticalArrangement = Arrangement.spacedBy(SubLearnSpacing.lg),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(SubLearnSpacing.xs)) {
            Text(stringResource(R.string.app_learn_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.app_learn_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Button(onClick = {}, enabled = FeatureFlags.PDF_BROWSER_IMAGE_LEARNING, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.app_pdf_learning))
        }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(SubLearnRadii.large)) {
            Column(Modifier.fillMaxWidth().padding(SubLearnSpacing.lg), verticalArrangement = Arrangement.spacedBy(SubLearnSpacing.md)) {
                Text(stringResource(R.string.app_learning_mode), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(SubLearnSpacing.sm)) {
                    FilterChip(selected = settings.learningMode == LearningMode.ENTERTAINMENT, onClick = { onModeChange(LearningMode.ENTERTAINMENT) }, label = { Text(stringResource(R.string.app_entertainment_mode)) })
                    FilterChip(selected = settings.learningMode == LearningMode.LEARNING, onClick = { onModeChange(LearningMode.LEARNING) }, label = { Text(stringResource(R.string.app_learning_mode)) })
                }
                Text(stringResource(R.string.app_level_assumption), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = onSetLevel, modifier = Modifier.fillMaxWidth()) { Text("${stringResource(R.string.app_level)} · ${settings.manualLevel}") }
                Button(onClick = onOpenWords, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.app_open_words)) }
            }
        }
        if (recentMedia.isNotEmpty()) {
            val mostRecent = recentMedia.first()
            Button(onClick = { onOpenRecent(mostRecent) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(SubLearnSpacing.xs))
                Text(stringResource(R.string.app_continue_learning) + ": " + mostRecent.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        } else {
            Surface(shape = RoundedCornerShape(SubLearnRadii.medium), color = MaterialTheme.colorScheme.surfaceVariant) {
                Text(stringResource(R.string.app_no_recent), modifier = Modifier.fillMaxWidth().padding(SubLearnSpacing.lg), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Button(onClick = onOpenHome, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.VideoLibrary, contentDescription = null)
            Spacer(Modifier.width(SubLearnSpacing.xs))
            Text(stringResource(R.string.app_home))
        }
    }
}

@Composable
private fun FolderVideosScreen(videos: List<MediaRequest>, onBack: () -> Unit, onOpen: (MediaRequest) -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(SubLearnSpacing.screen),
        verticalArrangement = Arrangement.spacedBy(SubLearnSpacing.md),
    ) {
        Text(stringResource(com.sublearn.feature.home.R.string.home_folder_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(stringResource(com.sublearn.feature.home.R.string.home_folder_permission), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (videos.isEmpty()) {
            Surface(shape = RoundedCornerShape(SubLearnRadii.medium), color = MaterialTheme.colorScheme.surfaceVariant) {
                Text(stringResource(com.sublearn.feature.home.R.string.home_folder_empty), modifier = Modifier.fillMaxWidth().padding(SubLearnSpacing.lg))
            }
        } else {
            videos.forEach { video ->
                Card(
                    onClick = { onOpen(video) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(SubLearnRadii.medium),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Row(Modifier.fillMaxWidth().padding(SubLearnSpacing.md), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SubLearnSpacing.md)) {
                        Icon(Icons.Rounded.VideoLibrary, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(video.title, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Icon(Icons.Rounded.PlayArrow, contentDescription = stringResource(R.string.app_continue_learning))
                    }
                }
            }
        }
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.app_done)) }
    }
}

@Composable
private fun ManualLevelScreen(settings: AppSettings, onUpdate: (AppSettings) -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(SubLearnSpacing.screen),
        verticalArrangement = Arrangement.spacedBy(SubLearnSpacing.md),
    ) {
        Text(stringResource(R.string.app_level_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.app_level_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(SubLearnSpacing.xs)) {
            listOf("A1", "A2", "B1", "B2", "C1", "C2").forEach { level ->
                FilterChip(selected = settings.manualLevel == level, onClick = { onUpdate(settings.copy(manualLevel = level)) }, label = { Text(level) })
            }
        }
        Text(stringResource(R.string.app_level_assumption), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private data class FolderChild(val documentId: String, val name: String, val mimeType: String?)

private fun listVideosInTree(context: Context, treeUri: Uri): List<MediaRequest> {
    val rootDocumentId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull() ?: return emptyList()
    val visited = mutableSetOf<String>()
    val videos = mutableListOf<MediaRequest>()
    val projection = arrayOf(
        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        DocumentsContract.Document.COLUMN_MIME_TYPE,
    )

    fun visit(parentDocumentId: String, depth: Int) {
        if (depth > 4 || videos.size >= 500 || !visited.add(parentDocumentId)) return
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocumentId)
        val children = runCatching {
            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val typeColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                buildList {
                    while (cursor.moveToNext() && size < 500) {
                        val id = cursor.getString(idColumn) ?: continue
                        val name = cursor.getString(nameColumn) ?: continue
                        val mime = cursor.getString(typeColumn)
                        add(FolderChild(id, name, mime))
                    }
                }
            }.orEmpty()
        }.getOrDefault(emptyList())
        children.forEach { child ->
            if (child.mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                visit(child.documentId, depth + 1)
            } else if (isSupportedVideo(child.name, child.mimeType)) {
                val uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, child.documentId)
                videos += MediaRequest(uri.toString(), child.name, child.mimeType)
            }
            if (videos.size >= 500) return
        }
    }

    runCatching { visit(rootDocumentId, 0) }
    return videos.distinctBy { it.uri }.sortedBy { it.title.lowercase(Locale.ROOT) }
}

private fun isSupportedVideo(name: String, mimeType: String?): Boolean {
    if (mimeType?.startsWith("video/", ignoreCase = true) == true) return true
    return name.substringAfterLast('.', "").lowercase(Locale.ROOT) in setOf("3gp", "avi", "flv", "m4v", "mkv", "mov", "mp4", "mpeg", "mpg", "mts", "ts", "webm", "wmv")
}

private fun RecentMedia.toRequest() = MediaRequest(uri, title, mimeType, lastPositionMs)

private fun String.toMediaRequest(): MediaRequest {
    val uri = Uri.parse(trim())
    val title = uri.host ?: uri.lastPathSegment?.substringAfterLast('/')?.takeIf(String::isNotBlank) ?: trim()
    return MediaRequest(uri.toString(), title, null)
}

private fun Uri.toMediaRequest(context: Context): MediaRequest {
    val mime = context.contentResolver.getType(this)
    val displayName = if (scheme == "content") runCatching {
        context.contentResolver.query(this, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)) else null
        }
    }.getOrNull() else null
    val title = displayName?.takeIf(String::isNotBlank) ?: lastPathSegment?.substringAfterLast('/') ?: context.getString(R.string.app_name)
    return MediaRequest(toString(), title, mime)
}

private fun playerRoute(request: MediaRequest): String {
    val safeTitle = request.title.replace('\n', ' ').replace('\r', ' ')
    val payload = listOf(request.uri, safeTitle, request.mimeType.orEmpty(), request.resumePositionMs.toString()).joinToString("\n")
    val encoded = android.util.Base64.encodeToString(payload.toByteArray(StandardCharsets.UTF_8), android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING)
    return "player/$encoded"
}

private fun decodeMediaRequest(payload: String): MediaRequest {
    val decoded = runCatching {
        String(android.util.Base64.decode(payload, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING), StandardCharsets.UTF_8)
    }.getOrDefault("")
    val fields = decoded.split('\n', limit = 4)
    val uri = fields.getOrNull(0).orEmpty()
    val title = fields.getOrNull(1).orEmpty().ifBlank { uri.substringAfterLast('/') }
    val mime = fields.getOrNull(2).orEmpty().ifBlank { null }
    val position = fields.getOrNull(3)?.toLongOrNull()?.coerceAtLeast(0) ?: 0L
    return MediaRequest(uri, title, mime, position)
}
