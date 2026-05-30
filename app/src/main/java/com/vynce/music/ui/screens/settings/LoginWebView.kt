package com.vynce.music.ui.screens.settings

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
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
    modifier: Modifier = Modifier,
    onLoginSuccess: ((String, String?) -> Unit)? = null,
    onLoginFailed: ((String) -> Unit)? = null
) {

    var isLoading by remember {
        mutableStateOf(true)
    }

    var hasExtracted by remember {
        mutableStateOf(false)
    }

    val loginUrl =
        "https://accounts.google.com/v3/signin/identifier?" +
                "continue=https%3A%2F%2Fmusic.youtube.com%2F" +
                "&service=youtube" +
                "&flowName=GlifWebSignIn" +
                "&flowEntry=ServiceLogin"

    Box(
        modifier = modifier.fillMaxSize()
    ) {

        AndroidView(
            modifier = Modifier.fillMaxSize(),

            factory = { context ->

                createLoginWebView(
                    context = context,

                    loginUrl = loginUrl,

                    onLoadingChanged = {
                        isLoading = it
                    },

                    hasExtracted = {
                        hasExtracted
                    },

                    setExtracted = {
                        hasExtracted = it
                    },

                    onLoginSuccess = { cookie, visitorData ->

                        try {

                            Toast.makeText(
                                context,
                                "Login successful",
                                Toast.LENGTH_SHORT
                            ).show()

                            // 🚀 ONLY EMIT DATA — NO STORAGE HERE
                            onLoginSuccess?.invoke(cookie, visitorData)

                        } catch (e: Exception) {

                            onLoginFailed?.invoke(
                                "Login callback failed"
                            )
                        }
                    },

                    onLoginFailed = onLoginFailed
                )
            }
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

@SuppressLint("SetJavaScriptEnabled")
private fun createLoginWebView(
    context: Context,
    loginUrl: String,
    onLoadingChanged: (Boolean) -> Unit,
    hasExtracted: () -> Boolean,
    setExtracted: (Boolean) -> Unit,
    onLoginSuccess: (String, String?) -> Unit,
    onLoginFailed: ((String) -> Unit)? = null
): WebView {

    return WebView(context).apply {

        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        settings.apply {

            javaScriptEnabled = true

            domStorageEnabled = true
            databaseEnabled = true

            allowFileAccess = true
            allowContentAccess = true

            cacheMode = WebSettings.LOAD_DEFAULT

            loadWithOverviewMode = true
            useWideViewPort = true

            mediaPlaybackRequiresUserGesture = false

            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(true)

            mixedContentMode =
                WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

            userAgentString =
                "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) " +
                        "Chrome/125.0.0.0 Mobile Safari/537.36"
        }

        val cookieManager = CookieManager.getInstance()

        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(this, true)
        cookieManager.flush()

        webChromeClient = WebChromeClient()

        webViewClient = object : WebViewClient() {

            override fun onPageStarted(
                view: WebView?,
                url: String?,
                favicon: Bitmap?
            ) {

                super.onPageStarted(view, url, favicon)

                onLoadingChanged(true)

                Log.d(
                    "LoginWebView",
                    "Page started: $url"
                )
            }

            override fun onPageFinished(
                view: WebView,
                url: String
            ) {

                super.onPageFinished(view, url)

                onLoadingChanged(false)

                Log.d(
                    "LoginWebView",
                    "Page finished: $url"
                )

                if (
                    hasExtracted() ||
                    !url.startsWith("https://music.youtube.com")
                ) {
                    return
                }

                view.postDelayed({

                    try {

                        CookieManager
                            .getInstance()
                            .flush()

                        val cookies =
                            CookieManager
                                .getInstance()
                                .getCookie(
                                    "https://music.youtube.com"
                                )

                        Log.d(
                            "LoginWebView",
                            "Cookies extracted: ${
                                if (cookies != null)
                                    "length=${cookies.length}"
                                else
                                    "null"
                            }"
                        )

                        if (!validateCookies(cookies)) {

                            Log.d(
                                "LoginWebView",
                                "Cookies not valid yet"
                            )

                            return@postDelayed
                        }

                        Log.d(
                            "LoginWebView",
                            "Valid cookies found"
                        )

                        view.evaluateJavascript(
                            """
                            (function() {
                                try {

                                    if (
                                        window.ytcfg &&
                                        ytcfg.get
                                    ) {

                                        return JSON.stringify({

                                            visitorData:
                                                ytcfg.get('VISITOR_DATA'),

                                            loggedIn:
                                                ytcfg.get('LOGGED_IN')
                                        })
                                    }

                                    return null

                                } catch(e) {
                                    return null
                                }
                            })();
                            """.trimIndent()
                        ) { result ->

                            try {

                                Log.d(
                                    "LoginWebView",
                                    "JS result: $result"
                                )

                                val cleanResult =
                                    result
                                        ?.removeSurrounding("\"")
                                        ?.replace("\\\"", "\"")
                                        ?.replace("\\\\", "\\")

                                val visitorData =
                                    Regex(
                                        """"visitorData":"(.*?)""""
                                    )
                                        .find(cleanResult ?: "")
                                        ?.groupValues
                                        ?.getOrNull(1)

                                Log.d(
                                    "LoginWebView",
                                    "visitorData=$visitorData"
                                )

                                setExtracted(true)

                                onLoginSuccess(
                                    cookies!!,
                                    visitorData
                                )

                                CookieManager
                                    .getInstance()
                                    .flush()

                                WebStorage
                                    .getInstance()
                                    .deleteAllData()

                            } catch (e: Exception) {

                                Log.e(
                                    "LoginWebView",
                                    "Failed parsing JS result",
                                    e
                                )

                                onLoginFailed?.invoke(
                                    "Failed to parse login data"
                                )
                            }
                        }

                    } catch (e: Exception) {

                        Log.e(
                            "LoginWebView",
                            "Cookie extraction failed",
                            e
                        )

                        onLoginFailed?.invoke(
                            "Cookie extraction failed"
                        )
                    }

                }, 1500)
            }
        }

        loadUrl(loginUrl)
    }
}

private fun validateCookies(
    cookieString: String?
): Boolean {

    if (cookieString.isNullOrBlank()) {
        return false
    }

    val hasAuth =
        listOf(
            "SAPISID",
            "APISID",
            "__Secure-3PAPISID",
            "__Secure-1PSID",
            "SID"
        ).any {
            cookieString.contains(it)
        }

    Log.d(
        "LoginWebView",
        "Cookie validation: hasAuth=$hasAuth"
    )

    return hasAuth
}