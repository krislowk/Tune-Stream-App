package com.vynce.vynceclient

import android.util.LruCache
import com.vynce.vynceclient.models.AudioStream
import com.yushosei.newpipe.util.ExtractorHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

object YtStream {
    // 1. Memory: Reduce cache size to 50 (URLs expire anyway, no need for 100)
    // 2. Efficiency: Store timestamp to prevent playing expired/broken URLs
    private class CachedStream(val stream: AudioStream, val timestamp: Long)
    private val cache = LruCache<String, CachedStream>(50)

    suspend fun getVideoStream(videoId: String): String? = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()

        // 3. Network Efficiency: Only return from cache if URL is < 5 hours old
        cache.get(videoId)?.let {
            if (now - it.timestamp < TimeUnit.HOURS.toMillis(5)) return@withContext it.stream.url
        }

        try {
            // 6. Reliability: Use NewPipe Extractor as a fallback for stream URLs
            val url = "https://music.youtube.com/watch?v=$videoId"
            // SERVICE_ID for YouTube in NewPipe is 0
            val info = ExtractorHelper.getStreamInfo(0, url)

            // Select the audio stream with the highest bitrate (Core implementation)
            val bestAudio = info.audioStreams.maxByOrNull { it.bitrate }

            bestAudio?.content?.let { streamUrl ->
                val audioStream = AudioStream(
                    url = streamUrl,
                    bitrate = bestAudio.bitrate,
                    mimeType = bestAudio.format.toString()
                )
                cache.put(videoId, CachedStream(audioStream, now))
                streamUrl
            }
        } catch (_: Exception) {
            null
        }
    }
}
