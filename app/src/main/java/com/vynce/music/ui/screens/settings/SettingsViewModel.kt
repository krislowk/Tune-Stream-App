package com.vynce.music.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.vynce.music.data.repository.PreferenceRepository
import com.vynce.vynceclient.Youtube
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferenceRepository: PreferenceRepository
) : ViewModel() {
    private val _isLoggedIn = MutableStateFlow(preferenceRepository.getCookie() != null)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _username = MutableStateFlow<String?>(null)
    val username: StateFlow<String?> = _username.asStateFlow()

    private val _userEmail = MutableStateFlow<String?>(null)
    val userEmail: StateFlow<String?> = _userEmail.asStateFlow()

    private val _userThumbnail = MutableStateFlow<String?>(null)
    val userThumbnail: StateFlow<String?> = _userThumbnail.asStateFlow()

    // Preferences (StateFlows for UI state)
    val audioQuality = MutableStateFlow("High (256kbps)")
    val downloadQuality = MutableStateFlow("High (256kbps)")
    val wifiOnlyDownloads = MutableStateFlow(true)
    val normalizeVolume = MutableStateFlow(false)
    val skipSilence = MutableStateFlow(false)
    val crossfadeEnabled = MutableStateFlow(false)
    val crossfadeDuration = MutableStateFlow(5) // seconds
    val dynamicColors = MutableStateFlow(true)
    val restrictedMode = MutableStateFlow(false)
    val useLoginForBrowse = MutableStateFlow(Youtube.useLoginForBrowse)
    val proxyEnabled = MutableStateFlow(false)
    val playbackSpeed = MutableStateFlow(1.0f)
    val sleepTimerActive = MutableStateFlow(false)
    val externalPlayerEnabled = MutableStateFlow(false)
    val language = MutableStateFlow("English")
    val contentRegion = MutableStateFlow("United States")
    val showLyricsOnLockscreen = MutableStateFlow(true)
    val enableHistory = MutableStateFlow(true)
    val pauseHistory = MutableStateFlow(false)
    val audioOutput = MutableStateFlow("System Default")
    val bufferSize = MutableStateFlow("Medium")
    val batteryOptimization = MutableStateFlow(true)

    init {
        Youtube.cookie = preferenceRepository.getCookie()
        Youtube.visitorData = preferenceRepository.getVisitorData() ?: ""
        refreshAccountInfo()
    }

    fun refreshAccountInfo() {
        if (Youtube.cookie != null) {
            viewModelScope.launch {
                Youtube.accountInfo().onSuccess { info ->
                    _username.value = info.name
                    _userEmail.value = info.email
                    _userThumbnail.value = info.thumbnail
                    _isLoggedIn.value = true
                    // Update visitorData if it changed during info refresh
                    preferenceRepository.saveVisitorData(Youtube.visitorData)
                }.onFailure {
                    FirebaseCrashlytics.getInstance().recordException(it)
                    // Cookie might be expired
                    logout()
                }
            }
        }
    }

    fun login(cookie: String) {
        viewModelScope.launch {
            preferenceRepository.saveCookie(cookie)
            refreshAccountInfo()
        }
    }

    fun logout() {
        viewModelScope.launch {
            preferenceRepository.clearSession()
            _isLoggedIn.value = false
            _username.value = null
            _userEmail.value = null
            _userThumbnail.value = null
        }
    }
    
    fun toggleUseLoginForBrowse(enabled: Boolean) {
        useLoginForBrowse.value = enabled
        Youtube.useLoginForBrowse = enabled
    }
}
