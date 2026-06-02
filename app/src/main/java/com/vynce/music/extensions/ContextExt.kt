package com.vynce.music.extensions

import android.content.Context
import android.net.ConnectivityManager
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.vynce.music.utils.dataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

fun Context.isInternetConnected(): Boolean {
    val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    return cm.activeNetworkInfo?.isConnectedOrConnecting == true
}

suspend fun Context.isSyncEnabled(): Boolean {
    val SYNC_ENABLED_KEY = booleanPreferencesKey("sync_enabled")
    return dataStore.data.map { it[SYNC_ENABLED_KEY] ?: true }.first()
}














