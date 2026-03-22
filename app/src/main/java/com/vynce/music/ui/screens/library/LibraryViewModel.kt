package com.vynce.music.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vynce.music.data.model.Song
import com.vynce.music.data.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    songRepository: SongRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val uiState: StateFlow<LibraryUiState> = combine(
        songRepository.getAllSongs(),
        _searchQuery
    ) { songs, query ->
        if (songs.isEmpty()) {
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
            
            if (filteredSongs.isEmpty()) {
                LibraryUiState.SearchEmpty
            } else {
                LibraryUiState.Success(filteredSongs)
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
}

sealed class LibraryUiState {
    object Loading : LibraryUiState()
    data class Success(val songs: List<Song>) : LibraryUiState()
    object Empty : LibraryUiState()
    object SearchEmpty : LibraryUiState()
    data class Error(val message: String) : LibraryUiState()
}
