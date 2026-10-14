package com.lastwave.app.ui.local

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lastwave.app.data.local.LocalAudioRepository
import com.lastwave.app.data.local.LocalAudioTrack
import com.lastwave.app.playback.MusicPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class LocalAudioPermissionState {
    CHECKING,
    GRANTED,
    DENIED,
    PERMANENTLY_DENIED,
}

enum class LocalSongSortOption(val label: String) {
    TITLE_AZ("Title (A-Z)"),
    ARTIST_AZ("Artist (A-Z)"),
    ALBUM_AZ("Album (A-Z)"),
    RECENT("Recently Added"),
    DURATION_DESC("Duration"),
    SIZE_DESC("File Size"),
}

enum class LocalTab(val label: String) {
    SONGS("Songs"),
    ARTISTS("Artists"),
    ALBUMS("Albums"),
}

data class LocalArtist(
    val name: String,
    val tracks: List<LocalAudioTrack>,
    val albumCount: Int,
    val artworkUri: Uri?,
)

data class LocalAlbum(
    val title: String,
    val artist: String,
    val tracks: List<LocalAudioTrack>,
    val artworkUri: Uri?,
    val totalDurationMs: Long,
)

@HiltViewModel
class LocalSongsViewModel @Inject constructor(
    private val repository: LocalAudioRepository,
    private val musicPlayer: MusicPlayer,
) : ViewModel() {

    private val _permissionState = MutableStateFlow(LocalAudioPermissionState.CHECKING)
    val permissionState: StateFlow<LocalAudioPermissionState> = _permissionState.asStateFlow()

    private val _rawTracks = MutableStateFlow<List<LocalAudioTrack>>(emptyList())
    val rawTracks: StateFlow<List<LocalAudioTrack>> = _rawTracks.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val searchQuery = MutableStateFlow("")
    val sortOption = MutableStateFlow(LocalSongSortOption.TITLE_AZ)

    val filteredTracks: StateFlow<List<LocalAudioTrack>> = combine(
        _rawTracks,
        searchQuery,
        sortOption,
    ) { tracks, query, sort ->
        val q = query.trim().lowercase()
        val filtered = if (q.isBlank()) {
            tracks
        } else {
            tracks.filter {
                it.title.lowercase().contains(q) ||
                    it.artist.lowercase().contains(q) ||
                    it.album.lowercase().contains(q)
            }
        }
        when (sort) {
            LocalSongSortOption.TITLE_AZ -> filtered.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
            LocalSongSortOption.ARTIST_AZ -> filtered.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.artist })
            LocalSongSortOption.ALBUM_AZ -> filtered.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.album })
            LocalSongSortOption.RECENT -> filtered.sortedByDescending { it.dateAddedSec }
            LocalSongSortOption.DURATION_DESC -> filtered.sortedByDescending { it.durationMs }
            LocalSongSortOption.SIZE_DESC -> filtered.sortedByDescending { it.sizeBytes }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val artists: StateFlow<List<LocalArtist>> = _rawTracks.combine(searchQuery) { tracks, query ->
        val q = query.trim().lowercase()
        tracks.groupBy { it.artist }
            .map { (artistName, artistTracks) ->
                val uniqueAlbums = artistTracks.map { it.album.trim() }.filter { it.isNotBlank() }.distinct()
                val latestArt = artistTracks.firstOrNull { it.artworkUri != null }?.artworkUri
                LocalArtist(
                    name = artistName,
                    tracks = artistTracks,
                    albumCount = uniqueAlbums.size,
                    artworkUri = latestArt,
                )
            }
            .filter { if (q.isBlank()) true else it.name.lowercase().contains(q) }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val albums: StateFlow<List<LocalAlbum>> = _rawTracks.combine(searchQuery) { tracks, query ->
        val q = query.trim().lowercase()
        tracks.filter { it.album.isNotBlank() }
            .groupBy { it.album.trim().lowercase() }
            .map { (_, albumTracks) ->
                val title = albumTracks.first().album
                val dominantArtist = albumTracks.groupBy { it.artist }.maxByOrNull { it.value.size }?.key ?: albumTracks.first().artist
                val latestArt = albumTracks.firstOrNull { it.artworkUri != null }?.artworkUri
                val duration = albumTracks.sumOf { it.durationMs }
                LocalAlbum(
                    title = title,
                    artist = dominantArtist,
                    tracks = albumTracks,
                    artworkUri = latestArt,
                    totalDurationMs = duration,
                )
            }
            .filter { if (q.isBlank()) true else it.title.lowercase().contains(q) || it.artist.lowercase().contains(q) }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onPermissionResult(isGranted: Boolean, permanentlyDenied: Boolean = false) {
        if (isGranted) {
            _permissionState.value = LocalAudioPermissionState.GRANTED
            loadTracks()
        } else if (permanentlyDenied) {
            _permissionState.value = LocalAudioPermissionState.PERMANENTLY_DENIED
        } else {
            _permissionState.value = LocalAudioPermissionState.DENIED
        }
    }

    fun loadTracks() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val scanned = withContext(Dispatchers.IO) { repository.getLocalAudioTracks() }
                _rawTracks.value = scanned
            } catch (e: Exception) {
                android.util.Log.e("LocalSongsViewModel", "Error scanning local songs", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadCustomFolder(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val custom = withContext(Dispatchers.IO) { repository.scanSafTree(uri) }
                if (custom.isNotEmpty()) {
                    _rawTracks.value = custom
                    _permissionState.value = LocalAudioPermissionState.GRANTED
                }
            } catch (e: Exception) {
                android.util.Log.e("LocalSongsViewModel", "Error scanning custom folder", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun playTrack(track: LocalAudioTrack, queue: List<LocalAudioTrack>? = null) {
        val currentList = queue ?: filteredTracks.value
        val playables = currentList.map { it.toPlayableTrack() }
        val startIndex = currentList.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        try {
            if (playables.isNotEmpty()) {
                musicPlayer.playQueue(
                    tracks = playables,
                    startIndex = startIndex,
                    sourceLabel = "Local Songs",
                )
            } else {
                musicPlayer.play(track.toPlayableTrack(), sourceLabel = "Local Songs", startRadio = false)
            }
        } catch (e: Exception) {
            android.util.Log.e("LocalSongsViewModel", "Could not play local track", e)
        }
    }

    fun playTracks(tracks: List<LocalAudioTrack>, startIndex: Int = 0, startShuffled: Boolean = false) {
        if (tracks.isEmpty()) return
        val playables = tracks.map { it.toPlayableTrack() }
        val actualStartIndex = if (startShuffled) playables.indices.random() else startIndex.coerceIn(0, playables.size - 1)
        try {
            musicPlayer.playQueue(
                tracks = playables,
                startIndex = actualStartIndex,
                sourceLabel = "Local Songs",
                startShuffled = startShuffled,
            )
        } catch (e: Exception) {
            android.util.Log.e("LocalSongsViewModel", "Could not play tracks queue", e)
        }
    }

    fun playAll(startShuffled: Boolean = false) {
        val currentTracks = filteredTracks.value
        if (currentTracks.isNotEmpty()) {
            playTracks(currentTracks, startIndex = 0, startShuffled = startShuffled)
        }
    }

    fun playNext(track: LocalAudioTrack) {
        try {
            musicPlayer.playNext(track.toPlayableTrack())
        } catch (e: Exception) {
            android.util.Log.e("LocalSongsViewModel", "Could not add to play next", e)
        }
    }

    fun addToQueue(track: LocalAudioTrack) {
        try {
            musicPlayer.addToQueue(track.toPlayableTrack())
        } catch (e: Exception) {
            android.util.Log.e("LocalSongsViewModel", "Could not add to queue", e)
        }
    }
}
