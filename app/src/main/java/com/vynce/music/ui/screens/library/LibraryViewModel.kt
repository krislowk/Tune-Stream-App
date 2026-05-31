package com.vynce.music.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vynce.music.models.Song
import com.vynce.music.repository.SongRepository
import com.vynce.music.repository.UserRepository
import com.vynce.music.repository.constants.LibraryFilter
import com.vynce.music.utils.SyncUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val syncUtils: SyncUtils,
    userRepository: UserRepository
) : ViewModel() {

    val currentUser = userRepository.currentUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val recentlyPlayed: StateFlow<List<Song>> = songRepository.getRecentlyPlayed(15)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            songRepository.cleanupHistory()
        }
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedTab = MutableStateFlow(LibraryFilter.LIKED_SONGS)
    val selectedTab = _selectedTab.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<LibraryUiState> = combine(
        _selectedTab,
        _searchQuery
    ) { tab, query ->
        tab to query
    }.flatMapLatest { (tab, query) ->
        val itemsFlow: Flow<List<Any>> = when (tab) {
            LibraryFilter.LIKED_SONGS -> songRepository.getLikedSongs()
            LibraryFilter.PLAYLISTS -> songRepository.getPlaylists()
            LibraryFilter.LOCAL_SONGS -> songRepository.getLocalSongs()
            LibraryFilter.LIKED_ALBUMS -> songRepository.getLikedAlbums()
            LibraryFilter.BOOKMARKED_ARTISTS -> songRepository.getBookmarkedArtists()
            LibraryFilter.PODCASTS -> songRepository.getSubscribedPodcasts()
            LibraryFilter.ALL_SONGS -> songRepository.getAllSongs()
            LibraryFilter.HISTORY -> songRepository.getHistory().map { historyList ->
                historyList.mapNotNull { history ->
                    songRepository.getSongByMediaId(history.mediaId)
                }
            }
        }
        
        itemsFlow.map { items: List<Any> ->
            if (items.isEmpty() && query.isBlank()) {
                LibraryUiState.Empty
            } else {
                val filteredItems = if (query.isBlank()) {
                    items
                } else {
                    items.filter { item: Any ->
                        when (item) {
                            is Song -> item.title.contains(query, ignoreCase = true) ||
                                    item.artist.contains(query, ignoreCase = true)
                            is com.vynce.music.db.entities.Playlist -> item.playlist.name.contains(query, ignoreCase = true)
                            is com.vynce.music.db.entities.Album -> item.album.title.contains(query, ignoreCase = true)
                            is com.vynce.music.db.entities.Artist -> item.artist.name.contains(query, ignoreCase = true)
                            else -> false
                        }
                    }
                }

                if (filteredItems.isEmpty() && query.isNotBlank()) {
                    LibraryUiState.SearchEmpty
                } else if (filteredItems.isEmpty()) {
                    LibraryUiState.Empty
                } else {
                    LibraryUiState.Success(filteredItems)
                }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LibraryUiState.Loading
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onTabSelected(filter: LibraryFilter) {
        _selectedTab.value = filter
    }

    fun toggleLike(song: Song) {
        viewModelScope.launch {
            songRepository.toggleLike(song)
        }
    }

    fun scanLocalSongs() {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                songRepository.scanLocalSongs()
            } catch (e: Exception) {
                // Handle or log error
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun togglePodcastSave(podcastId: String, save: Boolean) {
        syncUtils.savePodcast(podcastId, save)
    }

    fun toggleEpisodeSave(episodeId: String, save: Boolean, setVideoId: String? = null) {
        syncUtils.saveEpisode(episodeId, save, setVideoId)
    }
}

sealed class LibraryUiState {
    object Loading : LibraryUiState()
    data class Success(val items: List<Any>) : LibraryUiState()
    object Empty : LibraryUiState()
    object SearchEmpty : LibraryUiState()
    data class Error(val message: String) : LibraryUiState()
}











