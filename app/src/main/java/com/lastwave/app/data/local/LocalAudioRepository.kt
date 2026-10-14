package com.lastwave.app.data.local

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalAudioRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val albumArtBaseUri = Uri.parse("content://media/external/audio/albumart")

    suspend fun getLocalAudioTracks(): List<LocalAudioTrack> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<LocalAudioTrack>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATE_ADDED,
        )

        val selection = "(${MediaStore.Audio.Media.IS_MUSIC} != 0 OR " +
            "${MediaStore.Audio.Media.DATA} LIKE '%.mp3' OR " +
            "${MediaStore.Audio.Media.DATA} LIKE '%.flac' OR " +
            "${MediaStore.Audio.Media.DATA} LIKE '%.m4a' OR " +
            "${MediaStore.Audio.Media.DATA} LIKE '%.opus' OR " +
            "${MediaStore.Audio.Media.DATA} LIKE '%.ogg' OR " +
            "${MediaStore.Audio.Media.DATA} LIKE '%.wav') AND " +
            "${MediaStore.Audio.Media.DURATION} >= 1000"

        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        try {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                null,
                sortOrder,
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val rawTitle = cursor.getString(titleCol)?.trim().orEmpty()
                    val rawArtist = cursor.getString(artistCol)?.trim().orEmpty()
                    val rawAlbum = cursor.getString(albumCol)?.trim().orEmpty()
                    val durationMs = cursor.getLong(durationCol)
                    val dataPath = cursor.getString(dataCol)?.trim().orEmpty()
                    val mimeType = cursor.getString(mimeCol)?.trim().orEmpty()
                    val sizeBytes = cursor.getLong(sizeCol)
                    val albumId = cursor.getLong(albumIdCol)
                    val dateAdded = cursor.getLong(dateCol)

                    // Skip files that no longer exist if path is given
                    if (dataPath.isNotBlank() && dataPath.startsWith("/") && !File(dataPath).exists()) {
                        continue
                    }

                    // Metadata fallback: if title is empty or blank, fall back to file name
                    val title = when {
                        rawTitle.isNotBlank() -> rawTitle
                        dataPath.isNotBlank() -> File(dataPath).nameWithoutExtension
                        else -> "Track $id"
                    }

                    // Artist fallback: if artist is empty or unknown marker
                    val artist = when {
                        rawArtist.isNotBlank() && !rawArtist.equals("<unknown>", ignoreCase = true) -> rawArtist
                        else -> "Unknown Artist"
                    }

                    val album = if (rawAlbum.equals("<unknown>", ignoreCase = true)) "" else rawAlbum

                    val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    val artworkUri = if (albumId > 0) {
                        ContentUris.withAppendedId(albumArtBaseUri, albumId)
                    } else null

                    tracks.add(
                        LocalAudioTrack(
                            id = id,
                            title = title,
                            artist = artist,
                            album = album,
                            durationMs = durationMs,
                            contentUri = contentUri,
                            filePath = dataPath,
                            mimeType = mimeType,
                            sizeBytes = sizeBytes,
                            artworkUri = artworkUri,
                            dateAddedSec = dateAdded,
                        ),
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("LocalAudioRepository", "Failed to query MediaStore", e)
        }

        tracks
    }

    suspend fun scanSafTree(treeUri: Uri): List<LocalAudioTrack> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<LocalAudioTrack>()
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext emptyList()
        val audioExts = setOf("mp3", "flac", "m4a", "opus", "ogg", "wav")

        fun scanDir(dir: DocumentFile) {
            for (file in dir.listFiles()) {
                if (file.isDirectory) {
                    scanDir(file)
                } else if (file.isFile) {
                    val name = file.name.orEmpty()
                    val ext = name.substringAfterLast('.', "").lowercase()
                    if (ext in audioExts) {
                        val retriever = MediaMetadataRetriever()
                        var title = file.nameWithoutExtension()
                        var artist = "Unknown Artist"
                        var album = ""
                        var durationMs = 0L
                        val mime = file.type.orEmpty()
                        runCatching {
                            retriever.setDataSource(context, file.uri)
                            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.takeIf { it.isNotBlank() }?.let {
                                title = it.trim()
                            }
                            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.takeIf { it.isNotBlank() }?.let {
                                artist = it.trim()
                            }
                            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.takeIf { it.isNotBlank() }?.let {
                                album = it.trim()
                            }
                            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?.let {
                                durationMs = it
                            }
                        }
                        runCatching { retriever.release() }

                        tracks.add(
                            LocalAudioTrack(
                                id = file.uri.hashCode().toLong(),
                                title = title,
                                artist = artist,
                                album = album,
                                durationMs = durationMs,
                                contentUri = file.uri,
                                filePath = file.uri.toString(),
                                mimeType = mime,
                                sizeBytes = file.length(),
                                artworkUri = null,
                                dateAddedSec = file.lastModified() / 1000,
                            ),
                        )
                    }
                }
            }
        }

        runCatching { scanDir(root) }
        tracks
    }

    private fun DocumentFile.nameWithoutExtension(): String {
        val n = name.orEmpty()
        val dot = n.lastIndexOf('.')
        return if (dot > 0) n.substring(0, dot) else n
    }
}
