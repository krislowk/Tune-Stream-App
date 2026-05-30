package com.vynce.music.repository

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.vynce.music.db.daos.DatabaseDao
import com.vynce.music.models.AuthSession
import com.vynce.music.repository.constants.PreferenceConstants
import com.vynce.music.utils.dataStore
import com.vynce.music.utils.get
import com.vynce.vynceclient.YouTube
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferenceRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val databaseDao: DatabaseDao
) {
    private val dataStore = context.dataStore
    private val initializationJob: Job

    init {
        // Load from DB at startup to ensure persistence
        @OptIn(DelicateCoroutinesApi::class)
        initializationJob = GlobalScope.launch(Dispatchers.IO) {
            val session = databaseDao.getSession().firstOrNull()
            if (session != null) {
                if (session.cookie != null) {
                    YouTube.cookie = session.cookie
                    dataStore.edit { it[PreferenceConstants.INNER_TUBE_COOKIE] = session.cookie }
                }
                if (session.visitorData != null) {
                    YouTube.visitorData = session.visitorData
                    dataStore.edit { it[PreferenceConstants.VISITOR_DATA] = session.visitorData }
                }
            }

            // If visitorData is still the default or missing, fetch a fresh one
            if (YouTube.visitorData?.isBlank() == true || YouTube.visitorData == YouTube.DEFAULT_VISITOR_DATA) {
                YouTube.visitorData().onSuccess { data ->
                    YouTube.visitorData = data
                    // Save to DataStore and DB for next startup
                    dataStore.edit { it[PreferenceConstants.VISITOR_DATA] = data }
                    val current = databaseDao.getSession().firstOrNull() ?: AuthSession()
                    databaseDao.upsertSession(current.copy(visitorData = data))
                }
            }
        }
    }

    suspend fun ensureInitialized() {
        initializationJob.join()
    }

    suspend fun saveCookie(cookie: String?) {
        Log.d("PreferenceRepository", "Saving cookie: ${if (cookie != null) "length=" + cookie.length else "null"}")
        if (cookie != null) {
            dataStore.edit { it[PreferenceConstants.INNER_TUBE_COOKIE] = cookie }
        } else {
            dataStore.edit { it.remove(PreferenceConstants.INNER_TUBE_COOKIE) }
        }
        YouTube.cookie = cookie
        
        val current = databaseDao.getSession().firstOrNull() ?: AuthSession()
        databaseDao.upsertSession(current.copy(cookie = cookie))
    }

    fun getCookie(): String? = dataStore[PreferenceConstants.INNER_TUBE_COOKIE]

    suspend fun saveVisitorData(data: String?) {
        Log.d("PreferenceRepository", "Saving visitor data: $data")
        if (data != null) {
            dataStore.edit { it[PreferenceConstants.VISITOR_DATA] = data }
            YouTube.visitorData = data
        } else {
            dataStore.edit { it.remove(PreferenceConstants.VISITOR_DATA) }
        }
        
        val current = databaseDao.getSession().firstOrNull() ?: AuthSession()
        databaseDao.upsertSession(current.copy(visitorData = data))
    }

    fun getVisitorData(): String? = dataStore[PreferenceConstants.VISITOR_DATA]

    suspend fun clearSession() {
        dataStore.edit { 
            it.remove(PreferenceConstants.INNER_TUBE_COOKIE)
            it.remove(PreferenceConstants.VISITOR_DATA)
        }
        YouTube.cookie = null
        YouTube.visitorData = ""
        databaseDao.clearSession()
    }

    // --- DataStore accessors ---

    fun <T> get(key: Preferences.Key<T>): Flow<T?> = dataStore.data.map { it[key] }
    
    fun <T> getBlocking(key: Preferences.Key<T>, default: T): T = dataStore.get(key, default)

    suspend fun <T> set(key: Preferences.Key<T>, value: T) {
        dataStore.edit { it[key] = value }
    }

    fun getString(key: Preferences.Key<String>, default: String): String = getBlocking(key, default)
    fun getBoolean(key: Preferences.Key<Boolean>, default: Boolean): Boolean = getBlocking(key, default)
    fun getInt(key: Preferences.Key<Int>, default: Int): Int = getBlocking(key, default)
    fun getFloat(key: Preferences.Key<Float>, default: Float): Float = getBlocking(key, default)
}











