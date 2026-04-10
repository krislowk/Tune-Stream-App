package com.vynce.music.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.vynce.vynceclient.Youtube
import com.vynce.vynceclient.models.SearchSuggestions
import com.vynce.vynceclient.pages.SearchResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor() : ViewModel() {

    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    private val _suggestions = MutableStateFlow<SearchSuggestions?>(null)
    val suggestions = _suggestions.asStateFlow()

    private val _searchResult = MutableStateFlow<SearchResult?>(null)
    val searchResult = _searchResult.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun updateQuery(newQuery: String) {
        _query.value = newQuery
        if (newQuery.isBlank()) {
            _suggestions.value = null
            _searchResult.value = null
            return
        }
        fetchSuggestions(newQuery)
    }

    private fun fetchSuggestions(query: String) {
        viewModelScope.launch {
            delay(200)
            Youtube.searchSuggestions(query)
                .onSuccess { _suggestions.value = it }
                .onFailure {
                    FirebaseCrashlytics.getInstance().recordException(it)
                }
        }
    }

    fun search(query: String, filter: Youtube.SearchFilter = Youtube.SearchFilter.FILTER_SONG) {
        _query.value = query
        _suggestions.value = null
        viewModelScope.launch {
            _isLoading.value = true
            Youtube.search(query, filter)
                .onSuccess { 
                    _isLoading.value = false
                    _searchResult.value = it
                }
                .onFailure {
                    _isLoading.value = false
                    FirebaseCrashlytics.getInstance().recordException(it)
                }
        }
    }
}
