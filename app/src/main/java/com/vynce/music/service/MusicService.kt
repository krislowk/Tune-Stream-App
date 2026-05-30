package com.vynce.music.service

import android.content.Context
import androidx.media3.common.AudioAttributes
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
import com.vynce.music.repository.PreferenceRepository
import com.vynce.music.repository.SongRepository
import com.vynce.music.utils.NetworkConnectivityManager
import dagger.hilt.android.AndroidEntryPoint
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
            
        val callback = MediaLibrarySessionCallback(this, songRepository, preferenceRepository, networkConnectivityManager)
        mediaSession = MediaLibrarySession.Builder(this, player, callback).build()
    }
    
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = mediaSession

    override fun onDestroy() {
        mediaSession?.release()
        mediaSession = null
        player?.release()
        player = null
        super.onDestroy()
    }
}














