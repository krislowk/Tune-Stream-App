package com.vynce.music.utils

import android.net.ConnectivityManager
import android.net.Uri
import android.util.Log
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import com.vynce.music.db.daos.DatabaseDao
import com.vynce.music.models.StreamCache
import com.vynce.music.repository.constants.AudioQuality
import com.vynce.music.utils.YTPlayerUtils.MAIN_CLIENT
import com.vynce.music.utils.YTPlayerUtils.STREAM_FALLBACK_CLIENTS
import com.vynce.music.utils.YTPlayerUtils.validateStatus
import com.vynce.music.utils.cipher.CipherDeobfuscator
import com.vynce.music.utils.cipher.FunctionNameExtractor
import com.vynce.music.utils.cipher.PlayerJsFetcher
import com.vynce.music.utils.potoken.PoTokenGenerator
import com.vynce.music.utils.potoken.PoTokenResult
import com.vynce.vynceclient.NewPipeExtractor
import com.vynce.vynceclient.YouTube
import com.vynce.vynceclient.models.YouTubeClient
import com.vynce.vynceclient.models.YouTubeClient.Companion.ANDROID_CREATOR
import com.vynce.vynceclient.models.YouTubeClient.Companion.ANDROID_VR_1_43_32
import com.vynce.vynceclient.models.YouTubeClient.Companion.ANDROID_VR_1_61_48
import com.vynce.vynceclient.models.YouTubeClient.Companion.ANDROID_VR_NO_AUTH
import com.vynce.vynceclient.models.YouTubeClient.Companion.IOS
import com.vynce.vynceclient.models.YouTubeClient.Companion.IPADOS
import com.vynce.vynceclient.models.YouTubeClient.Companion.MOBILE
import com.vynce.vynceclient.models.YouTubeClient.Companion.TVHTML5
import com.vynce.vynceclient.models.YouTubeClient.Companion.TVHTML5_SIMPLY_EMBEDDED_PLAYER
import com.vynce.vynceclient.models.YouTubeClient.Companion.WEB
import com.vynce.vynceclient.models.YouTubeClient.Companion.WEB_CREATOR
import com.vynce.vynceclient.models.YouTubeClient.Companion.WEB_REMIX
import com.vynce.vynceclient.models.response.PlayerResponse
import okhttp3.OkHttpClient

@UnstableApi
object YTPlayerUtils {
    private const val logTag = "YTPlayerUtils"
    private const val TAG = "YTPlayerUtils"

    private val httpClient = OkHttpClient.Builder()
        .proxy(YouTube.proxy)
        .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val poTokenGenerator = PoTokenGenerator()

    private val MAIN_CLIENT: YouTubeClient = ANDROID_VR_1_43_32

    private val STREAM_FALLBACK_CLIENTS: Array<YouTubeClient> = arrayOf(
        ANDROID_VR_NO_AUTH,
        ANDROID_VR_1_61_48,
        ANDROID_CREATOR,
        TVHTML5_SIMPLY_EMBEDDED_PLAYER,
        IPADOS,
        MOBILE,
        IOS,
        WEB,
        WEB_CREATOR,
        TVHTML5,
        WEB_REMIX
    )

    data class PlaybackData(
        val audioConfig: PlayerResponse.PlayerConfig.AudioConfig?,
        val videoDetails: PlayerResponse.VideoDetails?,
        val playbackTracking: PlayerResponse.PlaybackTracking?,
        val format: PlayerResponse.StreamingData.Format,
        val streamUrl: String,
        val streamExpiresInSeconds: Int,
    )
    /**
     * Custom player response intended to use for playback.
     * Metadata like audioConfig and videoDetails are from [MAIN_CLIENT].
     * Format & stream can be from [MAIN_CLIENT] or [STREAM_FALLBACK_CLIENTS].
     */
    suspend fun playerResponseForPlayback(
        videoId: String,
        playlistId: String? = null,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager,
        databaseDao: DatabaseDao? = null
    ): Result<PlaybackData> = runCatching {

        // 1. Check Cache
        if (databaseDao != null) {
            databaseDao.getStream(videoId)?.let { cached ->
                val age = System.currentTimeMillis() - cached.timestamp
                if (age < 5 * 3600 * 1000) { // 5 hours cache
                    Log.d(TAG, "Cache hit for $videoId")
                    val metadata = playerResponseForMetadata(videoId, playlistId).getOrNull()
                    if (metadata != null) {
                        val format = metadata.streamingData?.adaptiveFormats?.find { it.bitrate == cached.bitrate && it.mimeType == cached.mimeType }
                            ?: metadata.streamingData?.adaptiveFormats?.find { it.isAudio }
                        
                        if (format != null) {
                            return@runCatching PlaybackData(
                                audioConfig = metadata.playerConfig?.audioConfig,
                                videoDetails = metadata.videoDetails,
                                playbackTracking = metadata.playbackTracking,
                                format = format,
                                streamUrl = cached.url,
                                streamExpiresInSeconds = (metadata.streamingData?.expiresInSeconds ?: 3600) - (age / 1000).toInt()
                            )
                        }
                    }
                }
            }
        }

        val traceId = System.currentTimeMillis().toString().takeLast(6)
        Log.d(TAG, "================ TRACE START [$traceId] ================")
        Log.d(TAG, "videoId=$videoId playlistId=$playlistId quality=$audioQuality")

        // Check if this is an uploaded/privately owned track
        val isUploadedTrack = playlistId == "MLPT" || playlistId?.contains("MLPT") == true
        Log.d(TAG, "Content type detection (preliminary):")
        Log.d(TAG, "  isUploadedTrack (from playlistId): $isUploadedTrack")

        val isLoggedIn = YouTube.cookie != null
        Log.d(TAG, "Authentication status: ${if (isLoggedIn) "LOGGED_IN" else "ANONYMOUS"}")

        // Get signature timestamp (same as before for normal content)
        val signatureTimestamp = getSignatureTimestampOrNull(videoId)
        Log.d(logTag, "Signature timestamp: ${signatureTimestamp.timestamp}")

        // Generate PoToken
        var poToken: PoTokenResult? = null
        val sessionId = if (isLoggedIn) YouTube.dataSyncId else YouTube.visitorData
        if (MAIN_CLIENT.useWebPoTokens && sessionId != null) {
            Log.d(logTag, "Generating PoToken for WEB_REMIX with sessionId")
            try {
                poToken = poTokenGenerator.getWebClientPoToken(videoId, sessionId)
                if (poToken != null) {
                    Log.d(logTag, "PoToken generated successfully")
                }
            } catch (e: Exception) {
                Log.e(logTag, "PoToken generation failed: ${e.message}", e)
            }
        }



        // Try WEB_REMIX with signature timestamp and poToken (same as before)
        Log.d(logTag, "Attempting to get player response using MAIN_CLIENT: ${MAIN_CLIENT.clientName}")
        var mainPlayerResponse = YouTube.player(videoId, playlistId, MAIN_CLIENT, signatureTimestamp.timestamp, poToken?.playerRequestPoToken).getOrThrow()
        var usedAgeRestrictedClient: YouTubeClient? = null
        val wasOriginallyAgeRestricted: Boolean

        // Check if WEB_REMIX response indicates age-restricted
        val mainStatus = mainPlayerResponse.playabilityStatus.status
        val isAgeRestrictedFromResponse = mainStatus in listOf("AGE_CHECK_REQUIRED", "AGE_VERIFICATION_REQUIRED", "LOGIN_REQUIRED", "CONTENT_CHECK_REQUIRED")
        wasOriginallyAgeRestricted = isAgeRestrictedFromResponse

        if (isAgeRestrictedFromResponse && isLoggedIn) {
            // Age-restricted: use WEB_CREATOR directly (no NewPipe needed from here)
            Log.d(logTag, "Age-restricted detected, using WEB_CREATOR")
            Log.i(logTag, "Age-restricted: using WEB_CREATOR for videoId=$videoId")
            val creatorResponse = YouTube.player(videoId, playlistId, WEB_CREATOR, null, null).getOrNull()
            if (creatorResponse?.playabilityStatus?.status == "OK") {
                Log.d(logTag, "WEB_CREATOR works for age-restricted content")
                mainPlayerResponse = creatorResponse
                usedAgeRestrictedClient = WEB_CREATOR
            }
        }

        // If we still don't have a valid response, throw

        var audioConfig = mainPlayerResponse.playerConfig?.audioConfig
        val videoDetails = mainPlayerResponse.videoDetails
        val playbackTracking = mainPlayerResponse.playbackTracking
        var format: PlayerResponse.StreamingData.Format? = null
        var streamUrl: String? = null
        var streamExpiresInSeconds: Int? = null
        var streamPlayerResponse: PlayerResponse? = null
        val retryMainPlayerResponse: PlayerResponse? = if (usedAgeRestrictedClient != null) mainPlayerResponse else null

        // Check current status
        val currentStatus = mainPlayerResponse.playabilityStatus.status
        val isAgeRestricted = currentStatus in listOf("AGE_CHECK_REQUIRED", "AGE_VERIFICATION_REQUIRED", "LOGIN_REQUIRED", "CONTENT_CHECK_REQUIRED")

        if (isAgeRestricted) {
            Log.d(logTag, "Content is still age-restricted (status: $currentStatus), will try fallback clients")
            Log.i(TAG, "Age-restricted content detected: videoId=$videoId, status=$currentStatus")
        }

        // For age-restricted: skip main client, start with fallbacks
        // For normal content: standard order
        val startIndex = when {
            isAgeRestricted -> 0
            else -> -1
        }

        var bestFallbackFormat: PlayerResponse.StreamingData.Format? = null
        var bestFallbackUrl: String? = null
        var bestFallbackExpiry: Int? = null
        var bestFallbackResponse: PlayerResponse? = null

        val hasHighQuality = mainPlayerResponse.streamingData?.adaptiveFormats?.any { it.audioQuality == "AUDIO_QUALITY_HIGH" } == true

        for (clientIndex in (startIndex until STREAM_FALLBACK_CLIENTS.size)) {
            // reset for each client
            format = null
            streamUrl = null
            streamExpiresInSeconds = null

            // decide which client to use for streams and load its player response
            val client: YouTubeClient
            if (clientIndex == -1) {
                // try with streams from main client first (use retry response if available)
                client = MAIN_CLIENT
                streamPlayerResponse = retryMainPlayerResponse ?: mainPlayerResponse
                Log.d(logTag, "Trying stream from MAIN_CLIENT: ${client.clientName}")
            } else {
                // after main client use fallback clients
                client = STREAM_FALLBACK_CLIENTS[clientIndex]
                Log.d(logTag, "Trying fallback client ${clientIndex + 1}/${STREAM_FALLBACK_CLIENTS.size}: ${client.clientName}")

                if (client.loginRequired && !isLoggedIn && YouTube.cookie == null) {
                    // skip client if it requires login but user is not logged in
                    Log.d(logTag, "Skipping client ${client.clientName} - requires login but user is not logged in")
                    continue
                }

                Log.d(logTag, "Fetching player response for fallback client: ${client.clientName}")
                // Only pass poToken for clients that support it
                val clientPoToken = if (client.useWebPoTokens) poToken?.playerRequestPoToken else null
                // Skip signature timestamp for age-restricted (faster), use it for normal content
                val clientSigTimestamp = if (wasOriginallyAgeRestricted) null else signatureTimestamp.timestamp
                streamPlayerResponse =
                    YouTube.player(videoId, playlistId, client, clientSigTimestamp, clientPoToken).getOrNull()
            }

            // process current client response
            if (streamPlayerResponse?.playabilityStatus?.status == "OK") {
                Log.d(logTag, "Player response status OK for client: ${if (clientIndex == -1) MAIN_CLIENT.clientName else STREAM_FALLBACK_CLIENTS[clientIndex].clientName}")
                // Skip NewPipe for age-restricted content (NewPipe doesn't use our auth)
                val responseToUse = if (wasOriginallyAgeRestricted) {
                    Log.d(logTag, "Skipping NewPipe for age-restricted content")
                    streamPlayerResponse
                } else {
                    // Try to get streams using newPipePlayer method
                    val newPipeResponse = YouTube.newPipePlayer(videoId, streamPlayerResponse)
                    newPipeResponse ?: streamPlayerResponse
                }

                if (audioConfig == null) {
                    audioConfig = responseToUse.playerConfig?.audioConfig

                    if (audioConfig != null) {
                        Log.d(logTag, "AudioConfig obtained from response of client: ${if (clientIndex == -1) MAIN_CLIENT.clientName else STREAM_FALLBACK_CLIENTS[clientIndex].clientName}")
                    } else {
                        Log.d(logTag, "No audioConfig found in responseToUse.")
                    }
                }

                format =
                    findFormat(
                        responseToUse,
                        audioQuality,
                        connectivityManager,
                    )

                if (format == null) {
                    Log.d(logTag, "No suitable format found for client: ${if (clientIndex == -1) MAIN_CLIENT.clientName else STREAM_FALLBACK_CLIENTS[clientIndex].clientName}")
                    continue
                }

                Log.d(logTag, "Format found: ${format.mimeType}, bitrate: ${format.bitrate}")

                streamUrl = findUrlOrNull(format, videoId, responseToUse, skipNewPipe = wasOriginallyAgeRestricted)
                if (streamUrl == null) {
                    Log.d(logTag, "Stream URL not found for format")
                    continue
                }

                // Apply n-transform for throttle parameter handling
                val currentClient = if (clientIndex == -1) {
                    usedAgeRestrictedClient ?: MAIN_CLIENT
                } else {
                    STREAM_FALLBACK_CLIENTS[clientIndex]
                }

                val musicVideoType = streamPlayerResponse.videoDetails?.musicVideoType

                Log.d(TAG, "=== N-TRANSFORM DECISION ===")
                Log.d(TAG, "Content type analysis:")
                Log.d(TAG, "  musicVideoType: $musicVideoType")
                Log.d(TAG, "  isUploadedTrack (from playlistId): $isUploadedTrack")
                Log.d(TAG, "  wasOriginallyAgeRestricted: $wasOriginallyAgeRestricted")
                Log.d(TAG, "Client analysis:")
                Log.d(TAG, "  currentClient: ${currentClient.clientName}")
                Log.d(TAG, "  useWebPoTokens: ${currentClient.useWebPoTokens}")

                // Apply n-transform and PoToken for web clients and Android VR
                val needsNTransform = currentClient.useWebPoTokens ||
                        currentClient.clientName in listOf("WEB", "WEB_REMIX", "WEB_CREATOR", "TVHTML5", "ANDROID_VR")

                Log.d(TAG, "N-transform decision:")
                Log.d(TAG, "  needsNTransform: $needsNTransform")
                Log.d(TAG, "  Reason: useWebPoTokens=${currentClient.useWebPoTokens}, " + "clientInList=${currentClient.clientName in listOf("WEB", "WEB_REMIX", "WEB_CREATOR", "TVHTML5")}")

                if (needsNTransform) {
                    try {
                        Log.d(TAG, "Applying n-transform to stream URL...")
                        Log.d(TAG, "  Original URL length: ${streamUrl.length}")
                        Log.d(TAG, "  Original URL preview: ${streamUrl.take(100)}...")

                        val originalUrl = streamUrl
                        // Use CipherDeobfuscator for n-transform (fixed implementation)
                        streamUrl = CipherDeobfuscator.transformNParamInUrl(streamUrl)

                        Log.d(TAG, "  Transformed URL length: ${streamUrl.length}")
                        Log.d(TAG, "  URL changed: ${originalUrl != streamUrl}")

                        // Append pot= parameter with streaming data poToken
                        val needsPoToken = currentClient.useWebPoTokens && poToken?.streamingDataPoToken != null
                        Log.d(TAG, "PoToken decision:")
                        Log.d(TAG, "  needsPoToken: $needsPoToken")
                        Log.d(TAG, "  hasStreamingDataPoToken: ${poToken?.streamingDataPoToken != null}")

                        if (needsPoToken) {
                            Log.d(TAG, "Appending pot= parameter to stream URL")
                            val separator = if ("?" in streamUrl) "&" else "?"
                            streamUrl = "${streamUrl}${separator}pot=${Uri.encode(poToken.streamingDataPoToken)}"
                            Log.d(TAG, "  Final URL length (with pot): ${streamUrl.length}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "N-transform or pot append failed: ${e.message}", e)
                        Log.e(TAG, "Stream expiration time not found", e)
                        // Continue with original URL
                    }
                } else {
                    Log.d(TAG, "Skipping n-transform (not required for this client/content)")
                }

                streamExpiresInSeconds = streamPlayerResponse.streamingData?.expiresInSeconds
                if (streamExpiresInSeconds == null) {
                    Log.d(logTag, "")
                    continue
                }

                Log.d(logTag, "Stream expires in: $streamExpiresInSeconds seconds")

                fun scoreFallbackQuality(quality: String?): Int = when (quality) {
                    "AUDIO_QUALITY_HIGH" -> 3
                    "AUDIO_QUALITY_MEDIUM" -> 2
                    "AUDIO_QUALITY_LOW" -> 1
                    else -> 0
                }

                fun scoreFallbackCodec(mimeType: String): Int = when {
                    mimeType.contains("opus", ignoreCase = true) -> 2
                    mimeType.contains("mp4a", ignoreCase = true) -> 1
                    else -> 0
                }

                if (audioQuality == AudioQuality.HIGH && format.audioQuality != "AUDIO_QUALITY_HIGH" && hasHighQuality) {
                    val isBetter = bestFallbackFormat == null ||
                            compareValuesBy(
                                format, bestFallbackFormat,
                                { scoreFallbackQuality(it.audioQuality) },
                                { it.audioChannels ?: 2 },
                                { scoreFallbackCodec(it.mimeType) },
                                { it.bitrate }
                            ) > 0
                    if (isBetter) {
                        Log.d(logTag, "Saving fallback format: ${format.mimeType}, bitrate: ${format.bitrate}")
                        bestFallbackFormat = format
                        bestFallbackUrl = streamUrl
                        bestFallbackExpiry = streamExpiresInSeconds
                        bestFallbackResponse = streamPlayerResponse
                    }
                    continue
                }

                if (clientIndex == STREAM_FALLBACK_CLIENTS.size - 1) {
                    /** skip [validateStatus] for last client */
                    Log.d(logTag, "Using last fallback client without validation: ${STREAM_FALLBACK_CLIENTS[clientIndex].clientName}")
                    Log.i(logTag, "Playback: client=${currentClient.clientName}, videoId=$videoId")
                    break
                }

                // Optimization: Trust MAIN_CLIENT for normal tracks
                val shouldValidate = clientIndex != -1 || isAgeRestricted || isUploadedTrack
                if (!shouldValidate || validateStatus(streamUrl)) {
                    // working stream found
                    Log.d(logTag, "Stream validated successfully with client: ${currentClient.clientName}")
                    // Log for release builds
                    Log.i(logTag, "Playback: client=${currentClient.clientName}, videoId=$videoId")
                    break
                } else {
                    Log.d(logTag, "Stream validation failed for client: ${currentClient.clientName}")
                }
            } else {
                Log.d(logTag, "Player response status not OK: ${streamPlayerResponse?.playabilityStatus?.status}, reason: ${streamPlayerResponse?.playabilityStatus?.reason}")
            }
        }

        if (audioQuality == AudioQuality.HIGH && format?.audioQuality != "AUDIO_QUALITY_HIGH" && bestFallbackFormat != null) {
            Log.d(logTag, "Using best fallback format: ${bestFallbackFormat.mimeType}, bitrate: ${bestFallbackFormat.bitrate}")
            format = bestFallbackFormat
            streamUrl = bestFallbackUrl
            streamExpiresInSeconds = bestFallbackExpiry
            streamPlayerResponse = bestFallbackResponse
        }

        if (streamPlayerResponse == null) {
            Log.e(logTag, "Bad stream player response - all clients failed")
            if (isUploadedTrack) {
                println("[PLAYBACK_DEBUG] FAILURE: All clients failed for uploaded track videoId=$videoId")
            }
            throw Exception("Bad stream player response")
        }

        if (streamPlayerResponse.playabilityStatus.status != "OK") {
            val errorReason = streamPlayerResponse.playabilityStatus.reason
            // YouTube often surfaces generic reasons (e.g. "error 2000") for restricted or
            // unavailable streams; Vynce cannot recover those without official playback.
            Log.e(logTag, "Playability status not OK: $errorReason")
            if (isUploadedTrack) {
                println("[PLAYBACK_DEBUG] FAILURE: Playability not OK for uploaded track - status=${streamPlayerResponse.playabilityStatus.status}, reason=$errorReason")
            }
            throw PlaybackException(
                errorReason,
                null,
                PlaybackException.ERROR_CODE_REMOTE_ERROR
            )
        }

        if (streamExpiresInSeconds == null) {
            Log.e(logTag, "Missing stream expire time")
            throw Exception("Missing stream expire time")
        }

        if (format == null) {
            Log.e(logTag, "Could not find format")
            throw Exception("Could not find format")
        }

        if (streamUrl == null) {
            Log.e(logTag, "Could not find stream url")
            throw Exception("Could not find stream url")
        }

        // 2. Save to Cache
        databaseDao?.upsertStream(
            StreamCache(
                videoId = videoId,
                url = streamUrl,
                bitrate = format.bitrate,
                mimeType = format.mimeType
            )
        )

        Log.d(logTag, "Successfully obtained playback data with format: ${format.mimeType}, bitrate: ${format.bitrate}")
        if (isUploadedTrack) {
            println("[PLAYBACK_DEBUG] SUCCESS: Got playback data for uploaded track - format=${format.mimeType}, streamUrl=${streamUrl.take(100)}...")
        }
        PlaybackData(
            audioConfig,
            videoDetails,
            playbackTracking,
            format,
            streamUrl,
            streamExpiresInSeconds,
        )
    }.onFailure { e ->
        println("[PLAYBACK_DEBUG] EXCEPTION during playback for videoId=$videoId: ${e::class.simpleName}: ${e.message}")
        e.printStackTrace()
    }
    /**
     * Simple player response intended to use for metadata only.
     * Stream URLs of this response might not work so don't use them.
     */
    suspend fun playerResponseForMetadata(
        videoId: String,
        playlistId: String? = null,
    ): Result<PlayerResponse> {
        Log.d(logTag, "Fetching metadata-only player response for videoId: $videoId using MAIN_CLIENT: ${MAIN_CLIENT.clientName}")
        return YouTube.player(videoId, playlistId, client = WEB_REMIX) // ANDROID_VR does not work with history
            .onSuccess { Log.d(logTag, "Successfully fetched metadata") }
            .onFailure { Log.e(logTag, "Failed to fetch metadata", it) }
    }

    private fun findFormat(
        playerResponse: PlayerResponse,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager,
    ): PlayerResponse.StreamingData.Format? {
        Log.d(logTag, "Finding format with audioQuality: $audioQuality, network metered: ${connectivityManager.isActiveNetworkMetered}")

        val adaptiveFormats = playerResponse.streamingData?.adaptiveFormats ?: return null

        val audioCapableFormats = adaptiveFormats.filter { it.isAudio }
        if (audioCapableFormats.isEmpty()) return null

        val maxBitrate = audioCapableFormats.maxOfOrNull { it.bitrate } ?: return null

        fun scoreCodec(mimeType: String): Int = when {
            mimeType.contains("opus", ignoreCase = true) -> 2
            mimeType.contains("mp4a", ignoreCase = true) -> 1
            else -> 0
        }

        val format = when (audioQuality) {
            AudioQuality.HIGH -> {
                audioCapableFormats.maxWithOrNull(
                    compareBy<PlayerResponse.StreamingData.Format> { format ->
                        when (format.audioQuality) {
                            "AUDIO_QUALITY_HIGH" -> 3
                            "AUDIO_QUALITY_MEDIUM" -> 2
                            "AUDIO_QUALITY_LOW" -> 1
                            else -> 0
                        }
                    }.thenBy { it.audioChannels ?: 2 }
                        .thenBy { scoreCodec(it.mimeType) }
                        .thenBy { it.bitrate }
                )
            }

            AudioQuality.LOW -> {
                val cappedFormats = audioCapableFormats.filter { it.bitrate <= 128000 }
                val lowFormat = cappedFormats
                    .filter { it.isOriginal }
                    .maxByOrNull { it.bitrate }
                    ?: cappedFormats.maxByOrNull { it.bitrate }
                    ?: audioCapableFormats
                        .filter { it.isOriginal }
                        .minByOrNull { kotlin.math.abs(it.bitrate.toDouble() - 128000.0) }
                    ?: audioCapableFormats.maxByOrNull { it.bitrate }

                if (lowFormat != null) {
                    Log.d(logTag, "Selected LOW format: itag=${lowFormat.itag}, bitrate: ${lowFormat.bitrate}")
                }

                lowFormat
            }

            AudioQuality.AUTO -> {
                val targetBitrate = if (connectivityManager.isActiveNetworkMetered) 128000.0 else maxBitrate.toDouble()
                val cappedFormats = audioCapableFormats.filter { it.bitrate <= targetBitrate }
                val autoFormat = cappedFormats
                    .filter { it.isOriginal }
                    .maxByOrNull { it.bitrate }
                    ?: cappedFormats.maxByOrNull { it.bitrate }
                    ?: audioCapableFormats
                        .filter { it.isOriginal }
                        .minByOrNull { kotlin.math.abs(it.bitrate - targetBitrate) }
                    ?: audioCapableFormats.maxByOrNull { it.bitrate }

                if (autoFormat != null) {
                    Log.d(logTag, "Selected AUTO format: itag=${autoFormat.itag}, bitrate: ${autoFormat.bitrate}")
                }

                autoFormat
            }
        }

        if (format != null) {
            Log.d(logTag, "Selected format: itag=${format.itag}, mimeType=${format.mimeType}, bitrate=${format.bitrate}, audioQuality label: ${format.audioQuality}")
        } else {
            Log.d(logTag, "No suitable audio format found")
        }

        return format
    }
    /**
     * Checks if the stream url returns a successful status.
     * If this returns true the url is likely to work.
     * If this returns false the url might cause an error during playback.
     */
    private fun validateStatus(url: String): Boolean {
        Log.d(logTag, "Validating stream URL status")
        try {
            val requestBuilder = okhttp3.Request.Builder()
                .head()
                .url(url)

            // Add authentication cookie for privately owned tracks
            YouTube.cookie?.let { cookie ->
                requestBuilder.addHeader("Cookie", cookie)
                println("[PLAYBACK_DEBUG] Added cookie to validation request")
            }

            val response = httpClient.newCall(requestBuilder.build()).execute()
            val isSuccessful = response.isSuccessful
            Log.d(logTag, "Stream URL validation result: ${if (isSuccessful) "Success" else "Failed"} (${response.code})")
            return isSuccessful
        } catch (e: Exception) {
            Log.e(logTag, "Stream URL validation failed with exception", e)
        }
        return false
    }
    data class SignatureTimestampResult(
        val timestamp: Int?,
        val isAgeRestricted: Boolean
    )

    private suspend fun getSignatureTimestampOrNull(videoId: String): SignatureTimestampResult {
        Log.d(logTag, "Getting signature timestamp for videoId: $videoId")
        val result = NewPipeExtractor.getSignatureTimestamp(videoId)
        return result.fold(
            onSuccess = { timestamp ->
                Log.d(logTag, "Signature timestamp obtained via NewPipe: $timestamp")
                SignatureTimestampResult(timestamp, isAgeRestricted = false)
            },
            onFailure = { error ->
                val isAgeRestricted = error.message?.contains("age-restricted", ignoreCase = true) == true ||
                        error.cause?.message?.contains("age-restricted", ignoreCase = true) == true
                if (isAgeRestricted) {
                    Log.d(logTag, "Age-restricted content detected from NewPipe")
                    Log.i(logTag, "Age-restricted detected early via NewPipe: videoId=$videoId")
                } else {
                    Log.e(logTag, "Failed to get signature timestamp via NewPipe", error)
                }
                // Fallback: extract signatureTimestamp directly from player.js when NewPipe fails.
                // This keeps playback working when the NewPipe extractor is outdated for a new
                // player version, as long as the player.js still embeds signatureTimestamp inline.
                val fallbackSts = runCatching {
                    Log.d(logTag, "Trying player.js fallback for signature timestamp")
                    val (playerJs, hash) = PlayerJsFetcher.getPlayerJs()
                        ?: error("PlayerJsFetcher returned null")
                    Log.d(logTag, "Got player.js (hash=$hash), extracting signatureTimestamp")
                    FunctionNameExtractor.extractSignatureTimestamp(playerJs)
                        ?: error("extractSignatureTimestamp returned null for hash=$hash")
                }.onSuccess { sts ->
                    Log.d(logTag, "Signature timestamp obtained via player.js fallback: $sts")
                }.onFailure { e ->
                    Log.e(logTag, "player.js fallback for signature timestamp also failed", e)
                }.getOrNull()
                SignatureTimestampResult(fallbackSts, isAgeRestricted)
            }
        )
    }

    private suspend fun findUrlOrNull(
        format: PlayerResponse.StreamingData.Format,
        videoId: String,
        playerResponse: PlayerResponse,
        skipNewPipe: Boolean = false
    ): String? {
        Log.d(logTag, "Finding stream URL for format: ${format.mimeType}, videoId: $videoId, skipNewPipe: $skipNewPipe")

        // First check if format already has a URL
        if (!format.url.isNullOrEmpty()) {
            Log.d(logTag, "Using URL from format directly")
            return format.url
        }

        // Try custom cipher deobfuscation for signatureCipher formats
        val signatureCipher = format.signatureCipher ?: format.cipher
        if (!signatureCipher.isNullOrEmpty()) {
            Log.d(logTag, "Format has signatureCipher, using custom deobfuscation")
            val customDeobfuscatedUrl = CipherDeobfuscator.deobfuscateStreamUrl(signatureCipher, videoId)
            if (customDeobfuscatedUrl != null) {
                Log.d(logTag, "Stream URL obtained via custom cipher deobfuscation")
                return customDeobfuscatedUrl
            }
            Log.d(logTag, "Custom cipher deobfuscation failed")
        }

        // Always try NewPipe signature deobfuscation - it doesn't need auth,
        // it just applies the cipher algorithm from player.js.
        // This is critical for privately-owned tracks where skipNewPipe is true.
        val deobfuscatedUrl = NewPipeExtractor.getStreamUrl(format, videoId)
        if (deobfuscatedUrl != null) {
            Log.d(logTag, "Stream URL obtained via NewPipe deobfuscation")
            return deobfuscatedUrl
        }

        // Skip StreamInfo fallback for age-restricted or private content
        // (StreamInfo fetch may fail without auth for these)
        if (skipNewPipe) {
            Log.d(logTag, "Skipping StreamInfo fallback for age-restricted/private content")
            return null
        }

        // Fallback: try to get URL from StreamInfo
        Log.d(logTag, "Trying StreamInfo fallback for URL")
        val streamUrls = YouTube.getNewPipeStreamUrls(videoId)
        if (streamUrls.isNotEmpty()) {
            val streamUrl = streamUrls.find { it.first == format.itag }?.second
            if (streamUrl != null) {
                Log.d(logTag, "Stream URL obtained from StreamInfo")
                return streamUrl
            }

            // If exact itag not found, try to find any audio stream
            val audioStream = streamUrls.find { urlPair ->
                playerResponse.streamingData?.adaptiveFormats?.any {
                    it.itag == urlPair.first && it.isAudio
                } == true
            }?.second

            if (audioStream != null) {
                Log.d(logTag, "Audio stream URL obtained from StreamInfo (different itag)")
                return audioStream
            }
        }

        Log.e(logTag, "Failed to get stream URL")
        return null
    }

    fun forceRefreshForVideo(videoId: String) {
        Log.d(logTag, "Force refreshing for videoId: $videoId")
    }
}



