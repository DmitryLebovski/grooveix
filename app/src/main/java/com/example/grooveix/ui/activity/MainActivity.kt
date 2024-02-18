package com.example.grooveix.ui.activity

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat.QueueItem
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.session.PlaybackStateCompat.*
import android.util.Size
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.signature.ObjectKey
import com.example.grooveix.R
import com.example.grooveix.databinding.ActivityMainBinding
import com.example.grooveix.ui.fragment.MiniPlayerFragment
import com.example.grooveix.ui.fragment.TrackFragment
import com.example.grooveix.ui.media.MediaContentObserver
import com.example.grooveix.ui.media.MusicPlayerService
import com.example.grooveix.ui.media.MusicViewModel
import com.example.grooveix.ui.media.QueueViewModel
import com.example.grooveix.ui.media.Track
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var currentPlaybackPosition = 0
    private var currentPlaybackDuration = 0
    private var currentQueueItemId = -1L
    private var mediaStoreContentObserver: MediaContentObserver? = null
    private var playQueue = listOf<QueueItem>()
    private val playQueueViewModel: QueueViewModel by viewModels()
    private lateinit var mediaBrowser: MediaBrowserCompat
    private lateinit var musicViewModel: MusicViewModel
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>

    private val playerState: Int
        get() = bottomSheetBehavior.state

    private val connectionCallbacks = object : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            super.onConnected()

            mediaBrowser.sessionToken.also { token ->
                val mediaControllerCompat = MediaControllerCompat(this@MainActivity, token)
                MediaControllerCompat.setMediaController(this@MainActivity, mediaControllerCompat)
            }

            MediaControllerCompat.getMediaController(this@MainActivity)
                .registerCallback(controllerCallback)
        }
    }

    private val controllerCallback = object : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)

            val mediaControllerCompat = MediaControllerCompat.getMediaController(this@MainActivity)
            playQueue = mediaControllerCompat.queue
            playQueueViewModel.playQueue.postValue(playQueue)
            if (state?.activeQueueItemId != currentQueueItemId) {
                currentQueueItemId = state?.activeQueueItemId ?: -1
                playQueueViewModel.currentQueueItemId.postValue(currentQueueItemId)
            }

            playQueueViewModel.playbackState.value = state?.state ?: STATE_NONE
            when (state?.state) {
                STATE_PLAYING, STATE_PAUSED -> {
                    currentPlaybackPosition = state.position.toInt()
                    state.extras?.let {
                        currentPlaybackDuration = it.getInt("duration", 0)
                        playQueueViewModel.playbackDuration.value = currentPlaybackDuration
                    }
                    playQueueViewModel.playbackPosition.value = currentPlaybackPosition
                }
                STATE_STOPPED -> {
                    currentPlaybackDuration = 0
                    playQueueViewModel.playbackDuration.value = 0
                    currentPlaybackPosition = 0
                    playQueueViewModel.playbackPosition.value = 0
                    playQueueViewModel.currentlyPlayingSongMetadata.value = null
                }
                STATE_ERROR -> refreshMusicLibrary()
                else -> return
            }
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)

            if (metadata?.description?.mediaId !=
                playQueueViewModel.currentlyPlayingSongMetadata.value?.description?.mediaId) {
                playQueueViewModel.playbackPosition.value = 0
            }

            playQueueViewModel.currentlyPlayingSongMetadata.value = metadata
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)

        navView.setupWithNavController(navController)

        mediaBrowser = MediaBrowserCompat(
            this,
            ComponentName(this, MusicPlayerService::class.java),
            connectionCallbacks,
            intent.extras
        )

        mediaBrowser.connect()

        createChannelForMediaPlayerNotification()

        musicViewModel = ViewModelProvider(this)[MusicViewModel::class.java]

        val handler = Handler(Looper.getMainLooper())
        mediaStoreContentObserver = MediaContentObserver(handler, this).also {
            this.contentResolver.registerContentObserver(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                true, it)
        }

        refreshMusicLibrary()


//        if (!hasStoragePermission()) {
//            Intent(this@MainActivity, PermissionActivity::class.java).also {
//                startActivity(it)
//                this@MainActivity.finish()
//            }
//        }
    }



    override fun onResume() {
        super.onResume()
        volumeControlStream = AudioManager.STREAM_MUSIC
    }

    override fun onDestroy() {
        super.onDestroy()

        MediaControllerCompat.getMediaController(this)?.apply {
            transportControls.stop()
            unregisterCallback(controllerCallback)
        }
        mediaBrowser.disconnect()

        mediaStoreContentObserver?.let {
            this.contentResolver.unregisterContentObserver(it)
        }
    }

    private fun createChannelForMediaPlayerNotification() {
        val channel = NotificationChannel(
            "music", "Notifications",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "All app notifications"
            setSound(null, null)
            setShowBadge(false)
        }
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }


    fun playNewPlayQueue(songs: List<Track>, startIndex: Int = 0, shuffle: Boolean = false)
            = lifecycleScope.launch(Dispatchers.Default) {
        if (songs.isEmpty() || startIndex >= songs.size) {
            Toast.makeText(this@MainActivity, getString(R.string.error), Toast.LENGTH_LONG).show()
            return@launch
        }

        mediaController.transportControls.stop()

        val startSongIndex = if (shuffle) (songs.indices).random()
        else startIndex

        val startSongDesc = buildMediaDescription(songs[startSongIndex], startSongIndex.toLong())

        val mediaControllerCompat = MediaControllerCompat.getMediaController(this@MainActivity)
        mediaControllerCompat.addQueueItem(startSongDesc)
        skipToAndPlayQueueItem(startSongIndex.toLong())

        for ((index, song) in songs.withIndex()) {
            if (index == startSongIndex) continue
            val songDesc = buildMediaDescription(song, index.toLong())
            mediaControllerCompat.addQueueItem(songDesc, index)
        }

        when {
            shuffle -> setShuffleMode(SHUFFLE_MODE_ALL)
            mediaControllerCompat.shuffleMode == SHUFFLE_MODE_ALL -> setShuffleMode(
                SHUFFLE_MODE_NONE
            )
        }
    }

    private fun buildMediaDescription(song: Track, queueId: Long? = null): MediaDescriptionCompat {
        val extrasBundle = Bundle().apply {
            putString("album", song.album)
            putString("album_id", song.albumId)
            queueId?.let {
                putLong("queue_id", queueId)
            }
        }

        return MediaDescriptionCompat.Builder()
            .setExtras(extrasBundle)
            .setMediaId(song.trackId.toString())
            .setSubtitle(song.artist)
            .setTitle(song.title)
            .build()
    }

    fun skipToAndPlayQueueItem(queueItemId: Long) {
        mediaController.transportControls.skipToQueueItem(queueItemId)
        mediaController.transportControls.play()
    }

    private fun setShuffleMode(shuffleMode: Int) {
        val bundle = Bundle().apply {
            putInt("SHUFFLE_MODE", shuffleMode)
        }

        mediaController.sendCommand("SET_SHUFFLE_MODE", bundle, null)
    }

    fun loadArtwork(albumId: String?, view: ImageView) {
        var file: File? = null
        if (albumId != null) {
            val directory = ContextWrapper(application).getDir("albumArt", Context.MODE_PRIVATE)
            file = File(directory, "$albumId.jpg")
        }

        Glide.with(application)
            .load(file)
            .error(R.drawable.grooveix)
            .transition(DrawableTransitionOptions.withCrossFade())
            .centerCrop()
            .signature(ObjectKey(file?.path + file?.lastModified()))
            .override(600, 600)
            .into(view)
    }

    fun showSongPopup(view: View, track: Track) {
        //TODO BOTTOM MENU
    }

    private fun saveImage(albumId: String, image: Bitmap) {
        val directory = ContextWrapper(application).getDir("albumArt", Context.MODE_PRIVATE)
        val path = File(directory, "$albumId.jpg")

        FileOutputStream(path).use {
            image.compress(Bitmap.CompressFormat.JPEG, 100, it)
        }
    }

    fun notifyQueueItemMoved(queueId: Long, newIndex: Int) {
        val bundle = Bundle().apply {
            putLong("queueItemId", queueId)
            putInt("newIndex", newIndex)
        }

        mediaController.sendCommand("MOVE_QUEUE_ITEM", bundle, null)
    }

    fun removeQueueItemById(queueId: Long) {
        if (playQueue.isNotEmpty()) {
            val bundle = Bundle().apply {
                putLong("queueItemId", queueId)
            }

            mediaController.sendCommand("REMOVE_QUEUE_ITEM", bundle, null)
        }
    }

    private fun playNext(song: Track) {
        val index = playQueue.indexOfFirst { it.queueId == currentQueueItemId } + 1

        val songDesc = buildMediaDescription(song)
        val mediaControllerCompat = MediaControllerCompat.getMediaController(this@MainActivity)
        mediaControllerCompat.addQueueItem(songDesc, index)

        Toast.makeText(this, getString(R.string.added_to_queue, song.title), Toast.LENGTH_SHORT).show()
    }

    fun hideKeyboard() {
        this.currentFocus?.let {
            val inputManager = this.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputManager.hideSoftInputFromWindow(it.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
        }
    }

    fun playPauseControl() {
        when (mediaController.playbackState?.state) {
            PlaybackState.STATE_PAUSED -> mediaController.transportControls.play()
            PlaybackState.STATE_PLAYING -> mediaController.transportControls.pause()
            else -> {
                // Load and play the user's music library if the play queue is empty
                if (playQueue.isEmpty()) {
                    playNewPlayQueue(musicViewModel.loadTracks.value ?: return)
                }
                else {
                    // It's possible a queue has been built without ever pressing play.
                    // In which case, commence playback
                    mediaController.transportControls.prepare()
                    mediaController.transportControls.play()
                }
            }
        }
    }

    fun skipBack() = mediaController.transportControls.skipToPrevious()

    fun skipForward() = mediaController.transportControls.skipToNext()

    fun fastRewind() = mediaController.transportControls.rewind()

    fun fastForward() = mediaController.transportControls.fastForward()

    fun getShuffleMode(): Int {
        val mediaControllerCompat = MediaControllerCompat.getMediaController(this@MainActivity)
        return mediaControllerCompat.shuffleMode
    }
    fun getRepeatMode(): Int {
        val mediaControllerCompat = MediaControllerCompat.getMediaController(this@MainActivity)
        return mediaControllerCompat.repeatMode
    }
    fun toggleShuffleMode(): Boolean {
        val newShuffleMode = if (getShuffleMode() == PlaybackStateCompat.SHUFFLE_MODE_NONE) {
            PlaybackStateCompat.SHUFFLE_MODE_ALL
        } else PlaybackStateCompat.SHUFFLE_MODE_NONE

        setShuffleMode(newShuffleMode)

        return newShuffleMode == PlaybackStateCompat.SHUFFLE_MODE_ALL
    }

    fun toggleRepeatMode(): Int {
        val newRepeatMode = when (getRepeatMode()) {
            PlaybackStateCompat.REPEAT_MODE_NONE -> PlaybackStateCompat.REPEAT_MODE_ALL
            PlaybackStateCompat.REPEAT_MODE_ALL -> PlaybackStateCompat.REPEAT_MODE_ONE
            else -> PlaybackStateCompat.REPEAT_MODE_NONE
        }

        val bundle = Bundle().apply {
            putInt("REPEAT_MODE", newRepeatMode)
        }
        mediaController.sendCommand("SET_REPEAT_MODE", bundle, null)

        return newRepeatMode
    }

    fun seekTo(position: Int) = mediaController.transportControls.seekTo(position.toLong())

    private fun getMediaStoreCursor(selection: String = MediaStore.Audio.Media.IS_MUSIC,
                                    selectionArgs: Array<String>? = null): Cursor? {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID
        )
        val sortOrder = MediaStore.Audio.Media.TITLE + " ASC"
        return contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )
    }

    private fun refreshMusicLibrary() = lifecycleScope.launch(Dispatchers.Default) {
        getMediaStoreCursor()?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val songIds = mutableListOf<Long>()
            while (cursor.moveToNext()) {
                val songId = cursor.getLong(idColumn)
                songIds.add(songId)
                val existingSong = musicViewModel.getTrackById(songId)
                if (existingSong == null) {
                    val song = createSongFromCursor(cursor)
                    musicViewModel.insertTrack(song)
                }
            }

            val songsToBeDeleted = musicViewModel.loadTracks.value?.filterNot {
                songIds.contains(it.trackId)
            }
            songsToBeDeleted?.let { songs ->
                for (song in songs) {
                    musicViewModel.deleteTrack(song)
                    findSongIdInPlayQueueToRemove(song.trackId)
                }
            }
        }
    }

    private fun createSongFromCursor(cursor: Cursor): Track {
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val trackColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
        val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
        val albumIDColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

        val id = cursor.getLong(idColumn)
        var trackString = cursor.getString(trackColumn) ?: "1001"

        val track = try {
            when (trackString.length) {
                4 -> trackString.toInt()
                in 1..3 -> {
                    val numberNeeded = 4 - trackString.length
                    trackString = when (numberNeeded) {
                        1 -> "1$trackString"
                        2 -> "10$trackString"
                        else -> "100$trackString"
                    }
                    trackString.toInt()
                }
                else -> 1001
            }
        } catch (_: NumberFormatException) {
            // If the Track value is unusual (e.g. you can get stuff like "12/23") then use 1001
            1001
        }

        val title = cursor.getString(titleColumn) ?: "Unknown song"
        val artist = cursor.getString(artistColumn) ?: "Unknown artist"
        val album = cursor.getString(albumColumn) ?: "Unknown album"
        val albumId = cursor.getString(albumIDColumn) ?: "unknown_album_id"
        val lyrics = "No lyrics for this track"
        val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

        val directory = ContextWrapper(application).getDir("albumArt", Context.MODE_PRIVATE)
        if (!File(directory, "$albumId.jpg").exists()) {
            val albumArt = try {
                contentResolver.loadThumbnail(uri, Size(640, 640), null)
            } catch (_: FileNotFoundException) { null }
            albumArt?.let {
                saveImage(albumId, albumArt)
            }
        }

        return Track(id, track, title, artist, album, albumId)
    }

    private fun findSongIdInPlayQueueToRemove(songId: Long) = lifecycleScope.launch(Dispatchers.Default) {
        val queueItemsToRemove = playQueue.filter { it.description.mediaId == songId.toString() }
        for (item in queueItemsToRemove) removeQueueItemById(item.queueId)
    }

    fun handleChangeToContentUri(uri: Uri) = lifecycleScope.launch(Dispatchers.IO) {
        val songIdString = uri.toString().removePrefix(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString() + "/")
        try {
            val selection = MediaStore.Audio.Media._ID + "=?"
            val selectionArgs = arrayOf(songIdString)
            val cursor = getMediaStoreCursor(selection, selectionArgs)

            val songId = songIdString.toLong()
            val existingSong = musicViewModel.getTrackById(songId)

            when {
                existingSong == null && cursor?.count!! > 0 -> {
                    cursor.apply {
                        this.moveToNext()
                        val createdSong = createSongFromCursor(this)
                        musicViewModel.insertTrack(createdSong)
                    }
                }
                cursor?.count == 0 -> {
                    existingSong?.let {
                        musicViewModel.deleteTrack(existingSong)
                        findSongIdInPlayQueueToRemove(songId)
                    }
                }
            }
        } catch (_: NumberFormatException) { refreshMusicLibrary() }
    }
}