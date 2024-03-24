package com.example.grooveix.ui.fragment

import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.grooveix.R
import com.example.grooveix.databinding.FragmentQueueBinding
import com.example.grooveix.ui.activity.MainActivity
import com.example.grooveix.ui.adapter.QueueAdapter
import com.example.grooveix.ui.media.QueueViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class QueueFragment : Fragment() {

    private var _binding: FragmentQueueBinding? = null
    private val binding get() = _binding!!
    private val queueViewModel: QueueViewModel by activityViewModels()
    private lateinit var mainActivity: MainActivity
    private lateinit var adapter: QueueAdapter
    private lateinit var onBackPressedCallback: OnBackPressedCallback
    private var fastForwarding = false
    private var fastRewinding = false

    private val itemTouchHelper by lazy {
        val simpleItemTouchCallback = object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
            var to: Int? = null
            var queueItem: MediaSessionCompat.QueueItem? = null

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)

                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) viewHolder?.itemView?.alpha = 0.5f
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)

                viewHolder.itemView.alpha = 1.0f

                if (to != null && queueItem != null) {
                    mainActivity.notifyQueueItemMoved(queueItem!!.queueId, to!!)
                    to = null
                    queueItem = null
                }
            }

            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                val from = viewHolder.layoutPosition
                to = target.layoutPosition
                if (from != to) {
                    queueItem = adapter.playQueue[from]
                    adapter.playQueue.removeAt(from)
                    adapter.playQueue.add(to!!, queueItem!!)
                    adapter.notifyItemMoved(from, to!!)
                }

                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
        }
        ItemTouchHelper(simpleItemTouchCallback)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQueueBinding.inflate(inflater, container, false)
        mainActivity = activity as MainActivity
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.queueView.itemAnimator = DefaultItemAnimator()

        adapter = QueueAdapter(mainActivity, this)
        binding.queueView.adapter = adapter

        binding.title.isSelected = true

        queueViewModel.playbackState.observe(viewLifecycleOwner) { state ->
            if (state == PlaybackStateCompat.STATE_PLAYING) binding.btnPlay.setBackgroundResource(R.drawable.baseline_pause_24)
            else binding.btnPlay.setBackgroundResource(R.drawable.baseline_play_arrow_24)
        }

        queueViewModel.currentlyPlayingSongMetadata.observe(viewLifecycleOwner) {
            updateCurrentlyDisplayedMetadata(it)
        }

        queueViewModel.playQueue.observe(viewLifecycleOwner) { playQueue ->
            if (adapter.playQueue.isEmpty()) {
                adapter.playQueue.addAll(playQueue)
                adapter.notifyItemRangeInserted(0, playQueue.size)
            } else {
                adapter.processNewPlayQueue(playQueue)
            }
        }

        queueViewModel.currentQueueItemId.observe(viewLifecycleOwner) { position ->
            position?.let { adapter.changeCurrentlyPlayingQueueItemId(it) }
        }

        itemTouchHelper.attachToRecyclerView(binding.queueView)
        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController().popBackStack()
                mainActivity.expandPanel()
                mainActivity.hideBar(false)
            }
        }

        binding.btnPlay.setOnClickListener { mainActivity.playPauseControl() }

        binding.btnBackward.setOnClickListener{
            if (fastRewinding) fastRewinding = false
            else mainActivity.skipBack()
        }

        binding.btnBackward.setOnLongClickListener {
            fastRewinding = true
            lifecycleScope.launch {
                do {
                    mainActivity.fastRewind()
                    delay(500)
                } while (fastRewinding)
            }
            return@setOnLongClickListener false
        }

        binding.btnForward.setOnClickListener{
            if (fastForwarding) fastForwarding = false
            else mainActivity.skipForward()
        }

        binding.btnForward.setOnLongClickListener {
            fastForwarding = true
            lifecycleScope.launch {
                do {
                    mainActivity.fastForward()
                    delay(500)
                } while (fastForwarding)
            }
            return@setOnLongClickListener false
        }

        if (mainActivity.getShuffleMode() == PlaybackStateCompat.SHUFFLE_MODE_ALL) {
            binding.btnShuffle.setBackgroundResource(R.drawable.ic_shuffle_on)
        }

        binding.btnShuffle.setOnClickListener{
            if (mainActivity.toggleShuffleMode()) binding.btnShuffle.setBackgroundResource(R.drawable.ic_shuffle_on)
            else binding.btnShuffle.setBackgroundResource(R.drawable.ic_shuffle)

        }

        when (mainActivity.getRepeatMode()) {
            PlaybackStateCompat.REPEAT_MODE_ALL -> binding.btnRepeat.setBackgroundResource(R.drawable.ic_repeat_all)
            PlaybackStateCompat.REPEAT_MODE_ONE -> {
                binding.btnRepeat.setBackgroundResource(R.drawable.ic_repeat_one)
            }
        }

        binding.btnRepeat.setOnClickListener {
            when (mainActivity.toggleRepeatMode()) {
                PlaybackStateCompat.REPEAT_MODE_NONE -> {
                    binding.btnRepeat.setBackgroundResource(R.drawable.ic_repeat)
                }
                PlaybackStateCompat.REPEAT_MODE_ALL -> {
                    binding.btnRepeat.setBackgroundResource(R.drawable.ic_repeat_all)
                }
                PlaybackStateCompat.REPEAT_MODE_ONE -> {
                    binding.btnRepeat.setBackgroundResource(R.drawable.ic_repeat_one)
                }
            }
        }

        binding.btnClose.setOnClickListener {
            findNavController().popBackStack()
            mainActivity.expandPanel()
            mainActivity.hideBar(false)
        }

        queueViewModel.playbackPosition.observe(viewLifecycleOwner) {
            binding.songProgressBar.progress = it
        }

        queueViewModel.playbackDuration.observe(viewLifecycleOwner) {
            binding.songProgressBar.max = it
        }


        mainActivity.onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackPressedCallback)
    }

    private fun updateCurrentlyDisplayedMetadata(metadata: MediaMetadataCompat?) {
        binding.title.text = metadata?.getString(MediaMetadataCompat.METADATA_KEY_TITLE)
        binding.artist.text = metadata?.getString(MediaMetadataCompat.METADATA_KEY_ARTIST)

        if (metadata != null) {
            val albumId = metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI)
            mainActivity.loadArtwork(albumId, binding.artwork)
        } else {
            Glide.with(mainActivity)
                .clear(binding.artwork)
        }
    }

    override fun onResume() {
        super.onResume()

        val currentlyPlayingQueueItemIndex = adapter.playQueue.indexOfFirst {queueItem ->
            queueItem.queueId == adapter.currentlyPlayingQueueId
        }

        if (currentlyPlayingQueueItemIndex != -1) {
            (binding.queueView.layoutManager as LinearLayoutManager)
                .scrollToPositionWithOffset(currentlyPlayingQueueItemIndex, 0)
        }
    }

    fun startDragging(viewHolder: RecyclerView.ViewHolder) = itemTouchHelper.startDrag(viewHolder)

    fun showPopup(view: View, queueId: Long) {
        //TODO:add bottomsheet
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}