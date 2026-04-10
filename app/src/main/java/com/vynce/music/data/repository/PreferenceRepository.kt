package com.vynce.music.data.repository

import android.content.Context
import androidx.core.content.edit
import com.vynce.music.data.local.AuthDao
import com.vynce.music.data.model.AuthSession
import com.vynce.vynceclient.Youtube
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferenceRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authDao: AuthDao
) {
    private val prefs = context.getSharedPreferences("groove_prefs", Context.MODE_PRIVATE)

    private val initializationJob: Job

    init {
        // Load from DB at startup to ensure persistence
        @OptIn(DelicateCoroutinesApi::class)
        initializationJob = GlobalScope.launch(Dispatchers.IO) {
            val session = authDao.getSession().firstOrNull()
            if (session != null) {
                if (session.cookie != null) {
                    Youtube.cookie = session.cookie
                    prefs.edit { putString("youtube_cookie", session.cookie) }
                }
                if (session.visitorData != null) {
                    Youtube.visitorData = session.visitorData
                    prefs.edit { putString("visitor_data", session.visitorData) }
                }
            }

            // If visitorData is still the default or missing, fetch a fresh one
            if (Youtube.visitorData == Youtube.DEFAULT_VISITOR_DATA) {
                Youtube.visitorData().onSuccess { data ->
                    Youtube.visitorData = data
                    // Save to shared prefs and DB for next startup
                    prefs.edit { putString("visitor_data", data) }
                    val current = authDao.getSession().firstOrNull() ?: AuthSession()
                    authDao.insertSession(current.copy(visitorData = data))
                }
            }
        }
    }

    suspend fun ensureInitialized() {
        initializationJob.join()
    }

    suspend fun saveCookie(cookie: String?) {
        prefs.edit { putString("youtube_cookie", cookie) }
        Youtube.cookie = cookie
        
        val current = authDao.getSession().firstOrNull() ?: AuthSession()
        authDao.insertSession(current.copy(cookie = cookie))
    }

    fun getCookie(): String? = prefs.getString("youtube_cookie", null)

    suspend fun saveVisitorData(data: String?) {
        prefs.edit { putString("visitor_data", data) }
        if (data != null) Youtube.visitorData = data
        
        val current = authDao.getSession().firstOrNull() ?: AuthSession()
        authDao.insertSession(current.copy(visitorData = data))
    }

    fun getVisitorData(): String? = prefs.getString("visitor_data", null)

    suspend fun clearSession() {
        prefs.edit { 
            remove("youtube_cookie")
            remove("visitor_data")
        }
        Youtube.cookie = null
        Youtube.visitorData = ""
        authDao.clearSession()
    }

    // --- Generic Preferences ---

    fun getString(key: String, default: String): String = prefs.getString(key, default) ?: default
    fun saveString(key: String, value: String) = prefs.edit { putString(key, value) }

    fun getBoolean(key: String, default: Boolean): Boolean = prefs.getBoolean(key, default)
    fun saveBoolean(key: String, value: Boolean) = prefs.edit { putBoolean(key, value) }

    fun getFloat(key: String, default: Float): Float = prefs.getFloat(key, default)
    fun saveFloat(key: String, value: Float) = prefs.edit { putFloat(key, value) }

    fun getInt(key: String, default: Int): Int = prefs.getInt(key, default)
    fun saveInt(key: String, value: Int) = prefs.edit { putInt(key, value) }
}
