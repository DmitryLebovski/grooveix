package com.example.grooveix.ui.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.grooveix.R
import com.example.grooveix.ui.activity.MainActivity
import com.example.grooveix.ui.fragment.PlaylistTracksFragment
import com.example.grooveix.ui.media.MusicDatabase
import com.example.grooveix.ui.media.entity.Playlist
import com.l4digital.fastscroll.FastScroller
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PlaylistAdapter(private val activity: MainActivity):
    RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder>(), FastScroller.SectionIndexer {
    val playlists = mutableListOf<Playlist>()
    private var musicDatabase: MusicDatabase? = null

    inner class PlaylistViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        internal var mArtwork = itemView.findViewById<View>(R.id.artwork) as ImageView
        internal var mTitle = itemView.findViewById<View>(R.id.title) as TextView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        return PlaylistViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.playlist_item, parent, false))
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        musicDatabase = MusicDatabase.getDatabase(activity)

        val current = playlists[position]

        activity.loadArtwork(current.artwork, holder.mArtwork)
        holder.mTitle.text = current.name

        holder.itemView.setOnClickListener {
            val fragment = PlaylistTracksFragment().apply {
                arguments = Bundle().apply {
                    putLong("playlistId", current.playlistId)
                }
            }
            activity.supportFragmentManager.beginTransaction().replace(R.id.playlist_view, fragment).commit()
        }
        holder.itemView.setOnLongClickListener {
            AlertDialog.Builder(activity)
                .setTitle(activity.getResources().getString(R.string.delete_playlist))
                .setNegativeButton(activity.getResources().getString(R.string.cancel), null)
                .setPositiveButton(activity.getResources().getString(R.string.delete)) { dialog, id ->
                    activity.lifecycleScope.launch(Dispatchers.IO) {
                        musicDatabase!!.musicDao().deletePlaylist(current.playlistId)
                    }
                    Toast.makeText(activity, activity.getResources().getString(R.string.delete_playlist_success), Toast.LENGTH_LONG).show()
                    dialog.dismiss()
                }
                .show()
            return@setOnLongClickListener true
        }
    }
    override fun getItemCount() = playlists.size

    override fun getSectionText(position: Int) = playlists[position].name.firstOrNull()?.toString()

    fun processPlaylists(newPlaylists: List<Playlist>) {
        playlists.clear()
        playlists.addAll(newPlaylists)

        notifyDataSetChanged()
    }
}