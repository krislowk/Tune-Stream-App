package com.vynce.music.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vynce.music.provider.YoutubeProvider
import com.vynce.vynceclient.YouTube
import com.vynce.vynceclient.models.SearchSuggestions
import com.vynce.vynceclient.pages.SearchResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val youtubeProvider: YoutubeProvider
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    private val _suggestions = MutableStateFlow<SearchSuggestions?>(null)
    val suggestions = _suggestions.asStateFlow()

    private val _searchResult = MutableStateFlow<SearchResult?>(null)
    val searchResult = _searchResult.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _selectedFilter = MutableStateFlow(YouTube.SearchFilter.FILTER_SONG)
    val selectedFilter = _selectedFilter.asStateFlow()

    fun updateQuery(newQuery: String) {
        _query.value = newQuery
        if (newQuery.isBlank()) {
            _suggestions.value = null
            _searchResult.value = null
            return
        }
        fetchSuggestions(newQuery)
    }

    fun setFilter(filter: YouTube.SearchFilter) {
        _selectedFilter.value = filter
        if (_query.value.isNotBlank()) {
            search(_query.value, filter)
        }
    }

    private fun fetchSuggestions(query: String) {
        viewModelScope.launch {
            delay(200)
            youtubeProvider.getSuggestions(query)
                .catch { e ->
                    e.printStackTrace()
                }
                .collect { _suggestions.value = it }
        }
    }

    fun search(query: String, filter: YouTube.SearchFilter = YouTube.SearchFilter.FILTER_SONG) {
        _query.value = query
        _suggestions.value = null
        viewModelScope.launch {
            _isLoading.value = true
            youtubeProvider.search(query, filter)
                .catch { e ->
                    _isLoading.value = false
                    e.printStackTrace()
                }
                .collect { 
                    _isLoading.value = false
                    _searchResult.value = it
                }
        }
    }
}












