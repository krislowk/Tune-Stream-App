package com.vynce.music.ui.screens.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.vynce.vynceclient.Youtube
import com.vynce.vynceclient.pages.ExplorePage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExploreViewModel @Inject constructor() : ViewModel() {

    private val _exploreData = MutableStateFlow<ExplorePage?>(null)
    val exploreData: StateFlow<ExplorePage?> = _exploreData.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        fetchExploreData()
    }

    fun fetchExploreData() {
        viewModelScope.launch {
            _isLoading.value = true
            Youtube.explore()
                .onSuccess { 
                    _exploreData.value = it 
                    _isLoading.value = false
                }
                .onFailure {
                    _isLoading.value = false
                    FirebaseCrashlytics.getInstance().recordException(it)
                }
        }
    }
}
