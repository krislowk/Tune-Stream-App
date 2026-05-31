package com.vynce.music.ui.screens.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vynce.music.provider.YoutubeProvider
import com.vynce.vynceclient.pages.ExplorePage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val youtubeProvider: YoutubeProvider
) : ViewModel() {

    private val _exploreData = MutableStateFlow<ExplorePage?>(null)
    val exploreData: StateFlow<ExplorePage?> = _exploreData.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    init {
        fetchExploreData()
    }

    fun fetchExploreData() {
        viewModelScope.launch {
            _isLoading.value = true
            youtubeProvider.getExplore()
                .catch { e ->
                    _isLoading.value = false
                    e.printStackTrace()
                }
                .collect { 
                    _exploreData.value = it 
                    _isLoading.value = false
                }
        }
    }

    fun loadMore() {
        val currentData = _exploreData.value
        val continuation = currentData?.continuation
        if (continuation != null && !_isLoadingMore.value) {
            viewModelScope.launch {
                _isLoadingMore.value = true
                youtubeProvider.getExplore(continuation)
                    .catch { e ->
                        _isLoadingMore.value = false
                        e.printStackTrace()
                    }
                    .collect { nextData ->
                        _exploreData.value = currentData.copy(
                            newReleaseAlbums = currentData.newReleaseAlbums + nextData.newReleaseAlbums,
                            moodAndGenres = currentData.moodAndGenres + nextData.moodAndGenres,
                            continuation = nextData.continuation
                        )
                        _isLoadingMore.value = false
                    }
            }
        }
    }
}












