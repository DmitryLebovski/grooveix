package com.example.grooveix.ui.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import com.example.grooveix.R
import com.example.grooveix.databinding.FragmentPlaylistTracksBinding
import com.example.grooveix.ui.activity.MainActivity
import com.example.grooveix.ui.adapter.TrackAdapter
import com.example.grooveix.ui.media.MusicDatabase
import com.example.grooveix.ui.media.MusicViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlaylistTracksFragment : Fragment() {
    private var _binding: FragmentPlaylistTracksBinding? = null
    private val binding get() = _binding!!
    private var playlistId: Long = -1
    private var musicDatabase: MusicDatabase? = null
    private lateinit var musicViewModel: MusicViewModel
    private lateinit var adapter: TrackAdapter
    private lateinit var mainActivity: MainActivity
    private lateinit var onBackPressedCallback: OnBackPressedCallback

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPlaylistTracksBinding.inflate(inflater, container, false)
        mainActivity = activity as MainActivity
        musicDatabase = MusicDatabase.getDatabase(requireContext())
        musicViewModel = ViewModelProvider(mainActivity)[MusicViewModel::class.java]

        arguments?.let {
            playlistId = it.getLong("playlistId", -1)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = TrackAdapter(mainActivity)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.itemAnimator = DefaultItemAnimator()

        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                backStack()
            }
        }

        mainActivity.onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackPressedCallback)

        CoroutineScope(Dispatchers.IO).launch {
            val tracks = musicViewModel.getTracksInPlaylist(playlistId)
            val playListName = musicViewModel.getPlaylistInfo(playlistId)

            withContext(Dispatchers.Main) {
                binding.noTracks.isGone = true
                binding.recyclerView.isVisible = true
                if (tracks.isEmpty()) {
                    binding.noTracks.isVisible = true
                    binding.recyclerView.isGone = true
                    binding.shufflePlaylist.isInvisible = true
                }
                binding.collapsingToolbar.title = playListName.name
                adapter.processNewTracks(tracks)
            }

            binding.shufflePlButton.setOnClickListener {
                mainActivity.playNewPlayQueue(tracks, shuffle = true)
                mainActivity.showPlayer()
            }

            binding.btnClose.setOnClickListener {
                backStack()
            }
        }

        mainActivity.onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackPressedCallback)
    }
    fun backStack() {
        mainActivity.supportFragmentManager.beginTransaction().replace(R.id.playlist_tracks_view, PlaylistFragment()).commit()
    }

}