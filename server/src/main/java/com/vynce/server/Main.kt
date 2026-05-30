package com.vynce.server

import com.vynce.vynceclient.Youtube
import com.yushosei.newpipe.extractor.NewPipe
import com.yushosei.newpipe.util.DefaultDownloaderImpl
import com.yushosei.newpipe.util.ExtractorHelper
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun main() {

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

            get("/home") {
                val params = call.request.queryParameters["params"]
                    ?: return@get call.respondText("params is required", status = HttpStatusCode.BadRequest)
                Youtube.search(params, Youtube.SearchFilter.FILTER_SONG).onSuccess { res ->
                    call.respond(res)
                }.onFailure {
                    call.respondText("Error: ${it.message}", status = HttpStatusCode.InternalServerError)
                }
            }
        }
        routing {
            get("/stream") {
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
                    // Fetch visitor data once logged in for better personalization
                    Youtube.visitorData().onSuccess {
                        Youtube.visitorData = it
                    }
                    call.respondText("Logged in successfully (Cookie and VisitorData updated)")
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












