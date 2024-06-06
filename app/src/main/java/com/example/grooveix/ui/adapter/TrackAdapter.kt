package com.example.grooveix.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.grooveix.R
import com.example.grooveix.ui.activity.MainActivity
import com.example.grooveix.ui.media.entity.Track
import com.l4digital.fastscroll.FastScroller

class TrackAdapter(private val activity: MainActivity):
    RecyclerView.Adapter<TrackAdapter.TracksViewHolder>(), FastScroller.SectionIndexer {
    val tracks = mutableListOf<Track>()

    inner class TracksViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

            internal var mArtwork = itemView.findViewById<View>(R.id.artwork) as ImageView
            internal var mTitle = itemView.findViewById<View>(R.id.title) as TextView
            internal var mArtist = itemView.findViewById<View>(R.id.artist) as TextView
            internal var mMenu = itemView.findViewById<ImageButton>(R.id.btnMenu)

            init {
                itemView.isClickable = true
                itemView.setOnClickListener {
                    activity.playNewPlayQueue(tracks, layoutPosition)
                    activity.showPlayer()
                }

                itemView.setOnLongClickListener{
                    activity.showTrackPopup(tracks[layoutPosition])
                    return@setOnLongClickListener true
                }
            }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TracksViewHolder {
        return TracksViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.track_item, parent, false))
    }

    override fun onBindViewHolder(holder: TracksViewHolder, position: Int) {
        val current = tracks[position]

        activity.loadArtwork(current.albumId, holder.mArtwork)

        holder.mTitle.text = current.title
        holder.mArtist.text = current.artist
        holder.mMenu.setOnClickListener {
            activity.showTrackPopup(current)
        }
    }
    override fun getItemCount() = tracks.size

    fun processNewTracks(newTracks: List<Track>) {
        for ((index, track) in newTracks.withIndex()) {
            when {
                tracks.isEmpty() -> {
                    tracks.addAll(newTracks)
                    notifyItemRangeInserted(0, newTracks.size)
                }
                index >= tracks.size -> {
                    tracks.add(track)
                    notifyItemInserted(index)
                }
                track.trackId != tracks[index].trackId -> {
                    val trackIsNewEntry = tracks.find { it.trackId == track.trackId } == null
                    if (trackIsNewEntry) {
                        tracks.add(index, track)
                        notifyItemInserted(index)
                        continue
                    }

                    fun trackIdsDoNotMatchAtCurrentIndex(): Boolean {
                        return newTracks.find { it.trackId == tracks[index].trackId } == null
                    }

                    if (trackIdsDoNotMatchAtCurrentIndex()) {
                        var numberOfItemsRemoved = 0
                        do {
                            tracks.removeAt(index)
                            ++numberOfItemsRemoved
                        } while (index < tracks.size && trackIdsDoNotMatchAtCurrentIndex())

                        when {
                            numberOfItemsRemoved == 1 -> notifyItemRemoved(index)
                            numberOfItemsRemoved > 1 -> notifyItemRangeRemoved(index,
                                numberOfItemsRemoved)
                        }

                        if (track.trackId == tracks[index].trackId) continue
                    }
                }
                track != tracks[index] -> {
                    tracks[index] = track
                    notifyItemChanged(index)
                }
            }
        }

        if (tracks.size > newTracks.size) {
            val numberItemsToRemove = tracks.size - newTracks.size
            repeat(numberItemsToRemove) { tracks.removeLast() }
            notifyItemRangeRemoved(newTracks.size, numberItemsToRemove)
        }
    }

    override fun getSectionText(position: Int) = tracks[position].title.firstOrNull()?.toString()
}