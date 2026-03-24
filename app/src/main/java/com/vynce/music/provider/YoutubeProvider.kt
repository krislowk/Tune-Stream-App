package com.vynce.music.provider

import com.vynce.vynceclient.Youtube
import com.vynce.vynceclient.YtStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YoutubeProvider @Inject constructor(){

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
