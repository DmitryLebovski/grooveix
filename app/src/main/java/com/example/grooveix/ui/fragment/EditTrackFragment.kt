package com.example.grooveix.ui.fragment

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.grooveix.R
import com.example.grooveix.databinding.FragmentEditTrackBinding
import com.example.grooveix.ui.activity.MainActivity
import com.example.grooveix.ui.media.entity.Track
import java.io.FileNotFoundException
import java.io.IOException

class EditTrackFragment : Fragment() {

    private var _binding: FragmentEditTrackBinding? = null
    private val binding get() = _binding!!
    private var track: Track? = null
    private var newArtwork: Bitmap? = null
    private lateinit var mainActivity: MainActivity
    private lateinit var onBackPressedCallback: OnBackPressedCallback

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
                        .into(binding.editSongArtwork)
                }
            } catch (_: FileNotFoundException) {
            } catch (_: IOException) { }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        arguments?.let {
            val safeArgs = EditTrackFragmentArgs.fromBundle(it)
            EditPlaylistFragmentDirections
            track = safeArgs.track
        }

        _binding = FragmentEditTrackBinding.inflate(inflater, container, false)
        mainActivity = activity as MainActivity
        binding.updateTrackButton.setOnClickListener {
            val newTitle = binding.editSongTitle.text.toString()
            val newArtist = binding.editSongArtist.text.toString()
            val newDisc = binding.editSongDisc.text.toString()
            val newTrack = binding.editSongTrack.text.toString()

            if (newTitle.isNotEmpty() && newArtist.isNotEmpty() && newDisc.isNotEmpty() && newTrack.isNotEmpty()) {
                val completeTrack = when (newTrack.length) {
                    3 -> newDisc + newTrack
                    2 -> newDisc + "0" + newTrack
                    else -> newDisc + "00" + newTrack
                }.toInt()

                if (newTitle != track!!.title || newArtist != track!!.artist || completeTrack != track!!.track || newArtwork != null) {
                    newArtwork?.let { artwork ->
                        mainActivity.saveImage(track?.albumId!!, artwork)
                    }

                    track!!.title = newTitle
                    track!!.artist = newArtist
                    track!!.track = completeTrack

                    mainActivity.updateTrack(track!!)
                }

                Toast.makeText(activity, getString(R.string.details_saved), Toast.LENGTH_SHORT).show()
                requireView().findNavController().popBackStack()
                mainActivity.showBar()
                mainActivity.showPanel()
            } else Toast.makeText(activity, getString(R.string.check_fields_not_empty), Toast.LENGTH_SHORT).show()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.editSongTitle.text = SpannableStringBuilder(track?.title)
        binding.editSongArtist.text = SpannableStringBuilder(track?.artist)
        binding.editSongDisc.text = SpannableStringBuilder(track?.track.toString().substring(0, 1))
        binding.editSongTrack.text = SpannableStringBuilder(track?.track.toString().substring(1, 4)
            .toInt().toString())

        mainActivity.loadArtwork(track?.albumId, binding.editSongArtwork)

        binding.editSongArtwork.setOnClickListener {
            permissionsResultCallback.launch(Manifest.permission.READ_MEDIA_AUDIO)
        }

        binding.editSongArtworkIcon.setOnClickListener {
            registerResult.launch(Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI))
        }

        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController().popBackStack()
                mainActivity.showBar()
                mainActivity.showPanel()
            }
        }
        mainActivity.hidePanel()

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
