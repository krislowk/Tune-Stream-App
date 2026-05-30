package com.vynce.music

import android.content.Context
import android.net.ConnectivityManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.vynce.music.repository.constants.AudioQuality
import com.vynce.music.utils.YTPlayerUtils
import com.vynce.vynceclient.YouTube
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StreamUrlTest {

    private lateinit var context: Context

    private val mockConnectivityManager by lazy {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        YouTube.visitorData = YouTube.DEFAULT_VISITOR_DATA
    }

    @Test
    fun testYTPlayerUtilsPlayback() = runBlocking {

        val videoId = "dQw4w9WgXcQ"
        val playlistId = "PLFgquLnL59alCl_2TQvOiD5Vgm1hCaGSI"

        println("\n=== TESTING YTPlayerUtils (ANDROID TEST) ===")

        val playbackData =
            YTPlayerUtils.playerResponseForPlayback(
                videoId = videoId,
                playlistId = playlistId,
                audioQuality = AudioQuality.HIGH,
                connectivityManager = mockConnectivityManager,
            ).getOrNull()

        println("\n=== RESULT ===")
        println("streamUrl: ${playbackData?.streamUrl}")
        println("itag: ${playbackData?.format?.itag}")
    }
}