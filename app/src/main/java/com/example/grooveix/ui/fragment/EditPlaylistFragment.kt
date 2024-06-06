package com.example.grooveix.ui.fragment

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.grooveix.R
import com.example.grooveix.databinding.FragmentEditPlaylistBinding
import com.example.grooveix.ui.activity.MainActivity
import com.example.grooveix.ui.adapter.PlaylistAdapter
import com.example.grooveix.ui.media.MusicDatabase
import com.example.grooveix.ui.media.MusicViewModel
import com.example.grooveix.ui.media.entity.Playlist
import java.io.FileNotFoundException
import java.io.IOException


class EditPlaylistFragment : Fragment() {
    private var _binding: FragmentEditPlaylistBinding? = null
    private val binding get() = _binding!!
    private lateinit var mainActivity: MainActivity
    private lateinit var musicViewModel: MusicViewModel
    private lateinit var adapter: PlaylistAdapter
    private var musicDatabase: MusicDatabase? = null
    private lateinit var onBackPressedCallback: OnBackPressedCallback
    private var playlistID: Long = 1
    private var playlist: Playlist? = null
    private var newArtwork: Bitmap? = null

    private val registerResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            try {
                result.data?.data?.let { uri ->
                    newArtwork = ImageDecoder.decodeBitmap(
                        ImageDecoder.createSource(requireActivity().contentResolver, uri)
                    )
                    Glide.with(this)
                        .load(uri)
                        .centerCrop()
                        .into(binding.artwork)
                }
            } catch (_: FileNotFoundException) {
            } catch (_: IOException) { }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        arguments?.let {
            val safeArgs = EditPlaylistFragmentArgs.fromBundle(it)
            EditPlaylistFragmentDirections
            playlist = safeArgs.playlist
        }

        _binding = FragmentEditPlaylistBinding.inflate(inflater, container, false)
        mainActivity = activity as MainActivity
        musicViewModel = ViewModelProvider(mainActivity)[MusicViewModel::class.java]
        musicDatabase = MusicDatabase.getDatabase(requireContext())

        val sharedPrefs = requireContext().getSharedPreferences("PlaylistCounter", Context.MODE_PRIVATE)
        playlistID = sharedPrefs.getLong("playlistID", 0)

        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = PlaylistAdapter(mainActivity)

        mainActivity.hidePanel()

        if (playlist?.name != null) {
            binding.playlistName.text = SpannableStringBuilder(playlist?.name)
            mainActivity.loadArtwork(playlist?.artwork, binding.artwork)
        }

        binding.addPlaylistButton.setOnClickListener {
            val playlistName = binding.playlistName.text.toString().trim()
            val artworkPath = newArtwork
            val newPlaylist = Playlist(playlistID + 1, playlistName, artworkPath.toString())
            musicViewModel.insertPlaylist(newPlaylist)
            playlistID++
            val sharedPrefs = requireContext().getSharedPreferences("PlaylistCounter", Context.MODE_PRIVATE)
            sharedPrefs.edit().putLong("playlistID", playlistID).apply()

            findNavController().popBackStack()
            mainActivity.showPanel()
            mainActivity.showBar()
        }

        binding.addImage.setOnClickListener {
            permissionsResultCallback.launch(Manifest.permission.READ_MEDIA_AUDIO)
        }

        binding.btnClose.setOnClickListener {
            findNavController().popBackStack()
            mainActivity.showPanel()
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

    private val permissionsResultCallback = registerForActivityResult(
        ActivityResultContracts.RequestPermission()){
        when (it) {
            true -> {
                registerResult.launch(Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI))
            }
            false -> {
                Toast.makeText(requireContext(), this.getString(R.string.permission_error), Toast.LENGTH_LONG).show()
                val intent: Intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                startActivity(intent)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
