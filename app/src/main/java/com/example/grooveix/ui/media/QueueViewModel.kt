package com.example.grooveix.ui.media

import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class QueueViewModel : ViewModel() {
    var playQueue = MutableLiveData<List<MediaSessionCompat.QueueItem>>()
    var currentQueueItemId = MutableLiveData<Long>()
    var currentlyPlayingSongMetadata = MutableLiveData<MediaMetadataCompat?>()
    var playbackDuration = MutableLiveData<Int>()
    var playbackPosition = MutableLiveData<Int>()
    var playbackState = MutableLiveData(PlaybackStateCompat.STATE_NONE)
}
