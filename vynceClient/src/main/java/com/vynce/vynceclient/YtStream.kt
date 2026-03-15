package com.vynce.vynceclient

import android.util.LruCache
import com.vynce.vynceclient.models.AudioStream
import com.vynce.vynceclient.models.YtClient
import com.vynce.vynceclient.models.response.PlayerResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

object YtStream {
    // 1. Memory: Reduce cache size to 50 (URLs expire anyway, no need for 100)
    // 2. Efficiency: Store timestamp to prevent playing expired/broken URLs
    private class CachedStream(val stream: AudioStream, val timestamp: Long)
    private val cache = LruCache<String, CachedStream>(50)

    suspend fun getStream(videoId: String): AudioStream? = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()

        // 3. Network Efficiency: Only return from cache if URL is < 5 hours old
        cache.get(videoId)?.let {
            if (now - it.timestamp < TimeUnit.HOURS.toMillis(5)) return@withContext it.stream
        }

        try {
            // 4. Speed: Concurrent/Race-style fetching isn't supported here,
            // but we use the most reliable client first to avoid fallback latency.
            val response = Youtube.innerTube.player(YtClient.ANDROID_MUSIC, videoId)

            if (response.playabilityStatus.status == "OK") {
                return@withContext processResponse(videoId, response)
            }

            // Fallback only if absolutely necessary
            val fallback = Youtube.innerTube.player(YtClient.TVHTML5, videoId)
            processResponse(videoId, fallback)
        } catch (_: Exception) {
            null
        }
    }

    private fun processResponse(videoId: String, response: PlayerResponse): AudioStream? {
        val formats = response.streamingData?.adaptiveFormats ?: return null

        // 5. Allocation Efficiency: Use a single-pass selection instead of multiple sort/filter passes
        var bestFormat: PlayerResponse.StreamingData.Format? = null
        var bestScore = -1

        for (format in formats) {
            if (!format.isAudio || format.url.isNullOrEmpty()) continue

            // Scoring system (Fastest way to compare without creating Comparator objects)
            // Opus (3) > M4A (2) > Other (1)
            val typeScore = when {
                format.mimeType.contains("opus", true) -> 3
                format.mimeType.contains("mp4", true) || format.mimeType.contains("m4a", true) -> 2
                else -> 1
            }

            if (bestFormat == null || typeScore > bestScore || (typeScore == bestScore && format.bitrate > bestFormat.bitrate)) {
                bestFormat = format
                bestScore = typeScore
            }
        }

        return bestFormat?.let {
            val audioStream = AudioStream(
                url = it.url!!,
                bitrate = it.bitrate,
                mimeType = it.mimeType
            )
            cache.put(videoId, CachedStream(audioStream, System.currentTimeMillis()))
            audioStream
        }
    }
}
