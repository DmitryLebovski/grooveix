package com.example.grooveix.ui.fragment

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.BitmapImageViewTarget
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.grooveix.R
import com.example.grooveix.databinding.FragmentEditPlaylistBinding
import com.example.grooveix.ui.activity.MainActivity
import com.example.grooveix.ui.adapter.PlaylistAdapter
import com.example.grooveix.ui.media.MusicDatabase
import com.example.grooveix.ui.media.MusicViewModel
import com.example.grooveix.ui.media.entity.Playlist
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date


class EditPlaylistFragment : Fragment() {
    private var _binding: FragmentEditPlaylistBinding? = null
    private val binding get() = _binding!!
    private lateinit var mainActivity: MainActivity
    private lateinit var musicViewModel: MusicViewModel
    private lateinit var adapter: PlaylistAdapter
    private var musicDatabase: MusicDatabase? = null
    private lateinit var onBackPressedCallback: OnBackPressedCallback
    private var playlistID: Long = 1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentEditPlaylistBinding.inflate(inflater, container, false)
        mainActivity = activity as MainActivity
        musicViewModel = ViewModelProvider(mainActivity)[MusicViewModel::class.java]
        musicDatabase = MusicDatabase.getDatabase(requireContext())

        val sharedPrefs = requireContext().getSharedPreferences("PlaylistCounter", Context.MODE_PRIVATE)
        playlistID = sharedPrefs.getLong("playlistID", 0)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = PlaylistAdapter(mainActivity)

        mainActivity.hidePanel()

        binding.addPlaylistButton.setOnClickListener {
            val playlistName = binding.playlistName.text.toString().trim()
            val artworkPath = requireContext().getDrawable(R.drawable.grooveix).toString()
            val newPlaylist = Playlist(playlistID + 1, playlistName, artworkPath.toString())
            musicViewModel.insertPlaylist(newPlaylist)
            playlistID++
            val sharedPrefs = requireContext().getSharedPreferences("PlaylistCounter", Context.MODE_PRIVATE)
            sharedPrefs.edit().putLong("playlistID", playlistID).apply()

            findNavController().popBackStack()
            mainActivity.showPanel()
            mainActivity.showBar()
        }

        binding.btnClose.setOnClickListener {
            findNavController().popBackStack()
            mainActivity.showBar()
        }

        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController().popBackStack()
                mainActivity.showPanel()
                mainActivity.showBar()
            }
        }

        mainActivity.onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackPressedCallback)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
