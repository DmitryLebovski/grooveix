package com.example.grooveix.ui.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import com.example.grooveix.R
import com.example.grooveix.databinding.FragmentPlaylistBinding
import com.example.grooveix.ui.activity.MainActivity
import com.example.grooveix.ui.adapter.PlaylistAdapter
import com.example.grooveix.ui.media.MusicDatabase
import com.example.grooveix.ui.media.MusicViewModel
import com.example.grooveix.ui.media.entity.Playlist

class PlaylistFragment : Fragment() {
    private var _binding: FragmentPlaylistBinding? = null
    private val binding get() = _binding!!
    private lateinit var mainActivity: MainActivity
    private lateinit var musicViewModel: MusicViewModel
    private lateinit var adapter: PlaylistAdapter
    private var musicDatabase: MusicDatabase? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPlaylistBinding.inflate(inflater, container, false)
        mainActivity = activity as MainActivity
        musicViewModel = ViewModelProvider(mainActivity)[MusicViewModel::class.java]
        musicDatabase = MusicDatabase.getDatabase(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.itemAnimator = DefaultItemAnimator()

        adapter = PlaylistAdapter(mainActivity)
        binding.recyclerView.adapter = adapter
        adapter.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY

        musicViewModel.loadPlaylists.observe(viewLifecycleOwner) {
            updateRecyclerView(it)
        }

        binding.addPlaylistView.setOnClickListener {
            findNavController().navigate(R.id.nav_edit_playlist)
            mainActivity.hideBar(true)
        }
    }

    private fun updateRecyclerView(playlists: List<Playlist>) {
        if (playlists.isEmpty()) {
            binding.noPlaylist.isVisible = true
        } else {
            binding.noPlaylist.isGone = true
        }
        adapter.processPlaylists(playlists)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}