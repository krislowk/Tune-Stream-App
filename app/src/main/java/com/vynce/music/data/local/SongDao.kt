package com.vynce.music.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vynce.music.data.model.Song
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM songs")
    fun getAllSongs(): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE id = :songId")
    suspend fun getSongById(songId: Long): Song?

    @Query("SELECT * FROM songs WHERE contentUri = :uri")
    suspend fun getSongByUri(uri: String): Song?

    @Query("SELECT * FROM songs WHERE contentUri IN (:uris)")
    fun getSongsByUris(uris: List<String>): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE isLiked = 1")
    fun getLikedSongs(): Flow<List<Song>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: Song)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<Song>)

    @Delete
    suspend fun deleteSong(song: Song)

    @Query("DELETE FROM songs")
    suspend fun clearSongs()
}
