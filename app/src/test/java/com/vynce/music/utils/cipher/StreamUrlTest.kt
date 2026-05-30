package com.vynce.music.utils.cipher

import android.net.ConnectivityManager
import com.vynce.music.repository.constants.AudioQuality
import com.vynce.music.utils.YTPlayerUtils
import com.vynce.vynceclient.YouTube
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class StreamUrlTest {

    private val mockConnectivityManager = mock<ConnectivityManager> {
        on { isActiveNetworkMetered } doReturn false
    }

    @Before
    fun setup() {
        YouTube.visitorData = YouTube.DEFAULT_VISITOR_DATA
        println("=== TEST INIT ===")
        println("VisitorData: ${YouTube.visitorData}")
        println("Cookie: ${YouTube.cookie}")
    }

    @Test
    fun testYTPlayerUtilsPlayback(): Unit = runBlocking {

        val videoId = "dQw4w9WgXcQ"
        val playlistId = "PLFgquLnL59alCl_2TQvOiD5Vgm1hCaGSI"

        println("\n==============================")
        println("🚀 STARTING PLAYBACK TEST")
        println("==============================")
        println("videoId      : $videoId")
        println("playlistId   : $playlistId")
        println("quality      : HIGH")
        println("metered      : ${mockConnectivityManager.isActiveNetworkMetered}")
        println("==============================\n")

        val result = YTPlayerUtils.playerResponseForPlayback(
            videoId = videoId,
            playlistId = playlistId,
            audioQuality = AudioQuality.HIGH,
            connectivityManager = mockConnectivityManager,
        )

        println("\n==============================")
        println("📦 RESULT WRAPPER")
        println("==============================")

        result.onSuccess { playbackData ->

            println("\n🎯 SUCCESS!")

            println("\n--- STREAM INFO ---")
            println("streamUrl : ${playbackData.streamUrl}")
            println("expires   : ${playbackData.streamExpiresInSeconds}")

            println("\n--- FORMAT INFO ---")
            println("itag      : ${playbackData.format.itag}")
            println("mimeType  : ${playbackData.format.mimeType}")
            println("bitrate   : ${playbackData.format.bitrate}")
            println("quality   : ${playbackData.format.audioQuality}")

            println("\n--- AUDIO CONFIG ---")
            println(playbackData.audioConfig)

            println("\n==============================")
            println("🏁 TEST COMPLETE (SUCCESS)")
            println("==============================")
        }.onFailure { e ->

            println("\n💥 FAILURE!")

            println("\n--- ERROR INFO ---")
            println("type    : ${e::class.simpleName}")
            println("message : ${e.message}")

            println("\n--- STACK TRACE ---")
            e.printStackTrace()

            println("\n==============================")
            println("❌ TEST FAILED")
            println("==============================")
        }
    }
}