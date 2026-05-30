package com.vynce.music.service

import android.content.Context
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.vynce.music.repository.PreferenceRepository
import com.vynce.music.repository.SongRepository
import com.vynce.music.repository.constants.AudioQuality
import com.vynce.music.repository.constants.PreferenceConstants
import com.vynce.music.utils.NetworkConnectivityManager
import com.vynce.music.utils.YTPlayerUtils
import com.vynce.music.utils.toMediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.withContext

@UnstableApi
class MediaLibrarySessionCallback(
    private val context: Context,
    private val songRepository: SongRepository,
    private val preferenceRepository: PreferenceRepository,
    private val networkConnectivityManager: NetworkConnectivityManager
) : MediaLibrarySession.Callback {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val connectivityManager = networkConnectivityManager.connectivityManager

    override fun onAddMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: MutableList<MediaItem>
    ): ListenableFuture<MutableList<MediaItem>> {
        return scope.future {
            val resolvedItems = mediaItems.map { item ->
                if (item.localConfiguration?.uri != null) {
                    item
                } else {
                    val videoId = item.mediaId
                    val playlistId = item.mediaMetadata.extras?.getString("playlist_id")
                    
                    val audioQuality = getPreferredAudioQuality()
                    val streamUrl = getStreamUrl(videoId, playlistId, audioQuality)

                    if (streamUrl != null) {
                        item.buildUpon()
                            .setUri(streamUrl.toUri())
                            .build()
                    } else {
                        item
                    }
                }
            }
            resolvedItems.toMutableList()
        }
    }

    private fun getPreferredAudioQuality(): AudioQuality {
        val qualityStr = preferenceRepository.getString(PreferenceConstants.AUDIO_QUALITY, "Auto")
        return when {
            qualityStr.contains("High", ignoreCase = true) -> AudioQuality.HIGH
            qualityStr.contains("Low", ignoreCase = true) -> AudioQuality.LOW
            else -> AudioQuality.AUTO
        }
    }

    private suspend fun getStreamUrl(
        videoId: String,
        playlistId: String?,
        audioQuality: AudioQuality = AudioQuality.HIGH
    ): String? = withContext(Dispatchers.IO) {
        val playbackData = YTPlayerUtils.playerResponseForPlayback(
            videoId = videoId,
            playlistId = playlistId,
            audioQuality = audioQuality,
            connectivityManager = connectivityManager
        ).getOrNull()
        
        println("Resolved stream URL for $videoId: ${playbackData?.streamUrl}")
        playbackData?.streamUrl
    }

    override fun onGetLibraryRoot(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        params: MediaLibraryService.LibraryParams?
    ): ListenableFuture<LibraryResult<MediaItem>> {
        val root = MediaItem.Builder()
            .setMediaId("root")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build()
            )
            .build()
        return Futures.immediateFuture(
            LibraryResult.ofItem(root, params)
        )
    }

    override fun onGetChildren(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        parentId: String,
        page: Int,
        pageSize: Int,
        params: MediaLibraryService.LibraryParams?
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        return scope.future {
            val items = when (parentId) {
                "root" -> listOf(
                    createBrowsableItem("liked_songs", "Liked Songs"),
                    createBrowsableItem("local_songs", "Local Songs"),
                    createBrowsableItem("playlists", "Playlists"),
                    createBrowsableItem("albums", "Albums"),
                    createBrowsableItem("artists", "Artists")
                )
                "liked_songs" -> songRepository.getLikedSongs().first().map { it.toMediaItem() }
                "local_songs" -> songRepository.getLocalSongs().first().map { it.toMediaItem() }
                "playlists" -> songRepository.getPlaylists().first().map { it.toMediaItem() }
                "albums" -> songRepository.getLikedAlbums().first().map { it.toMediaItem() }
                "artists" -> songRepository.getBookmarkedArtists().first().map { it.toMediaItem() }
                else -> emptyList()
            }
            
            LibraryResult.ofItemList(ImmutableList.copyOf(items), params)
        }
    }

    private fun createBrowsableItem(id: String, title: String): MediaItem {
        return MediaItem.Builder()
            .setMediaId(id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build()
            )
            .build()
    }
}












