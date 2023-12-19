package com.example.grooveix.ui.home

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.grooveix.SongViewModel
import com.example.grooveix.adapter.SongsAdapter
import com.example.grooveix.databinding.FragmentHomeBinding
import com.example.grooveix.repository.SongRepository
import com.example.grooveix.views.ScrollingViewOnApplyWindowInsetsListener
import com.zakariya.mzmusicplayer.ui.SongViewModelFactory
import me.zhanghai.android.fastscroll.FastScroller
import me.zhanghai.android.fastscroll.FastScrollerBuilder

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    private val binding get() = _binding!!
    private lateinit var viewModel: SongViewModel
    private lateinit var adapter: SongsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val repository = SongRepository(requireContext())
        val viewModelFactory = SongViewModelFactory(repository)
        viewModel = ViewModelProvider(this, viewModelFactory)[SongViewModel::class.java]

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        viewModel.forceReload()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initLayoutManager()
        initAdapter()

        val fastScroller = createFastScroller(binding.musicList)

        binding.musicList.setOnApplyWindowInsetsListener(
            ScrollingViewOnApplyWindowInsetsListener(binding.musicList, fastScroller)
        )

        viewModel.songLiveData.observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {
                adapter.updateSongList(it)
            } else {
                adapter.updateSongList(emptyList())
            }
        }
    }
    private fun createFastScroller(recyclerView: RecyclerView): FastScroller? {
        return FastScrollerBuilder(recyclerView).useMd2Style().build()
    }

    private fun initAdapter() {
        adapter = SongsAdapter(requireContext(), mutableListOf())
        binding.musicList.adapter = adapter
    }

    private fun initLayoutManager() {
        binding.musicList.layoutManager = LinearLayoutManager(activity as Context)
    }

}