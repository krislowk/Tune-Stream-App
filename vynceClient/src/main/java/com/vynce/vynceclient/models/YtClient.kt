package com.vynce.vynceclient.models
import kotlinx.serialization.Serializable


@Serializable
data class YtClient(
    val clientName: String,
    val clientVersion: String,
    val clientId: String,
    val userAgent: String,
    val loginRequired: Boolean = false,
    val api_key: String = "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX30",
    val osVersion: String? = null,
    val referer: String? = null,
) {
    fun toContext(locale: YtLocale, visitorData: String?) = Context(
        Context.Client(
            clientName = clientName,
            clientVersion = clientVersion,
            osVersion = osVersion,
            gl = locale.gl,
            hl = locale.hl,
            visitorData = visitorData
        )
    )
    companion object {
        const val USER_AGENT_WEB = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/145.0.0.0 Safari/537.36"

        const val ORIGIN_YOUTUBE_MUSIC = "https://music.youtube.com"
        const val REFERER_YOUTUBE_MUSIC = "$ORIGIN_YOUTUBE_MUSIC/"
        const val API_URL_YOUTUBE_MUSIC = "$ORIGIN_YOUTUBE_MUSIC/youtubei/v1/"
        const val USER_AGENT_ANDROID = "com.google.android.youtube/20.10.38 (Linux; U; Android 11) gzip"

        val WEB = YtClient(
            clientName = "WEB",
            clientVersion = "2.20250312.04.00",
            clientId = "1",
            userAgent = USER_AGENT_WEB,
        )

        val WEB_REMIX = YtClient(
            clientName = "WEB_REMIX",
            clientVersion = "1.20250310.01.00",
            clientId = "67",
            userAgent = USER_AGENT_WEB,
            referer = REFERER_YOUTUBE_MUSIC
        )

        val WEB_CREATOR = YtClient(
            clientName = "WEB_CREATOR",
            clientVersion = "1.20250312.03.01",
            clientId = "62",
            userAgent = USER_AGENT_WEB,
            loginRequired = true,
        )

        val TVHTML5 = YtClient(
            clientName = "TVHTML5",
            clientVersion = "7.20250312.16.00",
            clientId = "7",
            userAgent = "Mozilla/5.0(SMART-TV; Linux; Tizen 4.0.0.2) AppleWebkit/605.1.15 (KHTML, like Gecko) SamsungBrowser/9.2 TV Safari/605.1.15",
            loginRequired = true,
        )

        val TVHTML5_SIMPLY_EMBEDDED_PLAYER = YtClient(
            clientName = "TVHTML5_SIMPLY_EMBEDDED_PLAYER",
            clientVersion = "2.0",
            clientId = "85",
            userAgent = "Mozilla/5.0 (PlayStation; PlayStation 4/12.02) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.4 Safari/605.1.15",
            loginRequired = true,
        )

        val IOS = YtClient(
            clientName = "IOS",
            clientVersion = "20.10.4",
            clientId = "5",
            userAgent = "com.google.ios.youtube/20.10.4 (iPhone16,2; U; CPU iOS 18_3_2 like Mac OS X;)",
            osVersion = "18.3.2.22D82",
        )

        val ANDROID = YtClient(
            clientName = "ANDROID",
            clientVersion = "20.10.38",
            clientId = "3",
            userAgent = USER_AGENT_ANDROID,
        )
        val ANDROID_MUSIC = YtClient(
            clientName = "ANDROID_MUSIC",
            clientVersion = "5.01",
            api_key = "AIzaSyAOghZGza2MQSZkY_zfZ370N-PUdXEo8AI",
            userAgent = USER_AGENT_ANDROID,
            clientId = "21",
        )

        val ANDROID_VR_NO_AUTH = YtClient(
            clientName = "ANDROID_VR",
            clientVersion = "1.61.48",
            clientId = "28",
            userAgent = "com.google.android.apps.youtube.vr.oculus/1.61.48 (Linux; U; Android 12; en_US; Oculus Quest 3; Build/SQ3A.220605.009.A1; Cronet/132.0.6808.3)",
        )
    }
}