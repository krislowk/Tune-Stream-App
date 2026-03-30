package com.vynce.music.data.repository

import com.vynce.music.data.local.HistoryDao
import com.vynce.music.data.local.SongDao
import com.vynce.music.data.local.StreamCacheDao
import com.vynce.music.data.model.History
import com.vynce.music.data.model.Song
import com.vynce.music.data.model.SongItem
import com.vynce.music.data.model.StreamCache
import com.vynce.music.provider.YoutubeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SongRepository @Inject constructor(
    private val songDao: SongDao,
    private val historyDao: HistoryDao,
    private val streamCacheDao: StreamCacheDao,
    private val youtubeProvider: YoutubeProvider
) {
    // --- Optimized History ---
    fun getHistory(): Flow<List<History>> = historyDao.getHistory()
        .distinctUntilChanged()

    // --- Optimized Song Queries ---
    // Returns full entities for detail screens
    fun getAllSongs(): Flow<List<Song>> = songDao.getAllSongsFlow()
        .distinctUntilChanged()

    // Returns lightweight items for list screens (minimizes recomposition and memory)
    fun getAllSongItems(): Flow<List<SongItem>> = songDao.getAllSongItemsFlow()
        .distinctUntilChanged()

    fun getLikedSongs(): Flow<List<Song>> = songDao.getLikedSongsFlow()
        .distinctUntilChanged()

    fun getSongsByMediaIds(mediaIds: List<String>): Flow<List<Song>> = songDao.getSongsByMediaIds(mediaIds)
        .distinctUntilChanged()

    suspend fun getSongByMediaId(mediaId: String): Song? = songDao.getSongByMediaId(mediaId)

    suspend fun toggleLike(song: Song) {
        val current = songDao.getSongByMediaId(song.mediaId)
        if (current != null) {
            songDao.toggleLike(song.mediaId)
        } else {
            songDao.insert(song.copy(isLiked = true))
        }
    }

    suspend fun isLiked(mediaId: String): Boolean {
        return songDao.getSongByMediaId(mediaId)?.isLiked ?: false
    }

    suspend fun markAsPlayed(song: Song) {
        songDao.insert(song)
        historyDao.insertHistory(History(mediaId = song.mediaId))
    }

    fun getRecentlyPlayed(limit: Int = 10): Flow<List<Song>> = historyDao.getRecentlyPlayedSongs(limit)
        .distinctUntilChanged()

    // --- Cache Management ---
    suspend fun getStream(videoId: String): String? {
        val cached = streamCacheDao.getStream(videoId)
        val now = System.currentTimeMillis()
        
        if (cached != null && now - cached.timestamp < TimeUnit.HOURS.toMillis(5)) {
            return cached.url
        }

        val url = youtubeProvider.getStream(videoId)
        if (url != null) {
            streamCacheDao.insertStream(
                StreamCache(
                    videoId = videoId,
                    url = url,
                    bitrate = 0,
                    mimeType = ""
                )
            )
        }
        return url
    }

    suspend fun clearHistory() {
        historyDao.clearHistory()
    }

    suspend fun cleanupHistory(days: Int = 30) {
        val expiryTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days.toLong())
        historyDao.deleteOldHistory(expiryTime)
    }
}
