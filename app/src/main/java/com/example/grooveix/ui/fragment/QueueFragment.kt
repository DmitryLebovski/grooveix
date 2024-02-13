package com.example.grooveix.ui.fragment

import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.grooveix.R
import com.example.grooveix.databinding.FragmentQueueBinding
import com.example.grooveix.ui.activity.MainActivity
import com.example.grooveix.ui.adapter.QueueAdapter
import com.example.grooveix.ui.media.QueueViewModel
import java.util.Queue

class QueueFragment : Fragment() {
    private var _binding: FragmentQueueBinding? = null
    private val binding get() = _binding!!
    private val playQueueViewModel: QueueViewModel by activityViewModels()
    private lateinit var mainActivity: MainActivity
    private lateinit var adapter: QueueAdapter

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

        binding.root.itemAnimator = DefaultItemAnimator()

        adapter = QueueAdapter(mainActivity, this)
        binding.root.adapter = adapter

        playQueueViewModel.playQueue.observe(viewLifecycleOwner) { queue ->
            queue?.let {
                if (adapter.playQueue.size > it.size) {
                    val diff = adapter.playQueue - it.toSet()
                    for (item in diff) {
                        val index = adapter.playQueue.indexOfFirst { queueItem ->
                            queueItem.queueId == item.queueId
                        }
                        if (index != -1) {
                            adapter.playQueue.removeAt(index)
                            adapter.notifyItemRemoved(index)
                        }
                    }
                } else {
                    adapter.playQueue = it.toMutableList()
                    //todo fix: call notifyDataChange AFTER item moved
                    adapter.notifyDataSetChanged()
                }
            }
        }

//        playQueueViewModel.playQueue.observe(viewLifecycleOwner) { playQueue ->
//            if (adapter.playQueue.isEmpty()) {
//                adapter.playQueue.addAll(playQueue)
//                adapter.notifyItemRangeInserted(0, playQueue.size)
//            } else {
//                adapter.playQueue.clear()
//                adapter.playQueue.addAll(playQueue)
//                adapter.notifyDataSetChanged()
////                adapter.processNewPlayQueue(playQueue)
//            }
//        }

        playQueueViewModel.currentQueueItemId.observe(viewLifecycleOwner) { position ->
            position?.let { adapter.changeCurrentlyPlayingQueueItemId(it) }
        }

        itemTouchHelper.attachToRecyclerView(binding.root)
    }

    override fun onResume() {
        super.onResume()

        val currentlyPlayingQueueItemIndex = adapter.playQueue.indexOfFirst {queueItem ->
            queueItem.queueId == adapter.currentlyPlayingQueueId
        }

        if (currentlyPlayingQueueItemIndex != -1) {
            (binding.root.layoutManager as LinearLayoutManager)
                .scrollToPositionWithOffset(currentlyPlayingQueueItemIndex, 0)
        }
    }

    fun startDragging(viewHolder: RecyclerView.ViewHolder) = itemTouchHelper.startDrag(viewHolder)


    fun showPopup(view: View, queueId: Long) {
        PopupMenu(requireContext(), view).apply {
            inflate(R.menu.queue_item_menu)
            setOnMenuItemClickListener {
                if (it.itemId == R.id.remove_item) mainActivity.removeQueueItemById(queueId)
                true
            }
            show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
