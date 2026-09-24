package com.bmplayer.data.mediastore

import android.database.ContentObserver
import android.net.Uri
import android.os.Handler

class MediaStoreObserver(
    handler: Handler,
    private val onMediaChanged: () -> Unit
) : ContentObserver(handler) {
    override fun onChange(selfChange: Boolean, uri: Uri?) {
        onMediaChanged()
    }
}
