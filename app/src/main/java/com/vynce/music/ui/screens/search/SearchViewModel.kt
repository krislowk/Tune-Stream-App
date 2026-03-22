package com.vynce.music.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vynce.music.provider.YoutubeProvider
import com.vynce.vynceclient.Youtube
import com.vynce.vynceclient.models.SearchSuggestions
import com.vynce.vynceclient.pages.SearchResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: YoutubeProvider
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    private val _suggestions = MutableStateFlow<SearchSuggestions?>(null)
    val suggestions = _suggestions.asStateFlow()

    private val _searchResult = MutableStateFlow<SearchResult?>(null)
    val searchResult = _searchResult.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private var searchJob: Job? = null
    private var suggestionJob: Job? = null

    fun updateQuery(newQuery: String) {
        _query.value = newQuery
        if (newQuery.isBlank()) {
            _suggestions.value = null
            return
        }
        fetchSuggestions(newQuery)
    }

    private fun fetchSuggestions(query: String) {
        suggestionJob?.cancel()
        suggestionJob = viewModelScope.launch {
            repository.getSearchSuggestions(query)
                .catch { _suggestions.value = null }
                .collect { _suggestions.value = it }
        }
    }

    fun search(query: String, filter: Youtube.SearchFilter = Youtube.SearchFilter.FILTER_SONG) {
        _query.value = query
        _suggestions.value = null
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _isLoading.value = true
            repository.search(query, filter)
                .catch { 
                    _isLoading.value = false
                    _searchResult.value = null
                }
                .collect { 
                    _isLoading.value = false
                    _searchResult.value = it 
                }
        }
    }
}
