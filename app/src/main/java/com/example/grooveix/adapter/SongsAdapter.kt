package com.example.grooveix.adapter

import android.content.Context
import android.media.MediaMetadataRetriever
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.grooveix.R
import com.example.grooveix.databinding.SongItemBinding
import com.example.grooveix.ui.activity.MainActivity
import com.example.grooveix.model.Song
import com.example.grooveix.util.MusicPlayerRemote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.zhanghai.android.fastscroll.PopupTextProvider
import java.util.*
import kotlin.collections.ArrayList

class SongsAdapter(
    val context: Context,
    private var songList: MutableList<Song>
) : RecyclerView.Adapter<SongsAdapter.SongsViewHolder>(), PopupTextProvider {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongsViewHolder {
        val binding = SongItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SongsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SongsViewHolder, position: Int) {
        val songs = songList[position]
        val activity = context as MainActivity
        activity.lifecycleScope.launch(Dispatchers.IO) {
            val imgByte = getSongThumbnail(songs.path)
            withContext(Dispatchers.Main) {
                holder.bind(songs, imgByte)
            }
        }
        holder.itemView.setOnClickListener {
            MusicPlayerRemote.sendAllSong(songList, position)
        }
    }


    override fun getItemCount(): Int {
        return songList.size
    }

    override fun getPopupText(position: Int): String {
        val songName = songList[position].name
        return getSectionName(songName)
    }

    class SongsViewHolder(private val binding: SongItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(song: Song, imgByte: ByteArray?) {
            binding.tvSongName.text = song.name
            binding.tvArtistName.text = song.artistName
            Glide.with(binding.root.context).asBitmap().load(imgByte).error(R.drawable.ic_album)
                .into(binding.imgSong)
        }
    }


    fun updateSongList(songList: List<Song>) {
        this.songList = ArrayList(songList)
        notifyDataSetChanged()
    }

    private fun getSongThumbnail(songPath: String?): ByteArray? {
        val retriever = MediaMetadataRetriever()
        try {
            if (songPath != null)
                retriever.setDataSource(songPath)
        } catch (e: Exception) {
            Log.e("SongsAdapter", e.message.toString())
        }

        val imgByte = retriever.embeddedPicture
        retriever.release()
        return imgByte
    }

    private fun getSectionName(musicMediaTitle: String?): String {
        var songName = musicMediaTitle
        return try {
            if (TextUtils.isEmpty(songName)) {
                return ""
            }
            songName = songName!!.trim { it <= ' ' }.toLowerCase(Locale.ROOT)
            if (songName.startsWith("the ")) {
                songName = songName.substring(4)
            } else if (songName.startsWith("a ")) {
                songName = songName.substring(2)
            }
            if (songName.isEmpty()) {
                ""
            } else songName.substring(0, 1).toUpperCase(Locale.ROOT)
        } catch (e: Exception) {
            ""
        }
    }
}
