package com.vynce.vynceclient

import com.vynce.vynceclient.models.YtClient
import com.vynce.vynceclient.models.YtLocale
import com.vynce.vynceclient.models.response.AccountMenuResponse
import com.vynce.vynceclient.models.response.BrowseResponse
import com.vynce.vynceclient.models.response.GetQueueResponse
import com.vynce.vynceclient.models.response.GetSearchSuggestionsResponse
import com.vynce.vynceclient.models.response.GetTranscriptResponse
import com.vynce.vynceclient.models.response.NextResponse
import com.vynce.vynceclient.models.response.PlayerResponse
import com.vynce.vynceclient.models.response.SearchResponse
import io.ktor.client.HttpClient
import io.ktor.client.statement.HttpResponse
import java.net.Proxy

interface ApiProvider {

    var locale: YtLocale
    var visitorData: String
    var cookie: String?
    var proxy: Proxy?
    var useLoginForBrowse: Boolean

    suspend fun search(
        client: YtClient,
        query: String? = null,
        params: String? = null,
        continuation: String? = null,
    ): SearchResponse

    suspend fun browse(
        client: YtClient,
        browseId: String? = null,
        params: String? = null,
        continuation: String? = null,
        setLogin: Boolean = false,
    ): BrowseResponse

    suspend fun player(
        client: YtClient,
        videoId: String,
        playlistId: String? = null,
    ): PlayerResponse

    suspend fun next(
        client: YtClient,
        videoId: String?,
        playlistId: String?,
        playlistSetVideoId: String?,
        index: Int?,
        params: String?,
        continuation: String? = null,
    ): NextResponse

    suspend fun getSearchSuggestions(
        client: YtClient,
        input: String,
    ): GetSearchSuggestionsResponse

    suspend fun getQueue(
        client: YtClient,
        videoIds: List<String>?,
        playlistId: String?,
    ): GetQueueResponse

    suspend fun getTranscript(
        client: YtClient,
        videoId: String,
    ): GetTranscriptResponse

    suspend fun accountMenu(client: YtClient): AccountMenuResponse

    suspend fun getSwJsData(): HttpResponse

    fun getHttpClient(): HttpClient
}