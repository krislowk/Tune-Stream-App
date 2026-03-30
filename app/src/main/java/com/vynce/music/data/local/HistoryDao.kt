package com.vynce.music.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.vynce.music.data.model.History
import com.vynce.music.data.model.Song
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Query("SELECT * FROM history ORDER BY timestamp DESC")
    fun getHistory(): Flow<List<History>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: History)

    @Query("DELETE FROM history WHERE mediaId = :mediaId")
    suspend fun deleteHistory(mediaId: String)

    @Query("DELETE FROM history")
    suspend fun clearHistory()

    @Query("DELETE FROM history WHERE timestamp < :timestamp")
    suspend fun deleteOldHistory(timestamp: Long)

    /**
     * Optimized query to get unique recently played songs with their full metadata.
     * This avoids multiple queries and minimizes data processing in the UI layer.
     */
    @Transaction
    @Query("""
        SELECT s.* FROM songs s
        INNER JOIN (
            SELECT mediaId, MAX(timestamp) as latest_play 
            FROM history 
            GROUP BY mediaId
        ) h ON s.mediaId = h.mediaId
        ORDER BY h.latest_play DESC
        LIMIT :limit
    """)
    fun getRecentlyPlayedSongs(limit: Int = 20): Flow<List<Song>>
}
