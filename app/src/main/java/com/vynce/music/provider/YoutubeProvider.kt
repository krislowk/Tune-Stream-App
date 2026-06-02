package com.vynce.music.provider

import com.vynce.vynceclient.YouTube
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YoutubeProvider @Inject constructor(){

    fun getAlbum(browseId: String) = flow {
        YouTube.album(browseId)
            .onSuccess { emit(it) }
            .onFailure { throw it }
    }.flowOn(Dispatchers.IO)

    fun getPlaylist(playlistId: String) = flow {
        YouTube.playlist(playlistId)
            .onSuccess { emit(it) }
            .onFailure { throw it }
    }.flowOn(Dispatchers.IO)

    fun getArtist(browseId: String) = flow {
        YouTube.artist(browseId)
            .onSuccess { emit(it) }
            .onFailure { throw it }
    }.flowOn(Dispatchers.IO)

    fun getHome(params: String? = null, continuation: String? = null) = flow {
        YouTube.home(params = params, continuation = continuation)
            .onSuccess { emit(it) }
            .onFailure { throw it }
    }.flowOn(Dispatchers.IO)

    fun getExplore() = flow {
        YouTube.explore()
            .onSuccess { emit(it) }
            .onFailure { throw it }
    }.flowOn(Dispatchers.IO)

    fun search(query: String, filter: YouTube.SearchFilter) = flow {
        YouTube.search(query, filter)
            .onSuccess { emit(it) }
            .onFailure { throw it }
    }.flowOn(Dispatchers.IO)

    fun searchContinuation(continuation: String) = flow {
        YouTube.searchContinuation(continuation)
            .onSuccess { emit(it) }
            .onFailure { throw it }
    }.flowOn(Dispatchers.IO)

    fun getSuggestions(query: String) = flow {
        YouTube.searchSuggestions(query)
            .onSuccess { emit(it) }
            .onFailure { throw it }
    }.flowOn(Dispatchers.IO)

    fun likeVideo(videoId: String, like: Boolean) = flow {
        YouTube.likeVideo(videoId, like)
            .onSuccess { emit(it) }
            .onFailure { throw it }
    }.flowOn(Dispatchers.IO)

    fun library(browseId: String) = flow {
        YouTube.library(browseId)
            .onSuccess { emit(it) }
            .onFailure { throw it }
    }.flowOn(Dispatchers.IO)
}















