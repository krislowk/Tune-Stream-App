package com.vynce.music.repository

import com.vynce.music.db.daos.DatabaseDao
import com.vynce.music.db.entities.Album
import com.vynce.music.db.entities.Artist
import com.vynce.music.db.entities.LyricsEntity
import com.vynce.music.db.entities.Playlist
import com.vynce.music.db.entities.SongEntity
import com.vynce.music.models.History
import com.vynce.music.models.Song
import com.vynce.music.models.SongItem
import com.vynce.music.provider.LocalProvider
import com.vynce.music.utils.SyncUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SongRepository @Inject constructor(
    val databaseDao: DatabaseDao,
    private val localProvider: LocalProvider,
    private val syncUtils: SyncUtils
) {
    // --- Optimized History ---
    fun getHistory(): Flow<List<History>> = databaseDao.getHistory()
        .distinctUntilChanged()

    // --- Optimized Song Queries ---
    fun getAllSongs(): Flow<List<Song>> = databaseDao.getAllSongsFlow()
        .distinctUntilChanged()

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

    fun getSubscribedPodcasts(): Flow<List<com.vynce.music.db.entities.PodcastEntity>> = databaseDao.subscribedPodcasts()
        .distinctUntilChanged()

    fun getPlaylists(): Flow<List<Playlist>> = databaseDao.playlistsByNameAsc()
        .distinctUntilChanged()

    fun getSongsByMediaIds(mediaIds: List<String>): Flow<List<Song>> = databaseDao.getSongsByMediaIds(mediaIds)
        .distinctUntilChanged()

    suspend fun getSongByMediaId(mediaId: String): Song? = databaseDao.getSongByMediaId(mediaId)

    suspend fun toggleLike(song: Song) {
        val isCurrentlyLiked = isLiked(song.mediaId)
        val newLikedState = !isCurrentlyLiked

        // Update 'songs' table (SongModel)
        val current = databaseDao.getSongByMediaId(song.mediaId)
        if (current != null) {
            databaseDao.toggleLike(song.mediaId)
        } else {
            databaseDao.upsert(song.copy(isLiked = newLikedState))
        }

        // Update 'song' table (SongEntity)
        val songEntity = databaseDao.getSongByIdBlocking(song.mediaId)
        if (songEntity != null) {
            databaseDao.upsert(songEntity.song.copy(liked = newLikedState, likedDate = if (newLikedState) java.time.LocalDateTime.now() else null))
        } else {
            databaseDao.upsert(
                SongEntity(
                    id = song.mediaId,
                    title = song.title,
                    liked = newLikedState,
                    likedDate = if (newLikedState) java.time.LocalDateTime.now() else null,
                    thumbnailUrl = song.thumbnail,
                    albumName = song.album,
                    lyricsOffset = song.lyricsOffset
                )
            )
        }

        // Use SyncUtils for reliable online sync
        syncUtils.likeSong(
            SongEntity(
                id = song.mediaId,
                title = song.title,
                liked = newLikedState,
                lyricsOffset = song.lyricsOffset
            )
        )
    }

    suspend fun updateLyricsOffset(mediaId: String, offset: Int) {
        databaseDao.updateLyricsOffset(mediaId, offset)
    }

    suspend fun getLyrics(mediaId: String): LyricsEntity? = databaseDao.lyrics(mediaId).first()

    fun upsertLyrics(mediaId: String, lyricsText: String) {
        databaseDao.upsert(LyricsEntity(id = mediaId, lyrics = lyricsText))
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

    // --- Sync ---
    // Moved to SyncUtils for more robust implementation
}



