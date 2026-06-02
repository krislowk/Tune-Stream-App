package com.vynce.music.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.vynce.music.MainActivity
import com.vynce.music.repository.PreferenceRepository
import com.vynce.music.repository.SongRepository
import com.vynce.music.service.manager.AudioEffectsManager
import com.vynce.music.service.manager.NetworkConnectivityManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@UnstableApi
@AndroidEntryPoint
class MusicService: MediaLibraryService() {
    private var mediaSession: MediaLibrarySession? = null
    private var player: ExoPlayer? = null
    
    @Inject
    lateinit var songRepository: SongRepository
    
    @Inject
    lateinit var preferenceRepository: PreferenceRepository

    @Inject
    lateinit var networkConnectivityManager: NetworkConnectivityManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var audioEffectsManager: AudioEffectsManager? = null
    private var currentMediaItem: MediaItem? = null
    private val resolvingMediaItems = mutableSetOf<String>()
    
    companion object {
        private var cache: SimpleCache? = null
        
        @Synchronized
        private fun getCache(context: Context): SimpleCache {
            if (cache == null) {
                val databaseProvider = StandaloneDatabaseProvider(context)
                cache = SimpleCache(
                    context.filesDir.resolve("exoplayer"),
                    LeastRecentlyUsedCacheEvictor(512L * 1024 * 1024L),
                    databaseProvider
                )
            }
            return cache!!
        }
    }

    override fun onCreate() {
        super.onCreate()
        
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(AudioAttributes.DEFAULT, true)
            .setHandleAudioBecomingNoisy(true)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(this)
                    .setDataSourceFactory(
                        CacheDataSource.Factory()
                            .setCache(getCache(this))
                            .setUpstreamDataSourceFactory(DefaultHttpDataSource.Factory())
                            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
                    )
            )
            .build()
        this.player = player
        currentMediaItem = player.currentMediaItem
        
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                currentMediaItem = mediaItem
                
                // Resolve current item if missing URI
                if (mediaItem != null && (mediaItem.localConfiguration?.uri == null || mediaItem.localConfiguration?.uri == android.net.Uri.EMPTY)) {
                    resolveMediaItem(mediaItem)
                }

                // Pre-resolve next item for seamless transition
                val nextIndex = player.currentMediaItemIndex + 1
                if (nextIndex < player.mediaItemCount) {
                    val nextItem = player.getMediaItemAt(nextIndex)
                    if (nextItem.localConfiguration?.uri == null || nextItem.localConfiguration?.uri == android.net.Uri.EMPTY) {
                        resolveMediaItem(nextItem, nextIndex)
                    }
                }
            }
        })
        
        audioEffectsManager = AudioEffectsManager(player, preferenceRepository, serviceScope)
            
        val callback = MediaLibrarySessionCallback(this, songRepository, preferenceRepository, networkConnectivityManager)
        
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        mediaSession = MediaLibrarySession.Builder(this, player, callback)
            .setSessionActivity(pendingIntent)
            .build()
    }
    
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = mediaSession

    override fun onDestroy() {
        serviceScope.cancel()
        mediaSession?.release()
        mediaSession = null
        player?.release()
        player = null
        super.onDestroy()
    }

    private fun resolveMediaItem(mediaItem: MediaItem, index: Int = -1) {
        val videoId = mediaItem.mediaId
        if (resolvingMediaItems.contains(videoId)) return
        
        val playlistId = mediaItem.mediaMetadata.extras?.getString("playlist_id")
        val audioQuality = getPreferredAudioQuality()

        resolvingMediaItems.add(videoId)
        serviceScope.launch {
            try {
                val playbackData = com.vynce.music.utils.YTPlayerUtils.playerResponseForPlayback(
                    videoId = videoId,
                    playlistId = playlistId,
                    audioQuality = audioQuality,
                    connectivityManager = networkConnectivityManager.connectivityManager,
                    databaseDao = songRepository.databaseDao // Added this
                ).getOrNull()

                if (playbackData?.streamUrl != null) {
                    val extras = mediaItem.mediaMetadata.extras?.let { android.os.Bundle(it) } ?: android.os.Bundle()
                    playbackData.audioConfig?.loudnessDb?.let {
                        extras.putDouble("loudness_db", it)
                    }

                    val resolvedItem = mediaItem.buildUpon()
                        .setUri(playbackData.streamUrl.toUri())
                        .setMediaMetadata(
                            mediaItem.mediaMetadata.buildUpon()
                                .setExtras(extras)
                                .build()
                        )
                        .build()

                    val targetIndex = if (index != -1) index else player?.currentMediaItemIndex ?: -1
                    if (targetIndex != -1 && player?.getMediaItemAt(targetIndex)?.mediaId == videoId) {
                        player?.replaceMediaItem(targetIndex, resolvedItem)
                    }
                }
            } finally {
                resolvingMediaItems.remove(videoId)
            }
        }
    }

    private fun getPreferredAudioQuality(): com.vynce.music.repository.constants.AudioQuality {
        val qualityStr = preferenceRepository.getString(com.vynce.music.repository.constants.PreferenceConstants.AUDIO_QUALITY, "Auto")
        return when {
            qualityStr.contains("High", ignoreCase = true) -> com.vynce.music.repository.constants.AudioQuality.HIGH
            qualityStr.contains("Low", ignoreCase = true) -> com.vynce.music.repository.constants.AudioQuality.LOW
            else -> com.vynce.music.repository.constants.AudioQuality.AUTO
        }
    }
}

















