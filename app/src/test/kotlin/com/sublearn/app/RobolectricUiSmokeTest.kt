package com.sublearn.app

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sublearn.design.SubLearnTheme
import com.sublearn.domain.AppSettings
import com.sublearn.domain.SavedWord
import com.sublearn.feature.home.HomeScreen
import com.sublearn.feature.learning.MyWordsScreen
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Compose UI smoke tests that run on the JVM (Robolectric, API 31 to match Android 12).
 * These verify real screen wiring without an emulator; instrumented tests cover the rest.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [31])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RobolectricUiSmokeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun homeShowsEmptyLibraryAndRoutesActions() {
        var videoClicked = false
        var urlOpened: String? = null
        compose.setContent {
            SubLearnTheme {
                HomeScreen(
                    recentMedia = emptyList(),
                    settings = AppSettings(),
                    onChooseVideo = { videoClicked = true },
                    onChooseFolder = {},
                    onOpenMedia = {},
                    onOpenUrl = { urlOpened = it },
                    onRemoveRecent = {},
                    onOpenSettings = {},
                )
            }
        }
        compose.onNodeWithText("Make every scene a lesson").assertExists()
        compose.onNodeWithText("Your library starts here").assertExists()
        compose.onNodeWithText("Choose a video").assertHasClickAction().performClick()
        assertTrue(videoClicked)
    }

    @Test fun homeOpensUrlDialogFromButton() {
        var opened: String? = null
        compose.setContent {
            SubLearnTheme {
                HomeScreen(
                    recentMedia = emptyList(),
                    settings = AppSettings(),
                    onChooseVideo = {},
                    onChooseFolder = {},
                    onOpenMedia = {},
                    onOpenUrl = { opened = it },
                    onRemoveRecent = {},
                    onOpenSettings = {},
                )
            }
        }
        compose.onNodeWithText("Open a video URL").performClick()
        compose.onNodeWithText("Open a direct video link").assertExists()
        compose.onNodeWithText("Open").performClick()
        // Empty URL is rejected and never forwarded to the player entry point.
        compose.onNodeWithText("Enter a valid HTTP or HTTPS video URL.").assertExists()
        assertTrue(opened == null)
    }

    @Test fun myWordsRendersSavedWordAndPersianTranslation() {
        compose.setContent {
            SubLearnTheme {
                MyWordsScreen(
                    words = listOf(
                        SavedWord(
                            id = 1,
                            text = "serendipity",
                            translation = "اتفاق خوب",
                            languageTag = "en",
                            nativeLanguageTag = "fa",
                            context = "a happy accident",
                            mediaTitle = "lesson.mp4",
                        ),
                    ),
                    settings = AppSettings(),
                    query = "",
                    onQueryChange = {},
                    onMarkKnown = { _, _ -> },
                    onRemove = {},
                )
            }
        }
        compose.onNodeWithText("My Words").assertExists()
        compose.onNodeWithText("serendipity", substring = true).assertExists()
        compose.onNodeWithText("اتفاق خوب", substring = true).assertExists()
    }

    @Test fun myWordsShowsEmptyStateWithoutWords() {
        compose.setContent {
            SubLearnTheme {
                MyWordsScreen(
                    words = emptyList(),
                    settings = AppSettings(),
                    query = "",
                    onQueryChange = {},
                    onMarkKnown = { _, _ -> },
                    onRemove = {},
                )
            }
        }
        compose.onNodeWithText("Words you keep will live here").assertExists()
    }
}
