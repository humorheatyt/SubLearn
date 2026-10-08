package com.sublearn.platform

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sublearn.domain.AppSettings
import com.sublearn.domain.AppSettingsRepository
import com.sublearn.domain.SettingsCodec
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "sublearn_settings")

class DataStoreAppSettingsRepository(context: Context) : AppSettingsRepository {
    private val store = context.applicationContext.settingsDataStore
    private val payloadKey = stringPreferencesKey("settings_json")
    private val schemaKey = intPreferencesKey("schema_version")

    override val settings: Flow<AppSettings> = store.data
        .catch { error ->
            if (error is IOException) emit(androidx.datastore.preferences.core.emptyPreferences()) else throw error
        }
        .map { preferences -> preferences[payloadKey]?.let(SettingsCodec::decode) ?: AppSettings() }
        .distinctUntilChanged()

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        store.edit { preferences ->
            val current = preferences[payloadKey]?.let(SettingsCodec::decode) ?: AppSettings()
            val updated = transform(current).normalized()
            preferences[payloadKey] = SettingsCodec.encode(updated)
            preferences[schemaKey] = updated.schemaVersion
        }
    }

    override suspend fun replace(settings: AppSettings) {
        val normalized = settings.normalized()
        store.edit { preferences ->
            preferences[payloadKey] = SettingsCodec.encode(normalized)
            preferences[schemaKey] = normalized.schemaVersion
        }
    }

    override suspend fun exportJson(): String = settings.first().let(SettingsCodec::encode)

    override suspend fun importJson(json: String) {
        replace(SettingsCodec.decode(json))
    }
}
