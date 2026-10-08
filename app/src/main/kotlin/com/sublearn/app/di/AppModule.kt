package com.sublearn.app.di

import com.sublearn.domain.AiProviderFactory
import com.sublearn.domain.AppSettingsRepository
import com.sublearn.domain.PlayerController
import com.sublearn.domain.RecentMediaRepository
import com.sublearn.domain.SavedWordRepository
import com.sublearn.domain.SecretStore
import com.sublearn.domain.SubtitleFileLoader
import com.sublearn.domain.SubtitleRepository
import com.sublearn.domain.TranslationProvider
import com.sublearn.domain.WordLevelProvider
import com.sublearn.domain.UnknownWordLevelProvider
import com.sublearn.platform.AndroidKeystoreSecretStore
import com.sublearn.platform.ContentResolverSubtitleRepository
import com.sublearn.platform.DataStoreAppSettingsRepository
import com.sublearn.platform.HttpAiProviderFactory
import com.sublearn.platform.Media3PlayerController
import com.sublearn.platform.MlKitTranslationProvider
import com.sublearn.platform.RoomRecentMediaRepository
import com.sublearn.platform.RoomSavedWordRepository
import com.sublearn.platform.SubLearnDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

@androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])
val appModule = module {
    single { SubLearnDatabase.create(androidContext()) }
    single<RecentMediaRepository> { RoomRecentMediaRepository(get<SubLearnDatabase>().recentMediaDao()) }
    single<SavedWordRepository> { RoomSavedWordRepository(get<SubLearnDatabase>().savedWordDao()) }
    single<AppSettingsRepository> { DataStoreAppSettingsRepository(androidContext()) }
    single<SecretStore> { AndroidKeystoreSecretStore(androidContext()) }
    single<ContentResolverSubtitleRepository> { ContentResolverSubtitleRepository(androidContext()) }
    single<SubtitleRepository> { get<ContentResolverSubtitleRepository>() }
    single<SubtitleFileLoader> { get<ContentResolverSubtitleRepository>() }
    single<TranslationProvider> { MlKitTranslationProvider() }
    single<AiProviderFactory> { HttpAiProviderFactory() }
    single<WordLevelProvider> { UnknownWordLevelProvider() }
    factory<PlayerController> { Media3PlayerController(androidContext()) }
}
