package com.example.grooveix.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.grooveix.R
import com.example.grooveix.ui.activity.MainActivity
import com.example.grooveix.ui.media.entity.Playlist
import com.example.grooveix.ui.media.entity.Track
import com.l4digital.fastscroll.FastScroller

class PlaylistAdapter(private val activity: MainActivity):
    RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder>(), FastScroller.SectionIndexer {
    val playlists = mutableListOf<Playlist>()

    inner class PlaylistViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        internal var mArtwork = itemView.findViewById<View>(R.id.artwork) as ImageView
        internal var mTitle = itemView.findViewById<View>(R.id.title) as TextView

        init {
            itemView.isClickable = true
            itemView.setOnClickListener {
                //TODO
            }

            itemView.setOnLongClickListener{
                //TODO
                return@setOnLongClickListener true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        return PlaylistViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.playlist_item, parent, false))
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        val current = playlists[position]

        activity.loadArtwork(current.artwork, holder.mArtwork)
        holder.mTitle.text = current.name
    }
    override fun getItemCount() = playlists.size

    override fun getSectionText(position: Int) = playlists[position].name.firstOrNull()?.toString()

    fun processPlaylists(newPlaylists: List<Playlist>) {
        playlists.clear()
        playlists.addAll(newPlaylists)

        notifyDataSetChanged()
    }

}