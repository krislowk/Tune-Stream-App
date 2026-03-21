package com.vynce.music.data.repository

import android.content.Context
import androidx.core.content.edit
import com.vynce.vynceclient.Youtube
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferenceRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("groove_prefs", Context.MODE_PRIVATE)

    fun saveCookie(cookie: String?) {
        prefs.edit { putString("youtube_cookie", cookie) }
        Youtube.cookie = cookie
    }

    fun getCookie(): String? = prefs.getString("youtube_cookie", null)

    fun saveVisitorData(data: String?) {
        prefs.edit { putString("visitor_data", data) }
        if (data != null) Youtube.visitorData = data
    }

    fun getVisitorData(): String? = prefs.getString("visitor_data", null)

    fun clearSession() {
        saveCookie(null)
        saveVisitorData(null)
    }
}
