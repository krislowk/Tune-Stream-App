package com.vynce.music.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vynce.music.data.model.Song
import com.vynce.music.data.repository.SongRepository
import com.vynce.music.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab = _selectedTab.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<LibraryUiState> = combine(
        _selectedTab,
        _searchQuery
    ) { tab, query ->
        tab to query
    }.flatMapLatest { (tab, query) ->
        val songsFlow = when (tab) {
            0 -> songRepository.getLikedSongs() // Liked
            2 -> songRepository.getLocalSongs() // Songs (Local)
            else -> songRepository.getAllSongs() // Playlists, Albums, Artists (Filtered in Screen)
        }
        
        songsFlow.map { songs ->
            if (songs.isEmpty() && query.isBlank()) {
                LibraryUiState.Empty
            } else {
                val filteredSongs = if (query.isBlank()) {
                    songs
                } else {
                    songs.filter {
                        it.title.contains(query, ignoreCase = true) ||
                                it.artist.contains(query, ignoreCase = true)
                    }
                }

                if (filteredSongs.isEmpty() && query.isNotBlank()) {
                    LibraryUiState.SearchEmpty
                } else if (filteredSongs.isEmpty()) {
                    LibraryUiState.Empty
                } else {
                    LibraryUiState.Success(filteredSongs)
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

    fun onTabSelected(index: Int) {
        _selectedTab.value = index
    }

    fun toggleLike(song: Song) {
        viewModelScope.launch {
            songRepository.toggleLike(song)
        }
    }
}

sealed class LibraryUiState {
    object Loading : LibraryUiState()
    data class Success(val songs: List<Song>) : LibraryUiState()
    object Empty : LibraryUiState()
    object SearchEmpty : LibraryUiState()
    data class Error(val message: String) : LibraryUiState()
}
