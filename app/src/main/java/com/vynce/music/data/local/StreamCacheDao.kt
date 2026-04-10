package com.vynce.music.data.local

import androidx.room.*
import com.vynce.music.data.model.StreamCache

@Dao
interface StreamCacheDao {
    @Query("SELECT * FROM stream_cache WHERE videoId = :videoId")
    suspend fun getStream(videoId: String): StreamCache?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStream(stream: StreamCache)

    @Query("DELETE FROM stream_cache WHERE videoId = :videoId")
    suspend fun deleteStream(videoId: String)

    @Query("DELETE FROM stream_cache WHERE timestamp < :expiryTime")
    suspend fun clearExpiredStreams(expiryTime: Long)

    @Query("DELETE FROM stream_cache")
    suspend fun clearAllStreams()
}
