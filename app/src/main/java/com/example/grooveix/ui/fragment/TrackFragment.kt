package com.example.grooveix.ui.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.annotation.MenuRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import com.example.grooveix.R
import com.example.grooveix.databinding.FragmentHomeBinding
import com.example.grooveix.ui.activity.MainActivity
import com.example.grooveix.ui.adapter.TrackAdapter
import com.example.grooveix.ui.media.MusicDatabase
import com.example.grooveix.ui.media.MusicViewModel
import com.example.grooveix.ui.media.entity.Track


class TrackFragment : Fragment() {


    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private var isUpdating = false
    private var unhandledRequestReceived = false
    private lateinit var adapter: TrackAdapter
    private lateinit var musicViewModel: MusicViewModel
    private var musicDatabase: MusicDatabase? = null
    private lateinit var mainActivity: MainActivity

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        mainActivity = activity as MainActivity
        musicViewModel = ViewModelProvider(mainActivity)[MusicViewModel::class.java]
        musicDatabase = MusicDatabase.getDatabase(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.itemAnimator = DefaultItemAnimator()

        adapter = TrackAdapter(mainActivity)
        binding.recyclerView.adapter = adapter
        adapter.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY

        musicViewModel.loadTracks.observe(viewLifecycleOwner) {
            updateRecyclerView(it)
        }

        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0 && binding.fab.visibility == View.VISIBLE) {
                    binding.fab.hide()
                } else if (dy < 0 && binding.fab.visibility != View.VISIBLE) {
                    binding.fab.show()

                }
            }
        })

        binding.sortBtn.setOnClickListener {
            showMenu(it, R.menu.sort_menu)
            //adapter.songs.clear()
            Log.d("CHECKCHECK", musicViewModel.loadTracks.value.toString())
            Log.d("CHECKCHECK2", musicViewModel.loadTracksArtist.value.toString())

        }
    }

//    currentSortingOption = if (currentSortingOption == "loadTracks") {
//        "loadTracksArtist"
//    } else {
//        "loadTracks"
//    }
//    val songs = musicDatabase!!.musicDao().getSongListOrderBy(currentSortingOption)
//    adapter.songs.addAll(songs)

    private fun showMenu(v: View, @MenuRes menuRes: Int) {
        val popup = PopupMenu(requireContext(), v)
        popup.menuInflater.inflate(menuRes, popup.menu)

        popup.setOnMenuItemClickListener { menuItem: MenuItem ->
            return@setOnMenuItemClickListener when (menuItem.itemId) {
                        R.id.option_1 -> {
                            adapter.songs.clear()
                            musicViewModel.loadTracks.observe(viewLifecycleOwner) {
                                updateRecyclerView(it)
                            }
                            adapter.notifyDataSetChanged()
                            true
                        }

                        R.id.option_2 -> {
                            adapter.songs.clear()
                            musicViewModel.loadTracksArtist.observe(viewLifecycleOwner) {
                                updateRecyclerView(it)
                            }
                            adapter.notifyDataSetChanged()
                            true
                        }
                        else -> false
            }
        }

        popup.show()
    }

    private fun updateRecyclerView(songs: List<Track>) {
        if (isUpdating) {
            unhandledRequestReceived = true
            return
        }
        isUpdating = true

        binding.fab.setOnClickListener {
            mainActivity.playNewPlayQueue(songs, shuffle = true)
        }

        adapter.processNewSongs(songs)

        isUpdating = false
        if (unhandledRequestReceived) {
            unhandledRequestReceived = false
            musicViewModel.loadTracks.value?.let { updateRecyclerView(it) }
                //musicViewModel.loadTracks.value?.let { updateRecyclerView(it) }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}