package com.vynce.music.ui.screens.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.vynce.music.data.model.User
import com.vynce.music.data.repository.UserRepository
import com.vynce.vynceclient.Youtube
import com.vynce.vynceclient.pages.HomePage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    userRepository: UserRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState = _uiState.asStateFlow()

    val currentUser: StateFlow<User?> = userRepository.currentUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    var refreshing by mutableStateOf(false)
        private set
    private var currentParams: String? = null

    fun refresh() {
        viewModelScope.launch {
            refreshing = true
            fetchHomeSuspend(currentParams)
            refreshing = false
        }
    }

    init {
        fetchHome(null)
    }

    fun onFilterSelected(filter: HomePage.Filter) {
        fetchHome(filter.endpoint.params)
    }

    fun fetchHome(params: String? = null) {
        viewModelScope.launch {
            fetchHomeSuspend(params)
        }
    }

    private suspend fun fetchHomeSuspend(params: String? = null) {
        currentParams = params
        if (_uiState.value !is HomeUiState.Success) {
            _uiState.value = HomeUiState.Loading
        }
        Youtube.home(params = params)
            .onSuccess { _uiState.value = HomeUiState.Success(it) }
            .onFailure {
                FirebaseCrashlytics.getInstance().recordException(it)
                _uiState.value = HomeUiState.Error(it.message ?: "Unknown error")
            }
    }
}

sealed class HomeUiState {
    data object Loading : HomeUiState()
    data class Success(val data: HomePage) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}
