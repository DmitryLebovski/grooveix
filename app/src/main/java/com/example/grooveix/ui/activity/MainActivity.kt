package com.example.grooveix.ui.activity

import android.animation.ObjectAnimator
import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
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
import android.support.v4.media.session.PlaybackStateCompat.REPEAT_MODE_ALL
import android.support.v4.media.session.PlaybackStateCompat.REPEAT_MODE_NONE
import android.support.v4.media.session.PlaybackStateCompat.REPEAT_MODE_ONE
import android.support.v4.media.session.PlaybackStateCompat.SHUFFLE_MODE_ALL
import android.support.v4.media.session.PlaybackStateCompat.SHUFFLE_MODE_NONE
import android.support.v4.media.session.PlaybackStateCompat.STATE_ERROR
import android.support.v4.media.session.PlaybackStateCompat.STATE_NONE
import android.support.v4.media.session.PlaybackStateCompat.STATE_PAUSED
import android.support.v4.media.session.PlaybackStateCompat.STATE_PLAYING
import android.support.v4.media.session.PlaybackStateCompat.STATE_STOPPED
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.signature.ObjectKey
import com.example.grooveix.MobileNavigationDirections
import com.example.grooveix.R
import com.example.grooveix.databinding.ActivityMainBinding
import com.example.grooveix.ui.adapter.LitePlaylistAdapter
import com.example.grooveix.ui.fragment.PlayerFragment
import com.example.grooveix.ui.media.MediaContentObserver
import com.example.grooveix.ui.media.MusicPlayerService
import com.example.grooveix.ui.media.MusicViewModel
import com.example.grooveix.ui.media.QueueViewModel
import com.example.grooveix.ui.media.entity.Track
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
    private lateinit var bottomSheetDialog: BottomSheetDialog
    private lateinit var playerFragment: PlayerFragment
    var bottomViewHeight = 0

    private val panelState: Int
        get() = bottomSheetBehavior.state

    private val bottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            when (newState) {
                BottomSheetBehavior.STATE_COLLAPSED -> {
                    binding.dimBackground.visibility = View.GONE
                }
                else -> {}
            }
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            setMiniPlayerAlpha(slideOffset)
            setBottomNavigationViewTransition(slideOffset)
            binding.dimBackground.visibility = View.VISIBLE
            binding.dimBackground.alpha = slideOffset
            playerFragment.closeWebView()
        }
    }

    private fun setMiniPlayerAlpha(slideOffset: Float) {
        val alpha = 1 - slideOffset
        binding.miniPlayerFragment.alpha = alpha
        binding.miniPlayerFragment.visibility = if (alpha == 0f) View.GONE else View.VISIBLE
    }

    private fun setBottomNavigationViewTransition(slideOffset: Float) {
        binding.navView.translationY = slideOffset * 500
    }

    private fun setUpBottomNavigationNavController() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        val navController = navHostFragment.navController
        binding.navView.setupWithNavController(navController)
    }

    fun collapsePanel() {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        setMiniPlayerAlpha(0f)
    }

    fun expandPanel() {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        setMiniPlayerAlpha(1f)
    }

    fun hideBar(hide: Boolean) {
        if (hide) {
            hideBottomSheet(binding.navView)
            hideBottomSheet(binding.slidingPanel)
        }
        else {
            showBottomSheet(binding.slidingPanel)
        }
    }

    fun showBar() {
        showBottomSheet(binding.navView)
        showBottomSheet(binding.slidingPanel)
    }

    fun hidePanel() {
        binding.slidingPanel.isGone = true
        hideBottomSheet(binding.slidingPanel)
    }

    fun showPanel() {
        binding.slidingPanel.isVisible = true
    }

    private fun hideBottomSheet(view: View) {
        setMainViewMargins(0)
        val height = view.height
        ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, 0f, height.toFloat()).apply {
            duration = 700
            start()
        }
    }

    private fun showBottomSheet(view: View) {

        setMainViewMargins(bottomViewHeight)
        val height = view.height
        ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, height.toFloat(), 0f).apply {
            duration = 300
            start()
        }
    }

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
                    playQueueViewModel.currentlyPlayingTrackMetadata.value = null
                }
                STATE_ERROR -> refreshMusicLibrary()
                else -> return
            }
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)

            if (metadata?.description?.mediaId !=
                playQueueViewModel.currentlyPlayingTrackMetadata.value?.description?.mediaId) {
                playQueueViewModel.playbackPosition.value = 0
            }

            playQueueViewModel.currentlyPlayingTrackMetadata.value = metadata
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.setFlags(
            FLAG_LAYOUT_NO_LIMITS,
            FLAG_LAYOUT_NO_LIMITS
        )

        mediaBrowser = MediaBrowserCompat(
            this,
            ComponentName(this, MusicPlayerService::class.java),
            connectionCallbacks,
            intent.extras
        )

        mediaBrowser.connect()
        musicViewModel = ViewModelProvider(this)[MusicViewModel::class.java]

        val handler = Handler(Looper.getMainLooper())
        mediaStoreContentObserver = MediaContentObserver(handler, this).also {
            this.contentResolver.registerContentObserver(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                true, it)
        }
        refreshMusicLibrary()

        setUpBottomNavigationNavController()

        bottomSheetBehavior = BottomSheetBehavior.from(binding.slidingPanel)
        bottomSheetBehavior.addBottomSheetCallback(bottomSheetCallback)

        binding.navView.post {
            val navViewHeight = binding.navView.height
            bottomSheetBehavior.peekHeight = navViewHeight + 200
            setMargins(binding.navHostFragmentActivityMain, 0, 0,0, navViewHeight)
            bottomViewHeight = bottomSheetBehavior.peekHeight
        }


        binding.miniPlayerFragment.setOnClickListener {
            expandPanel()
        }

        bottomSheetDialog = BottomSheetDialog(this)
        playerFragment = supportFragmentManager.findFragmentById(R.id.playerFragmentConatiner) as PlayerFragment
    }

    fun setMargins(view: View, left: Int, top: Int, right: Int, bottom: Int) {
        if (view.layoutParams is MarginLayoutParams) {
            val p = view.layoutParams as MarginLayoutParams
            p.setMargins(left, top, right, bottom)
            view.requestLayout()
        }
    }

    override fun onBackPressed() {
        if (!handleBackPress()) super.onBackPressed()
    }

    open fun handleBackPress(): Boolean {
        if (panelState == BottomSheetBehavior.STATE_EXPANDED) {
            collapsePanel()
            return true
        }
        return false
    }

    override fun onResume() {
        super.onResume()
        volumeControlStream = AudioManager.STREAM_MUSIC

        if(!hasStoragePermission()) {
            Intent(this@MainActivity, PermissionActivity::class.java).also {
                startActivity(it)
                this@MainActivity.finish()
            }
        }

        lifecycleScope.launch(Dispatchers.Main) {
            delay(3000L)
            refreshMusicLibrary()
        }
    }

    private fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            (checkSelfPermission(android.Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED)
        } else {
            (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
        }
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

    fun playNewPlayQueue(tracks: List<Track>, startIndex: Int = 0, shuffle: Boolean = false)
            = lifecycleScope.launch(Dispatchers.Default) {
        if (tracks.isEmpty() || startIndex >= tracks.size) {
            Toast.makeText(this@MainActivity, getString(R.string.error), Toast.LENGTH_LONG).show()
            return@launch
        }

        mediaController.transportControls.stop()

        val startTrackIndex = if (shuffle) (tracks.indices).random()
        else startIndex

        val startTrackDesc = buildMediaDescription(tracks[startTrackIndex], startTrackIndex.toLong())

        val mediaControllerCompat = MediaControllerCompat.getMediaController(this@MainActivity)
        mediaControllerCompat.addQueueItem(startTrackDesc)
        skipToAndPlayQueueItem(startTrackIndex.toLong())

        for ((index, track) in tracks.withIndex()) {
            if (index == startTrackIndex) continue
            val trackDesc = buildMediaDescription(track, index.toLong())
            mediaControllerCompat.addQueueItem(trackDesc, index)
        }

        when {
            shuffle -> setShuffleMode(SHUFFLE_MODE_ALL)
            mediaControllerCompat.shuffleMode == SHUFFLE_MODE_ALL -> setShuffleMode(
                SHUFFLE_MODE_NONE
            )
        }
    }

    private fun buildMediaDescription(track: Track, queueId: Long? = null): MediaDescriptionCompat {
        val extrasBundle = Bundle().apply {
            putString("album", track.album)
            putString("album_id", track.albumId)
            queueId?.let {
                putLong("queue_id", queueId)
            }
        }

        return MediaDescriptionCompat.Builder()
            .setExtras(extrasBundle)
            .setMediaId(track.trackId.toString())
            .setSubtitle(track.artist)
            .setTitle(track.title)
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
            .error(R.drawable.artwork_placeholder)
            .transition(DrawableTransitionOptions.withCrossFade())
            .centerCrop()
            .signature(ObjectKey(file?.path + file?.lastModified()))
            .override(600, 600)
            .into(view)

        view.apply {
            alpha = 0f
            visibility = View.VISIBLE
            animate()
                .alpha(1f)
                .setDuration(300)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        }
    }

    fun getArtwork(albumId: String?): File? {
        var file: File? = null
        if (albumId != null) {
            val directory = ContextWrapper(application).getDir("albumArt", Context.MODE_PRIVATE)
            file = File(directory, "$albumId.jpg")
        }
        return file
    }

    fun showPlayer() {
        binding.slidingPanel.isVisible = true
        setMainViewMargins(bottomViewHeight)
    }

    private fun setMainViewMargins(margin: Int) {
        val playParam = binding.navHostFragmentActivityMain.layoutParams as MarginLayoutParams
        playParam.setMargins(0,0,0,margin)
        binding.navHostFragmentActivityMain.layoutParams = playParam
    }

    fun showTrackPopup(track: Track) {

        bottomSheetDialog.apply {
            setContentView(R.layout.fragment_info_track)

            val mArtwork = findViewById<ImageView>(R.id.artworkIN)
            val mTitle = findViewById<TextView>(R.id.titleIN)
            val mArtist = findViewById<TextView>(R.id.artistIN)

            val bQueue = findViewById<CardView>(R.id.addToQueue)
            val bPlst = findViewById<CardView>(R.id.addToPlist)
            val eTrack = findViewById<CardView>(R.id.editTrack)
            val bLyrics = findViewById<CardView>(R.id.checkLyrics)
            //val bDel = findViewById<CardView>(R.id.delTrack)

            mArtwork?.let { loadArtwork(track.albumId, it) }

            mTitle?.text = track.title
            mArtist?.text = track.artist

            bQueue?.setOnClickListener {
                playNext(track)
                dismiss()
            }

            bPlst?.setOnClickListener {
                showPlaylistsDialog(track.trackId)
                dismiss()
            }

            eTrack?.setOnClickListener {
                val action = MobileNavigationDirections.actionEditTrack(track)
                findNavController(R.id.nav_host_fragment_activity_main).navigate(action)
                dismiss()
                hideBar(true)
            }

            bLyrics?.setOnClickListener {
                val url = playerFragment.convertToGeniusUrl("${track.artist} ${track.title}")
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(browserIntent)
                dismiss()
            }
            show()
        }
    }

    private fun showPlaylistsDialog(trackId: Long) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_playlist, null)
        val recyclerView: RecyclerView = dialogView.findViewById(R.id.playlists_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val alertDialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.choose_playlist))
            .setView(dialogView)
            .setNegativeButton(getString(R.string.cancel), null)
            .show()

        val adapter = LitePlaylistAdapter(this) { playlist ->
            musicViewModel.addTrackToPlaylist(playlist.playlistId, trackId)
            Toast.makeText(this, this.getString(R.string.track_added_success), Toast.LENGTH_LONG).show()
            alertDialog.dismiss()
        }
        recyclerView.adapter = adapter

        musicViewModel.loadPlaylists.observe(this) { playlists ->
            adapter.processPlaylists(playlists)
        }
    }


    fun saveImage(albumId: String, image: Bitmap) {
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

    private fun playNext(track: Track) {
        val index = playQueue.indexOfFirst { it.queueId == currentQueueItemId } + 1

        val trackDesc = buildMediaDescription(track)
        val mediaControllerCompat = MediaControllerCompat.getMediaController(this@MainActivity)
        mediaControllerCompat.addQueueItem(trackDesc, index)

        Toast.makeText(this, getString(R.string.added_to_queue, track.title), Toast.LENGTH_SHORT).show()
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
                if (playQueue.isEmpty()) {
                    playNewPlayQueue(musicViewModel.loadTracks.value ?: return)
                }
                else {
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
        return mediaControllerCompat?.shuffleMode ?: SHUFFLE_MODE_NONE
    }

    fun getRepeatMode(): Int {
        val mediaControllerCompat = MediaControllerCompat.getMediaController(this@MainActivity)
        return mediaControllerCompat?.repeatMode ?: REPEAT_MODE_NONE
    }
    fun toggleShuffleMode(): Boolean {
        val newShuffleMode = if (getShuffleMode() == SHUFFLE_MODE_NONE) {
            SHUFFLE_MODE_ALL
        } else SHUFFLE_MODE_NONE

        setShuffleMode(newShuffleMode)

        return newShuffleMode == SHUFFLE_MODE_ALL
    }

    fun toggleRepeatMode(): Int {
        val newRepeatMode = when (getRepeatMode()) {
            REPEAT_MODE_NONE -> REPEAT_MODE_ALL
            REPEAT_MODE_ALL -> REPEAT_MODE_ONE
            else -> REPEAT_MODE_NONE
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

    fun refreshMusicLibrary() = lifecycleScope.launch(Dispatchers.Default) {
        getMediaStoreCursor()?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val trackIds = mutableListOf<Long>()
            while (cursor.moveToNext()) {
                val trackId = cursor.getLong(idColumn)
                trackIds.add(trackId)
                val existingTrack = musicViewModel.getTrackById(trackId)
                if (existingTrack == null) {
                    val track = createTrackFromCursor(cursor)
                    musicViewModel.insertTrack(track)
                }
            }

            val tracksToBeDeleted = musicViewModel.loadTracks.value?.filterNot {
                trackIds.contains(it.trackId)
            }
            tracksToBeDeleted?.let { tracks ->
                for (track in tracks) {
                    musicViewModel.deleteTrack(track)
                    findTrackIdInPlayQueueToRemove(track.trackId)
                }
            }
        }
    }

    private fun createTrackFromCursor(cursor: Cursor): Track {
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
            1001
        }

        val title = cursor.getString(titleColumn) ?: "Unknown track"
        val artist = cursor.getString(artistColumn) ?: "Unknown artist"
        val album = cursor.getString(albumColumn) ?: "Unknown album"
        val albumId = cursor.getString(albumIDColumn) ?: "unknown_album_id"
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

    private fun findTrackIdInPlayQueueToRemove(trackId: Long) = lifecycleScope.launch(Dispatchers.Default) {
        val queueItemsToRemove = playQueue.filter { it.description.mediaId == trackId.toString() }
        for (item in queueItemsToRemove) removeQueueItemById(item.queueId)
    }

    fun handleChangeToContentUri(uri: Uri) = lifecycleScope.launch(Dispatchers.IO) {
        val trackIdString = uri.toString().removePrefix(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString() + "/")
        try {
            val selection = MediaStore.Audio.Media._ID + "=?"
            val selectionArgs = arrayOf(trackIdString)
            val cursor = getMediaStoreCursor(selection, selectionArgs)

            val trackId = trackIdString.toLong()
            val existingTrack = musicViewModel.getTrackById(trackId)

            when {
                existingTrack == null && cursor?.count!! > 0 -> {
                    cursor.apply {
                        this.moveToNext()
                        val createdTrack = createTrackFromCursor(this)
                        musicViewModel.insertTrack(createdTrack)
                    }
                }
                cursor?.count == 0 -> {
                    existingTrack?.let {
                        musicViewModel.deleteTrack(existingTrack)
                        findTrackIdInPlayQueueToRemove(trackId)
                    }
                }
            }
        } catch (_: NumberFormatException) { refreshMusicLibrary() }
    }

    fun updateTrack(track: Track) {
        musicViewModel.updateTrack(track)

        val affectedQueueItems = playQueue.filter { it.description.mediaId == track.trackId.toString() }
        if (affectedQueueItems.isEmpty()) return

        val metadataBundle = Bundle().apply {
            putString("album", track.album)
            putString("album_id", track.albumId)
            putString("artist", track.artist)
            putString("title", track.title)
        }
        for (queueItem in affectedQueueItems) {
            metadataBundle.putLong("queue_id", queueItem.queueId)
            mediaController.sendCommand("UPDATE_QUEUE_ITEM", metadataBundle, null)
        }
    }
}