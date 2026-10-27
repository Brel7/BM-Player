package com.bmplayer

import android.app.Application
import com.bmplayer.ui.trimArtworkCache
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class BMPlayerApplication : Application() {
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        trimArtworkCache(level)
    }
}
