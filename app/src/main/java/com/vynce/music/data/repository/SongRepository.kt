package com.vynce.music.data.repository

import com.vynce.music.data.local.HistoryDao
import com.vynce.music.data.local.SongDao
import com.vynce.music.data.local.StreamCacheDao
import com.vynce.music.data.model.History
import com.vynce.music.data.model.Song
import com.vynce.music.data.model.StreamCache
import com.vynce.music.provider.YoutubeProvider
import kotlinx.coroutines.flow.Flow
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
    fun getHistory(): Flow<List<History>> = historyDao.getHistory()

    fun getAllSongs(): Flow<List<Song>> = songDao.getAllSongs()

    fun getLikedSongs(): Flow<List<Song>> = songDao.getLikedSongs()

    suspend fun toggleLike(song: Song) {
        val current = songDao.getSongByUri(song.contentUri)
        if (current != null) {
            songDao.insertSong(current.copy(isLiked = !current.isLiked))
        } else {
            songDao.insertSong(song.copy(isLiked = true))
        }
    }

    suspend fun isLiked(contentUri: String): Boolean {
        return songDao.getSongByUri(contentUri)?.isLiked ?: false
    }

    suspend fun markAsPlayed(song: Song) {
        songDao.insertSong(song)
        historyDao.insertHistory(History(videoId = song.contentUri))
    }

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
                    bitrate = 0, // Should be populated if available
                    mimeType = ""
                )
            )
        }
        return url
    }

    suspend fun clearHistory() {
        historyDao.clearHistory()
    }
}
