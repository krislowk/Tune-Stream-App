package com.vynce.music.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.vynce.music.data.model.AuthSession
import com.vynce.music.data.model.History
import com.vynce.music.data.model.Playlist
import com.vynce.music.data.model.PlaylistSongCrossRef
import com.vynce.music.data.model.Song
import com.vynce.music.data.model.StreamCache
import com.vynce.music.data.model.User

@Database(
    entities = [
        Song::class,
        User::class,
        History::class,
        StreamCache::class,
        AuthSession::class,
        Playlist::class,
        PlaylistSongCrossRef::class
    ],
    version = 7, // Incrementing version for new tables and schema changes
    exportSchema = true
)
abstract class VynceDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun userDao(): UserDao
    abstract fun historyDao(): HistoryDao
    abstract fun streamCacheDao(): StreamCacheDao
    abstract fun authDao(): AuthDao

    companion object {
        const val DATABASE_NAME = "vynce_db"
    }
}
