package com.example.grooveix.ui.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.grooveix.R
import com.example.grooveix.databinding.FragmentEditPlaylistBinding
import com.example.grooveix.databinding.FragmentHomeBinding
import com.example.grooveix.databinding.FragmentPlaylistBinding

class EditPlaylistFragment : Fragment() {
    private var _binding: FragmentEditPlaylistBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_edit_playlist, container, false)
    }
}