package com.example.grooveix.ui.media

import androidx.lifecycle.LiveData

class MusicRepository(private val musicRoom: MusicRoom) {

    val loadTracks: LiveData<List<Track>> = musicRoom.getSongListOrderByTitle()

    suspend fun insertTrack(track: Track) {
        musicRoom.insert(track)
    }

    suspend fun deleteTrack(track: Track) {
        musicRoom.delete(track)
    }

    suspend fun updateTrack(track: Track){
        musicRoom.update(track)
    }

    suspend fun getTrackById(trackId: Long): Track? = musicRoom.getTrackById(trackId)
}
