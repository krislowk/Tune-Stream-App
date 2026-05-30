package com.vynce.music.di

import android.content.Context
import androidx.media3.common.util.UnstableApi
import com.vynce.music.db.MusicDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@UnstableApi
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    @Singleton
    @Provides
    fun provideMusicDatabase(
        @ApplicationContext context: Context,
    ): MusicDatabase = MusicDatabase.getInstance(context)

    @Provides
    fun provideDatabaseDao(db: MusicDatabase) = db.databaseDao
}














