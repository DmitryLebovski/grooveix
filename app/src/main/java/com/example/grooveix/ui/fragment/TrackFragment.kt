package com.example.grooveix.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import com.example.grooveix.databinding.FragmentHomeBinding
import com.example.grooveix.ui.activity.MainActivity
import com.example.grooveix.ui.adapter.TrackAdapter
import com.example.grooveix.ui.media.MusicViewModel
import com.example.grooveix.ui.media.Track


class TrackFragment : Fragment() {


    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private var isUpdating = false
    private var unhandledRequestReceived = false
    private lateinit var adapter: TrackAdapter
    private lateinit var musicViewModel: MusicViewModel
    private lateinit var mainActivity: MainActivity

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        mainActivity = activity as MainActivity
        musicViewModel = ViewModelProvider(mainActivity)[MusicViewModel::class.java]
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
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}