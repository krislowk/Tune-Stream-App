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
    private val upNextBuffer = ArrayDeque<MediaItem>()
    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState = _uiState.asStateFlow()

    private val _lyrics = MutableStateFlow<String?>(null)
    val lyrics = _lyrics.asStateFlow()

    private var metadataJob: kotlinx.coroutines.Job? = null
    private var lastFetchedVideoId: String? = null

    init {
        setupController()
        startPositionUpdates()
    }

    private fun setupController() {
        controllerFuture.addListener({
            try {
                controller = controllerFuture.get().apply {
                    addListener(playerListener)
                    syncState()
                }
            } catch (e: Exception) {
                FirebaseCrashlytics.getInstance().recordException(e)
            }
        }, MoreExecutors.directExecutor())
    }

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (events.containsAny(
                    Player.EVENT_PLAYBACK_STATE_CHANGED,
                    Player.EVENT_IS_PLAYING_CHANGED,
                    Player.EVENT_MEDIA_ITEM_TRANSITION,
                    Player.EVENT_REPEAT_MODE_CHANGED,
                    Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED,
                    Player.EVENT_TIMELINE_CHANGED
                )
            ) {
                syncState()
            }

            if (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)) {
                player.currentMediaItem?.let { item ->
                    fetchMetadata(item.mediaId)
                    saveToHistory(item)
                }
            }
        }
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
            currentItem?.let { updateLikedState(it.mediaId) }
        }
    }

    private fun startPositionUpdates() = viewModelScope.launch {
        while (isActive) {
            controller?.let { p ->
                if (p.isPlaying && _uiState.value.currentPosition != p.currentPosition) {
                    _uiState.update { it.copy(currentPosition = p.currentPosition) }
                }
            }
            delay(500)
        }
    }

    private fun fetchMetadata(videoId: String) {
        if (videoId == lastFetchedVideoId) return
        lastFetchedVideoId = videoId

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

                val allSongs = result.items.map { it.toMediaItem() }
                val currentIndex = result.currentIndex ?: 0

                val upNextSongs = if (currentIndex in allSongs.indices) {
                    allSongs.drop(currentIndex + 1)
                } else {
                    allSongs
                }

                upNextBuffer.clear()
                upNextBuffer.addAll(upNextSongs)

                controller?.let { p ->
                    if (_uiState.value.isAutoplayEnabled && upNextBuffer.isNotEmpty()) {

                        val remaining = p.mediaItemCount - p.currentMediaItemIndex - 1

                        if (remaining <= 2) {
                            val toAdd = mutableListOf<MediaItem>()

                            while (upNextBuffer.isNotEmpty() && toAdd.size < 10) {
                                val item = upNextBuffer.removeFirst()

                                // prevent duplicates
                                val exists = (0 until p.mediaItemCount)
                                    .any { p.getMediaItemAt(it).mediaId == item.mediaId }

                                if (!exists) {
                                    toAdd.add(item)
                                }
                            }

                            if (toAdd.isNotEmpty()) {
                                p.addMediaItems(toAdd)
                            }
                        }
                    }
                }
                launch { loadLyrics(result) }
                launch { loadRelated(result) }

            } catch (e: Exception) {
                if (e !is CancellationException) {
                    FirebaseCrashlytics.getInstance().apply {
                        setCustomKey("videoId", videoId)
                        recordException(e)
                    }
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

    /* ---------------- Player Controls ---------------- */

    fun play(mediaItem: MediaItem) = runPlayer {
        lastFetchedVideoId = null
        stop()
        setMediaItems(listOf(mediaItem), 0, C.TIME_UNSET)
        prepare()
        play()
    }

    fun playAll(items: List<MediaItem>, startIndex: Int = 0) = runPlayer {
        lastFetchedVideoId = null
        setMediaItems(items, startIndex, C.TIME_UNSET)
        prepare()
        play()
    }

    fun togglePlayPause() = runPlayer {
        if (isPlaying) pause() else play()
    }

    fun seekTo(position: Long) = runPlayer { seekTo(position) }

    fun skipNext() = runPlayer {
        if (hasNextMediaItem()) {
            seekToNext()
        } else if (_uiState.value.isAutoplayEnabled) {
            _uiState.value.currentTrack?.let {
                lastFetchedVideoId = null
                fetchMetadata(it.mediaId)
            }
        }
    }

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

    fun toggleAutoplay() {
        _uiState.update { it.copy(isAutoplayEnabled = !it.isAutoplayEnabled) }
        runPlayer{ playWhenReady = true }
    }

    fun setSpeed(speed: Float) = runPlayer { setPlaybackSpeed(speed) }

    /* ---------------- Repository Actions ---------------- */

    private fun updateLikedState(videoId: String) = viewModelScope.launch {
        _uiState.update { it.copy(isLiked = songRepository.isLiked(videoId)) }
    }

    fun toggleLike() = viewModelScope.launch {
        val currentTrack = _uiState.value.currentTrack ?: return@launch
        val song = Song(
            mediaId = currentTrack.mediaId,
            title = currentTrack.mediaMetadata.title?.toString() ?: "Unknown",
            artist = currentTrack.mediaMetadata.artist?.toString() ?: "Unknown",
            album = currentTrack.mediaMetadata.albumTitle?.toString(),
            duration = 0,
            thumbnail = currentTrack.mediaMetadata.artworkUri?.toString() ?: "",
            isYoutube = true
        )
        songRepository.toggleLike(song)
        _uiState.update { it.copy(isLiked = !it.isLiked) }
    }


    private fun saveToHistory(mediaItem: MediaItem) = viewModelScope.launch {
        val song = Song(
            mediaId = mediaItem.mediaId,
            title = mediaItem.mediaMetadata.title?.toString() ?: "Unknown",
            artist = mediaItem.mediaMetadata.artist?.toString() ?: "Unknown",
            album = mediaItem.mediaMetadata.albumTitle?.toString(),
            duration = 0,
            thumbnail = mediaItem.mediaMetadata.artworkUri?.toString() ?: "",
            isYoutube = true
        )
        songRepository.markAsPlayed(song)
    }

    /* ---------------- Helpers ---------------- */

    private fun runPlayer(block: MediaController.() -> Unit) {
        controller?.apply(block)
    }

    override fun onCleared() {
        MediaController.releaseFuture(controllerFuture)
        super.onCleared()
    }
}
