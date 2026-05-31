package com.vynce.music.ui.screens.settings

import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vynce.music.repository.PreferenceRepository
import com.vynce.music.repository.SongRepository
import com.vynce.music.repository.UserRepository
import com.vynce.music.repository.constants.PreferenceConstants
import com.vynce.music.utils.SyncStatus
import com.vynce.music.utils.SyncUtils
import com.vynce.vynceclient.YouTube
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferenceRepository: PreferenceRepository,
    private val userRepository: UserRepository,
    private val songRepository: SongRepository,
    private val syncUtils: SyncUtils
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
    
    val currentUser = userRepository.currentUser.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    val isLoggedIn = currentUser.map { it != null }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        false
    )

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

    val isSyncing: StateFlow<Boolean> = syncUtils.syncState
        .map { it.overallStatus is SyncStatus.Syncing }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _isDeepSyncing = MutableStateFlow(false)
    val isDeepSyncing = _isDeepSyncing.asStateFlow()

    init {
        initializeSession()
    }

    private fun initializeSession() {
        viewModelScope.launch {
            preferenceRepository.ensureInitialized()
            userRepository.refreshAccountInfo()
        }
    }

    fun refreshAccountInfo() {
        viewModelScope.launch {
            userRepository.refreshAccountInfo()
        }
    }

    fun login(
        cookie: String,
        visitorData: String? = null
    ) {
        viewModelScope.launch {
            userRepository.login(cookie, visitorData)
        }
    }

    fun logout() {
        viewModelScope.launch {
            userRepository.logout()
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

    fun syncOnlineData() {
        syncUtils.runAllSyncs()
    }

    fun cleanupDuplicates() {
        syncUtils.cleanupDuplicatePlaylists()
    }

    fun syncAllAlbums() {
        syncUtils.syncAllAlbums()
    }

    fun syncLikedSongs() = syncUtils.syncLikedSongs()
    fun syncLibrarySongs() = syncUtils.syncLibrarySongs()
    fun syncUploadedSongs() = syncUtils.syncUploadedSongs()
    fun syncLikedAlbums() = syncUtils.syncLikedAlbums()
    fun syncUploadedAlbums() = syncUtils.syncUploadedAlbums()
    fun syncArtists() = syncUtils.syncArtistsSubscriptions()
    fun syncPlaylists() = syncUtils.syncSavedPlaylists()
    fun syncAutoPlaylists() = syncUtils.syncAutoSyncPlaylists()

    fun syncAllArtists() {
        syncUtils.syncAllArtists()
    }

    fun syncPodcasts() {
        syncUtils.syncPodcastSubscriptions()
        syncUtils.syncEpisodesForLater()
    }

    fun clearPodcastData() {
        syncUtils.clearPodcastData()
    }

    fun cancelAllSyncs() {
        syncUtils.cancelAllSyncs()
    }

    fun deepSync() {
        viewModelScope.launch {
            _isDeepSyncing.value = true
            try {
                syncUtils.performFullSyncSuspend()
                syncUtils.syncLikedSongsSuspend()
                syncUtils.syncLibrarySongsSuspend()
                syncUtils.syncUploadedSongsSuspend()
                syncUtils.syncLikedAlbumsSuspend()
                syncUtils.syncUploadedAlbumsSuspend()
                syncUtils.syncArtistsSubscriptionsSuspend()
                syncUtils.syncPodcastSubscriptionsSuspend()
                syncUtils.syncEpisodesForLaterSuspend()
                syncUtils.syncSavedPlaylistsSuspend()
                syncUtils.syncAutoSyncPlaylistsSuspend()
                syncUtils.cleanupDuplicatePlaylistsSuspend()
            } finally {
                _isDeepSyncing.value = false
            }
        }
    }

    fun clearAllSynced() {
        syncUtils.clearAllSyncedContent()
    }

    fun clearAllLibraryData() {
        viewModelScope.launch {
            syncUtils.clearAllSyncedContentSuspend()
            syncUtils.clearAllLibraryData()
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

    fun setCrossfadeDuration(duration: Int) {
        updatePreference(
            PreferenceConstants.CROSSFADE_DURATION,
            crossfadeDuration,
            duration
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