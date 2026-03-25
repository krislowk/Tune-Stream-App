package com.vynce.music.ui.screens.player

import android.app.Application
import android.content.ComponentName
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.vynce.music.data.model.Song
import com.vynce.music.data.repository.SongRepository
import com.vynce.music.service.PlayerService
import com.vynce.music.utils.toMediaItem
import com.vynce.vynceclient.Youtube
import com.vynce.vynceclient.models.WatchEndpoint
import com.vynce.vynceclient.pages.NextResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class PlayerUiState(
    val currentTrack: MediaItem? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val isBuffering: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val shuffleEnabled: Boolean = false,
    val queue: List<MediaItem> = emptyList(),
    val upNext: List<MediaItem> = emptyList(),
    val relatedSongs: List<MediaItem> = emptyList(),
    val isFetchingMetadata: Boolean = false,
    val isLiked: Boolean = false,
    val isAutoplayEnabled: Boolean = true
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val application: Application,
    private val songRepository: SongRepository
) : ViewModel() {

    private var controller: MediaController? = null
    private val controllerFuture: ListenableFuture<MediaController> by lazy {
        val token = SessionToken(application, ComponentName(application, PlayerService::class.java))
        MediaController.Builder(application, token).buildAsync()
    }

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState = _uiState.asStateFlow()

    private val _lyrics = MutableStateFlow<String?>(null)
    val lyrics = _lyrics.asStateFlow()

    private var metadataJob: Job? = null
    private var lastFetchedMediaId: String? = null

    init {
        setupController()
        startPositionUpdates()
    }

    private fun setupController() {
        controllerFuture.addListener({
            controller = controllerFuture.get().apply {
                addListener(playerListener)
                syncState()
            }
        }, MoreExecutors.directExecutor())
    }

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (events.containsAny(
                    Player.EVENT_MEDIA_ITEM_TRANSITION,
                    Player.EVENT_PLAYBACK_STATE_CHANGED,
                    Player.EVENT_IS_PLAYING_CHANGED,
                    Player.EVENT_REPEAT_MODE_CHANGED,
                    Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED,
                    Player.EVENT_TIMELINE_CHANGED
                )
            ) {
                syncState()
            }

            if (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)) {
                player.currentMediaItem?.let { item ->
                    onTrackTransition(item)
                }
            }
        }
    }

    private fun onTrackTransition(item: MediaItem) {
        val videoId = item.mediaId
        if (videoId == lastFetchedMediaId) return
        
        updateLikedState(videoId)
        saveToHistory(item)
        fetchMetadata(videoId)
    }

    private fun updateLikedState(videoId: String) = viewModelScope.launch {
        val isLiked = songRepository.isLiked(videoId)
        _uiState.update { it.copy(isLiked = isLiked) }
    }

    fun toggleLike() = viewModelScope.launch {
        val currentTrack = _uiState.value.currentTrack ?: return@launch
        val song = Song(
            title = currentTrack.mediaMetadata.title?.toString() ?: "Unknown",
            artist = currentTrack.mediaMetadata.artist?.toString() ?: "Unknown",
            album = currentTrack.mediaMetadata.albumTitle?.toString(),
            duration = 0,
            contentUri = currentTrack.mediaId,
            thumbnail = currentTrack.mediaMetadata.artworkUri?.toString() ?: "",
            isYoutube = true
        )
        songRepository.toggleLike(song)
        _uiState.update { it.copy(isLiked = !it.isLiked) }
    }

    fun toggleAutoplay() {
        _uiState.update { it.copy(isAutoplayEnabled = !it.isAutoplayEnabled) }
        // If enabled, trigger a fetch for the current track to populate the queue
        _uiState.value.currentTrack?.let { fetchMetadata(it.mediaId, force = true) }
    }

    private fun saveToHistory(mediaItem: MediaItem) = viewModelScope.launch {
        val song = Song(
            title = mediaItem.mediaMetadata.title?.toString() ?: "Unknown",
            artist = mediaItem.mediaMetadata.artist?.toString() ?: "Unknown",
            album = mediaItem.mediaMetadata.albumTitle?.toString(),
            duration = 0,
            contentUri = mediaItem.mediaId,
            thumbnail = mediaItem.mediaMetadata.artworkUri?.toString() ?: ""
        )
        songRepository.markAsPlayed(song)
    }

    private fun syncState() {
        controller?.let { p ->
            val currentItem = p.currentMediaItem
            _uiState.update { state ->
                state.copy(
                    currentTrack = currentItem,
                    isPlaying = p.isPlaying,
                    duration = if (p.duration != C.TIME_UNSET) p.duration else 0L,
                    isBuffering = p.playbackState == Player.STATE_BUFFERING,
                    repeatMode = p.repeatMode,
                    shuffleEnabled = p.shuffleModeEnabled,
                    queue = List(p.mediaItemCount) { p.getMediaItemAt(it) }
                )
            }
        }
    }

    private fun startPositionUpdates() = viewModelScope.launch {
        while (isActive) {
            controller?.let { p ->
                if (_uiState.value.currentPosition != p.currentPosition) {
                    _uiState.update { it.copy(currentPosition = p.currentPosition) }
                }
            }
            delay(500)
        }
    }

    private fun fetchMetadata(videoId: String, force: Boolean = false) {
        if (!force && videoId == lastFetchedMediaId) return
        lastFetchedMediaId = videoId

        metadataJob?.cancel()
        metadataJob = viewModelScope.launch {
            _uiState.update { it.copy(isFetchingMetadata = true) }
            try {
                val result = withContext(Dispatchers.IO) {
                    Youtube.next(WatchEndpoint(videoId)).getOrNull()
                } ?: run {
                    _uiState.update { it.copy(isFetchingMetadata = false) }
                    return@launch
                }

                // Load sections in parallel
                launch { loadLyrics(result) }
                launch { loadRelated(result) }
                launch { handleNextResult(result) }

            } catch (e: Exception) {
                if (e !is CancellationException) {
                    FirebaseCrashlytics.getInstance().recordException(e)
                }
                _uiState.update { it.copy(isFetchingMetadata = false) }
            }
        }
    }

    private suspend fun loadLyrics(result: NextResult) {
        val lyricsText = withContext(Dispatchers.IO) {
            result.lyricsEndpoint?.let { Youtube.lyrics(it).getOrNull() }
        }
        _lyrics.value = lyricsText ?: "Lyrics not available."
    }

    private suspend fun loadRelated(result: NextResult) {
        val related = withContext(Dispatchers.IO) {
            result.relatedEndpoint?.let { Youtube.related(it).getOrNull() }
        }
        _uiState.update { state ->
            state.copy(relatedSongs = related?.songs?.map { it.toMediaItem() } ?: emptyList())
        }
    }

    private suspend fun handleNextResult(result: NextResult) {
        val allSongs = withContext(Dispatchers.IO) {
            result.items.map { it.toMediaItem() }
        }
        val currentIndexInResult = result.currentIndex ?: 0
        
        // Update UI state for "Up Next" tab
        val upNextList = if (currentIndexInResult >= 0 && currentIndexInResult < allSongs.size) {
            allSongs.drop(currentIndexInResult + 1)
        } else {
            allSongs
        }

        _uiState.update { it.copy(
            upNext = upNextList,
            isFetchingMetadata = false 
        ) }

        // Sync with Player Queue if Autoplay is on
        controller?.let { p ->
            if (_uiState.value.isAutoplayEnabled && allSongs.isNotEmpty()) {
                val currentQueueIds = (0 until p.mediaItemCount).map { p.getMediaItemAt(it).mediaId }
                val newQueueIds = allSongs.map { it.mediaId }

                // Only update if the queue is actually different to prevent flickering/re-transition
                if (currentQueueIds != newQueueIds) {
                    p.setMediaItems(allSongs, currentIndexInResult, C.TIME_UNSET)
                }
            }
        }
    }

    /* ---------------- Player Controls ---------------- */

    fun play(mediaItem: MediaItem) = runPlayer {
        lastFetchedMediaId = null // Reset to ensure metadata fetch for new items
        setMediaItems(listOf(mediaItem), 0, C.TIME_UNSET)
        prepare()
        play()
    }

    fun playAll(items: List<MediaItem>, startIndex: Int = 0) = runPlayer {
        lastFetchedMediaId = null
        setMediaItems(items, startIndex, C.TIME_UNSET)
        prepare()
        play()
    }

    fun togglePlayPause() = runPlayer {
        if (isPlaying) pause() else play()
    }

    fun seekTo(position: Long) = runPlayer { seekTo(position) }

    fun skipNext() = runPlayer { seekToNext() }

    fun skipPrevious() = runPlayer { seekToPrevious() }

    fun addToQueue(item: MediaItem) = runPlayer { addMediaItem(item) }

    fun removeFromQueue(index: Int) = runPlayer { removeMediaItem(index) }

    fun moveQueueItem(from: Int, to: Int) = runPlayer { moveMediaItem(from, to) }

    fun playQueueItem(index: Int) = runPlayer {
        seekTo(index, 0)
        play()
    }

    fun toggleShuffle() = runPlayer {
        shuffleModeEnabled = !shuffleModeEnabled
    }

    fun toggleRepeat() = runPlayer {
        repeatMode = when (repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun setSpeed(speed: Float) = runPlayer { setPlaybackSpeed(speed) }

    private fun runPlayer(block: MediaController.() -> Unit) {
        controller?.apply(block)
    }

    override fun onCleared() {
        MediaController.releaseFuture(controllerFuture)
        super.onCleared()
    }
}
