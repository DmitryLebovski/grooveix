package com.example.grooveix.ui.fragment

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.session.PlaybackStateCompat.REPEAT_MODE_ALL
import android.support.v4.media.session.PlaybackStateCompat.REPEAT_MODE_NONE
import android.support.v4.media.session.PlaybackStateCompat.REPEAT_MODE_ONE
import android.support.v4.media.session.PlaybackStateCompat.SHUFFLE_MODE_ALL
import android.transition.TransitionInflater
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.grooveix.R
import com.example.grooveix.databinding.FragmentPlayerBinding
import com.example.grooveix.ui.activity.MainActivity
import com.example.grooveix.ui.media.QueueViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
            else mainActivity.skipBack()
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
            else mainActivity.skipForward()
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

        binding.currentClose.setOnClickListener {
            findNavController().popBackStack()
        }

//        binding.artwork.setOnClickListener {
//            showPopup()
//        }

        binding.currentSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                val progress = seekBar.progress
                mainActivity.seekTo(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.currentPosition.text = SimpleDateFormat("mm:ss", Locale.UK).format(progress)
            }
        })
    }

    override fun onResume() {
        super.onResume()
        mainActivity.hideBar(true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        mainActivity.hideBar(false)
    }

    private fun updateCurrentlyDisplayedMetadata(metadata: MediaMetadataCompat?) = lifecycleScope.launch(
        Dispatchers.Main) {
        binding.title.text = metadata?.getString(MediaMetadataCompat.METADATA_KEY_TITLE)
        binding.artist.text = metadata?.getString(MediaMetadataCompat.METADATA_KEY_ARTIST)

        if (metadata != null) {
            val albumId = metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI)
            mainActivity.loadArtwork(albumId, binding.artwork)
        } else {
            Glide.with(mainActivity)
                .clear(binding.artwork)
        }
    }

//    private fun showPopup() {
//        PopupMenu(this.context, binding.currentClose).apply {
//            inflate(R.menu.currently_playing_menu)
//
//            setForceShowIcon(true)
//
//            setOnMenuItemClickListener { menuItem ->
//                when (menuItem.itemId) {
//                    R.id.search -> {
//                        findNavController().popBackStack()
//                        mainActivity.findNavController(R.id.nav_host_fragment).navigate(R.id.nav_search)
//                    }
//                    R.id.queue -> {
//                        findNavController().popBackStack()
//                        mainActivity.findNavController(R.id.nav_host_fragment).navigate(R.id.nav_queue)
//                    }
//                }
//                true
//            }
//            show()
//        }
//    }
}
