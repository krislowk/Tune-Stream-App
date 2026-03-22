package com.vynce.music

import android.app.Application
import com.gravatar.Gravatar
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class VynceApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Gravatar.apiKey(BuildConfig.GRAVATAR_API_KEY)
            .context(this) // Use 'this' for the application context.
    }
}
