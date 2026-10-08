package com.sublearn.app

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sublearn.domain.MediaRequest
import com.sublearn.domain.RecentMediaRepository
import com.sublearn.domain.SavedWord
import com.sublearn.domain.SavedWordRepository
import com.sublearn.domain.SubtitleLayer
import com.sublearn.platform.RoomRecentMediaRepository
import com.sublearn.platform.RoomSavedWordRepository
import com.sublearn.platform.SubLearnDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomRepositoryTest {
    @Test fun recentSubtitleLayerAndMyWordsStatePersistInRoom() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, SubLearnDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val recentRepository: RecentMediaRepository = RoomRecentMediaRepository(database.recentMediaDao())
            val request = MediaRequest("content://provider/tree/root/document/video", "lesson.mp4", "video/mp4")
            recentRepository.recordOpened(request, positionMs = 1_250, durationMs = 9_000, subtitleUri = "content://provider/subtitles/fa.srt", subtitleLayer = SubtitleLayer.NATIVE)
            val recent = recentRepository.observeRecent().first().single()
            assertEquals(1_250L, recent.lastPositionMs)
            assertEquals(SubtitleLayer.NATIVE, recent.lastSubtitleLayer)

            val wordsRepository: SavedWordRepository = RoomSavedWordRepository(database.savedWordDao())
            wordsRepository.save(SavedWord(text = "good morning", translation = "صبح بخیر", languageTag = "en", nativeLanguageTag = "fa"))
            wordsRepository.markKnown("good morning", "en", known = true)
            val saved = wordsRepository.find("good morning", "en")
            assertTrue(saved?.isKnown == true)
            assertEquals("صبح بخیر", saved?.translation)
        } finally {
            database.close()
        }
    }
}
