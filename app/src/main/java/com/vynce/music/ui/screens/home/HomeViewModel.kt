package com.vynce.music.ui.screens.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.vynce.music.provider.YoutubeProvider
import com.vynce.vynceclient.pages.HomePage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: YoutubeProvider
) : ViewModel() {
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private var _refreshing by  mutableStateOf(false)
    val refreshing = _refreshing
    fun refresh() {
        viewModelScope.launch {
            _refreshing = true
            _uiState.value = HomeUiState.Loading
            fetchHome()
            delay(1000)
            _refreshing = false
        }
    }
    init {
        fetchHome()
    }

    fun fetchHome() {
        viewModelScope.launch {
            repository.getHome()
                .catch { e ->
                    FirebaseCrashlytics.getInstance().recordException(e)
                    if (_uiState.value !is HomeUiState.Success) {
                        _uiState.value = HomeUiState.Error(e.message ?: "Unknown error")
                    }
                }
                .collect { homePage ->
                    _uiState.value = HomeUiState.Success(homePage)
                }
        }
    }
}

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(val data: HomePage) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}
