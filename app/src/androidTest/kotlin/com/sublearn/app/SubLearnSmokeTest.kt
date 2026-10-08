package com.sublearn.app

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sublearn.design.SubLearnTheme
import com.sublearn.domain.AiProvider
import com.sublearn.domain.AiProviderFactory
import com.sublearn.domain.AiRequest
import com.sublearn.domain.AiResponse
import com.sublearn.domain.AppSettings
import com.sublearn.domain.Cue
import com.sublearn.domain.MediaRequest
import com.sublearn.domain.RecentMedia
import com.sublearn.domain.RecentMediaRepository
import com.sublearn.domain.SavedWord
import com.sublearn.domain.SavedWordRepository
import com.sublearn.domain.SecretStore
import com.sublearn.domain.SubtitleLayer
import com.sublearn.domain.SubtitleRepository
import com.sublearn.domain.SubtitleFileLoader
import com.sublearn.domain.TranslationProvider
import com.sublearn.feature.home.HomeScreen
import com.sublearn.feature.player.PlayerScreen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SubLearnSmokeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun homeExposesRealLibraryEntryActions() {
        var folderClicked = false
        compose.setContent {
            SubLearnTheme {
                HomeScreen(
                    recentMedia = emptyList(),
                    settings = AppSettings(),
                    onChooseVideo = {},
                    onChooseFolder = { folderClicked = true },
                    onOpenMedia = {},
                    onOpenUrl = {},
                    onRemoveRecent = {},
                    onOpenSettings = {},
                )
            }
        }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.onNodeWithText(context.getString(com.sublearn.feature.home.R.string.home_title)).assertExists()
        compose.onNodeWithText(context.getString(com.sublearn.feature.home.R.string.home_open_folder)).performClick()
        assertTrue(folderClicked)
    }

    @Test fun playerControlsDriveFakePlayerState() {
        val player = FakePlayer()
        val request = MediaRequest("https://example.invalid/sample.mp4", "sample.mp4", "video/mp4")
        compose.setContent {
            SubLearnTheme {
                PlayerScreen(
                    request = request,
                    controller = player,
                    settings = AppSettings(),
                    recentMediaRepository = EmptyRecentMediaRepository,
                    savedWordRepository = EmptySavedWordRepository,
                    subtitleRepository = EmptySubtitleRepository,
                    subtitleLoader = object : SubtitleFileLoader {
                        override suspend fun load(uri: String, trackId: String): List<Cue> = emptyList()
                    },
                    translationProvider = ReadyTranslation,
                    aiProviderFactory = EmptyAiProviderFactory,
                    secretStore = EmptySecretStore,
                    onSettingsChange = {},
                    onOpenSettings = {},
                    onBack = {},
                )
            }
        }
        compose.waitUntil(5_000) { player.snapshot.value.request?.uri == request.uri }
        assertTrue(player.snapshot.value.isPlaying)
        val pauseLabel = InstrumentationRegistry.getInstrumentation().targetContext
            .getString(com.sublearn.feature.player.R.string.player_pause)
        compose.onAllNodesWithContentDescription(pauseLabel).onFirst().performClick()
        compose.waitUntil(2_000) { !player.snapshot.value.isPlaying }
        assertFalse(player.snapshot.value.isPlaying)
    }
}

private object EmptyRecentMediaRepository : RecentMediaRepository {
    override fun observeRecent(limit: Int): Flow<List<RecentMedia>> = flowOf(emptyList())
    override suspend fun recordOpened(request: MediaRequest, positionMs: Long, durationMs: Long, subtitleUri: String?, subtitleLayer: SubtitleLayer?) = Unit
    override suspend fun saveProgress(uri: String, positionMs: Long, durationMs: Long) = Unit
    override suspend fun remove(uri: String) = Unit
}

private object EmptySavedWordRepository : SavedWordRepository {
    override fun observeWords(query: String): Flow<List<SavedWord>> = flowOf(emptyList())
    override suspend fun find(text: String, languageTag: String): SavedWord? = null
    override suspend fun save(word: SavedWord): Long = 1
    override suspend fun update(word: SavedWord) = Unit
    override suspend fun remove(id: Long) = Unit
    override suspend fun markKnown(text: String, languageTag: String, known: Boolean) = Unit
    override suspend fun knownWords(languageTag: String): Set<String> = emptySet()
}

private object EmptySubtitleRepository : SubtitleRepository {
    override suspend fun parse(bytes: ByteArray, trackId: String, formatHint: String?): List<Cue> = emptyList()
    override fun cueAt(cues: List<Cue>, timeMs: Long): Cue? = null
    override fun findExternalSidecar(videoUri: String, videoName: String): String? = null
}

private object ReadyTranslation : TranslationProvider {
    override val providerId: String = "test"
    override suspend fun isModelReady(sourceLanguage: String, targetLanguage: String): Boolean = true
    override suspend fun downloadModel(sourceLanguage: String, targetLanguage: String) = Unit
    override suspend fun translate(text: String, sourceLanguage: String, targetLanguage: String): String = text
}

private object EmptyAiProviderFactory : AiProviderFactory {
    override fun get(providerId: String): AiProvider = object : AiProvider {
        override val id: String = providerId
        override val displayName: String = "Test"
        override suspend fun complete(request: AiRequest, apiKey: String) = AiResponse("", providerId, "test")
    }
    override fun supportedProviders(): List<Pair<String, String>> = listOf("test" to "Test")
}

private object EmptySecretStore : SecretStore {
    override suspend fun read(providerId: String): String? = null
    override suspend fun write(providerId: String, secret: String) = Unit
    override suspend fun delete(providerId: String) = Unit
}
