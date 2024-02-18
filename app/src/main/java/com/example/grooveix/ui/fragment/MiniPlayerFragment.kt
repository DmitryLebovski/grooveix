package com.example.grooveix.ui.fragment

import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.grooveix.R
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.grooveix.databinding.FragmentMiniPlayerBinding
import com.example.grooveix.ui.activity.MainActivity
import com.example.grooveix.ui.media.QueueViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MiniPlayerFragment : Fragment() {
    private var _binding: FragmentMiniPlayerBinding? = null
    private val binding get() = _binding!!
    private var fastForwarding = false
    private var fastRewinding = false
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

        binding.root.setOnClickListener {
            playQueueViewModel.currentlyPlayingSongMetadata.value?.let {
                val extras = FragmentNavigatorExtras(
                    binding.artwork to binding.artwork.transitionName,
                    binding.title to binding.title.transitionName,
                    binding.album to binding.album.transitionName,
                    binding.artist to binding.artist.transitionName,
                    binding.btnPlay to binding.btnPlay.transitionName,
                    binding.btnBackward to binding.btnBackward.transitionName,
                    binding.btnForward to binding.btnForward.transitionName
                )
                findNavController().navigate(R.id.nav_currently_playing, null, null, extras)
            }
        }

        playQueueViewModel.playbackState.observe(viewLifecycleOwner) { state ->
            if (state == PlaybackStateCompat.STATE_PLAYING) binding.btnPlay.setImageResource(R.drawable.baseline_pause_24)
            else binding.btnPlay.setImageResource(R.drawable.baseline_play_arrow_24)
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

        binding.btnBackward.setOnClickListener{
            if (fastRewinding) fastRewinding = false
            else mainActivity.skipBack()
        }

        binding.btnBackward.setOnLongClickListener {
            fastRewinding = true
            lifecycleScope.launch {
                do {
                    mainActivity.fastRewind()
                    delay(100)
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
        binding.album.text = metadata?.getString(MediaMetadataCompat.METADATA_KEY_ALBUM)

        if (metadata != null) {
            val albumId = metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI)
            Log.d("AlbumID", "Album ID1WORK: $albumId")
            Log.d("AlbumID", "Album ID2WORK: ${binding.title.text}")
            Log.d("AlbumID", "Album ID3WORK: ${binding.artist.text}")
            mainActivity.loadArtwork(albumId, binding.artwork)
        } else {
            Glide.with(mainActivity)
                .clear(binding.artwork)
        }
    }
}