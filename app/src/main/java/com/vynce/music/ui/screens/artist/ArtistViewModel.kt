package com.vynce.music.ui.screens.artist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vynce.music.provider.YoutubeProvider
import com.vynce.vynceclient.pages.ArtistPage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArtistViewModel @Inject constructor(
    private val repository: YoutubeProvider
) : ViewModel() {

    private val _artist = MutableStateFlow<ArtistPage?>(null)
    val artist = _artist.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun fetchArtist(browseId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getArtist(browseId)
                .catch { e -> 
                    _isLoading.value = false
                    e.printStackTrace()
                }
                .collect {
                    _isLoading.value = false
                    _artist.value = it
                }
        }
    }
}












