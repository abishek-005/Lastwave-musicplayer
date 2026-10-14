package com.lastwave.app.data.local

import android.net.Uri
import com.lastwave.app.playback.PlayableTrack

data class LocalAudioTrack(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val contentUri: Uri,
    val filePath: String,
    val mimeType: String,
    val sizeBytes: Long,
    val artworkUri: Uri? = null,
    val dateAddedSec: Long = 0L,
) {
    fun toPlayableTrack(): PlayableTrack {
        val effectiveMime = when {
            mimeType.isNotBlank() && mimeType != "application/octet-stream" -> mimeType
            filePath.endsWith(".flac", ignoreCase = true) -> "audio/flac"
            filePath.endsWith(".m4a", ignoreCase = true) || filePath.endsWith(".mp4", ignoreCase = true) || filePath.endsWith(".aac", ignoreCase = true) -> "audio/mp4"
            filePath.endsWith(".opus", ignoreCase = true) || filePath.endsWith(".ogg", ignoreCase = true) -> "audio/ogg"
            filePath.endsWith(".mp3", ignoreCase = true) -> "audio/mpeg"
            filePath.endsWith(".wav", ignoreCase = true) -> "audio/x-wav"
            else -> "audio/flac"
        }
        return PlayableTrack(
            title = title,
            artist = artist,
            album = album.takeIf { it.isNotBlank() },
            artworkUrl = artworkUri?.toString(),
            videoId = null,
            internalTrackId = "local_media_$id",
            playbackUrl = contentUri.toString(),
            playbackMimeType = effectiveMime,
            durationMs = durationMs.takeIf { it > 0L },
            isLocal = true,
        )
    }
}
