package com.vynce.music.repository

import com.vynce.music.db.daos.DatabaseDao
import com.vynce.music.db.entities.SyncedLyric
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Repository layer connecting Room DAO queries nicely with ViewModels.
 */
class LyricRepository @Inject constructor(
    private val databaseDao: DatabaseDao
) {

    val allLyrics: Flow<List<SyncedLyric>> = databaseDao.getAllSyncedLyrics()

    suspend fun getById(id: Int): SyncedLyric? {
        return databaseDao.getLyricById(id)
    }

    suspend fun insert(lyric: SyncedLyric): Long {
        return databaseDao.insertSyncedLyric(lyric)
    }

    suspend fun deleteById(id: Int) {
        databaseDao.deleteLyricById(id)
    }

    suspend fun clearAll() {
        databaseDao.deleteAll()
    }
}
