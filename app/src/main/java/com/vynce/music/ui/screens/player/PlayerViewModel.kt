package com.vynce.music.ui.screens.player

import android.app.Application
import android.content.ComponentName
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.vynce.music.db.MusicDatabase
import com.vynce.music.db.entities.LyricsEntity
import com.vynce.music.lyrics.GeminiService
import com.vynce.music.lyrics.LyricsEntry
import com.vynce.music.lyrics.LyricsParser
import com.vynce.music.lyrics.LyricsTranslationHelper
import com.vynce.music.models.Song
import com.vynce.music.repository.PreferenceRepository
import com.vynce.music.repository.SongRepository
import com.vynce.music.service.MusicService
import com.vynce.music.utils.toMediaItem
import com.vynce.vynceclient.YouTube
import com.vynce.vynceclient.models.AlbumItem
import com.vynce.vynceclient.models.ArtistItem
import com.vynce.vynceclient.models.BrowseEndpoint
import com.vynce.vynceclient.models.PlaylistItem
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
import kotlin.time.Duration.Companion.milliseconds

data class PlayerUiState(
    val currentTrack: MediaItem? = null,
    val isPlaying: Boolean = false,
    val duration: Long = 0L,
    val isBuffering: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val shuffleEnabled: Boolean = false,
    val queue: List<MediaItem> = emptyList(),
    val currentIndex: Int = -1,
    val relatedSongs: List<MediaItem> = emptyList(),
    val relatedAlbums: List<AlbumItem> = emptyList(),
    val relatedArtists: List<ArtistItem> = emptyList(),
    val relatedPlaylists: List<PlaylistItem> = emptyList(),
    val isFetchingMetadata: Boolean = false,
    val isLiked: Boolean = false,
    val isAutoplayEnabled: Boolean = true,
    val lyricsOffset: Int = 0,
    val currentPosition: Long = 0L,
    val lyrics: List<LyricsEntry> = emptyList(),
    val isLyricsLoading: Boolean = false
)

@UnstableApi
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val application: Application,
    private val songRepository: SongRepository,
    private val preferenceRepository: PreferenceRepository
) : ViewModel() {

    // ==================== PLAYER STATE ====================
    private var controller: MediaController? = null
    private val controllerFuture: ListenableFuture<MediaController> by lazy {
        val token = SessionToken(application, ComponentName(application, MusicService::class.java))
        MediaController.Builder(application, token).buildAsync()
    }
    private val upNextBuffer = ArrayDeque<MediaItem>()

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState = _uiState.asStateFlow()

    // ==================== METADATA ====================
    private var metadataJob: kotlinx.coroutines.Job? = null
    private var lastFetchedVideoId: String? = null

    init {
        setupController()
        startPositionUpdates()
    }

    // ==================== CONTROLLER SETUP ====================
    private fun setupController() {
        controllerFuture.addListener({
            try {
                controller = controllerFuture.get().apply {
                    addListener(playerListener)
                    syncState()
                }
            } catch (e: Exception) {
                e.printStackTrace()
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
                val newQueue = if (state.queue.size != p.mediaItemCount) {
                    List(p.mediaItemCount) { p.getMediaItemAt(it) }
                } else {
                    state.queue
                }

                state.copy(
                    currentTrack = currentItem,
                    isPlaying = p.isPlaying,
                    duration = if (p.duration != C.TIME_UNSET) p.duration else 0L,
                    isBuffering = p.playbackState == Player.STATE_BUFFERING,
                    repeatMode = p.repeatMode,
                    shuffleEnabled = p.shuffleModeEnabled,
                    queue = newQueue,
                    currentIndex = p.currentMediaItemIndex,
                    currentPosition = p.currentPosition,
                    lyricsOffset = currentItem?.mediaMetadata?.extras?.getInt("lyrics_offset") ?: 0
                )
            }
            currentItem?.let { updateLikedState(it.mediaId) }
        }
    }

    private fun startPositionUpdates() = viewModelScope.launch {
        while (isActive) {
            controller?.let { p ->
                _uiState.update { it.copy(currentPosition = p.currentPosition) }
            }
            delay(50.milliseconds)
        }
    }

    // ==================== METADATA FETCHING ====================

    private fun fetchMetadata(videoId: String) {
        if (videoId == lastFetchedVideoId) return
        lastFetchedVideoId = videoId

        metadataJob?.cancel()
        metadataJob = viewModelScope.launch {
            _uiState.update { it.copy(isFetchingMetadata = true) }

            try {
                val result = withContext(Dispatchers.IO) {
                    YouTube.next(WatchEndpoint(videoId)).getOrNull()
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

                // Auto-add songs to queue if autoplay enabled
                controller?.let { p ->
                    if (_uiState.value.isAutoplayEnabled && upNextBuffer.isNotEmpty()) {
                        val remaining = p.mediaItemCount - p.currentMediaItemIndex - 1

                        if (remaining <= 2) {
                            val toAdd = mutableListOf<MediaItem>()

                            while (upNextBuffer.isNotEmpty() && toAdd.size < 10) {
                                val item = upNextBuffer.removeFirst()
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

                // Load content in parallel
                launch { loadRelated(result) }
                launch { loadLyrics(videoId, result.lyricsEndpoint) }

            } catch (e: Exception) {
                if (e !is CancellationException) {
                    e.printStackTrace()
                }
            } finally {
                _uiState.update { it.copy(isFetchingMetadata = false) }
            }
        }
    }


    private suspend fun loadRelated(result: NextResult) {
        val related = withContext(Dispatchers.IO) {
            result.relatedEndpoint?.let { YouTube.related(it).getOrNull() }
        }

        _uiState.update { state ->
            state.copy(
                relatedSongs = related?.songs?.map { it.toMediaItem() } ?: emptyList(),
                relatedAlbums = related?.albums ?: emptyList(),
                relatedArtists = related?.artists ?: emptyList(),
                relatedPlaylists = related?.playlists ?: emptyList()
            )
        }
    }

    private suspend fun loadLyrics(videoId: String, endpoint: BrowseEndpoint?) {
        _uiState.update { it.copy(isLyricsLoading = true, lyrics = emptyList()) }

        val dbLyrics: LyricsEntity? = withContext(Dispatchers.IO) {
            songRepository.getLyrics(videoId)
        }

        if (dbLyrics != null) {
            val entries = LyricsParser.parse(dbLyrics.lyrics)
            _uiState.update { it.copy(lyrics = entries, isLyricsLoading = false) }

            // Try to load translations if any
            LyricsTranslationHelper.loadTranslationsFromDatabase(
                lyrics = entries,
                lyricsEntity = dbLyrics,
                targetLanguage = "en", // TODO: Get from settings
                mode = "Natural"
            )
            return
        }

        val currentTrack = _uiState.value.currentTrack
        val artist = currentTrack?.mediaMetadata?.artist?.toString() ?: ""
        val title = currentTrack?.mediaMetadata?.title?.toString() ?: ""
        val duration = (_uiState.value.duration / 1000).toInt()

        // 1. Try YouTube Transcript (Synced)
        val youtubeTranscript = withContext(Dispatchers.IO) {
            YouTube.transcript(videoId).getOrNull()
        }
        if (youtubeTranscript != null) {
            processAndSaveLyrics(videoId, youtubeTranscript, title, artist, duration)
            return
        }

        // 2. Try LRCLIB (Synced/Plain)
        if (artist.isNotEmpty() && title.isNotEmpty()) {
            val lrclibResult = withContext(Dispatchers.IO) {
                YouTube.lrclibLyrics(artist, title, duration).getOrNull()
            }
            if (lrclibResult != null) {
                processAndSaveLyrics(videoId, lrclibResult, title, artist, duration)
                return
            }
        }

        // 3. Try NetEase (Synced/Translated)
        if (artist.isNotEmpty() && title.isNotEmpty()) {
            val neteaseResult = withContext(Dispatchers.IO) {
                YouTube.neteaseLyrics(title, artist).getOrNull()
            }
            if (neteaseResult != null) {
                processAndSaveLyrics(videoId, neteaseResult, title, artist, duration)
                return
            }
        }

        // 4. Try QQ Music (Synced)
        if (artist.isNotEmpty() && title.isNotEmpty()) {
            val qqResult = withContext(Dispatchers.IO) {
                YouTube.qqLyrics(title, artist).getOrNull()
            }
            if (qqResult != null) {
                processAndSaveLyrics(videoId, qqResult, title, artist, duration)
                return
            }
        }

        // 5. Try YouTube Static Lyrics
        if (endpoint != null) {
            val staticResult = withContext(Dispatchers.IO) {
                YouTube.lyrics(endpoint).getOrNull()
            }
            if (staticResult != null) {
                processAndSaveLyrics(videoId, staticResult, title, artist, duration)
                return
            }
        }

        _uiState.update { it.copy(isLyricsLoading = false) }
    }

    fun generateLyricsWithGemini() {
        val currentTrack = _uiState.value.currentTrack ?: return
        val videoId = currentTrack.mediaId
        val artist = currentTrack.mediaMetadata.artist?.toString() ?: ""
        val title = currentTrack.mediaMetadata.title?.toString() ?: ""
        val duration = (_uiState.value.duration / 1000).toInt()

        _uiState.update { it.copy(isLyricsLoading = true) }

        viewModelScope.launch {
            val geminiGenerated = withContext(Dispatchers.IO) {
                GeminiService.generateLyrics(
                    title = title,
                    artist = artist,
                    apiKey = "AIzaSyCbtMgl7JO0qr7tfyi14723oPUyryfJDzA", // TODO: Get from preferences
                    model = "gemini-3.5-flash"
                ).getOrNull()
            }

            if (geminiGenerated != null) {
                processAndSaveLyrics(videoId, geminiGenerated, title, artist, duration)
            } else {
                _uiState.update { it.copy(isLyricsLoading = false) }
            }
        }
    }

    private suspend fun processAndSaveLyrics(
        videoId: String,
        lyricsText: String,
        title: String,
        artist: String,
        duration: Int
    ) {
        val parsed = LyricsParser.parse(lyricsText)
        if (parsed.isEmpty()) return

        // Check if lyrics are static (no timestamps or all same)
        val isStatic = parsed.all { it.time == 0L } || (parsed.size > 1 && parsed[0].time == parsed[1].time)
        
        val finalLyrics = if (isStatic && duration > 0) {
            withContext(Dispatchers.IO) {
                GeminiService.generateTimedLyrics(
                    lyrics = lyricsText,
                    title = title,
                    artist = artist,
                    durationSeconds = duration,
                    apiKey = "AIzaSyCbtMgl7JO0qr7tfyi14723oPUyryfJDzA", // TODO: Get from preferences
                    model = "gemini-1.5-flash"
                ).getOrNull() ?: lyricsText
            }
        } else {
            lyricsText
        }

        val entries = LyricsParser.parse(finalLyrics)
        _uiState.update { it.copy(lyrics = entries, isLyricsLoading = false) }

        withContext(Dispatchers.IO) {
            songRepository.upsertLyrics(videoId, finalLyrics)
        }
    }

    private fun parseLyrics(lyricsText: String): List<LyricsEntry> {
        return LyricsParser.parse(lyricsText)
    }

    // ==================== PLAYER CONTROLS ====================

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

    fun stopPlayer() = runPlayer {
        stop()
        clearMediaItems()
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
        runPlayer { playWhenReady = true }
    }

    fun setSpeed(speed: Float) = runPlayer { setPlaybackSpeed(speed) }

    fun playQueue(endpoint: WatchEndpoint) {
        viewModelScope.launch {
            YouTube.next(endpoint).onSuccess { result ->
                val mediaItems = result.items.map { it.toMediaItem() }
                if (mediaItems.isNotEmpty()) {
                    playAll(mediaItems)
                }
            }.onFailure {
                it.printStackTrace()
            }
        }
    }

    fun playAlbum(album: AlbumItem) {
        playQueue(WatchEndpoint(playlistId = album.playlistId))
    }

    fun playArtist(artist: ArtistItem) {
        artist.playEndpoint?.let { playQueue(it) } ?: artist.shuffleEndpoint?.let { playQueue(it) }
    }

    fun playPlaylist(playlist: PlaylistItem) {
        playlist.playEndpoint?.let { playQueue(it) }
    }

    fun addToPlaylist(playlistId: String, videoId: String) {
        viewModelScope.launch {
            YouTube.addToPlaylist(playlistId, videoId)
        }
    }

    fun translateLyrics() {
        val currentLyrics = _uiState.value.lyrics
        if (currentLyrics.isEmpty()) return

        val videoId = _uiState.value.currentTrack?.mediaId ?: return

        LyricsTranslationHelper.translateLyrics(
            lyrics = currentLyrics,
            targetLanguage = "en", // TODO: Get from preferences
            apiKey = "AIzaSyCbtMgl7JO0qr7tfyi14723oPUyryfJDzA", // TODO: Get from preferences
            baseUrl = "https://generativelanguage.googleapis.com",
            model = "gemini-1.5-flash",
            mode = "Natural",
            scope = viewModelScope,
            context = application,
            songId = videoId,
            database = MusicDatabase.getInstance(application)
        )
    }

    // ==================== REPOSITORY ACTIONS ====================

    private fun updateLikedState(videoId: String) = viewModelScope.launch {
        val song = songRepository.getSongByMediaId(videoId)
        _uiState.update { it.copy(
            isLiked = song?.isLiked ?: false,
            lyricsOffset = song?.lyricsOffset ?: 0
        ) }
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
            isYoutube = true,
            lyricsOffset = _uiState.value.lyricsOffset
        )

        songRepository.toggleLike(song)
        _uiState.update { it.copy(isLiked = !it.isLiked) }
    }

    fun updateLyricsOffset(offset: Int) = viewModelScope.launch {
        val currentTrack = _uiState.value.currentTrack ?: return@launch

        _uiState.update { it.copy(lyricsOffset = offset) }

        songRepository.getSongByMediaId(currentTrack.mediaId)?.let { song ->
            songRepository.updateLyricsOffset(song.mediaId, offset)
        }
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

    // ==================== HELPERS ====================

    private fun runPlayer(block: MediaController.() -> Unit) {
        controller?.apply(block)
    }

    override fun onCleared() {
        MediaController.releaseFuture(controllerFuture)
        super.onCleared()
    }
}


