package com.vynce.music.provider

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.vynce.vynceclient.Youtube
import com.vynce.vynceclient.YtStream
import com.vynce.vynceclient.pages.HomePage
import com.vynce.vynceclient.pages.SearchResult
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
    ): Flow<HomePage> = flow {
        Youtube.home(browseId, params)
            .onSuccess { emit(it) }
            .onFailure {
                FirebaseCrashlytics.getInstance().recordException(it)
                throw it
            }
    }.flowOn(Dispatchers.IO)

    fun search(query: String, filter: Youtube.SearchFilter = Youtube.SearchFilter.FILTER_SONG): Flow<SearchResult> = flow {
        Youtube.search(query, filter)
            .onSuccess { emit(it) }
            .onFailure { throw it }
    }.flowOn(Dispatchers.IO)

    fun getSearchSuggestions(query: String) = flow {
        Youtube.searchSuggestions(query)
            .onSuccess { emit(it) }
            .onFailure { throw it }
    }.flowOn(Dispatchers.IO)

    fun getAlbum(browseId: String) = flow {
        Youtube.album(browseId)
            .onSuccess { emit(it) }
            .onFailure { throw it }
    }.flowOn(Dispatchers.IO)

    fun getPlaylist(playlistId: String) = flow {
        Youtube.playlist(playlistId)
            .onSuccess { emit(it) }
            .onFailure { throw it }
    }.flowOn(Dispatchers.IO)

    fun getArtist(browseId: String) = flow {
        Youtube.artist(browseId)
            .onSuccess { emit(it) }
            .onFailure { throw it }
    }.flowOn(Dispatchers.IO)

    suspend fun getStream(videoId: String): String? {
        return YtStream.getVideoStream(videoId)
    }
}
