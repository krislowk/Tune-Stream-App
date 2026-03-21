package com.vynce.server

import com.vynce.vynceclient.Youtube
import com.yushosei.newpipe.extractor.NewPipe
import com.yushosei.newpipe.util.DefaultDownloaderImpl
import com.yushosei.newpipe.util.ExtractorHelper
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.encoding.zstd.ZstdEncoder
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytesWriter
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.copyTo

fun main() {

    val client = HttpClient(OkHttp) {
        install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
            json()
        }
        install(ContentEncoding) {

            customEncoder(ZstdEncoder())

            gzip()

            deflate()
        }
    }

    embeddedServer(
        Netty,
        port = 8080,
        watchPaths = listOf("classes")
    ) {
        install(ContentNegotiation) {
            json()
        }

        routing {
            get("/") {
                call.respondText("Hot reload test")
            }
        }

        routing {

            get("/search") {

                val response: HttpResponse = client.post(
                    "https://www.youtube.com/youtubei/v1/search"
                ) {

                    contentType(ContentType.Application.Json)
                    headers {
                        append(
                            HttpHeaders.UserAgent,
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/145.0.0.0 Safari/537.36"
                        )
                    }
                    setBody(
                        """
                        """.trimIndent()
                    )
                }

                val channel: ByteReadChannel = response.body()
                call.respondBytesWriter(ContentType.Application.Json) {
                    channel.copyTo(this)
                }
            }
        }
        routing {
            get("/streamurl") {
                NewPipe.init(DefaultDownloaderImpl.initDefault())
                val videoId = call.request.queryParameters["videoId"]
                    ?: return@get call.respondText("videoId is required", status = HttpStatusCode.BadRequest)

                val url = "https://www.youtube.com/watch?v=$videoId"
                try {
                    val info = ExtractorHelper.getStreamInfo(0, url)
                    val bestAudio = info.audioStreams.maxByOrNull { it.bitrate }

                    if (bestAudio?.content != null) {
                        call.respondText(bestAudio.content)
                    } else {
                        call.respondText("Stream URL not found", status = HttpStatusCode.NotFound)
                    }
                } catch (e: Exception) {
                    call.respondText("Failed to extract stream: ${e.message}", status = HttpStatusCode.InternalServerError)
                }
            }
        }

        routing {
            get("/login") {
                val cookie = call.request.queryParameters["cookie"]
                if (cookie != null) {
                    Youtube.cookie = cookie
                    call.respondText("Logged in successfully (Cookie updated)")
                } else {
                    call.respondText(
                        "Error: 'cookie' query parameter is required",
                        status = HttpStatusCode.BadRequest
                    )
                }
            }

            get("/account") {
                Youtube.accountInfo().onSuccess { info ->
                    call.respond(info)
                }.onFailure {
                    call.respondText("Error fetching account info: ${it.message}", status = HttpStatusCode.InternalServerError)
                }
            }
        }

    }.start(wait = true)
}
