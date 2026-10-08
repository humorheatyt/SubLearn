package com.sublearn.platform

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sublearn.domain.RecentMedia
import com.sublearn.domain.RecentMediaRepository
import com.sublearn.domain.SavedWord
import com.sublearn.domain.SavedWordRepository
import com.sublearn.domain.SubtitleLayer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Entity(tableName = "recent_media")
data class RecentMediaEntity(
    @PrimaryKey val uri: String,
    val title: String,
    val mimeType: String?,
    val lastPositionMs: Long,
    val durationMs: Long,
    val lastOpenedEpochMs: Long,
    val lastSubtitleUri: String?,
    val lastSubtitleLayer: String?,
)

@Entity(
    tableName = "saved_words",
    indices = [Index(value = ["normalizedKey", "languageTag"], unique = true)],
)
data class SavedWordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val normalizedKey: String,
    val translation: String,
    val languageTag: String,
    val nativeLanguageTag: String,
    val context: String,
    val mediaTitle: String,
    val startMs: Long,
    val createdEpochMs: Long,
    val isKnown: Boolean,
)

@Fts4(contentEntity = SavedWordEntity::class)
@Entity(tableName = "saved_words_fts")
data class SavedWordFtsEntity(
    @PrimaryKey @androidx.room.ColumnInfo(name = "rowid") val rowId: Long,
    val text: String,
    val translation: String,
    val context: String,
)

@Dao
interface RecentMediaDao {
    @Query("SELECT * FROM recent_media ORDER BY lastOpenedEpochMs DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<RecentMediaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: RecentMediaEntity)

    @Query("UPDATE recent_media SET lastPositionMs = :positionMs, durationMs = :durationMs, lastOpenedEpochMs = :openedAt WHERE uri = :uri")
    suspend fun saveProgress(uri: String, positionMs: Long, durationMs: Long, openedAt: Long): Int

    @Query("DELETE FROM recent_media WHERE uri = :uri")
    suspend fun remove(uri: String)
}

@Dao
interface SavedWordDao {
    @Query("SELECT * FROM saved_words ORDER BY createdEpochMs DESC")
    fun observeAll(): Flow<List<SavedWordEntity>>

    @Query("SELECT words.* FROM saved_words AS words INNER JOIN saved_words_fts ON words.id = saved_words_fts.rowid WHERE saved_words_fts MATCH :query ORDER BY words.createdEpochMs DESC")
    fun observeFts(query: String): Flow<List<SavedWordEntity>>

    @Query("SELECT * FROM saved_words WHERE normalizedKey = :normalizedKey AND languageTag = :languageTag LIMIT 1")
    suspend fun find(normalizedKey: String, languageTag: String): SavedWordEntity?

    @Query("SELECT * FROM saved_words WHERE languageTag = :languageTag AND isKnown = 1")
    suspend fun known(languageTag: String): List<SavedWordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SavedWordEntity): Long

    @Query("UPDATE saved_words SET isKnown = :known WHERE normalizedKey = :normalizedKey AND languageTag = :languageTag")
    suspend fun setKnown(normalizedKey: String, languageTag: String, known: Boolean)

    @Query("DELETE FROM saved_words WHERE id = :id")
    suspend fun remove(id: Long)

    @Transaction
    suspend fun saveUnique(entity: SavedWordEntity): Long {
        val existing = find(entity.normalizedKey, entity.languageTag)
        return if (existing == null) upsert(entity) else upsert(entity.copy(id = existing.id, isKnown = existing.isKnown))
    }
}

@Database(
    entities = [RecentMediaEntity::class, SavedWordEntity::class, SavedWordFtsEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class SubLearnDatabase : RoomDatabase() {
    abstract fun recentMediaDao(): RecentMediaDao
    abstract fun savedWordDao(): SavedWordDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE recent_media ADD COLUMN lastSubtitleLayer TEXT")
            }
        }

        fun create(context: Context): SubLearnDatabase = Room.databaseBuilder(
            context.applicationContext,
            SubLearnDatabase::class.java,
            "sublearn-local.db",
        ).addMigrations(MIGRATION_1_2).build()
    }
}

class RoomRecentMediaRepository(private val dao: RecentMediaDao) : RecentMediaRepository {
    override fun observeRecent(limit: Int): Flow<List<RecentMedia>> = dao.observeRecent(limit.coerceIn(1, 100)).map { rows -> rows.map(RecentMediaEntity::toDomain) }

    override suspend fun recordOpened(request: com.sublearn.domain.MediaRequest, positionMs: Long, durationMs: Long, subtitleUri: String?, subtitleLayer: SubtitleLayer?) {
        val existing = dao.observeRecent(100).first().firstOrNull { it.uri == request.uri }
        dao.upsert(
            RecentMediaEntity(
                uri = request.uri,
                title = request.title,
                mimeType = request.mimeType,
                lastPositionMs = positionMs.coerceAtLeast(0),
                durationMs = durationMs.coerceAtLeast(0),
                lastOpenedEpochMs = System.currentTimeMillis(),
                lastSubtitleUri = subtitleUri ?: existing?.lastSubtitleUri,
                lastSubtitleLayer = subtitleLayer?.name
                    ?: if (subtitleUri == null || subtitleUri == existing?.lastSubtitleUri) existing?.lastSubtitleLayer else null,
            ),
        )
    }

    override suspend fun saveProgress(uri: String, positionMs: Long, durationMs: Long) {
        val changed = dao.saveProgress(uri, positionMs.coerceAtLeast(0), durationMs.coerceAtLeast(0), System.currentTimeMillis())
        if (changed == 0) dao.upsert(RecentMediaEntity(uri, uri.substringAfterLast('/'), null, positionMs.coerceAtLeast(0), durationMs.coerceAtLeast(0), System.currentTimeMillis(), null, null))
    }

    override suspend fun remove(uri: String) = dao.remove(uri)
}

class RoomSavedWordRepository(private val dao: SavedWordDao) : SavedWordRepository {
    override fun observeWords(query: String): Flow<List<SavedWord>> {
        val normalizedQuery = query.trim()
        val source = if (normalizedQuery.isBlank()) dao.observeAll() else dao.observeFts(toFtsQuery(normalizedQuery))
        return source.map { rows -> rows.map(SavedWordEntity::toDomain) }
    }

    override suspend fun find(text: String, languageTag: String): SavedWord? =
        dao.find(normalizeKey(text), languageTag)?.toDomain()

    override suspend fun save(word: SavedWord): Long = dao.saveUnique(word.toEntity())
    override suspend fun update(word: SavedWord) { dao.upsert(word.toEntity()) }
    override suspend fun remove(id: Long) = dao.remove(id)
    override suspend fun markKnown(text: String, languageTag: String, known: Boolean) = dao.setKnown(normalizeKey(text), languageTag, known)
    override suspend fun knownWords(languageTag: String): Set<String> = dao.known(languageTag).mapTo(mutableSetOf()) { it.normalizedKey }

    private fun toFtsQuery(raw: String): String = raw
        .split(Regex("[^\\p{L}\\p{M}\\p{N}]+"))
        .filter { it.isNotBlank() }
        .take(8)
        .joinToString(" AND ") { "\"${it.replace("\"", "\"\"")}\"*" }
}

private fun normalizeKey(text: String): String = text.trim().lowercase().replace(Regex("\\s+"), " ")

private fun RecentMediaEntity.toDomain() = RecentMedia(
    uri, title, mimeType, lastPositionMs, durationMs, lastOpenedEpochMs, lastSubtitleUri,
    lastSubtitleLayer?.let { runCatching { SubtitleLayer.valueOf(it) }.getOrNull() },
)

private fun SavedWordEntity.toDomain() = SavedWord(id, text, translation, languageTag, nativeLanguageTag, context, mediaTitle, startMs, createdEpochMs, isKnown)

private fun SavedWord.toEntity() = SavedWordEntity(
    id = id,
    text = text.trim(),
    normalizedKey = normalizeKey(text),
    translation = translation.trim(),
    languageTag = languageTag,
    nativeLanguageTag = nativeLanguageTag,
    context = context,
    mediaTitle = mediaTitle,
    startMs = startMs.coerceAtLeast(0),
    createdEpochMs = createdEpochMs.takeIf { it > 0 } ?: System.currentTimeMillis(),
    isKnown = isKnown,
)
