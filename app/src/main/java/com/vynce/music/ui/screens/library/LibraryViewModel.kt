package com.vynce.music.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vynce.vynceclient.YtStream
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class LibraryViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<StreamUiState>(StreamUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        explicitNulls = true
    }

    init {
        fetchStream("l3XDIjQjkzk")
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