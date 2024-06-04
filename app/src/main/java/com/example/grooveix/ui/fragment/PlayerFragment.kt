package com.example.grooveix.ui.fragment

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.session.PlaybackStateCompat.REPEAT_MODE_ALL
import android.support.v4.media.session.PlaybackStateCompat.REPEAT_MODE_NONE
import android.support.v4.media.session.PlaybackStateCompat.REPEAT_MODE_ONE
import android.support.v4.media.session.PlaybackStateCompat.SHUFFLE_MODE_ALL
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.SeekBar
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.palette.graphics.Palette
import com.bumptech.glide.Glide
import com.example.grooveix.R
import com.example.grooveix.databinding.FragmentPlayerBinding
import com.example.grooveix.ui.activity.MainActivity
import com.example.grooveix.ui.media.QueueViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Locale


class PlayerFragment : Fragment() {

    private val queueViewModel: QueueViewModel by activityViewModels()
    private var _binding: FragmentPlayerBinding? = null
    private val binding get() = _binding!!
    private var fastForwarding = false
    private var fastRewinding = false
    private lateinit var mainActivity: MainActivity
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlayerBinding.inflate(inflater, container, false)
        mainActivity = activity as MainActivity
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.setOnTouchListener { _, _ ->
            return@setOnTouchListener true
        }

        binding.title.isSelected = true

        queueViewModel.currentlyPlayingSongMetadata.observe(viewLifecycleOwner) {
            updateCurrentlyDisplayedMetadata(it)
        }

        queueViewModel.playbackState.observe(viewLifecycleOwner) { state ->
            if (state == PlaybackStateCompat.STATE_PLAYING) binding.btnPlay.setBackgroundResource(R.drawable.baseline_pause_24)
            else binding.btnPlay.setBackgroundResource(R.drawable.baseline_play_arrow_24)
        }

        queueViewModel.playbackDuration.observe(viewLifecycleOwner) { duration ->
            duration?.let {
                binding.currentSeekBar.max = it
                binding.currentMax.text = SimpleDateFormat("mm:ss", Locale.UK).format(it)
            }
        }

        queueViewModel.playbackPosition.observe(viewLifecycleOwner) { position ->
            position?.let {
                binding.currentSeekBar.progress = position
                binding.currentPosition.text = SimpleDateFormat("mm:ss", Locale.UK).format(it)
            }
        }

        binding.btnPlay.setOnClickListener { mainActivity.playPauseControl() }

        binding.btnBackward.setOnClickListener{
            if (fastRewinding) fastRewinding = false
            else {
                mainActivity.skipBack()
                closeWebView()
            }
        }

        binding.btnBackward.setOnLongClickListener {
            fastRewinding = true
            lifecycleScope.launch {
                do {
                    mainActivity.fastRewind()
                    delay(500)
                } while (fastRewinding)
            }
            return@setOnLongClickListener false
        }

        binding.btnForward.setOnClickListener{
            if (fastForwarding) fastForwarding = false
            else {
                mainActivity.skipForward()
                closeWebView()
            }
        }

        binding.btnForward.setOnLongClickListener {
            fastForwarding = true
            lifecycleScope.launch {
                do {
                    mainActivity.fastForward()
                    delay(500)
                } while (fastForwarding)
            }
            return@setOnLongClickListener false
        }

        if (mainActivity.getShuffleMode() == SHUFFLE_MODE_ALL) {
            binding.currentButtonShuffle.setBackgroundResource(R.drawable.ic_shuffle_on)
        }

        binding.currentButtonShuffle.setOnClickListener{
            if (mainActivity.toggleShuffleMode()) binding.currentButtonShuffle.setBackgroundResource(R.drawable.ic_shuffle_on)
            else binding.currentButtonShuffle.setBackgroundResource(R.drawable.ic_shuffle)
        }

        when (mainActivity.getRepeatMode()) {
            REPEAT_MODE_ALL -> binding.currentButtonRepeat.setBackgroundResource(R.drawable.ic_repeat_all)
            REPEAT_MODE_ONE -> {
                binding.currentButtonRepeat.setBackgroundResource(R.drawable.ic_repeat_one)
            }
        }

        binding.currentButtonRepeat.setOnClickListener {
            when (mainActivity.toggleRepeatMode()) {
                REPEAT_MODE_NONE -> {
                    binding.currentButtonRepeat.setBackgroundResource(R.drawable.ic_repeat)
                }
                REPEAT_MODE_ALL -> {
                    binding.currentButtonRepeat.setBackgroundResource(R.drawable.ic_repeat_all)
                }
                REPEAT_MODE_ONE -> {
                    binding.currentButtonRepeat.setBackgroundResource(R.drawable.ic_repeat_one)
                }
            }
        }

        binding.currentLyricView.setOnClickListener {
            showLyricWebCardView()
            val trackTitle = binding.title.text.toString()
            val trackArtist = binding.artist.text.toString()
            val url = convertToGeniusUrl("$trackArtist $trackTitle")
            binding.lyricWebView.loadUrl(url)
            Log.d("CHECKWEBVIEW", url)
        }

        binding.lyricWebView.apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                javaScriptCanOpenWindowsAutomatically = true
                blockNetworkLoads = false
                allowContentAccess = true
            }

            webViewClient = WebViewClient()

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    view?.loadUrl(request?.url.toString())
                    return true
                }
            }
        }

        binding.closeWeb.setOnClickListener {
            closeWebView()
        }

        binding.btnQueue.setOnClickListener {
            findNavController().navigate(R.id.nav_queue)
            mainActivity.collapsePanel()
            mainActivity.hideBar(true)
            closeWebView()
         }

        binding.currentSeekBar.setOnSeekBarChangeListener( object : SeekBar.OnSeekBarChangeListener {
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                val progress = seekBar.progress
                mainActivity.seekTo(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.currentPosition.text = SimpleDateFormat("mm:ss", Locale.UK).format(progress)
            }
        })

        val audioManager =  requireContext().getSystemService(AudioManager::class.java)

        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        binding.volumeSeekBar.progress = currentVolume

        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        binding.volumeSeekBar.max = maxVolume

        binding.volumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun closeWebView() {
        hideLyricWebCardView()
    }

    fun showLyricWebCardView() {
        binding.lyricWebCardView.apply {
            alpha = 0f
            visibility = View.VISIBLE
            animate()
                .alpha(1f)
                .setDuration(300)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        }
    }
    fun hideLyricWebCardView() {
        binding.lyricWebCardView.apply {
            animate()
                .alpha(0f)
                .setDuration(300)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withEndAction { visibility = View.GONE }
                .start()
        }
    }

    fun convertToGeniusUrl(artistTitle: String): String {
        val baseUrl = "https://genius.com/"
        val searchBaseUrl = "https://genius.com/search?q="

        val separatorIndex = artistTitle.indexOfFirst { it == ',' || it == '/' }
        val (artist, title) = if (separatorIndex != -1) {
            val artist = artistTitle.substring(0, separatorIndex).trim()
            val title = artistTitle.substring(separatorIndex + 1).trim()
            artist to title
        } else {
            val parts = artistTitle.split(" ", limit = 2)
            if (parts.size == 2) parts[0] to parts[1] else artistTitle to ""
        }

        val formattedArtist = artist.replace(" ", "-").replace("+", "-").toLowerCase()
        val formattedTitle = title.replace(" ", "-").replace("+", "-").toLowerCase()

        return if (separatorIndex != -1) {
            "$searchBaseUrl${artist.replace(" ", "%20")}%20${title.replace(" ", "%20")}"
        } else {
            "$baseUrl${formattedArtist}-${formattedTitle}-lyrics"
        }
    }

    private fun updateCurrentlyDisplayedMetadata(metadata: MediaMetadataCompat?) = lifecycleScope.launch(
        Dispatchers.Main) {
        binding.title.text = metadata?.getString(MediaMetadataCompat.METADATA_KEY_TITLE)
        binding.artist.text = metadata?.getString(MediaMetadataCompat.METADATA_KEY_ARTIST)

        if (metadata != null) {
            val albumId = metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI)
            mainActivity.loadArtwork(albumId, binding.artwork)
            var dominantColor = requireContext().getColor(R.color.botttom_light_3)
            var copyColor = requireContext().getColor(R.color.botttom_light_3)

            val bitmap = BitmapFactory.decodeFile(mainActivity.getArtwork(albumId)?.path)

            if (bitmap != null) {
                val palette = Palette.from(bitmap).generate()
                dominantColor = palette.getDominantColor(requireContext().getColor(R.color.botttom_light_3))
                if (copyColor != dominantColor) copyColor = dominantColor
            }

            val gradientDrawable = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(requireContext().getColor(R.color.transparent_color), dominantColor, dominantColor)
            )

            val cornerRadii = floatArrayOf(
                30f, 30f,
                30f, 30f,
                0f, 0f,
                0f, 0f
            )

            gradientDrawable.cornerRadii = cornerRadii

            //binding.dimBackground.background = BitmapDrawable(resources, bitmap)
            binding.dimBackground.background = gradientDrawable

        } else {
            Glide.with(mainActivity)
                .clear(binding.artwork)
        }
    }
}
