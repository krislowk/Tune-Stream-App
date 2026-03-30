package com.vynce.music.service

import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.ListenableFuture
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.vynce.vynceclient.YtStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.guava.future

class PlayerService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private inner class PlayerListener : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            // Service stays alive while playing
        }
    }

    private inner class MusicSessionCallback : MediaSession.Callback {
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<MutableList<MediaItem>> {

            return serviceScope.future {
                mediaItems.map { item ->
                    async {
                        if (item.localConfiguration?.uri == null) {
                            try {
                                val streamUrl = YtStream.getVideoStream(item.mediaId)
                                if (streamUrl != null) {
                                    item.buildUpon()
                                        .setUri(streamUrl)
                                        .build()
                                } else item
                            } catch (e: Exception) {
                                FirebaseCrashlytics.getInstance().recordException(e)
                                item
                            }
                        } else {
                            item
                        }
                    }
                }
                .awaitAll().toMutableList()
            }
        }
    }
    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        player.addListener(PlayerListener())

        mediaSession = MediaSession.Builder(this, player)
            .setCallback(MusicSessionCallback())
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val currentPlayer = mediaSession?.player
        if (currentPlayer == null || !currentPlayer.playWhenReady || currentPlayer.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
