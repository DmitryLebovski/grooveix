package com.example.grooveix.ui.media

import androidx.lifecycle.LiveData

class MusicRepository(private val musicRoom: MusicRoom) {

    val loadTracks: LiveData<List<Track>> = musicRoom.getSongsOrderByTitle()

    suspend fun insertTrack(song: Track) {
        musicRoom.insert(song)
    }

    suspend fun deleteSong(song: Track) {
        musicRoom.delete(song)
    }

    suspend fun updateSong(song: Track){
        musicRoom.update(song)
    }

    suspend fun getSongById(songId: Long): Track? = musicRoom.getSongById(songId)
}
