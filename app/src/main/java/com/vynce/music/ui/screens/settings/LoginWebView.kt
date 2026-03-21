package com.vynce.music.ui.screens.settings

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.vynce.music.ui.theme.VynceTheme

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LoginWebView(
    onCookieSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isLoading by remember { mutableStateOf(true) }
    var hasExtracted by remember { mutableStateOf(false) }
    
    // Login to YouTube Music via Google sign-in
    val loginUrl = "https://accounts.google.com/v3/signin/identifier?continue=https%3A%2F%2Fmusic.youtube.com%2F&service=youtube&flowName=GlifWebSignIn&flowEntry=ServiceLogin"

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        allowFileAccess = true
                        allowContentAccess = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        // A more recent Chrome User Agent to avoid "Insecure Browser" blocks
                        userAgentString = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36"
                    }

                    // Ensure CookieManager is set to accept cookies properly
                    val cookieManager = CookieManager.getInstance()
                    cookieManager.setAcceptCookie(true)
                    cookieManager.setAcceptThirdPartyCookies(this, true)
                    
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            isLoading = true
                        }

                        override fun onPageFinished(view: WebView, url: String) {
                            isLoading = false
                            
                            // Important: Google login redirects to music.youtube.com after success
                            // We check the URL and ensure we only extract the cookies once
                            if (url.startsWith("https://music.youtube.com") && !hasExtracted) {
                                val cookies = CookieManager.getInstance().getCookie(url)
                                if (validateCookies(cookies)) {
                                    hasExtracted = true
                                    onCookieSelected(cookies)
                                }
                            }
                        }
                    }
                    loadUrl(loginUrl)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                color = VynceTheme.colors.primary
            )
        }
    }
}

/**
 * Validates that the cookie string contains the minimum required fields for InnerTube.
 * These are usually SAPISID, HSID, SSID, and SID for authenticated SAPI calls.
 */
private fun validateCookies(cookieString: String?): Boolean {
    if (cookieString.isNullOrBlank()) return false
    
    // Check for the core authentication tokens
    return cookieString.contains("SAPISID") && 
           cookieString.contains("HSID") && 
           cookieString.contains("SSID") &&
           cookieString.contains("SID")
}
