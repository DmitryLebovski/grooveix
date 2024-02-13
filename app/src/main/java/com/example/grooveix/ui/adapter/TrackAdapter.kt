package com.example.grooveix.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.codersguidebook.recyclerviewfastscroller.RecyclerViewScrollbar
import com.example.grooveix.R
import com.example.grooveix.ui.activity.MainActivity
import com.example.grooveix.ui.media.Track

class TrackAdapter(private val activity: MainActivity):
    RecyclerView.Adapter<TrackAdapter.SongsViewHolder>(), RecyclerViewScrollbar.ValueLabelListener {
    val songs = mutableListOf<Track>()

    override fun getValueLabelText(position: Int): String {
        return if (songs[position].title.isNotEmpty()) {
            songs[position].title[0].uppercase()
        } else ""
    }

    inner class SongsViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        internal var mArtwork = itemView.findViewById<View>(R.id.artwork) as ImageView
        internal var mTitle = itemView.findViewById<View>(R.id.title) as TextView
        internal var mArtist = itemView.findViewById<View>(R.id.artist) as TextView
        internal var mMenu = itemView.findViewById<Button>(R.id.btnMenu)

        init {
            itemView.isClickable = true
            itemView.setOnClickListener {
                activity.playNewPlayQueue(songs, layoutPosition)
            }

            itemView.setOnLongClickListener{
                activity.showSongPopup(it, songs[layoutPosition])
                return@setOnLongClickListener true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongsViewHolder {
        return SongsViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.track_item, parent, false))
    }

    override fun onBindViewHolder(holder: SongsViewHolder, position: Int) {
        val current = songs[position]

        activity.loadArtwork(current.albumId, holder.mArtwork)

        holder.mTitle.text = current.title
        holder.mArtist.text = current.artist
        holder.mMenu.setOnClickListener {
            activity.showSongPopup(it, current)
        }
    }

    override fun getItemCount() = songs.size

    fun processNewSongs(newSongs: List<Track>) {
        for ((index, song) in newSongs.withIndex()) {
            when {
                songs.isEmpty() -> {
                    songs.addAll(newSongs)
                    notifyItemRangeInserted(0, newSongs.size)
                }
                index >= songs.size -> {
                    songs.add(song)
                    notifyItemInserted(index)
                }
                song.trackId != songs[index].trackId -> {
                    // Check if the song is a new entry to the list
                    val songIsNewEntry = songs.find { it.trackId == song.trackId } == null
                    if (songIsNewEntry) {
                        songs.add(index, song)
                        notifyItemInserted(index)
                        continue
                    }

                    // Check if the song has been removed from the list
                    fun songIdsDoNotMatchAtCurrentIndex(): Boolean {
                        return newSongs.find { it.trackId == songs[index].trackId } == null
                    }

                    if (songIdsDoNotMatchAtCurrentIndex()) {
                        var numberOfItemsRemoved = 0
                        do {
                            songs.removeAt(index)
                            ++numberOfItemsRemoved
                        } while (index < songs.size && songIdsDoNotMatchAtCurrentIndex())

                        when {
                            numberOfItemsRemoved == 1 -> notifyItemRemoved(index)
                            numberOfItemsRemoved > 1 -> notifyItemRangeRemoved(index,
                                numberOfItemsRemoved)
                        }

                        // Check if removing the song(s) has fixed the list
                        if (song.trackId == songs[index].trackId) continue
                    }
                }
                song != songs[index] -> {
                    songs[index] = song
                    notifyItemChanged(index)
                }
            }
        }

        if (songs.size > newSongs.size) {
            val numberItemsToRemove = songs.size - newSongs.size
            repeat(numberItemsToRemove) { songs.removeLast() }
            notifyItemRangeRemoved(newSongs.size, numberItemsToRemove)
        }
    }
}