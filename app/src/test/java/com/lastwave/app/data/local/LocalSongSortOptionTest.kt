package com.lastwave.app.data.local

import org.junit.Assert.assertEquals
import org.junit.Test

class LocalSongSortOptionTest {

    @Test
    fun fromId_mapsAllValidIdsCorrectly() {
        assertEquals(LocalSongSortOption.TITLE_AZ, LocalSongSortOption.fromId("title_az"))
        assertEquals(LocalSongSortOption.ARTIST_AZ, LocalSongSortOption.fromId("artist_az"))
        assertEquals(LocalSongSortOption.ALBUM_AZ, LocalSongSortOption.fromId("album_az"))
        assertEquals(LocalSongSortOption.DATE_MODIFIED_DESC, LocalSongSortOption.fromId("recent"))
        assertEquals(LocalSongSortOption.DURATION_DESC, LocalSongSortOption.fromId("duration_desc"))
        assertEquals(LocalSongSortOption.SIZE_DESC, LocalSongSortOption.fromId("size_desc"))
    }

    @Test
    fun fromId_fallsBackToTitleAzOnUnknownOrNull() {
        assertEquals(LocalSongSortOption.TITLE_AZ, LocalSongSortOption.fromId(null))
        assertEquals(LocalSongSortOption.TITLE_AZ, LocalSongSortOption.fromId(""))
        assertEquals(LocalSongSortOption.TITLE_AZ, LocalSongSortOption.fromId("invalid_sort_id"))
        assertEquals(LocalSongSortOption.TITLE_AZ, LocalSongSortOption.fromId("TITLE_AZ"))
    }

    @Test
    fun id_matchesExpectedStoredKeys() {
        assertEquals("title_az", LocalSongSortOption.TITLE_AZ.id)
        assertEquals("artist_az", LocalSongSortOption.ARTIST_AZ.id)
        assertEquals("album_az", LocalSongSortOption.ALBUM_AZ.id)
        assertEquals("recent", LocalSongSortOption.DATE_MODIFIED_DESC.id)
        assertEquals("duration_desc", LocalSongSortOption.DURATION_DESC.id)
        assertEquals("size_desc", LocalSongSortOption.SIZE_DESC.id)
    }
}
