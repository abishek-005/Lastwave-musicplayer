package com.lastwave.app.data.local

import com.lastwave.app.data.generate.GeneratedTrack
import com.lastwave.app.data.generate.sameSongAs

data class LocalDuplicateFilterResult(
    val uniqueTracks: List<GeneratedTrack>,
    val duplicateCount: Int,
)

object LocalPlaylistHelper {

    /**
     * Pure logic: filters out incoming tracks that already exist in the target playlist,
     * or appear multiple times in the incoming batch.
     *
     * Tracks are considered identical if they have the same content URI / local URL,
     * or match [sameSongAs] semantics.
     *
     * NOTE: This does NOT move, copy, rename or delete any audio files on the phone.
     * Playlists store only references (URIs) to the songs.
     */
    fun filterDuplicates(
        existingTracks: List<GeneratedTrack>,
        incomingTracks: List<GeneratedTrack>,
    ): LocalDuplicateFilterResult {
        if (incomingTracks.isEmpty()) {
            return LocalDuplicateFilterResult(emptyList(), 0)
        }

        val unique = ArrayList<GeneratedTrack>(incomingTracks.size)
        var duplicateCount = 0

        for (track in incomingTracks) {
            val isDuplicate = existingTracks.any { existing ->
                isTrackEqual(existing, track)
            } || unique.any { inBatch ->
                isTrackEqual(inBatch, track)
            }

            if (isDuplicate) {
                duplicateCount++
            } else {
                unique.add(track)
            }
        }

        return LocalDuplicateFilterResult(
            uniqueTracks = unique,
            duplicateCount = duplicateCount,
        )
    }

    private fun isTrackEqual(a: GeneratedTrack, b: GeneratedTrack): Boolean {
        if (a.isLocal && b.isLocal) {
            if (a.url.isNotBlank() && b.url.isNotBlank() && a.url.trim() == b.url.trim()) return true
        }
        return a.sameSongAs(b)
    }

    /**
     * Checks if a local track's content URI or file path is still accessible on the device.
     */
    fun isLocalTrackAccessible(context: android.content.Context, url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        return runCatching {
            if (url.startsWith("content://")) {
                val uri = android.net.Uri.parse(url)
                context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { true } == true
            } else {
                val file = if (url.startsWith("file://")) {
                    java.io.File(android.net.Uri.parse(url).path.orEmpty())
                } else {
                    java.io.File(url)
                }
                file.exists() && file.canRead()
            }
        }.getOrDefault(false)
    }
}

/**
 * Converts a [LocalAudioTrack] to a [GeneratedTrack] representation suitable for
 * storing inside saved playlists as a local reference.
 * Note: No file operations occur; only metadata and the device content URI are stored.
 */
fun LocalAudioTrack.toGeneratedTrack(): GeneratedTrack = GeneratedTrack(
    name = title,
    artist = artist,
    album = album.takeIf { it.isNotBlank() },
    artworkUrl = artworkUri?.toString(),
    url = contentUri.toString(),
    isLocal = true,
    durationMs = durationMs.takeIf { it > 0L },
    mimeType = mimeType.takeIf { it.isNotBlank() },
)
