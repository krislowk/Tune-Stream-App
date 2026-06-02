package com.vynce.vynceclient

import com.vynce.vynceclient.models.YouTubeClient
import com.vynce.vynceclient.models.YouTubeLocale
import io.ktor.client.HttpClient
import io.ktor.client.statement.HttpResponse
import java.net.Proxy

interface ApiProvider {

    var locale: YouTubeLocale
    var visitorData: String?
    var cookie: String?
    var proxy: Proxy?
    var proxyAuth: String?
    var useLoginForBrowse: Boolean

    suspend fun search(
        client: YouTubeClient,
        query: String? = null,
        params: String? = null,
        continuation: String? = null,
    ): HttpResponse

    suspend fun browse(
        client: YouTubeClient,
        browseId: String? = null,
        params: String? = null,
        continuation: String? = null,
        setLogin: Boolean = false,
    ): HttpResponse

    suspend fun player(
        client: YouTubeClient,
        videoId: String,
        playlistId: String? = null,
        signatureTimestamp: Int?,
        poToken: String? = null,
    ): HttpResponse

    suspend fun next(
        client: YouTubeClient,
        videoId: String?,
        playlistId: String?,
        playlistSetVideoId: String?,
        index: Int?,
        params: String?,
        continuation: String? = null,
    ): HttpResponse

    suspend fun getSearchSuggestions(
        client: YouTubeClient,
        input: String,
    ): HttpResponse

    suspend fun getQueue(
        client: YouTubeClient,
        videoIds: List<String>?,
        playlistId: String?,
    ): HttpResponse

    suspend fun getTranscript(
        client: YouTubeClient,
        videoId: String,
    ): HttpResponse

    suspend fun accountMenu(client: YouTubeClient): HttpResponse

    suspend fun getSwJsData(): HttpResponse

    fun getHttpClient(): HttpClient
}















