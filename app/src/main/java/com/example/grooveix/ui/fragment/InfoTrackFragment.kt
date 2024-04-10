package com.example.grooveix.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.grooveix.databinding.FragmentInfoTrackBinding
import com.example.grooveix.ui.activity.MainActivity
import com.example.grooveix.ui.media.QueueViewModel

class InfoTrackFragment : Fragment() {


    private var _binding: FragmentInfoTrackBinding? = null
    private val binding get() = _binding!!
    private lateinit var mainActivity: MainActivity
    private val playQueueViewModel: QueueViewModel by activityViewModels()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInfoTrackBinding.inflate(inflater, container, false)
        mainActivity = activity as MainActivity
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }
}