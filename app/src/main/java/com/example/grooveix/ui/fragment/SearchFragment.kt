package com.example.grooveix.ui.fragment

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context.DOWNLOAD_SERVICE
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.DownloadListener
import android.webkit.URLUtil
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import android.widget.SearchView
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
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
import java.net.URLEncoder
import java.nio.charset.StandardCharsets


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

    @SuppressLint("SetJavaScriptEnabled")
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

        binding.resultWebView.getSettings().javaScriptEnabled = true;

        binding.searchButton.setOnClickListener {
            binding.searchLayout.isGone = true
            binding.searchButton.isGone = true
            binding.webViewLayout.isVisible = true

            binding.resultWebView.settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                javaScriptCanOpenWindowsAutomatically = true
                blockNetworkLoads = false
                allowContentAccess = true
            }

            binding.resultWebView.setDownloadListener(DownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
                val request = DownloadManager.Request(Uri.parse(url))
                request.setMimeType(mimeType)
                request.addRequestHeader("User-Agent", userAgent)
                request.setDescription("Downloading file...")
                request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType))
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                request.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    URLUtil.guessFileName(url, contentDisposition, mimeType)
                )
                val dm = requireContext().getSystemService(DOWNLOAD_SERVICE) as DownloadManager?
                dm!!.enqueue(request)
                Toast.makeText(context, "Downloading File", Toast.LENGTH_LONG)
                    .show()
            })


            val encodedQuery = URLEncoder.encode(binding.searchView.query.toString(), StandardCharsets.UTF_8.toString())
            binding.resultWebView.loadUrl("https://rus.hitmotop.com/search?q=$encodedQuery")
        }

        binding.closeWeb.setOnClickListener {
            binding.searchLayout.isVisible = true
            binding.searchButton.isVisible = true
            binding.webViewLayout.isGone = true
        }
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
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(newText: String): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                if (newText.isNotEmpty()) search("%$newText%")
                return true
            }
        })

        binding.searchView.apply {
            queryHint = getString(R.string.search_hint)
        }
    }

    private fun search(query: String) = lifecycleScope.launch(Dispatchers.IO) {
        val songs = musicDatabase!!.musicDao().getTracksLikeSearch(query).take(10)

        withContext(Dispatchers.Main) {
            binding.noResults.isGone = true
            binding.startSearch.isGone = true
            if (songs.isEmpty()) {
                binding.searchButton.isVisible = true
                binding.noResults.isVisible = true
            }
            adapter.processNewSongs(songs)
        }
    }
}