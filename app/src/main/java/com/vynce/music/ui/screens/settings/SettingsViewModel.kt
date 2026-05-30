package com.vynce.music.ui.screens.settings

import android.util.Log
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vynce.music.repository.PreferenceRepository
import com.vynce.music.repository.SongRepository
import com.vynce.music.repository.constants.PreferenceConstants
import com.vynce.vynceclient.YouTube
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

    val language = MutableStateFlow(
        preferenceRepository.getString(
            PreferenceConstants.LANGUAGE,
            "English"
        )
    )

    val contentRegion = MutableStateFlow(
        preferenceRepository.getString(
            PreferenceConstants.CONTENT_REGION,
            "United States"
        )
    )

    val enableHistory = MutableStateFlow(
        preferenceRepository.getBoolean(
            PreferenceConstants.ENABLE_HISTORY,
            true
        )
    )

    val showLyricsOnLockscreen = MutableStateFlow(
        preferenceRepository.getBoolean(
            PreferenceConstants.SHOW_LYRICS_LOCKSCREEN,
            true
        )
    )

    val batteryOptimization = MutableStateFlow(
        preferenceRepository.getBoolean(
            PreferenceConstants.BATTERY_OPTIMIZATION,
            true
        )
    )

    val externalPlayerEnabled = MutableStateFlow(
        preferenceRepository.getBoolean(
            PreferenceConstants.EXTERNAL_PLAYER_ENABLED,
            false
        )
    )
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _username = MutableStateFlow<String?>(null)
    val username: StateFlow<String?> = _username.asStateFlow()

    private val _userEmail = MutableStateFlow<String?>(null)
    val userEmail: StateFlow<String?> = _userEmail.asStateFlow()

    private val _userThumbnail = MutableStateFlow<String?>(null)
    val userThumbnail: StateFlow<String?> = _userThumbnail.asStateFlow()

    // Preferences
    val audioQuality = MutableStateFlow(
        preferenceRepository.getString(
            PreferenceConstants.AUDIO_QUALITY,
            "Auto"
        )
    )

    val downloadQuality = MutableStateFlow(
        preferenceRepository.getString(
            PreferenceConstants.DOWNLOAD_QUALITY,
            "High (256kbps)"
        )
    )

    val wifiOnlyDownloads = MutableStateFlow(
        preferenceRepository.getBoolean(
            PreferenceConstants.WIFI_ONLY_DOWNLOADS,
            true
        )
    )

    val normalizeVolume = MutableStateFlow(
        preferenceRepository.getBoolean(
            PreferenceConstants.NORMALIZE_VOLUME,
            false
        )
    )

    val skipSilence = MutableStateFlow(
        preferenceRepository.getBoolean(
            PreferenceConstants.SKIP_SILENCE,
            false
        )
    )

    val crossfadeEnabled = MutableStateFlow(
        preferenceRepository.getBoolean(
            PreferenceConstants.CROSSFADE_ENABLED,
            false
        )
    )

    val crossfadeDuration = MutableStateFlow(
        preferenceRepository.getInt(
            PreferenceConstants.CROSSFADE_DURATION,
            5
        )
    )

    val dynamicColors = MutableStateFlow(
        preferenceRepository.getBoolean(
            PreferenceConstants.DYNAMIC_COLORS,
            true
        )
    )

    val restrictedMode = MutableStateFlow(
        preferenceRepository.getBoolean(
            PreferenceConstants.RESTRICTED_MODE,
            false
        )
    )

    val useLoginForBrowse = MutableStateFlow(
        preferenceRepository.getBoolean(
            PreferenceConstants.USE_LOGIN_FOR_BROWSE,
            YouTube.useLoginForBrowse
        )
    )

    val proxyEnabled = MutableStateFlow(
        preferenceRepository.getBoolean(
            PreferenceConstants.PROXY_ENABLED,
            false
        )
    )

    val playbackSpeed = MutableStateFlow(
        preferenceRepository.getFloat(
            PreferenceConstants.PLAYBACK_SPEED,
            1.0f
        )
    )

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    init {
        initializeSession()
    }

    private fun initializeSession() {
        viewModelScope.launch {

            preferenceRepository.ensureInitialized()

            val cookie = preferenceRepository.getCookie()
            val visitorData = preferenceRepository.getVisitorData()

            Log.d(
                "SettingsViewModel",
                "Initialized session cookie=${cookie != null}"
            )

            YouTube.cookie = cookie
            YouTube.visitorData = visitorData ?: ""
            YouTube.useLoginForBrowse = useLoginForBrowse.value

            if (!cookie.isNullOrBlank()) {
                refreshAccountInfo()
            }
        }
    }

    fun refreshAccountInfo() {

        val cookie = YouTube.cookie

        if (cookie.isNullOrBlank()) {
            _isLoggedIn.value = false
            return
        }

        viewModelScope.launch {

            try {

                Log.d("SettingsViewModel", "Refreshing account info")

                if (
                    YouTube.visitorData?.isBlank() == true ||
                    YouTube.visitorData == YouTube.DEFAULT_VISITOR_DATA
                ) {

                    Log.d(
                        "SettingsViewModel",
                        "Fetching fresh visitorData"
                    )

                    YouTube.visitorData()
                        .onSuccess { data ->

                            Log.d(
                                "SettingsViewModel",
                                "visitorData fetched: $data"
                            )

                            preferenceRepository.saveVisitorData(data)
                        }
                        .onFailure {
                            Log.e(
                                "SettingsViewModel",
                                "visitorData fetch failed",
                                it
                            )
                        }
                }

                YouTube.accountInfo()
                    .onSuccess { info ->

                        Log.d(
                            "SettingsViewModel",
                            "Logged in as ${info.name}"
                        )

                        _username.value = info.name
                        _userEmail.value = info.email
                        _userThumbnail.value = info.thumbnailUrl
                        _isLoggedIn.value = true
                    }
                    .onFailure { error ->

                        Log.e(
                            "SettingsViewModel",
                            "accountInfo failed",
                            error
                        )

                        _isLoggedIn.value = false
                    }

            } catch (e: Exception) {

                Log.e(
                    "SettingsViewModel",
                    "refreshAccountInfo crash",
                    e
                )

                _isLoggedIn.value = false
            }
        }
    }

    fun login(
        cookie: String,
        visitorData: String? = null
    ) {

        viewModelScope.launch {

            try {

                Log.d(
                    "SettingsViewModel",
                    "Starting login flow"
                )

                preferenceRepository.saveCookie(cookie)

                if (!visitorData.isNullOrBlank()) {

                    preferenceRepository.saveVisitorData(
                        visitorData
                    )

                } else {

                    Log.d(
                        "SettingsViewModel",
                        "No visitorData supplied, fetching"
                    )

                    YouTube.visitorData()
                        .onSuccess { data ->

                            preferenceRepository.saveVisitorData(
                                data
                            )
                        }
                        .onFailure {

                            Log.e(
                                "SettingsViewModel",
                                "visitorData fetch failed",
                                it
                            )
                        }
                }

                refreshAccountInfo()

            } catch (e: Exception) {

                Log.e(
                    "SettingsViewModel",
                    "Login failed",
                    e
                )
            }
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

    fun <T> updatePreference(
        key: Preferences.Key<T>,
        state: MutableStateFlow<T>,
        value: T
    ) {

        state.value = value

        viewModelScope.launch {

            preferenceRepository.set(key, value)

            if (key == PreferenceConstants.USE_LOGIN_FOR_BROWSE) {
                YouTube.useLoginForBrowse = value as Boolean
            }
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            songRepository.clearCache()
        }
    }

    fun scanLocalSongs() {

        viewModelScope.launch {

            _isScanning.value = true

            try {
                songRepository.scanLocalSongs()
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun setPlaybackSpeed(speed: Float) {

        updatePreference(
            PreferenceConstants.PLAYBACK_SPEED,
            playbackSpeed,
            speed
        )
    }

    fun setAudioQuality(quality: String) {

        updatePreference(
            PreferenceConstants.AUDIO_QUALITY,
            audioQuality,
            quality
        )
    }

    fun setDownloadQuality(quality: String) {

        updatePreference(
            PreferenceConstants.DOWNLOAD_QUALITY,
            downloadQuality,
            quality
        )
    }

    fun setLanguage(languageValue: String) {

        updatePreference(
            PreferenceConstants.LANGUAGE,
            language,
            languageValue
        )
    }

    fun setContentRegion(region: String) {

        updatePreference(
            PreferenceConstants.CONTENT_REGION,
            contentRegion,
            region
        )
    }

    fun toggleBoolean(
        key: Preferences.Key<Boolean>,
        state: MutableStateFlow<Boolean>,
        value: Boolean
    ) {

        updatePreference(
            key,
            state,
            value
        )
    }
}