package com.lastwave.app.data.local

import android.net.Uri
import com.lastwave.app.data.generate.GeneratedTrack
import android.app.Application
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE, application = Application::class)
class LocalPlaylistHelperTest {

    @Test
    fun filterDuplicates_emptyIncoming_returnsEmptyAndZeroCount() {
        val existing = listOf(
            GeneratedTrack(name = "Song A", artist = "Artist A", isLocal = true, url = "content://media/1"),
        )
        val result = LocalPlaylistHelper.filterDuplicates(existing, emptyList())

        assertTrue(result.uniqueTracks.isEmpty())
        assertEquals(0, result.duplicateCount)
    }

    @Test
    fun filterDuplicates_allUnique_addsAllTracks() {
        val existing = listOf(
            GeneratedTrack(name = "Song A", artist = "Artist A", isLocal = true, url = "content://media/1"),
        )
        val incoming = listOf(
            GeneratedTrack(name = "Song B", artist = "Artist B", isLocal = true, url = "content://media/2"),
            GeneratedTrack(name = "Song C", artist = "Artist C", isLocal = true, url = "content://media/3"),
        )

        val result = LocalPlaylistHelper.filterDuplicates(existing, incoming)

        assertEquals(2, result.uniqueTracks.size)
        assertEquals(0, result.duplicateCount)
        assertEquals("Song B", result.uniqueTracks[0].name)
        assertEquals("Song C", result.uniqueTracks[1].name)
    }

    @Test
    fun filterDuplicates_filtersMatchesInExistingPlaylistByLocalUrl() {
        val existing = listOf(
            GeneratedTrack(name = "Song A", artist = "Artist A", isLocal = true, url = "content://media/1"),
        )
        val incoming = listOf(
            GeneratedTrack(name = "Song A Renamed", artist = "Artist A", isLocal = true, url = "content://media/1"),
            GeneratedTrack(name = "Song B", artist = "Artist B", isLocal = true, url = "content://media/2"),
        )

        val result = LocalPlaylistHelper.filterDuplicates(existing, incoming)

        assertEquals(1, result.uniqueTracks.size)
        assertEquals(1, result.duplicateCount)
        assertEquals("Song B", result.uniqueTracks[0].name)
    }

    @Test
    fun filterDuplicates_filtersDuplicatesWithinBatchItself() {
        val existing = emptyList<GeneratedTrack>()
        val incoming = listOf(
            GeneratedTrack(name = "Song A", artist = "Artist A", isLocal = true, url = "content://media/1"),
            GeneratedTrack(name = "Song A", artist = "Artist A", isLocal = true, url = "content://media/1"),
            GeneratedTrack(name = "Song B", artist = "Artist B", isLocal = true, url = "content://media/2"),
        )

        val result = LocalPlaylistHelper.filterDuplicates(existing, incoming)

        assertEquals(2, result.uniqueTracks.size)
        assertEquals(1, result.duplicateCount)
        assertEquals("content://media/1", result.uniqueTracks[0].url)
        assertEquals("content://media/2", result.uniqueTracks[1].url)
    }

    @Test
    fun toGeneratedTrack_mapsLocalAudioTrackCorrectly() {
        val localTrack = LocalAudioTrack(
            id = 42L,
            title = "Midnight City",
            artist = "M83",
            album = "Hurry Up, We're Dreaming",
            durationMs = 243000L,
            contentUri = Uri.parse("content://media/external/audio/media/42"),
            filePath = "/storage/emulated/0/Music/m83.flac",
            mimeType = "audio/flac",
            sizeBytes = 25000000L,
            artworkUri = Uri.parse("content://media/external/audio/albumart/10"),
            dateAddedSec = 1600000000L,
        )

        val generated = localTrack.toGeneratedTrack()

        assertTrue(generated.isLocal)
        assertEquals("Midnight City", generated.name)
        assertEquals("M83", generated.artist)
        assertEquals("Hurry Up, We're Dreaming", generated.album)
        assertEquals(243000L, generated.durationMs)
        assertEquals("content://media/external/audio/media/42", generated.url)
        assertEquals("audio/flac", generated.mimeType)
        assertEquals("content://media/external/audio/albumart/10", generated.artworkUrl)
    }

    @Test
    fun isLocalTrackAccessible_withInvalidUrls_returnsFalse() {
        val context = RuntimeEnvironment.getApplication()

        assertFalse(LocalPlaylistHelper.isLocalTrackAccessible(context, null))
        assertFalse(LocalPlaylistHelper.isLocalTrackAccessible(context, ""))
        assertFalse(LocalPlaylistHelper.isLocalTrackAccessible(context, "file:///non/existent/path/song.mp3"))
    }
}
