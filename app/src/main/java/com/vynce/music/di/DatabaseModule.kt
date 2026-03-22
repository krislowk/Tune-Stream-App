package com.vynce.music.di

import android.content.Context
import androidx.room.Room
import com.vynce.music.data.local.AuthDao
import com.vynce.music.data.local.HistoryDao
import com.vynce.music.data.local.SongDao
import com.vynce.music.data.local.StreamCacheDao
import com.vynce.music.data.local.UserDao
import com.vynce.music.data.local.VynceDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideVynceDatabase(
        @ApplicationContext context: Context
    ): VynceDatabase {
        return Room.databaseBuilder(
                context,
                VynceDatabase::class.java,
                VynceDatabase.DATABASE_NAME
            ).fallbackToDestructiveMigration(true).build()
    }

    @Provides
    fun provideSongDao(database: VynceDatabase): SongDao {
        return database.songDao()
    }

    @Provides
    fun provideUserDao(database: VynceDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    fun provideHistoryDao(database: VynceDatabase): HistoryDao {
        return database.historyDao()
    }

    @Provides
    fun provideStreamCacheDao(database: VynceDatabase): StreamCacheDao {
        return database.streamCacheDao()
    }

    @Provides
    fun provideAuthDao(database: VynceDatabase): AuthDao {
        return database.authDao()
    }
}
