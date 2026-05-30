package com.vynce.music.repository

import com.vynce.music.db.daos.DatabaseDao
import com.vynce.music.db.entities.Album
import com.vynce.music.db.entities.Artist
import com.vynce.music.db.entities.Playlist
import com.vynce.music.models.History
import com.vynce.music.models.Song
import com.vynce.music.models.SongItem
import com.vynce.music.provider.LocalProvider
import com.vynce.music.provider.YoutubeProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SongRepository @Inject constructor(
    private val databaseDao: DatabaseDao,
    private val youtubeProvider: YoutubeProvider,
    private val localProvider: LocalProvider
) {
    // --- Optimized History ---
    fun getHistory(): Flow<List<History>> = databaseDao.getHistory()
        .distinctUntilChanged()

    // --- Optimized Song Queries ---
    // Returns full entities for detail screens
    fun getAllSongs(): Flow<List<Song>> = databaseDao.getAllSongsFlow()
        .distinctUntilChanged()

    // Returns lightweight items for list screens (minimizes recomposition and memory)
    fun getAllSongItems(): Flow<List<SongItem>> = databaseDao.getAllSongItemsFlow()
        .distinctUntilChanged()

    fun getLikedSongs(): Flow<List<Song>> = databaseDao.getLikedSongsFlow()
        .distinctUntilChanged()

    fun getLocalSongs(): Flow<List<Song>> = databaseDao.getLocalSongsFlow()
        .distinctUntilChanged()

    // --- Library Items ---
    fun getLikedAlbums(): Flow<List<Album>> = databaseDao.albumsLikedByNameAsc()
        .distinctUntilChanged()

    fun getBookmarkedArtists(): Flow<List<Artist>> = databaseDao.artistsBookmarkedByNameAsc()
        .distinctUntilChanged()

    fun getPlaylists(): Flow<List<Playlist>> = databaseDao.playlistsByNameAsc()
        .distinctUntilChanged()

    fun getSongsByMediaIds(mediaIds: List<String>): Flow<List<Song>> = databaseDao.getSongsByMediaIds(mediaIds)
        .distinctUntilChanged()

    suspend fun getSongByMediaId(mediaId: String): Song? = databaseDao.getSongByMediaId(mediaId)

    suspend fun toggleLike(song: Song) {
        val current = databaseDao.getSongByMediaId(song.mediaId)
        if (current != null) {
            databaseDao.toggleLike(song.mediaId)
        } else {
            databaseDao.upsert(song.copy(isLiked = true))
        }
    }

    suspend fun isLiked(mediaId: String): Boolean {
        return databaseDao.getSongByMediaId(mediaId)?.isLiked ?: false
    }

    suspend fun markAsPlayed(song: Song) {
        val existing = databaseDao.getSongByMediaId(song.mediaId)
        val songToInsert = existing?.let {
            song.copy(id = it.id, isLiked = it.isLiked)
        } ?: song
        
        databaseDao.upsert(songToInsert)
        databaseDao.upsertHistory(History(mediaId = song.mediaId))
    }

    fun getRecentlyPlayed(limit: Int = 10): Flow<List<Song>> = databaseDao.getRecentlyPlayedSongs(limit)
        .distinctUntilChanged()

    suspend fun clearHistory() {
        databaseDao.clearHistory()
    }

    suspend fun clearCache() {
        databaseDao.clearAllStreams()
    }

    suspend fun cleanupHistory(days: Int = 30) {
        val expiryTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days.toLong())
        databaseDao.deleteOldHistory(expiryTime)
    }

    // --- Local Scanning ---
    suspend fun scanLocalSongs() = withContext(Dispatchers.IO) {
        val localSongs = localProvider.fetchAudioFiles()
        localSongs.forEach { song ->
            val existing = databaseDao.getSongByMediaId(song.mediaId)
            if (existing == null) {
                databaseDao.upsert(song)
            }
        }
    }
}












