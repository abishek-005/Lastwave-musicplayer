package com.lastwave.app.data.local

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LocalAudioTrackTest {

    @Test
    fun toPlayableTrack_mapsAllFieldsCorrectlyAndSetsIsLocalTrue() {
        val uri = Uri.parse("content://media/external/audio/media/1234")
        val artUri = Uri.parse("content://media/external/audio/albumart/567")
        val localTrack = LocalAudioTrack(
            id = 1234L,
            title = "Bohemian Rhapsody",
            artist = "Queen",
            album = "A Night at the Opera",
            durationMs = 354000L,
            contentUri = uri,
            filePath = "/storage/emulated/0/Music/Queen - Bohemian Rhapsody.flac",
            mimeType = "audio/flac",
            sizeBytes = 45000000L,
            artworkUri = artUri,
            dateAddedSec = 1700000000L,
        )

        val playable = localTrack.toPlayableTrack()

        assertTrue(playable.isLocal)
        assertEquals("Bohemian Rhapsody", playable.title)
        assertEquals("Queen", playable.artist)
        assertEquals("A Night at the Opera", playable.album)
        assertEquals(354000L, playable.durationMs)
        assertEquals("content://media/external/audio/media/1234", playable.playbackUrl)
        assertEquals("local_media_1234", playable.internalTrackId)
        assertEquals("content://media/external/audio/albumart/567", playable.artworkUrl)
    }

    @Test
    fun toPlayableTrack_withoutArtwork_hasNullArtworkUrl() {
        val uri = Uri.parse("content://media/external/audio/media/999")
        val localTrack = LocalAudioTrack(
            id = 999L,
            title = "Track Without Art",
            artist = "Unknown Artist",
            album = "",
            durationMs = 180000L,
            contentUri = uri,
            filePath = "/storage/emulated/0/Music/track.mp3",
            mimeType = "audio/mpeg",
            sizeBytes = 5000000L,
            artworkUri = null,
            dateAddedSec = 1700000000L,
        )

        val playable = localTrack.toPlayableTrack()

        assertTrue(playable.isLocal)
        assertNull(playable.artworkUrl)
        assertNull(playable.album)
    }

    @Test
    fun toPlayableTrack_infersMimeTypeFromExtension() {
        val uri = Uri.parse("content://media/external/audio/media/100")
        val localTrack = LocalAudioTrack(
            id = 100L,
            title = "Wav Song",
            artist = "Artist",
            album = "Album",
            durationMs = 60000L,
            contentUri = uri,
            filePath = "/storage/emulated/0/Music/song.wav",
            mimeType = "",
            sizeBytes = 1000000L,
        )

        val playable = localTrack.toPlayableTrack()
        assertEquals("audio/x-wav", playable.playbackMimeType)
    }
}
