package com.vynce.music.ui.screens.player

import android.app.Application
import android.content.ComponentName
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.vynce.music.data.model.Song
import com.vynce.music.provider.LocalProvider
import com.vynce.music.service.PlayerService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val application: Application,
    private val mediaProvider: LocalProvider
) : ViewModel() {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val controller: MediaController? get() = if (controllerFuture?.isDone == true) controllerFuture?.get() else null
    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()
    private val _currentSong = MutableStateFlow<MediaItem?>(null)
    val currentSong: StateFlow<MediaItem?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    init {
        initController()
        getSongs()
        startPositionUpdates()
    }

    private fun initController() {
        val sessionToken = SessionToken(
            application,
            ComponentName(application, PlayerService::class.java)
        )
        controllerFuture = MediaController.Builder(application, sessionToken).buildAsync()
        controllerFuture?.addListener({
            val controller = controller ?: return@addListener
            controller.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    // Update current song based on mediaId if needed
                    // For now we might need a way to map MediaItem back to Song
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        _duration.value = controller.duration
                    }
                }
            })
            _isPlaying.value = controller.isPlaying
            _duration.value = controller.duration
        }, MoreExecutors.directExecutor())
    }
    fun getSongs() {
        _songs.value = mediaProvider.fetchAudioFiles()
    }

    private fun startPositionUpdates() {
        viewModelScope.launch {
            while (true) {
                controller?.let {
                    _currentPosition.value = it.currentPosition
                }
                delay(1000)
            }
        }
    }
    fun play(song: MediaItem) {
        val mediaItem = MediaItem.Builder()
            .setMediaId(song.mediaId)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(song.mediaMetadata.title)
                    .setArtist(song.mediaMetadata.artist)
                    .setArtworkUri(song.mediaMetadata.artworkUri)
                    .build()
            )
            .setUri(song.localConfiguration?.uri)
            .build()

        controller?.setMediaItem(mediaItem)
        controller?.prepare()
        controller?.play()
        _currentSong.value = song
    }

    fun togglePlayPause() {
        controller?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
    }

    fun skipToNext() {
        controller?.seekToNext()
    }

    fun skipToPrevious() {
        controller?.seekToPrevious()
    }

    fun seekTo(position: Long) {
        controller?.seekTo(position)
    }

    override fun onCleared() {
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
        super.onCleared()
    }

    fun addToQueue(toMediaItem: MediaItem) {}
}
