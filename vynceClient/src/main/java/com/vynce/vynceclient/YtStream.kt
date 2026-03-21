package com.vynce.vynceclient

import com.vynce.vynceclient.models.AudioStream
import com.yushosei.newpipe.util.ExtractorHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Collections
import java.util.concurrent.TimeUnit

object YtStream {
    private class CachedStream(val stream: AudioStream, val timestamp: Long)
    
    // Simple JVM-compatible LRU Cache
    private val cache = Collections.synchronizedMap(object : LinkedHashMap<String, CachedStream>(50, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<String, CachedStream>?): Boolean {
            return size > 50
        }
    })

    suspend fun getVideoStream(videoId: String): String? = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()

        cache[videoId]?.let {
            if (now - it.timestamp < TimeUnit.HOURS.toMillis(5)) return@withContext it.stream.url
        }

        try {
            val url = "https://music.youtube.com/watch?v=$videoId"
            // SERVICE_ID for YouTube in NewPipe is 0
            val info = ExtractorHelper.getStreamInfo(0, url)

            val bestAudio = info.audioStreams.maxByOrNull { it.bitrate }

            bestAudio?.content?.let { streamUrl ->
                val audioStream = AudioStream(
                    url = streamUrl,
                    bitrate = bestAudio.bitrate,
                    mimeType = bestAudio.format.toString()
                )
                cache[videoId] = CachedStream(audioStream, now)
                streamUrl
            }
        } catch (_: Exception) {
            null
        }
    }
}
