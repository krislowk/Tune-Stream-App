package com.vynce.music.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vynce.music.data.model.Song
import com.vynce.music.data.model.SongItem
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {

    // --- Basic CRUD ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(song: Song)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(songs: List<Song>)

    @Delete
    suspend fun delete(song: Song)

    @Query("DELETE FROM songs")
    suspend fun clearAll()

    // --- Optimized Queries (Flows with distinctUntilChanged() in Repository) ---

    @Query("SELECT * FROM songs ORDER BY dateAdded DESC")
    fun getAllSongsFlow(): Flow<List<Song>>

    @Query("SELECT mediaId, title, artist, thumbnail, isLiked FROM songs ORDER BY dateAdded DESC")
    fun getAllSongItemsFlow(): Flow<List<SongItem>>

    @Query("SELECT * FROM songs WHERE mediaId = :mediaId")
    suspend fun getSongByMediaId(mediaId: String): Song?

    @Query("SELECT * FROM songs WHERE isLiked = 1 ORDER BY dateAdded DESC")
    fun getLikedSongsFlow(): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE artist = :artist ORDER BY album, title")
    fun getSongsByArtist(artist: String): Flow<List<Song>>

    // --- Paging Support for Large Datasets ---
    // Room automatically generates PagingSource for Paging 3
    @Query("SELECT * FROM songs ORDER BY title ASC")
    fun getPagedSongs(): PagingSource<Int, Song>

    // --- Search with Optimization ---
    @Query("""
        SELECT * FROM songs 
        WHERE title LIKE '%' || :query || '%' 
        OR artist LIKE '%' || :query || '%'
        ORDER BY CASE 
            WHEN title LIKE :query || '%' THEN 1
            WHEN artist LIKE :query || '%' THEN 2
            ELSE 3
        END
        LIMIT 50
    """)
    fun searchSongs(query: String): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE mediaId IN (:mediaIds)")
    fun getSongsByMediaIds(mediaIds: List<String>): Flow<List<Song>>

    // --- Optimized Atomic Operations ---
    @Query("UPDATE songs SET isLiked = NOT isLiked WHERE mediaId = :mediaId")
    suspend fun toggleLike(mediaId: String)

    @Query("UPDATE songs SET duration = :duration, durationMs = :durationMs WHERE mediaId = :mediaId")
    suspend fun updateDuration(mediaId: String, duration: Long, durationMs: Long)
}
