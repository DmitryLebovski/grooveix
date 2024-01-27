package com.example.grooveix.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.grooveix.databinding.FragmentHomeBinding
import com.example.grooveix.ui.activity.MainActivity


class TrackFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val mainActivity = activity as MainActivity?

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}