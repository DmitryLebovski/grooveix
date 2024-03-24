package com.example.grooveix.ui.fragment

import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.grooveix.R
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.grooveix.databinding.FragmentMiniPlayerBinding
import com.example.grooveix.ui.activity.MainActivity
import com.example.grooveix.ui.media.QueueViewModel

class MiniPlayerFragment : Fragment() {

    private var _binding: FragmentMiniPlayerBinding? = null
    private val binding get() = _binding!!
    private val playQueueViewModel: QueueViewModel by activityViewModels()
    private lateinit var mainActivity: MainActivity

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMiniPlayerBinding.inflate(inflater, container, false)
        mainActivity = activity as MainActivity
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        playQueueViewModel.currentlyPlayingSongMetadata.observe(viewLifecycleOwner) {
            updateCurrentlyDisplayedMetadata(it)
        }

        binding.title.isSelected = true

        binding.root.setOnClickListener {
            playQueueViewModel.currentlyPlayingSongMetadata.value?.let {
                findNavController().navigate(R.id.nav_currently_playing, null, null)
            }
        }

        playQueueViewModel.playbackState.observe(viewLifecycleOwner) { state ->
            if (state == PlaybackStateCompat.STATE_PLAYING) binding.btnPlay.setBackgroundResource(R.drawable.baseline_pause_24)
            else binding.btnPlay.setBackgroundResource(R.drawable.baseline_play_arrow_24)
        }

        playQueueViewModel.playbackPosition.observe(viewLifecycleOwner) {
            binding.songProgressBar.progress = it
        }

        playQueueViewModel.playbackDuration.observe(viewLifecycleOwner) {
            binding.songProgressBar.max = it
        }

        binding.btnPlay.setOnClickListener {
            mainActivity.playPauseControl()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()

        binding.songProgressBar.max = playQueueViewModel.playbackDuration.value ?: 0
        binding.songProgressBar.progress = playQueueViewModel.playbackPosition.value ?: 0
    }

    private fun updateCurrentlyDisplayedMetadata(metadata: MediaMetadataCompat?) {
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
}