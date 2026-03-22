package com.vynce.music.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.vynce.music.data.model.AuthSession
import com.vynce.music.data.model.History
import com.vynce.music.data.model.Song
import com.vynce.music.data.model.StreamCache
import com.vynce.music.data.model.User

@Database(entities = [Song::class, User::class, History::class, StreamCache::class, AuthSession::class], version = 6, exportSchema = true)
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
