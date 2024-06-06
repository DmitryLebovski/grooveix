package com.example.grooveix.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.grooveix.R
import com.example.grooveix.ui.activity.MainActivity
import com.example.grooveix.ui.media.entity.Playlist

class LitePlaylistAdapter(
    private val activity: MainActivity,
    private val onClick: (Playlist) -> Unit
) : RecyclerView.Adapter<LitePlaylistAdapter.LitePlaylistViewHolder>() {
    val playlists = mutableListOf<Playlist>()
    class LitePlaylistViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        internal var mArtwork = itemView.findViewById<View>(R.id.artwork) as ImageView
        internal var mTitle = itemView.findViewById<View>(R.id.title) as TextView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LitePlaylistViewHolder {
        return LitePlaylistViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.lite_playlist_item, parent, false))
    }

    override fun onBindViewHolder(holder: LitePlaylistViewHolder, position: Int) {
        val current = playlists[position]

        activity.loadArtwork(current.artwork, holder.mArtwork)
        holder.mTitle.text = current.name

        holder.itemView.setOnClickListener {
            onClick(current)
        }
    }

    override fun getItemCount() = playlists.size

    fun processPlaylists(newPlaylists: List<Playlist>) {
        playlists.clear()
        playlists.addAll(newPlaylists)

        notifyDataSetChanged()
    }
}
