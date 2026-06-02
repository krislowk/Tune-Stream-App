package com.vynce.music

import android.app.Application
import com.vynce.music.utils.cipher.CipherDeobfuscator
import com.vynce.music.BuildConfig
import com.vynce.vynceclient.NewPipeExtractor
import com.vynce.vynceclient.YouTube
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class VynceApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CipherDeobfuscator.initialize(this)
        YouTube.apiKey = BuildConfig.INNERTUBE_API_KEY
        NewPipeExtractor.init(YouTube.proxy, YouTube.proxyAuth)
    }
}















