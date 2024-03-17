package com.example.grooveix.ui.media

import android.content.BroadcastReceiver
import android.content.ContentUris
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioManager.AUDIOFOCUS_REQUEST_GRANTED
import android.media.MediaPlayer
import android.media.MediaPlayer.MEDIA_ERROR_IO
import android.media.session.PlaybackState.STATE_NONE
import android.media.session.PlaybackState.STATE_PAUSED
import android.media.session.PlaybackState.STATE_SKIPPING_TO_NEXT
import android.media.session.PlaybackState.STATE_STOPPED
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
import android.provider.MediaStore
import android.service.media.MediaBrowserService
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.session.PlaybackStateCompat.REPEAT_MODE_ALL
import android.support.v4.media.session.PlaybackStateCompat.REPEAT_MODE_ONE
import android.support.v4.media.session.PlaybackStateCompat.STATE_PLAYING
import android.text.TextUtils
import android.view.KeyEvent
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.media.AudioManagerCompat.AUDIOFOCUS_GAIN
import androidx.media.MediaBrowserServiceCompat
import com.example.grooveix.R
import java.io.File
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException
import java.lang.NullPointerException

const val ACTION_PREVIOUS = "action previous"
const val ACTION_PLAY = "action play"
const val ACTION_NEXT = "action next"
const val ACTION_PAUSE = "action pause"

class MusicPlayerService : MediaBrowserServiceCompat(), MediaPlayer.OnErrorListener {

    private val channelId = "grooveix music"
    private var currentlyPlayingQueueItemId = -1L
    private val handler = Handler(Looper.getMainLooper())
    private var mediaPlayer: MediaPlayer? = null
    private val playQueue: MutableList<MediaSessionCompat.QueueItem> = mutableListOf()
    private lateinit var audioFocusRequest: AudioFocusRequest
    private lateinit var mediaSessionCompat: MediaSessionCompat

    private val afChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                mediaSessionCompat.controller.transportControls.pause()
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> mediaPlayer?.setVolume(0.3f, 0.3f)
            AudioManager.AUDIOFOCUS_GAIN -> mediaPlayer?.setVolume(1.0f, 1.0f)
        }
    }

    private var playbackPositionRunnable = object : Runnable {
        override fun run() {
            try {
                if (mediaPlayer?.isPlaying == true) setMediaPlaybackState(STATE_PLAYING)
            } finally {
                handler.postDelayed(this, 1000L)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        mediaSessionCompat = MediaSessionCompat(baseContext, channelId).apply {
            setFlags(MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS)
            setCallback(mediaSessionCallback)
            setSessionToken(sessionToken)
            val builder = PlaybackStateCompat.Builder().setActions(PlaybackStateCompat.ACTION_PLAY)
            setPlaybackState(builder.build())
        }

        val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        registerReceiver(noisyReceiver, filter)
        playbackPositionRunnable.run()
    }

    val mediaSessionCallback: MediaSessionCompat.Callback = object : MediaSessionCompat.Callback() {
        override fun onPrepare() {
            super.onPrepare()

            if (playQueue.isEmpty()) {
                onError(mediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0)
                return
            }

            if (currentlyPlayingQueueItemId == -1L) currentlyPlayingQueueItemId = playQueue[0].queueId

            mediaPlayer?.apply {
                stop()
                release()
            }

            try {
                val currentQueueItem = getCurrentQueueItem()
                val currentQueueItemUri = currentQueueItem?.description?.mediaId?.let {
                    ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        it.toLong())
                }

                if (currentQueueItem == null) {
                    onError(mediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN,
                        MediaPlayer.MEDIA_ERROR_MALFORMED
                    )
                    return
                }

                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )

                    //currentQueueItemUri cant be null here
                    setDataSource(application, currentQueueItemUri!!)
                    setOnErrorListener(this@MusicPlayerService)
                    prepare()
                }
                setCurrentMetadata()
                setMediaPlaybackState(STATE_NONE)
            } catch (_: IllegalStateException) {
                onError(mediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, MEDIA_ERROR_IO)
            } catch (_: IllegalArgumentException) {
                onError(mediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, MEDIA_ERROR_IO)
            }
        }

        override fun onPlay() {
            super.onPlay()

            try {
                if (mediaPlayer != null && !mediaPlayer!!.isPlaying) {
                    val audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE)
                            as AudioManager

                    audioFocusRequest = AudioFocusRequest.Builder(AUDIOFOCUS_GAIN).run {
                        setAudioAttributes(AudioAttributes.Builder().run {
                            setOnAudioFocusChangeListener(afChangeListener)
                            setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            build()
                        })
                        build()
                    }

                    val audioFocusRequestOutcome = audioManager.requestAudioFocus(audioFocusRequest)
                    if (audioFocusRequestOutcome == AUDIOFOCUS_REQUEST_GRANTED) {
                        startService(Intent(applicationContext, MediaBrowserService::class.java))
                        mediaSessionCompat.isActive = true
                        try {
                            mediaPlayer?.apply {
                                start()
                                setOnCompletionListener {
                                    val inRepeat = mediaSessionCompat.controller.repeatMode
                                    when {
                                        inRepeat == REPEAT_MODE_ONE -> {}
                                        inRepeat == REPEAT_MODE_ALL ||
                                                playQueue.isNotEmpty() &&
                                                playQueue[playQueue.size - 1].queueId != currentlyPlayingQueueItemId -> {
                                            onSkipToNext()
                                            return@setOnCompletionListener
                                        }

                                        else -> {
                                            onStop()
                                            return@setOnCompletionListener
                                        }
                                    }

                                    onPrepare()
                                    onPlay()
                                }
                            }
                            //TODO update Notification Player
                            setMediaPlaybackState(STATE_PLAYING, getBundleWithSongDuration())
                        } catch (_: NullPointerException) {
                            onError(mediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0)
                        }
                    }
                }
            } catch (_: IllegalStateException) {
                onError(mediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, MEDIA_ERROR_IO)
            }
        }

        override fun onPause() {
            super.onPause()
            mediaPlayer?.pause()
            setMediaPlaybackState(STATE_PAUSED, getBundleWithSongDuration())
            //TODO: update Notification Player
        }

        override fun onSkipToQueueItem(id: Long) {
            super.onSkipToQueueItem(id)

            if (playQueue.find { it.queueId == id } != null) {
                val playState = mediaSessionCompat.controller.playbackState.state
                currentlyPlayingQueueItemId = id
                onPrepare()
                if (playState == STATE_PLAYING || playState == STATE_SKIPPING_TO_NEXT) {
                    onPlay()
                }
            }
        }

        override fun onSkipToNext() {
            super.onSkipToNext()

            val repeatMode = mediaSessionCompat.controller.repeatMode
            currentlyPlayingQueueItemId = when {
                playQueue[playQueue.size - 1].queueId != currentlyPlayingQueueItemId -> {
                    val indexOfCurrentQueueItem = playQueue.indexOfFirst {
                        it.queueId == currentlyPlayingQueueItemId
                    }
                    playQueue[indexOfCurrentQueueItem + 1].queueId
                }
                repeatMode == REPEAT_MODE_ALL -> playQueue[0].queueId
                else -> return
            }
            onSkipToQueueItem(currentlyPlayingQueueItemId)
        }

        override fun onSkipToPrevious() {
            super.onSkipToPrevious()

            if (playQueue.isNotEmpty()) {
                if (mediaPlayer != null && mediaPlayer!!.currentPosition > 5000 ||
                    currentlyPlayingQueueItemId == playQueue[0].queueId) onSeekTo(0L)
            } else {
                val indexOfCurrentQueueItem = playQueue.indexOfFirst {
                    it.queueId == currentlyPlayingQueueItemId
                }
                currentlyPlayingQueueItemId = playQueue[indexOfCurrentQueueItem - 1].queueId
                onSkipToQueueItem(currentlyPlayingQueueItemId)
            }
        }

        override fun onStop() {
            super.onStop()

            playQueue.clear()
            mediaSessionCompat.setQueue(playQueue)
            currentlyPlayingQueueItemId = -1L
            if (mediaPlayer != null) {
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null
                stopForeground(STOP_FOREGROUND_REMOVE)

                val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                audioManager.abandonAudioFocusRequest(audioFocusRequest)
            }
            setMediaPlaybackState(STATE_STOPPED)
            stopSelf()
        }

        override fun onSeekTo(pos: Long) {
            super.onSeekTo(pos)

            mediaPlayer?.apply {
                if (pos > this.duration.toLong()) return@apply

                val ifWasPlayed = this.isPlaying
                if (ifWasPlayed) this.pause()

                this.seekTo(pos.toInt())

                if (ifWasPlayed) {
                    this.start()
                    setMediaPlaybackState(STATE_PLAYING, getBundleWithSongDuration())
                } else setMediaPlaybackState(STATE_PAUSED, getBundleWithSongDuration())
            }
        }

        override fun onAddQueueItem(description: MediaDescriptionCompat?) {
            onAddQueueItem(description, playQueue.size)
        }

        override fun onAddQueueItem(description: MediaDescriptionCompat?, index: Int) {
            super.onAddQueueItem(description, index)

            val sortedQueue = playQueue.sortedByDescending {
                it.queueId
            }
            val presetQueueId = description?.extras?.getLong("queue_id")
            val queueId = when {
                presetQueueId != null && sortedQueue.find { it.queueId == presetQueueId } == null -> {
                    presetQueueId
                }
                sortedQueue.isNotEmpty() -> sortedQueue[0].queueId + 1
                else -> 0
            }

            val queueItem = MediaSessionCompat.QueueItem(description, queueId)
            try {
                playQueue.add(index, queueItem)
            } catch (exception: IndexOutOfBoundsException) {
                playQueue.add(playQueue.size, queueItem)
            }

            mediaSessionCompat.setQueue(playQueue)
        }
        override fun onFastForward() {
            super.onFastForward()

            val newPlaybackPosition = mediaPlayer?.currentPosition?.plus(5000) ?: return
            if (newPlaybackPosition > (mediaPlayer?.duration ?: return)) onSkipToNext()
            else onSeekTo(newPlaybackPosition.toLong())
        }

        override fun onRewind() {
            super.onRewind()

            val newPlaybackPosition = mediaPlayer?.currentPosition?.minus(5000) ?: return
            if (newPlaybackPosition < 0) onSkipToPrevious()
            else onSeekTo(newPlaybackPosition.toLong())
        }

        override fun onCommand(command: String?, extras: Bundle?, cb: ResultReceiver?) {
            super.onCommand(command, extras, cb)

            when (command) {

                "MOVE_QUEUE_ITEM" -> {
                    extras?.let {
                        val queueItemId = it.getLong("queueItemId", -1L)
                        val newIndex = it.getInt("newIndex", -1)
                        if (queueItemId == -1L || newIndex == -1 || newIndex >= playQueue.size) return@let

                        val oldIndex = playQueue.indexOfFirst { queueItem -> queueItem.queueId == queueItemId }
                        if (oldIndex == -1) return@let
                        val queueItem = playQueue[oldIndex]
                        playQueue.removeAt(oldIndex)
                        playQueue.add(newIndex, queueItem)
                        mediaSessionCompat.setQueue(playQueue)
                    }
                }

                "REMOVE_QUEUE_ITEM" -> {
                    extras?.let {
                        val queueItemId = extras.getLong("queueItemId", -1L)
                        when (queueItemId) {
                            -1L -> return@let
                            currentlyPlayingQueueItemId -> onSkipToNext()
                        }
                        playQueue.removeIf { it.queueId == queueItemId }
                        setPlayQueue()
                    }
                }

                "SET_REPEAT_MODE" -> {
                    extras?.let {
                        val repeatMode = extras.getInt("REPEAT_MODE",
                            PlaybackStateCompat.REPEAT_MODE_NONE
                        )
                        mediaSessionCompat.setRepeatMode(repeatMode)
                    }
                }

                "SET_SHUFFLE_MODE" -> {
                    extras?.let {
                        val shuffleMode = extras.getInt("SHUFFLE_MODE",
                            PlaybackStateCompat.SHUFFLE_MODE_NONE
                        )
                        mediaSessionCompat.setShuffleMode(shuffleMode)

                        if (shuffleMode == PlaybackStateCompat.SHUFFLE_MODE_ALL) {
                            getCurrentQueueItem()?.let { currentQueueItem ->
                                playQueue.remove(currentQueueItem)
                                playQueue.shuffle()
                                playQueue.add(0, currentQueueItem)
                            }
                        } else {
                            playQueue.sortBy { it.queueId }
                        }
//
//                        val adapter = PlayQueueAdapter(activity = MainActivity(), fragment = PlayQueueFragment())
//                        adapter.playQueue.clear()
//                        adapter.playQueue.addAll(playQueue)
//                        adapter.notifyDataSetChanged()

                        setPlayQueue()
                    }
                }

                "UPDATE_QUEUE_ITEM" -> {
                    extras?.let {
                        val queueItemId = it.getLong("queue_id")

                        val index = playQueue.indexOfFirst { item ->
                            item.queueId == queueItemId
                        }

                        if (index == -1) return

                        val extrasBundle = Bundle().apply {
                            putString("album", it.getString("album"))
                            putString("album_id", it.getString("album_id"))
                        }

                        val mediaDescription = MediaDescriptionCompat.Builder()
                            .setExtras(extrasBundle)
                            .setMediaId(playQueue[index].description.mediaId)
                            .setSubtitle(it.getString("artist"))
                            .setTitle(it.getString("title"))
                            .build()

                        playQueue.removeAt(index)
                        val updatedQueueItem =
                            MediaSessionCompat.QueueItem(mediaDescription, queueItemId)
                        playQueue.add(index, updatedQueueItem)

                        if (queueItemId == currentlyPlayingQueueItemId) {
                            setCurrentMetadata()
                            //TODO
                            //refreshNotification()
                        }
                        setPlayQueue()
                    }
                }
            }
        }

        //TODO: FIX DEPRECATION
        override fun onMediaButtonEvent(mediaButtonEvent: Intent?): Boolean {
            val key: KeyEvent? = mediaButtonEvent?.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
            if (key != null && mediaPlayer != null) {
                when(key.keyCode) {
                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                        if (mediaPlayer!!.isPlaying) onPause()
                        else onPlay()
                    }
                    KeyEvent.KEYCODE_MEDIA_PLAY -> onPlay()
                    KeyEvent.KEYCODE_MEDIA_PAUSE -> onPause()
                    KeyEvent.KEYCODE_MEDIA_NEXT -> onSkipToNext()
                    KeyEvent.KEYCODE_MEDIA_PREVIOUS-> onSkipToPrevious()
                }
            }
            return super.onMediaButtonEvent(mediaButtonEvent)
        }
    }

    private fun setCurrentMetadata() {
        val currentQueueItem = getCurrentQueueItem() ?: return
        val currentQueueItemDescription = currentQueueItem.description
        val metadataBuilder= MediaMetadataCompat.Builder().apply {
            putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, currentQueueItemDescription.mediaId)
            putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentQueueItemDescription.title.toString())
            putString(MediaMetadataCompat.METADATA_KEY_ARTIST, currentQueueItemDescription.subtitle.toString())
            val extras = currentQueueItemDescription.extras
            val albumName = extras?.getString("album") ?: getString(R.string.unknown_album)
            putString(MediaMetadataCompat.METADATA_KEY_ALBUM, albumName)
            val albumId = extras?.getString("album_id")
            putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, getArtworkByAlbumId(albumId))
            putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, albumId)
        }
        mediaSessionCompat.setMetadata(metadataBuilder.build())
    }

    private fun setPlayQueue() {
        mediaSessionCompat.setQueue(playQueue)
        setMediaPlaybackState(mediaSessionCompat.controller.playbackState.state)
    }

    private fun getArtworkByAlbumId(albumId: String?): Bitmap {
        albumId?.let {
            try {
                val directory = ContextWrapper(applicationContext).getDir("albumArt", Context.MODE_PRIVATE)
                val imageFile = File(directory, "$albumId.jpg")
                if (imageFile.exists()) {
                    return ContextCompat.getDrawable(applicationContext, R.drawable.grooveix)!!.toBitmap()
                }
            } catch (_: Exception) { }
        }

        return ContextCompat.getDrawable(applicationContext, R.drawable.grooveix)!!.toBitmap()
    }

    private fun setMediaPlaybackState(state: Int, bundle: Bundle? = null) {
        val playbackPosition = mediaPlayer?.currentPosition?.toLong() ?: 0L
        val playbackSpeed = mediaPlayer?.playbackParams?.speed ?: 0f
        val playbackStateBuilder = PlaybackStateCompat.Builder()
            .setState(state, playbackPosition, playbackSpeed)
            .setActiveQueueItemId(currentlyPlayingQueueItemId)
        bundle?.let { playbackStateBuilder.setExtras(it) }
        mediaSessionCompat.setPlaybackState(playbackStateBuilder.build())
    }

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (mediaPlayer != null && mediaPlayer!!.isPlaying) mediaSessionCallback.onPause()
        }
    }

    private fun getBundleWithSongDuration(): Bundle {
        val playbackDuration = mediaPlayer?.duration ?: 0
        return Bundle().apply {
            putInt("duration", playbackDuration)
        }
    }

    private fun getCurrentQueueItem(): MediaSessionCompat.QueueItem? {
        return playQueue.find {
            it.queueId == currentlyPlayingQueueItemId
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaSessionCompat.controller.transportControls.stop()
        handler.removeCallbacks(playbackPositionRunnable)
        unregisterReceiver(noisyReceiver)
        mediaSessionCompat.release()
        NotificationManagerCompat.from(this).cancel(1)
    }


    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        return if (TextUtils.equals(clientPackageName, packageName)) {
            BrowserRoot(getString(R.string.app_name), null)
        } else null
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        result.sendResult(null)
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        setMediaPlaybackState(PlaybackStateCompat.STATE_ERROR)
        mediaSessionCompat.controller.transportControls.stop()
        stopForeground(STOP_FOREGROUND_REMOVE)
        Toast.makeText(application, getString(R.string.error), Toast.LENGTH_LONG).show()
        return true
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        intent.action?.let {
            when (it) {
                ACTION_PLAY -> mediaSessionCallback.onPlay()
                ACTION_PAUSE -> mediaSessionCallback.onPause()
                ACTION_NEXT -> mediaSessionCallback.onSkipToNext()
                ACTION_PREVIOUS -> mediaSessionCallback.onSkipToPrevious()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }
}