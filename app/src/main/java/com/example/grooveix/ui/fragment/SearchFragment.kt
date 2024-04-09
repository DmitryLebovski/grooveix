package com.example.grooveix.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.activity.OnBackPressedCallback
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import com.example.grooveix.R
import com.example.grooveix.databinding.FragmentSearchBinding
import com.example.grooveix.ui.activity.MainActivity
import com.example.grooveix.ui.adapter.TrackAdapter
import com.example.grooveix.ui.media.MusicDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private var musicDatabase: MusicDatabase? = null
    private lateinit var adapter: TrackAdapter
    private lateinit var mainActivity: MainActivity
    private lateinit var onBackPressedCallback: OnBackPressedCallback

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        mainActivity = activity as MainActivity
        musicDatabase = MusicDatabase.getDatabase(requireContext())

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = TrackAdapter(mainActivity)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.itemAnimator = DefaultItemAnimator()

        setupMenu()

        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController().popBackStack()
            }
        }

        mainActivity.onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackPressedCallback)
    }

    override fun onStop() {
        super.onStop()
        mainActivity.hideKeyboard()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        onBackPressedCallback.remove()
    }

    private fun setupMenu() {
        val onQueryListener = object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String): Boolean {
                if (newText.isNotEmpty()) search("%$newText%")
                return true
            }
            override fun onQueryTextSubmit(query: String): Boolean = true
        }

        binding.searchView.apply {
            queryHint = getString(R.string.search_hint)
            setOnQueryTextListener(onQueryListener)
        }
    }

    private fun search(query: String) = lifecycleScope.launch(Dispatchers.IO) {
        val songs = musicDatabase!!.musicDao().getTracksLikeSearch(query).take(10)

        withContext(Dispatchers.Main) {
            binding.noResults.isGone = true
            binding.searchButton.isGone = true
            binding.startSearch.isGone = true
            if (songs.isEmpty()) {
                binding.searchButton.isVisible = true
                binding.noResults.isVisible = true
            }
            adapter.processNewSongs(songs)
        }
    }
}