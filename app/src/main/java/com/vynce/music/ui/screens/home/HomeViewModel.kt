package com.vynce.music.ui.screens.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.vynce.music.data.model.User
import com.vynce.music.data.repository.UserRepository
import com.vynce.music.provider.YoutubeProvider
import com.vynce.vynceclient.pages.HomePage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: YoutubeProvider,
    private val userRepository: UserRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState = _uiState.asStateFlow()

    val currentUser: StateFlow<User?> = userRepository.currentUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private var _refreshing by  mutableStateOf(false)
    val refreshing = _refreshing

    private var fetchJob: Job? = null

    private var currentBrowseId = "FEmusic_home"
    private var currentParams: String? = null

    fun refresh() {
        viewModelScope.launch {
            _refreshing = true
            fetchHome(currentBrowseId, currentParams)
            // Ensure the refreshing state stays for a bit even if fast
            delay(500)
            _refreshing = false
        }
    }

    init {
        fetchHome()
    }

    fun onFilterSelected(filter: HomePage.Filter) {
        fetchHome(filter.endpoint.browseId, filter.endpoint.params)
    }

    fun fetchHome(browseId: String = "FEmusic_home", params: String? = null) {
        currentBrowseId = browseId
        currentParams = params
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            repository.getHome(browseId, params)
                .onStart {
                    if (_uiState.value !is HomeUiState.Success) {
                        _uiState.value = HomeUiState.Loading
                    }
                }
                .catch { e ->
                    FirebaseCrashlytics.getInstance().recordException(e)
                    _uiState.value = HomeUiState.Error(e.message ?: "Unknown error")
                }
                .collect { homePage ->
                    _uiState.value = HomeUiState.Success(homePage)
                }
        }
    }
}

sealed class HomeUiState {
    data object Loading : HomeUiState()
    data class Success(val data: HomePage) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}
