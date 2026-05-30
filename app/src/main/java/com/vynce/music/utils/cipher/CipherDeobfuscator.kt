package com.vynce.music.utils.cipher

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Main cipher deobfuscation orchestrator for YouTube stream URLs.
 *
 * Handles both signature deobfuscation (for signatureCipher streams) and
 * n-parameter transformation (for throttle avoidance / 403 fix).
 */
object CipherDeobfuscator {
    private const val TAG = "Metrolist_CipherDeobfusc"

    lateinit var appContext: Context
        private set

    fun initialize(context: Context) {
        Log.d(TAG, "CipherDeobfuscator initializing...")
        appContext = context.applicationContext
        Log.d(TAG, "CipherDeobfuscator initialized")
    }

    private var cipherWebView: CipherWebView? = null
    private var currentPlayerHash: String? = null

    /**
     * Deobfuscate a signatureCipher stream URL.
     *
     * The signatureCipher is a query string containing:
     * - s: The obfuscated signature
     * - sp: The signature parameter name (usually "sig" or "signature")
     * - url: The base stream URL
     *
     * Returns the full URL with deobfuscated signature, or null if failed.
     */
    suspend fun deobfuscateStreamUrl(signatureCipher: String, videoId: String): String? {
        Log.d(TAG, "=== DEOBFUSCATE STREAM URL ===")
        Log.d(TAG, "videoId: $videoId")
        Log.d(TAG, "signatureCipher length: ${signatureCipher.length}")
        Log.d(TAG, "signatureCipher preview: ${signatureCipher.take(100)}...")

        return try {
            deobfuscateInternal(signatureCipher, videoId, isRetry = false)
        } catch (e: Exception) {
            Log.e(TAG, "Cipher deobfuscation failed, retrying with fresh JS: ${e.message}", e)
            Log.d(TAG, "Invalidating cache and retrying...")
            try {
                PlayerJsFetcher.invalidateCache()
                closeWebView()
                deobfuscateInternal(signatureCipher, videoId, isRetry = true)
            } catch (retryE: Exception) {
                Log.e(TAG, "Cipher deobfuscation retry also failed: ${retryE.message}", retryE)
                null
            }
        }
    }

    private suspend fun deobfuscateInternal(signatureCipher: String, videoId: String, isRetry: Boolean): String? {
        Log.d(TAG, "deobfuscateInternal: videoId=$videoId, isRetry=$isRetry")

        // Parse the signatureCipher query string
        val params = parseQueryParams(signatureCipher)
        val obfuscatedSig = params["s"]
        val sigParam = params["sp"] ?: "signature"
        val baseUrl = params["url"]

        Log.d(TAG, "Parsed signatureCipher params:")
        Log.d(TAG, "  s (obfuscated sig): ${obfuscatedSig?.take(30)}... (length=${obfuscatedSig?.length})")
        Log.d(TAG, "  sp (sig param name): $sigParam")
        Log.d(TAG, "  url: ${baseUrl?.take(80)}...")

        if (obfuscatedSig == null || baseUrl == null) {
            Log.e(TAG, "Could not parse signatureCipher params: s=${obfuscatedSig != null}, url=${baseUrl != null}")
            return null
        }

        val webView = getOrCreateWebView(forceRefresh = isRetry)
        if (webView == null) {
            Log.e(TAG, "Failed to get/create CipherWebView")
            return null
        }

        Log.d(TAG, "Calling webView.deobfuscateSignature()...")
        val deobfuscatedSig = webView.deobfuscateSignature(obfuscatedSig)
        Log.d(TAG, "Deobfuscated signature: ${deobfuscatedSig.take(30)}... (length=${deobfuscatedSig.length})")

        // Build the URL with deobfuscated signature
        val separator = if ("?" in baseUrl) "&" else "?"
        val finalUrl = "$baseUrl${separator}${sigParam}=${Uri.encode(deobfuscatedSig)}"

        Log.d(TAG, "=== CIPHER DEOBFUSCATION SUCCESS ===")
        Log.d(TAG, "videoId: $videoId")
        Log.d(TAG, "Final URL length: ${finalUrl.length}")
        Log.d(TAG, "Final URL preview: ${finalUrl.take(100)}...")

        return finalUrl
    }

    /**
     * Transform the 'n' parameter in a streaming URL to avoid throttling/403.
     *
     * Uses the runtime-discovered n-function from the player JS WebView.
     * Returns the URL with the transformed 'n' value, or the original URL if transform fails.
     *
     * IMPORTANT: This must be called for WEB_REMIX, WEB, WEB_CREATOR, TVHTML5 clients
     * and for privately owned tracks (uploaded songs).
     */
    suspend fun transformNParamInUrl(url: String): String {
        Log.d(TAG, "=== N-TRANSFORM URL ===")
        Log.d(TAG, "Input URL length: ${url.length}")
        Log.d(TAG, "Input URL preview: ${url.take(100)}...")

        return try {
            transformNInternal(url)
        } catch (e: Exception) {
            Log.e(TAG, "N-transform failed, returning original URL: ${e.message}", e)
            url
        }
    }

    private suspend fun transformNInternal(url: String): String {
        // Extract the 'n' parameter value from the URL
        val nMatch = Regex("[?&]n=([^&]+)").find(url)
        if (nMatch == null) {
            Log.d(TAG, "No 'n' parameter found in URL, skipping transform")
            return url
        }

        val nValueEncoded = nMatch.groupValues[1]
        val nValue = Uri.decode(nValueEncoded)
        Log.d(TAG, "N-param found:")
        Log.d(TAG, "  encoded: $nValueEncoded")
        Log.d(TAG, "  decoded: $nValue")

        val webView = getOrCreateWebView(forceRefresh = false)
        if (webView == null) {
            Log.e(TAG, "Failed to get CipherWebView for n-transform")
            return url
        }

        Log.d(TAG, "CipherWebView state:")
        Log.d(TAG, "  nFunctionAvailable: ${webView.nFunctionAvailable}")
        Log.d(TAG, "  discoveredNFuncName: ${webView.discoveredNFuncName}")
        Log.d(TAG, "  usingHardcodedMode: ${webView.usingHardcodedMode}")

        if (!webView.nFunctionAvailable) {
            Log.e(TAG, "N-transform function was not discovered at init time")
            return url
        }

        Log.d(TAG, "Calling webView.transformN()...")
        val transformedN = webView.transformN(nValue)

        Log.d(TAG, "=== N-TRANSFORM SUCCESS ===")
        Log.d(TAG, "N-param: $nValue -> $transformedN")

        // Replace n= parameter in URL
        val transformedUrl = url.replaceFirst(
            Regex("([?&])n=[^&]+"),
            "$1n=${Uri.encode(transformedN)}"
        )

        Log.d(TAG, "Transformed URL length: ${transformedUrl.length}")
        return transformedUrl
    }

    private suspend fun getOrCreateWebView(forceRefresh: Boolean): CipherWebView? {
        Log.d(TAG, "getOrCreateWebView: forceRefresh=$forceRefresh, existing=${cipherWebView != null}")

        if (!forceRefresh && cipherWebView != null) {
            Log.d(TAG, "Reusing existing CipherWebView (hash=$currentPlayerHash)")
            return cipherWebView
        }

        // Close existing WebView if any
        if (cipherWebView != null) {
            Log.d(TAG, "Closing existing CipherWebView...")
            closeWebView()
        }

        // Fetch player JS
        Log.d(TAG, "Fetching player JS...")
        val result = PlayerJsFetcher.getPlayerJs(forceRefresh = forceRefresh)
        if (result == null) {
            Log.e(TAG, "Failed to get player JS")
            return null
        }
        val (playerJs, hash) = result
        Log.d(TAG, "Got player JS: hash=$hash, length=${playerJs.length}")

        // Run full analysis for logging - pass the known hash from PlayerJsFetcher
        Log.d(TAG, "Analyzing player JS for cipher functions (knownHash=$hash)...")
        val analysis = FunctionNameExtractor.analyzePlayerJs(playerJs, knownHash = hash)

        if (analysis.sigInfo == null) {
            Log.e(TAG, "Could not extract signature function info from player JS")
            return null
        }

        if (analysis.nFuncInfo == null) {
            Log.w(TAG, "Could not extract n-function info from player JS (will try brute-force)")
        }

        Log.d(TAG, "Creating CipherWebView...")
        Log.d(TAG, "  sig: ${analysis.sigInfo.name} (constantArg=${analysis.sigInfo.constantArg}, hardcoded=${analysis.sigInfo.isHardcoded})")
        Log.d(TAG, "  nFunc: ${analysis.nFuncInfo?.name}[${analysis.nFuncInfo?.arrayIndex}] (hardcoded=${analysis.nFuncInfo?.isHardcoded})")

        // Create WebView
        val webView = CipherWebView.create(
            context = appContext,
            playerJs = playerJs,
            sigInfo = analysis.sigInfo,
            nFuncInfo = analysis.nFuncInfo,
        )

        Log.d(TAG, "CipherWebView created successfully")
        Log.d(TAG, "  nFunctionAvailable: ${webView.nFunctionAvailable}")
        Log.d(TAG, "  sigFunctionAvailable: ${webView.sigFunctionAvailable}")
        Log.d(TAG, "  discoveredNFuncName: ${webView.discoveredNFuncName}")

        cipherWebView = webView
        currentPlayerHash = hash
        return webView
    }

    private suspend fun closeWebView() {
        Log.d(TAG, "closeWebView: existing=${cipherWebView != null}")
        withContext(Dispatchers.Main) {
            cipherWebView?.close()
        }
        cipherWebView = null
        currentPlayerHash = null
        Log.d(TAG, "CipherWebView closed and cleared")
    }

    private fun parseQueryParams(query: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        for (pair in query.split("&")) {
            val idx = pair.indexOf('=')
            if (idx > 0) {
                val key = Uri.decode(pair.substring(0, idx))
                val value = Uri.decode(pair.substring(idx + 1))
                result[key] = value
            }
        }
        Log.v(TAG, "parseQueryParams: ${result.keys.joinToString()}")
        return result
    }

    /**
     * Debug method: Get current state information
     */
    fun getDebugInfo(): Map<String, Any?> {
        return mapOf(
            "hasWebView" to (cipherWebView != null),
            "playerHash" to currentPlayerHash,
            "nFunctionAvailable" to cipherWebView?.nFunctionAvailable,
            "sigFunctionAvailable" to cipherWebView?.sigFunctionAvailable,
            "discoveredNFuncName" to cipherWebView?.discoveredNFuncName,
            "usingHardcodedMode" to cipherWebView?.usingHardcodedMode,
        )
    }
}




