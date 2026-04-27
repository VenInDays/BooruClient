package com.booru.client

import android.app.Application
import android.webkit.WebView

/**
 * Application class for BooruClient.
 * Initializes WebView debugging and global configurations.
 */
class BooruApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Enable WebView debugging in debug builds
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
    }
}
