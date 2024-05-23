package com.example.grooveix.ui.adapter

import android.annotation.SuppressLint
import android.graphics.Color
import android.support.v4.media.session.MediaSessionCompat.QueueItem
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.grooveix.R
import com.example.grooveix.ui.activity.MainActivity
import com.example.grooveix.ui.fragment.QueueFragment
import com.google.android.material.color.MaterialColors

class QueueAdapter(private val activity: MainActivity, private val fragment: QueueFragment): RecyclerView.Adapter<QueueAdapter.PlayQueueViewHolder>() {
    var currentlyPlayingQueueId = -1L
    val playQueue = mutableListOf<QueueItem>()

    inner class PlayQueueViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        internal var mTitle = itemView.findViewById<View>(R.id.title) as TextView
        internal var mArtist = itemView.findViewById<View>(R.id.artist) as TextView
        internal var mHandle = itemView.findViewById<ImageView>(R.id.handle)

        init {
            itemView.isClickable = true
            itemView.setOnClickListener {
                activity.skipToAndPlayQueueItem(playQueue[layoutPosition].queueId)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayQueueViewHolder {
        return PlayQueueViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.queue_item, parent, false))
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: PlayQueueViewHolder, position: Int) {
        val currentQueueItemDescription = playQueue[position].description

        holder.mTitle.text = currentQueueItemDescription.title
        holder.mArtist.text = currentQueueItemDescription.subtitle

        val textColour = if (playQueue[position].queueId == currentlyPlayingQueueId) {
            ContextCompat.getColor(activity, R.color.icon_color)
        } else MaterialColors.getColor(
            activity, com.google.android.material.R.attr.colorOnSurface, Color.LTGRAY
        )

        holder.mTitle.setTextColor(textColour)
        holder.mArtist.setTextColor(textColour)

        holder.mHandle.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) fragment.startDragging(holder)
            return@setOnTouchListener true
        }
    }

    override fun getItemCount() = playQueue.size

    fun processNewPlayQueue(newPlayQueue: List<QueueItem>) {
        if (newPlayQueue.map { it.queueId } == playQueue.map { it.queueId }) {
            return
        }

        for ((index, queueItem) in newPlayQueue.withIndex()) {
            when {
                index >= playQueue.size -> {
                    playQueue.add(queueItem)
                    notifyItemInserted(index)
                }
                playQueue.find { it.queueId == queueItem.queueId } == null -> {
                    playQueue.add(index, queueItem)
                    notifyItemInserted(index)
                }
                newPlayQueue.find { it.queueId == playQueue[index].queueId } == null -> {
                    var numberOfItemsRemoved = 0
                    do {
                        playQueue.removeAt(index)
                        ++numberOfItemsRemoved
                    } while (index < playQueue.size &&
                        newPlayQueue.find { it.queueId == playQueue[index].queueId } == null)

                    when {
                        numberOfItemsRemoved == 1 -> notifyItemRemoved(index)
                        numberOfItemsRemoved > 1 -> notifyItemRangeRemoved(index,
                            numberOfItemsRemoved)
                    }
                }
            }
        }

        if (playQueue.size > newPlayQueue.size) {
            val numberItemsToRemove = playQueue.size - newPlayQueue.size
            repeat(numberItemsToRemove) { playQueue.removeLast() }
            notifyItemRangeRemoved(newPlayQueue.size, numberItemsToRemove)
        }
    }

    fun changeCurrentlyPlayingQueueItemId(newQueueId: Long) {
        val oldCurrentlyPlayingIndex = playQueue.indexOfFirst {
            it.queueId == currentlyPlayingQueueId
        }

        currentlyPlayingQueueId = newQueueId
        if (oldCurrentlyPlayingIndex != -1) notifyItemChanged(oldCurrentlyPlayingIndex)

        val newCurrentlyPlayingIndex = playQueue.indexOfFirst {
            it.queueId == currentlyPlayingQueueId
        }
        if (newCurrentlyPlayingIndex != -1) {
            notifyItemChanged(newCurrentlyPlayingIndex)
        }
    }
}
