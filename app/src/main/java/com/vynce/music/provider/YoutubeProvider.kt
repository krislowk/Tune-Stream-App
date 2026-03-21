package com.vynce.music.provider

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.vynce.vynceclient.Youtube
import com.vynce.vynceclient.YtStream
import com.vynce.vynceclient.pages.HomePage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YoutubeProvider @Inject constructor(){
    fun getHome(
        browseId: String = "FEmusic_home",
        params: String? = null,
        setLogin: Boolean = false
    ): Flow<HomePage> = flow {

        Youtube.home(browseId, params, setLogin)
            .onSuccess { emit(it) }
            .onFailure {
                FirebaseCrashlytics.getInstance().recordException(it)
                throw it
            }

    }.flowOn(Dispatchers.IO)

    suspend fun getStream(videoId: String): String? {
        return YtStream.getVideoStream(videoId)
    }
}