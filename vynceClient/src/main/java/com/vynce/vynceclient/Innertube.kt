package com.vynce.vynceclient

import com.vynce.vynceclient.models.Context
import com.vynce.vynceclient.models.YouTubeClient
import com.vynce.vynceclient.models.YtLocale
import com.vynce.vynceclient.models.body.AccountMenuBody
import com.vynce.vynceclient.models.body.Action
import com.vynce.vynceclient.models.body.BrowseBody
import com.vynce.vynceclient.models.body.CreatePlaylistBody
import com.vynce.vynceclient.models.body.EditPlaylistBody
import com.vynce.vynceclient.models.body.FeedbackBody
import com.vynce.vynceclient.models.body.GetQueueBody
import com.vynce.vynceclient.models.body.GetSearchSuggestionsBody
import com.vynce.vynceclient.models.body.GetTranscriptBody
import com.vynce.vynceclient.models.body.LikeBody
import com.vynce.vynceclient.models.body.NextBody
import com.vynce.vynceclient.models.body.PlayerBody
import com.vynce.vynceclient.models.body.PlaylistDeleteBody
import com.vynce.vynceclient.models.body.SearchBody
import com.vynce.vynceclient.models.body.SubscribeBody
import com.vynce.vynceclient.utils.parseCookieString
import com.vynce.vynceclient.utils.sha1
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.userAgent
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import okhttp3.ConnectionPool
import java.io.IOException
import java.net.Proxy
import java.util.Collections
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.io.encoding.Base64
import kotlin.time.Duration.Companion.milliseconds

class Innertube : ApiProvider {

    override var locale = YtLocale(
        gl = Locale.getDefault().country,
        hl = Locale.getDefault().language
    )
    var dataSyncId: String? = null

    override var useLoginForBrowse: Boolean = true

    override var visitorData: String? = null

    override var cookie: String? = null
        set(value) {
            field = value
            cookieMap =
                if (value == null) Collections.emptyMap() else parseCookieString(
                    value
                )
        }
    private var cookieMap = Collections.emptyMap<String, String>()

    override var proxy: Proxy?
        get() = Companion.proxy
        set(value) {
            Companion.proxy = value
        }

    override var proxyAuth: String?
        get() = Companion.proxyAuth
        set(value) {
            Companion.proxyAuth = value
        }

    companion object {
        var proxy: Proxy? = null
        var proxyAuth: String? = null

        private val sharedClient by lazy {
            HttpClient(OkHttp) {
                expectSuccess = true

                install(ContentNegotiation) {
                    json(Json {
                        ignoreUnknownKeys = true
                        explicitNulls = false
                        encodeDefaults = true
                    })
                }

                install(ContentEncoding) {
                    gzip(0.9F)
                    deflate(0.8F)
                }

                engine {
                    config {
                        connectionPool(
                            ConnectionPool(
                                10,
                                5,
                                TimeUnit.MINUTES
                            )
                        )

                        connectTimeout(30, TimeUnit.SECONDS)
                        readTimeout(60, TimeUnit.SECONDS)
                        writeTimeout(60, TimeUnit.SECONDS)

                        protocols(listOf(okhttp3.Protocol.HTTP_2, okhttp3.Protocol.HTTP_1_1))
                        retryOnConnectionFailure(true)

                        cache(
                            okhttp3.Cache(
                                directory = java.io.File(System.getProperty("java.io.tmpdir"), "http_cache"),
                                maxSize = 50L * 1024L * 1024L
                            )
                        )

                        proxy?.let { proxyConfig ->
                            proxy(proxyConfig)
                        }

                        proxyAuth?.let { auth ->
                            proxyAuthenticator { _, response ->
                                response.request.newBuilder()
                                    .header("Proxy-Authorization", auth)
                                    .build()
                            }
                        }
                    }
                }
                // Request timeout configuration
                install(HttpTimeout) {
                    requestTimeoutMillis = 60000
                    connectTimeoutMillis = 30000
                    socketTimeoutMillis = 60000
                }

                defaultRequest {
                    url(YouTubeClient.API_URL_YOUTUBE_MUSIC)
                    // Add common headers for better compatibility
                    header("Accept", "application/json")
                    header("Accept-Language", "en-US,en;q=0.9")
                    header("Cache-Control", "no-cache")
                }
            }
        }
    }

    private fun HttpRequestBuilder.ytClient(client: YouTubeClient, setLogin: Boolean = false) {
        contentType(ContentType.Application.Json)
        headers {
            append("X-Goog-Api-Format-Version", "1")
            append("X-YouTube-Client-Name", client.clientId /* Not a typo. The Client-Name header does contain the client id. */)
            append("X-YouTube-Client-Version", client.clientVersion)
            append("X-Origin", YouTubeClient.ORIGIN_YOUTUBE_MUSIC)
            append("Referer", YouTubeClient.REFERER_YOUTUBE_MUSIC)
            visitorData?.let { append("X-Goog-Visitor-Id", it) }
            if (setLogin && client.loginSupported) {
                cookie?.let { cookie ->
                    append("cookie", cookie)
                    if ("SAPISID" !in cookieMap) return@let
                    val currentTime = System.currentTimeMillis() / 1000
                    val sapisidHash = sha1("$currentTime ${cookieMap["SAPISID"]} ${YouTubeClient.ORIGIN_YOUTUBE_MUSIC}")
                    append("Authorization", "SAPISIDHASH ${currentTime}_${sapisidHash}")
                }
            }
        }
        userAgent(client.userAgent)
        parameter("prettyPrint", false)
    }

    private suspend fun <T> withRetry(
        maxAttempts: Int = 3,
        initialDelay: Long = 500L,
        factor: Double = 2.0,
        block: suspend () -> T,
    ): T {
        var currentDelay = initialDelay
        var attempt = 0
        while (true) {
            try {
                return block()
            } catch (e: IOException) {
                attempt++
                if (attempt >= maxAttempts) throw e
                delay(currentDelay.milliseconds)
                currentDelay = (currentDelay * factor).toLong()
            }
        }
    }

    override suspend fun search(
        client: YouTubeClient,
        query: String?,
        params: String?,
        continuation: String?,
    ) = withRetry {
        sharedClient.post("search") {
            ytClient(client, setLogin = useLoginForBrowse)
            setBody(
                SearchBody(
                    context = client.toContext(
                        locale,
                        visitorData,
                        if (useLoginForBrowse) dataSyncId else null
                    ),
                    query = query,
                    params = params
                )
            )
            parameter("continuation", continuation)
            parameter("ctoken", continuation)
        }
    }

    override suspend fun player(
        client: YouTubeClient,
        videoId: String,
        playlistId: String?,
        signatureTimestamp: Int?,
        poToken: String?,
    ) = withRetry {
        sharedClient.post("player") {
            ytClient(client, setLogin = true)
            setBody(
                PlayerBody(
                    context = client.toContext(locale, visitorData, dataSyncId).let {
                        if (client.isEmbedded) {
                            it.copy(
                                thirdParty = Context.ThirdParty(
                                    embedUrl = "https://www.youtube.com/watch?v=${videoId}"
                                )
                            )
                        } else it
                    },
                    videoId = videoId,
                    playlistId = playlistId,
                    playbackContext = if (client.useSignatureTimestamp && signatureTimestamp != null) {
                        PlayerBody.PlaybackContext(
                            PlayerBody.PlaybackContext.ContentPlaybackContext(
                                signatureTimestamp
                            )
                        )
                    } else null,
                    serviceIntegrityDimensions = if (client.useWebPoTokens && poToken != null) {
                        PlayerBody.ServiceIntegrityDimensions(poToken)
                    } else null,
                )
            )
        }
    }


    override suspend fun browse(
        client: YouTubeClient,
        browseId: String?,
        params: String?,
        continuation: String?,
        setLogin: Boolean,
    ) = withRetry {
        sharedClient.post("browse") {
            ytClient(client, setLogin = setLogin || useLoginForBrowse)
            setBody(
                BrowseBody(
                    context = client.toContext(
                        locale,
                        visitorData,
                        if (setLogin || useLoginForBrowse) dataSyncId else null
                    ),
                    browseId = browseId,
                    params = params,
                    continuation = continuation
                )
            )
        }
    }

    override suspend fun next(
        client: YouTubeClient,
        videoId: String?,
        playlistId: String?,
        playlistSetVideoId: String?,
        index: Int?,
        params: String?,
        continuation: String?,
    ) = withRetry {
        sharedClient.post("next") {
            ytClient(client, setLogin = true)
            setBody(
                NextBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    videoId = videoId,
                    playlistId = playlistId,
                    playlistSetVideoId = playlistSetVideoId,
                    index = index,
                    params = params,
                    continuation = continuation
                )
            )
        }
    }

    override suspend fun getSearchSuggestions(
        client: YouTubeClient,
        input: String,
    ) = withRetry {
        sharedClient.post("music/get_search_suggestions") {
            ytClient(client)
            setBody(
                GetSearchSuggestionsBody(
                    context = client.toContext(locale, visitorData, null),
                    input = input
                )
            )
        }
    }

    override suspend fun getQueue(
        client: YouTubeClient,
        videoIds: List<String>?,
        playlistId: String?,
    ) = withRetry {
        sharedClient.post("music/get_queue") {
            ytClient(client)
            setBody(
                GetQueueBody(
                    context = client.toContext(locale, visitorData, null),
                    videoIds = videoIds,
                    playlistId = playlistId
                )
            )
        }
    }

    override suspend fun getTranscript(
        client: YouTubeClient,
        videoId: String,
    ) = withRetry {
        sharedClient.post("https://music.youtube.com/youtubei/v1/get_transcript") {
            parameter("key", "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX3")
            headers {
                append("Content-Type", "application/json")
            }
            setBody(
                GetTranscriptBody(
                    context = client.toContext(locale, null, null),
                    params = Base64.Default.encode(
                        "\n${11.toChar()}$videoId".encodeToByteArray()
                    )
                )
            )
        }
    }

    override suspend fun getSwJsData() = withRetry { sharedClient.get("https://music.youtube.com/sw.js_data") }

    override suspend fun accountMenu(client: YouTubeClient) = withRetry {
        val response =sharedClient.post("account/account_menu") {
            ytClient(client, setLogin = true)
            setBody(AccountMenuBody(client.toContext(locale, visitorData, dataSyncId)))
        }
        println(response.bodyAsText())
        response
    }

    suspend fun likeVideo(
        client: YouTubeClient,
        videoId: String,
    ) = withRetry {
        sharedClient.post("like/like") {
            ytClient(client, setLogin = true)
            setBody(
                LikeBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    target = LikeBody.Target.video(videoId)
                )
            )
        }
    }

    suspend fun unlikeVideo(
        client: YouTubeClient,
        videoId: String,
    ) = withRetry {
        sharedClient.post("like/removelike") {
            ytClient(client, setLogin = true)
            setBody(
                LikeBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    target = LikeBody.Target.video(videoId)
                )
            )
        }
    }

    suspend fun subscribeChannel(
        client: YouTubeClient,
        channelId: String,
        params: String? = null,
    ) = withRetry {
        sharedClient.post("subscription/subscribe") {
            ytClient(client, setLogin = true)
            setBody(
                SubscribeBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    channelIds = listOf(channelId),
                    params = params
                )
            )
        }
    }

    suspend fun unsubscribeChannel(
        client: YouTubeClient,
        channelId: String,
        params: String? = null,
    ) = withRetry {
        sharedClient.post("subscription/unsubscribe") {
            ytClient(client, setLogin = true)
            setBody(
                SubscribeBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    channelIds = listOf(channelId),
                    params = params
                )
            )
        }
    }

    suspend fun feedback(
        client: YouTubeClient,
        tokens: List<String>
    ) = withRetry {
        sharedClient.post("feedback") {
            ytClient(client, setLogin = true)
            setBody(
                FeedbackBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    feedbackTokens = tokens
                )
            )
        }
    }

    suspend fun likePlaylist(
        client: YouTubeClient,
        playlistId: String,
    ) = withRetry {
        sharedClient.post("like/like") {
            ytClient(client, setLogin = true)
            setBody(
                LikeBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    target = LikeBody.Target.playlist(playlistId)
                )
            )
        }
    }

    suspend fun unlikePlaylist(
        client: YouTubeClient,
        playlistId: String,
    ) = withRetry {
        sharedClient.post("like/removelike") {
            ytClient(client, setLogin = true)
            setBody(
                LikeBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    target = LikeBody.Target.playlist(playlistId)
                )
            )
        }
    }

    suspend fun addToPlaylist(
        client: YouTubeClient,
        playlistId: String,
        videoId: String,
    ) = withRetry {
        sharedClient.post("browse/edit_playlist") {
            ytClient(client, setLogin = true)
            setBody(
                EditPlaylistBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    playlistId = playlistId.removePrefix("VL"),
                    actions = listOf(
                        Action.AddVideoAction(addedVideoId = videoId)
                    )
                )
            )
        }
    }

    suspend fun addPlaylistToPlaylist(
        client: YouTubeClient,
        playlistId: String,
        addPlaylistId: String,
    ) = withRetry {
        sharedClient.post("browse/edit_playlist") {
            ytClient(client, setLogin = true)
            setBody(
                EditPlaylistBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    playlistId = playlistId.removePrefix("VL"),
                    actions = listOf(
                        Action.AddPlaylistAction(addedFullListId = addPlaylistId)
                    )
                )
            )
        }
    }

    suspend fun removeFromPlaylist(
        client: YouTubeClient,
        playlistId: String,
        videoId: String,
        setVideoId: String,
    ) = withRetry {
        sharedClient.post("browse/edit_playlist") {
            ytClient(client, setLogin = true)
            setBody(
                EditPlaylistBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    playlistId = playlistId.removePrefix("VL"),
                    actions = listOf(
                        Action.RemoveVideoAction(
                            removedVideoId = videoId,
                            setVideoId = setVideoId,
                        )
                    )
                )
            )
        }
    }

    suspend fun moveSongPlaylist(
        client: YouTubeClient,
        playlistId: String,
        setVideoId: String,
        successorSetVideoId: String?,
    ) = withRetry {
        sharedClient.post("browse/edit_playlist") {
            ytClient(client, setLogin = true)
            setBody(
                EditPlaylistBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    playlistId = playlistId,
                    actions = listOf(
                        Action.MoveVideoAction(
                            movedSetVideoIdSuccessor = successorSetVideoId,
                            setVideoId = setVideoId,
                        )
                    )
                )
            )
        }
    }

    suspend fun createPlaylist(
        client: YouTubeClient,
        title: String,
    ) = withRetry {
        sharedClient.post("playlist/create") {
            ytClient(client, true)
            setBody(
                CreatePlaylistBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    title = title
                )
            )
        }
    }

    suspend fun renamePlaylist(
        client: YouTubeClient,
        playlistId: String,
        name: String,
    ) = withRetry {
        sharedClient.post("browse/edit_playlist") {
            ytClient(client, setLogin = true)
            setBody(
                EditPlaylistBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    playlistId = playlistId,
                    actions = listOf(
                        Action.RenamePlaylistAction(
                            playlistName = name
                        )
                    )
                )
            )
        }
    }

    suspend fun deletePlaylist(
        client: YouTubeClient,
        playlistId: String,
    ) = withRetry {
        sharedClient.post("playlist/delete") {
            println("deleting $playlistId")
            ytClient(client, setLogin = true)
            setBody(
                PlaylistDeleteBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    playlistId = playlistId
                )
            )
        }
    }

    override fun getHttpClient(): HttpClient = sharedClient
}












