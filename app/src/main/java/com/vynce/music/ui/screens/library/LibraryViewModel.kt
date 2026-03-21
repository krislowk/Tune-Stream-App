package com.vynce.music.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vynce.music.data.repository.PreferenceRepository
import com.vynce.vynceclient.Youtube
import com.vynce.vynceclient.YtStream
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val preferenceRepository: PreferenceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<StreamUiState>(StreamUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _loginDebugInfo = MutableStateFlow("")
    val loginDebugInfo = _loginDebugInfo.asStateFlow()

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        explicitNulls = true
    }

    init {
        fetchStream("l3XDIjQjkzk")
        updateLoginDebugInfo()
    }

    fun updateLoginDebugInfo() {
        val cookie = preferenceRepository.getCookie() ?: "NULL"
        val visitorData = preferenceRepository.getVisitorData() ?: "NULL"
        
        val sb = StringBuilder()
        sb.append("--- PreferenceRepository ---\n")
        sb.append("Cookie: ${cookie.take(30)}...${cookie.takeLast(30)}\n")
        sb.append("VisitorData: $visitorData\n\n")
        
        sb.append("--- Youtube.innerTube ---\n")
        sb.append("InnerTube Cookie Set: ${Youtube.innerTube.cookie != null}\n")
        sb.append("InnerTube VisitorData: ${Youtube.innerTube.visitorData}\n")
        
        viewModelScope.launch {
            Youtube.accountInfo().onSuccess { info ->
                sb.append("\n--- Account Info ---\n")
                sb.append("Name: ${info.name}\n")
                sb.append("Email: ${info.email}\n")
                _loginDebugInfo.value = sb.toString()
            }.onFailure {
                sb.append("\n--- Account Info Error ---\n")
                sb.append(it.message ?: "Unknown Error")
                _loginDebugInfo.value = sb.toString()
            }
        }
    }

    fun fetchStream(videoId: String) {

        viewModelScope.launch {

            _uiState.value = StreamUiState.Loading

            try {

                val result = YtStream.getVideoStream(videoId)

                if (result != null) {

                    val rawJson = json.encodeToString(result)

                    _uiState.value = StreamUiState.Success(
                        stream = result,
                        rawData = rawJson
                    )

                } else {

                    _uiState.value = StreamUiState.Error("Stream not found")

                }

            } catch (e: Exception) {

                _uiState.value = StreamUiState.Error(
                    e.message ?: "Unknown error"
                )
            }
        }
    }
}

sealed class StreamUiState {

    object Loading : StreamUiState()

    data class Success(
        val stream: String?,
        val rawData: String
    ) : StreamUiState()

    data class Error(
        val message: String
    ) : StreamUiState()
}