package com.sublearn.platform

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import com.sublearn.domain.Cue
import com.sublearn.domain.SubtitleFileLoader
import com.sublearn.domain.SubtitleRepository
import com.sublearn.subtitles.CueTimelineIndex
import com.sublearn.subtitles.DefaultSubtitleParser
import java.io.File
import java.util.Locale
import java.util.WeakHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ContentResolverSubtitleRepository(context: Context) : SubtitleRepository, SubtitleFileLoader {
    private val appContext = context.applicationContext
    private val parser = DefaultSubtitleParser()
    private val indices = WeakHashMap<List<Cue>, CueTimelineIndex>()

    override suspend fun parse(bytes: ByteArray, trackId: String, formatHint: String?): List<Cue> =
        withContext(Dispatchers.Default) { parser.parse(bytes, trackId, formatHint) }

    override fun cueAt(cues: List<Cue>, timeMs: Long): Cue? = synchronized(indices) {
        indices.getOrPut(cues) { CueTimelineIndex(cues) }.cueAt(timeMs)
    }

    override fun findExternalSidecar(videoUri: String, videoName: String): String? {
        val uri = runCatching { Uri.parse(videoUri) }.getOrNull() ?: return null
        val basename = videoName.substringBeforeLast('.', videoName).trim().lowercase(Locale.ROOT)
        if (basename.isBlank()) return null
        val candidates = listOf("srt", "vtt", "ass", "ssa")
        val file = if (uri.scheme == "file") uri.path?.let(::File) else null
        if (file != null) {
            return file.parentFile?.listFiles()?.asSequence()
                ?.filter { child -> child.isFile && child.extension.lowercase(Locale.ROOT) in candidates }
                ?.sortedBy { sidecarRank(it.name, basename) }
                ?.firstOrNull { sidecarRank(it.name, basename) < Int.MAX_VALUE }
                ?.toURI()?.toString()
        }
        return findDocumentSidecar(uri, basename, candidates)
    }

    override suspend fun load(uri: String, trackId: String): List<Cue> {
        val parsed = Uri.parse(uri)
        return parser.parse(read(parsed), trackId, displayName(parsed))
    }

    suspend fun read(uri: Uri): ByteArray = withContext(Dispatchers.IO) {
        if (uri.scheme == "file") return@withContext uri.path?.let(::File)?.readBytes()
            ?: throw IllegalArgumentException("Cannot read the selected subtitle")
        appContext.contentResolver.openInputStream(uri)?.use { input -> input.readBytes() }
            ?: throw IllegalArgumentException("Cannot read the selected subtitle")
    }

    fun displayName(uri: Uri): String? {
        if (uri.scheme == "file") return uri.path?.let(::File)?.name
        return appContext.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { it.firstString() } ?: uri.lastPathSegment
    }

    private fun findDocumentSidecar(videoUri: Uri, basename: String, extensions: List<String>): String? {
        if (!DocumentsContract.isDocumentUri(appContext, videoUri) || !DocumentsContract.isTreeUri(videoUri)) return null
        return runCatching {
            val documentId = DocumentsContract.getDocumentId(videoUri)
            val parentId = documentId.substringBeforeLast('/', "")
            if (parentId.isBlank()) return@runCatching null
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(videoUri, parentId)
            val projection = arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            appContext.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                var best: Pair<Int, String>? = null
                val idIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameIndex) ?: continue
                    if (name.substringAfterLast('.', "").lowercase(Locale.ROOT) !in extensions) continue
                    val rank = sidecarRank(name, basename)
                    if (rank < (best?.first ?: Int.MAX_VALUE)) {
                        val child = DocumentsContract.buildDocumentUriUsingTree(videoUri, cursor.getString(idIndex))
                        best = rank to child.toString()
                    }
                }
                best?.takeIf { it.first < Int.MAX_VALUE }?.second
            }
        }.getOrNull()
    }

    private fun sidecarRank(name: String, basename: String): Int {
        val lower = name.lowercase(Locale.ROOT)
        val fileBase = lower.substringBeforeLast('.', lower)
        return when {
            fileBase == basename -> 0
            fileBase == "$basename.en" -> 1
            fileBase == "$basename.fa" -> 2
            fileBase.startsWith("$basename.") -> 3
            else -> Int.MAX_VALUE
        }
    }
}

private fun Cursor.firstString(): String? = if (moveToFirst() && !isNull(0)) getString(0) else null
