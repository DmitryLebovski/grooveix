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
import android.media.MediaMetadata
import android.media.MediaPlayer
import android.media.MediaPlayer.MEDIA_ERROR_IO
import android.media.session.PlaybackState.STATE_NONE
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.service.media.MediaBrowserService
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.session.PlaybackStateCompat.REPEAT_MODE_ALL
import android.support.v4.media.session.PlaybackStateCompat.REPEAT_MODE_ONE
import android.support.v4.media.session.PlaybackStateCompat.STATE_PLAYING
import android.text.TextUtils
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.media.AudioManagerCompat.AUDIOFOCUS_GAIN
import androidx.media.MediaBrowserServiceCompat
import com.example.grooveix.R
import java.io.File
import java.io.IOException
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException
import java.lang.NullPointerException


const val ACTION_PREVIOUS = "previous"
const val ACTION_PLAYP = "play"
const val ACTION_PAUSEP = "pause"
const val ACTION_NEXT = "next"
class MusicPlayerService : MediaBrowserServiceCompat(), MediaPlayer.OnErrorListener {

    private val channelId = "grooveix music"
    private var currentlyPlayingQueueItemId = -1L
    private val handler = Handler(Looper.getMainLooper())
    private var mediaPlayer: MediaPlayer? = null
    private val playQueue: MutableList<MediaSessionCompat.QueueItem> = mutableListOf()
    private lateinit var afRequest: AudioFocusRequest
    private lateinit var mediaSessionCompat: MediaSessionCompat

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

    private val mediaSessionCallback: MediaSessionCompat.Callback = object : MediaSessionCompat.Callback() {
        override fun onPrepare() {
            super.onPrepare()

            if (playQueue.isEmpty()) {
                onError(mediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0)
                return
            }

            //if nothing in queue right now - set queue from beginning of trackList
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
                setCurrentData()
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

                    afRequest = AudioFocusRequest.Builder(AUDIOFOCUS_GAIN).run {
                        setAudioAttributes(AudioAttributes.Builder().run {
                            setOnAudioFocusChangeListener(afChangeListener)
                            setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            build()
                        })
                        build()
                    }

                    val afRequest = audioManager.requestAudioFocus(afRequest)
                    if (afRequest == AUDIOFOCUS_REQUEST_GRANTED) {
                        startService(Intent(applicationContext, MediaBrowserService::class.java))
                        mediaSessionCompat.isActive = true
                        try {
                            mediaPlayer?.apply {
                                start()
                                setOnCompletionListener {
                                    val inRepeat = mediaSessionCompat.controller.repeatMode
                                    when {
                                        //TODO
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
                            setMediaPlaybackState(STATE_PLAYING)
                        } catch (_: NullPointerException) {
                            onError(mediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0)
                        }
                    }
                }
            } catch (_: IllegalStateException) {
                onError(mediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, MEDIA_ERROR_IO)
            }
        }

    }

    private fun setCurrentData() {
        val currentQueueItem = getCurrentQueueItem() ?: return
        val currentQueueItemDescription = currentQueueItem.description
        val metadataBuilder = MediaMetadataCompat.Builder().apply {
            putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, currentQueueItemDescription.mediaId)
            putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentQueueItemDescription.title.toString())
            putString(MediaMetadataCompat.METADATA_KEY_ARTIST, currentQueueItemDescription.subtitle.toString())

            val additional = currentQueueItemDescription.extras
            putString(MediaMetadataCompat.METADATA_KEY_ALBUM,
                additional?.getString("album") ?: "Unknown album")
            putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, getArtworkByAlbumId(
                additional?.getString("album_id"))
            )
        }
        mediaSessionCompat.setMetadata(metadataBuilder.build())
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
        // if an error or album ID null - return app logo as a Album Art
        return ContextCompat.getDrawable(applicationContext, R.drawable.grooveix)!!.toBitmap()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        intent.action?.let {
            when (it) {
                "play" -> mediaSessionCallback.onPlay()
                "pause" -> mediaSessionCallback.onPause()
                "next" -> mediaSessionCallback.onSkipToNext()
                "previous" -> mediaSessionCallback.onSkipToPrevious()
            }
        }
        return super.onStartCommand(intent, flags, startId)
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

    private var playbackPositionRunnable = object : Runnable {
        override fun run() {
            try {
                if (mediaPlayer?.isPlaying == true) setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING)
            } finally {
                handler.postDelayed(this, 1000L)
            }
        }
    }

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (mediaPlayer != null && mediaPlayer!!.isPlaying) mediaSessionCallback.onPause()
        }
    }

    // for audio focus change
    private val afChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                mediaSessionCompat.controller.transportControls.pause()
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> mediaPlayer?.setVolume(0.3f, 0.3f)
            AudioManager.AUDIOFOCUS_GAIN -> mediaPlayer?.setVolume(1.0f, 1.0f)
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

}