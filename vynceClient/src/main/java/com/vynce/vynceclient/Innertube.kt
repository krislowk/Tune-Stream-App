package com.vynce.vynceclient

import com.vynce.vynceclient.models.Context
import com.vynce.vynceclient.models.YtClient
import com.vynce.vynceclient.models.YtLocale
import com.vynce.vynceclient.models.body.AccountMenuBody
import com.vynce.vynceclient.models.body.BrowseBody
import com.vynce.vynceclient.models.body.GetQueueBody
import com.vynce.vynceclient.models.body.GetSearchSuggestionsBody
import com.vynce.vynceclient.models.body.GetTranscriptBody
import com.vynce.vynceclient.models.body.NextBody
import com.vynce.vynceclient.models.body.PlayerBody
import com.vynce.vynceclient.models.body.SearchBody
import com.vynce.vynceclient.models.response.AccountMenuResponse
import com.vynce.vynceclient.models.response.BrowseResponse
import com.vynce.vynceclient.models.response.GetQueueResponse
import com.vynce.vynceclient.models.response.GetSearchSuggestionsResponse
import com.vynce.vynceclient.models.response.GetTranscriptResponse
import com.vynce.vynceclient.models.response.NextResponse
import com.vynce.vynceclient.models.response.PlayerResponse
import com.vynce.vynceclient.models.response.SearchResponse
import com.vynce.vynceclient.utils.parseCookieString
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.encoding.zstd.ZstdEncoder
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.userAgent
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.net.Proxy
import java.util.Collections
import java.util.Locale
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class Innertube() : ApiProvider {

    override var locale = YtLocale(
        gl = Locale.getDefault().country,
        hl = Locale.getDefault().language
    )

    override var proxy: Proxy? = null

    override var useLoginForBrowse: Boolean = false

    override var visitorData: String = "CgtsZG1ySnZiQWtSbyiMjuGSBg%3D%3D"

    override var cookie: String? = null
        set(value) {
            field = value
            cookieMap =
                if (value == null) Collections.emptyMap() else parseCookieString(
                    value
                )
        }
    private var cookieMap = Collections.emptyMap<String, String>()

    companion object {
        private val jsonConfig = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            encodeDefaults = true
        }

        private val sharedClient by lazy {
            HttpClient(CIO) {
                expectSuccess = true

                install(ContentNegotiation) {
                    json(jsonConfig)
                }

                install(ContentEncoding) {
                    customEncoder(ZstdEncoder())
                    gzip(0.9F)
                    deflate(0.8F)
                }

                install(HttpCache)

                install(HttpRequestRetry) {
                    retryOnExceptionOrServerErrors(maxRetries = 3)
                    exponentialDelay()
                }

                install(HttpTimeout) {
                    requestTimeoutMillis = 15000
                    connectTimeoutMillis = 10000
                    socketTimeoutMillis = 10000
                }

                install(Logging) {
                    level = LogLevel.INFO
                }

                defaultRequest {
                    url(YtClient.API_URL_YOUTUBE_MUSIC)
                }
            }
        }
    }

    private fun HttpRequestBuilder.ytClient(client: YtClient, setLogin: Boolean = false) {
        contentType(ContentType.Application.Json)

        headers {
            append("X-Goog-Api-Format-Version", "1")
            append("X-YouTube-Client-Name", client.clientId /* Not a typo. The Client-Name header does contain the client id. */)
            append("X-YouTube-Client-Version", client.clientVersion)
            append("X-Origin", YtClient.ORIGIN_YOUTUBE_MUSIC)
            append("Referer", YtClient.REFERER_YOUTUBE_MUSIC)
        }
        userAgent(client.userAgent)
        parameter("key", client.api_key)
        parameter("prettyPrint", false)
    }
    override suspend fun search(
        client: YtClient,
        query: String?,
        params: String?,
        continuation: String?,
    ): SearchResponse = sharedClient.post("search") {
        ytClient(client, setLogin = useLoginForBrowse)
        setBody(
            SearchBody(
                context = client.toContext(locale, visitorData),
                query = query,
                params = params
            )
        )
        parameter("continuation", continuation)
        parameter("ctoken", continuation)
    }.body()

    override suspend fun player(
        client: YtClient,
        videoId: String,
        playlistId: String?,
    ): PlayerResponse = sharedClient.post("player") {
        ytClient(client, setLogin = true)
        setBody(
            PlayerBody(
                context = client.toContext(locale, visitorData).let {
                    if (client == YtClient.TVHTML5) {
                        it.copy(
                            thirdParty = Context.ThirdParty(
                                embedUrl = "https://www.youtube.com/watch?v=${videoId}"
                            )
                        )
                    } else it
                },
                videoId = videoId,
                playlistId = playlistId
            )
        )
    }.body()

    override suspend fun browse(
        client: YtClient,
        browseId: String?,
        params: String?,
        continuation: String?,
        setLogin: Boolean,
    ): BrowseResponse = sharedClient.post("browse") {
        ytClient(client, setLogin = setLogin || useLoginForBrowse)
        setBody(
            BrowseBody(
                context = client.toContext(locale, visitorData),
                browseId = browseId,
                params = params
            )
        )
        parameter("continuation", continuation)
        parameter("ctoken", continuation)
        if (continuation != null) {
            parameter("type", "next")
        }
    }.body()

    override suspend fun next(
        client: YtClient,
        videoId: String?,
        playlistId: String?,
        playlistSetVideoId: String?,
        index: Int?,
        params: String?,
        continuation: String?,
    ): NextResponse = sharedClient.post("next") {
        ytClient(client, setLogin = true)
        setBody(
            NextBody(
                context = client.toContext(locale, visitorData),
                videoId = videoId,
                playlistId = playlistId,
                playlistSetVideoId = playlistSetVideoId,
                index = index,
                params = params,
                continuation = continuation
            )
        )
    }.body()

    override suspend fun getSearchSuggestions(
        client: YtClient,
        input: String,
    ): GetSearchSuggestionsResponse = sharedClient.post("music/get_search_suggestions") {
        ytClient(client)
        setBody(
            GetSearchSuggestionsBody(
                context = client.toContext(locale, visitorData),
                input = input
            )
        )
    }.body()

    override suspend fun getQueue(
        client: YtClient,
        videoIds: List<String>?,
        playlistId: String?,
    ): GetQueueResponse = sharedClient.post("music/get_queue") {
        ytClient(client)
        setBody(
            GetQueueBody(
                context = client.toContext(locale, visitorData),
                videoIds = videoIds,
                playlistId = playlistId
            )
        )
    }.body()

    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun getTranscript(
        client: YtClient,
        videoId: String,
    ): GetTranscriptResponse = sharedClient.post("https://music.youtube.com/youtubei/v1/get_transcript") {
        parameter("key", "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX3")
        headers {
            append("Content-Type", "application/json")
        }
        setBody(
            GetTranscriptBody(
                context = client.toContext(locale, null),
                params = Base64.Default.encode("\n${11.toChar()}$videoId".toByteArray())
            )
        )
    }.body()

    override suspend fun accountMenu(client: YtClient): AccountMenuResponse =
        sharedClient.post("account/account_menu") {
            ytClient(client, setLogin = true)
            setBody(
                AccountMenuBody(
                    client.toContext(
                        locale,
                        visitorData
                    )
                )
            )
        }.body()

    override suspend fun getSwJsData(): HttpResponse =
        sharedClient.get("https://music.youtube.com/sw.js_data")

    override fun getHttpClient(): HttpClient = sharedClient
}