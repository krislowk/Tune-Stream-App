package com.vynce.music.ui.screens.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vynce.music.provider.YoutubeProvider
import com.vynce.music.utils.SyncUtils
import com.vynce.vynceclient.pages.PlaylistPage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val repository: YoutubeProvider,
    private val syncUtils: SyncUtils
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

    fun syncPlaylist(browseId: String, playlistId: String) {
        syncUtils.syncPlaylist(browseId, playlistId)
    }

    fun registerPendingAdd(browseId: String, songId: String) {
        syncUtils.registerPendingAdd(browseId, songId)
    }

    fun unregisterPendingAdd(browseId: String, songId: String) {
        syncUtils.unregisterPendingAdd(browseId, songId)
    }

    fun removeFromPlaylist(browseId: String, songId: String, playlistId: String, getSetVideoId: suspend () -> String?) {
        syncUtils.scheduleRemoveFromPlaylist(browseId, songId, playlistId, getSetVideoId)
    }

    suspend fun removeFromPlaylistSuspend(browseId: String, songId: String, setVideoId: String, playlistId: String) {
        syncUtils.removeFromPlaylistAndAwaitSync(browseId, songId, setVideoId, playlistId)
    }

    fun toggleLike(playlistId: String, like: Boolean) {
        syncUtils.likePlaylist(playlistId, like)
    }
}















