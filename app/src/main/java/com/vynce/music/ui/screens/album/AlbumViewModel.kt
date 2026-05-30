package com.vynce.music.ui.screens.album

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vynce.music.provider.YoutubeProvider
import com.vynce.vynceclient.pages.AlbumPage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlbumViewModel @Inject constructor(
    private val repository: YoutubeProvider
) : ViewModel() {

    private val _album = MutableStateFlow<AlbumPage?>(null)
    val album = _album.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun fetchAlbum(browseId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getAlbum(browseId)
                .catch { e -> 
                    _isLoading.value = false
                    e.printStackTrace()
                }
                .collect {
                    _isLoading.value = false
                    _album.value = it
                }
        }
    }
}












