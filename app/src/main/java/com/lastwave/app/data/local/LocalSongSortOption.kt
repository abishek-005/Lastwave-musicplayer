package com.lastwave.app.data.local

/**
 * Stable sort options for the Local Songs screen.
 * [id] is the persisted DataStore key string (never ordinal or display text),
 * ensuring renames and reorderings never corrupt saved settings.
 */
enum class LocalSongSortOption(val id: String, val label: String) {
    TITLE_AZ("title_az", "Title (A-Z)"),
    ARTIST_AZ("artist_az", "Artist (A-Z)"),
    ALBUM_AZ("album_az", "Album (A-Z)"),
    RECENT("recent", "Recently Added"),
    DURATION_DESC("duration_desc", "Duration"),
    SIZE_DESC("size_desc", "File Size");

    companion object {
        fun fromId(id: String?): LocalSongSortOption =
            entries.firstOrNull { it.id == id } ?: TITLE_AZ
    }
}
