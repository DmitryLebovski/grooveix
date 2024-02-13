package com.example.grooveix.ui.media

import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import com.example.grooveix.ui.activity.MainActivity

class MediaContentObserver(handler: Handler, private val activity: MainActivity): ContentObserver(handler) {

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)

        uri?.let {
            activity.handleChangeToContentUri(uri)
        }
    }
}