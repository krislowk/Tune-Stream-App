package com.vynce.music.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.vynce.music.data.repository.PreferenceRepository
import com.vynce.music.data.repository.SongRepository
import com.vynce.vynceclient.Youtube
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferenceRepository: PreferenceRepository,
    private val songRepository: SongRepository
) : ViewModel() {
    private val _isLoggedIn = MutableStateFlow(preferenceRepository.getCookie() != null)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _username = MutableStateFlow<String?>(null)
    val username: StateFlow<String?> = _username.asStateFlow()

    private val _userEmail = MutableStateFlow<String?>(null)
    val userEmail: StateFlow<String?> = _userEmail.asStateFlow()

    private val _userThumbnail = MutableStateFlow<String?>(null)
    val userThumbnail: StateFlow<String?> = _userThumbnail.asStateFlow()

    // Preferences
    val audioQuality = MutableStateFlow(preferenceRepository.getString("audio_quality", "High (256kbps)"))
    val downloadQuality = MutableStateFlow(preferenceRepository.getString("download_quality", "High (256kbps)"))
    val wifiOnlyDownloads = MutableStateFlow(preferenceRepository.getBoolean("wifi_only_downloads", true))
    val normalizeVolume = MutableStateFlow(preferenceRepository.getBoolean("normalize_volume", false))
    val skipSilence = MutableStateFlow(preferenceRepository.getBoolean("skip_silence", false))
    val crossfadeEnabled = MutableStateFlow(preferenceRepository.getBoolean("crossfade_enabled", false))
    val crossfadeDuration = MutableStateFlow(preferenceRepository.getInt("crossfade_duration", 5))
    val dynamicColors = MutableStateFlow(preferenceRepository.getBoolean("dynamic_colors", true))
    val restrictedMode = MutableStateFlow(preferenceRepository.getBoolean("restricted_mode", false))
    val useLoginForBrowse = MutableStateFlow(preferenceRepository.getBoolean("use_login_for_browse", Youtube.useLoginForBrowse))
    val proxyEnabled = MutableStateFlow(preferenceRepository.getBoolean("proxy_enabled", false))
    val playbackSpeed = MutableStateFlow(preferenceRepository.getFloat("playback_speed", 1.0f))
    val externalPlayerEnabled = MutableStateFlow(preferenceRepository.getBoolean("external_player_enabled", false))
    val language = MutableStateFlow(preferenceRepository.getString("language", "English"))
    val contentRegion = MutableStateFlow(preferenceRepository.getString("content_region", "United States"))
    val showLyricsOnLockscreen = MutableStateFlow(preferenceRepository.getBoolean("show_lyrics_lockscreen", true))
    val enableHistory = MutableStateFlow(preferenceRepository.getBoolean("enable_history", true))
    val audioOutput = MutableStateFlow(preferenceRepository.getString("audio_output", "System Default"))
    val bufferSize = MutableStateFlow(preferenceRepository.getString("buffer_size", "Medium"))
    val batteryOptimization = MutableStateFlow(preferenceRepository.getBoolean("battery_optimization", true))

    init {
        Youtube.cookie = preferenceRepository.getCookie()
        Youtube.visitorData = preferenceRepository.getVisitorData() ?: ""
        Youtube.useLoginForBrowse = useLoginForBrowse.value
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
                    preferenceRepository.saveVisitorData(Youtube.visitorData)
                }.onFailure {
                    FirebaseCrashlytics.getInstance().recordException(it)
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
    
    fun toggleBoolean(key: String, state: MutableStateFlow<Boolean>, value: Boolean) {
        state.value = value
        preferenceRepository.saveBoolean(key, value)
        
        // Specific logic for some settings
        when (key) {
            "use_login_for_browse" -> Youtube.useLoginForBrowse = value
        }
    }

    fun updateString(key: String, state: MutableStateFlow<String>, value: String) {
        state.value = value
        preferenceRepository.saveString(key, value)
    }

    fun updateFloat(key: String, state: MutableStateFlow<Float>, value: Float) {
        state.value = value
        preferenceRepository.saveFloat(key, value)
    }

    fun updateInt(key: String, state: MutableStateFlow<Int>, value: Int) {
        state.value = value
        preferenceRepository.saveInt(key, value)
    }

    fun setPlaybackSpeed(speed: Float) {
        playbackSpeed.value = speed
        preferenceRepository.saveFloat("playback_speed", speed)
    }

    fun setAudioQuality(quality: String) {
        audioQuality.value = quality
        preferenceRepository.saveString("audio_quality", quality)
    }

    fun setDownloadQuality(quality: String) {
        downloadQuality.value = quality
        preferenceRepository.saveString("download_quality", quality)
    }

    fun setLanguage(lang: String) {
        language.value = lang
        preferenceRepository.saveString("language", lang)
    }

    fun setContentRegion(region: String) {
        contentRegion.value = region
        preferenceRepository.saveString("content_region", region)
    }

    fun clearCache() {
        viewModelScope.launch {
            songRepository.clearCache()
        }
    }
}
