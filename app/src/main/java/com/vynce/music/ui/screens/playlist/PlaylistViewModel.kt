package com.vynce.music.ui.screens.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vynce.music.provider.YoutubeProvider
import com.vynce.vynceclient.pages.PlaylistPage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val repository: YoutubeProvider
) : ViewModel() {

    private val _playlist = MutableStateFlow<PlaylistPage?>(null)
    val playlist = _playlist.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun fetchPlaylist(playlistId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getPlaylist(playlistId)
                .catch { _isLoading.value = false }
                .collect {
                    _isLoading.value = false
                    _playlist.value = it
                }
        }
    }
}












