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
import androidx.recyclerview.widget.RecyclerView
import com.example.grooveix.R
import com.example.grooveix.ui.activity.MainActivity
import com.example.grooveix.ui.fragment.QueueFragment
import com.google.android.material.color.MaterialColors

class QueueAdapter(private val activity: MainActivity, private val fragment: QueueFragment): RecyclerView.Adapter<QueueAdapter.QueueViewHolder>() {
    var currentlyPlayingQueueId = -1L
    var playQueue = mutableListOf<QueueItem>()

    inner class QueueViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        internal var mTitle = itemView.findViewById<View>(R.id.title) as TextView
        internal var mArtist = itemView.findViewById<View>(R.id.artist) as TextView
        internal var mHandle = itemView.findViewById<ImageView>(R.id.handle)
        //internal var mMenu = itemView.findViewById<ImageButton>(R.id.menu)

        init {
            itemView.isClickable = true
            itemView.setOnClickListener {
                activity.skipToAndPlayQueueItem(playQueue[layoutPosition].queueId)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QueueViewHolder {
        return QueueViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.queue_item, parent, false))
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: QueueViewHolder, position: Int) {
        val currentQueueItemDescription = playQueue[position].description

        holder.mTitle.text = currentQueueItemDescription.title
        holder.mArtist.text = currentQueueItemDescription.subtitle

        val textColour = if (playQueue[position].queueId == currentlyPlayingQueueId) {
            MaterialColors.getColor(
                activity, com.google.android.material.R.attr.colorAccent, Color.CYAN
            )
        } else MaterialColors.getColor(
            activity, com.google.android.material.R.attr.colorOnSurface, Color.LTGRAY
        )

        holder.mTitle.setTextColor(textColour)
        holder.mArtist.setTextColor(textColour)

        holder.mHandle.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) fragment.startDragging(holder)
            return@setOnTouchListener true
        }

//        holder.mViewQueue.setOnLongClickListener {
//            fragment.showPopup(it, playQueue[position].queueId)
//            return@setOnLongClickListener false
//        }
    }

    override fun getItemCount() = playQueue.size

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
