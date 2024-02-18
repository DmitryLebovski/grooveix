package com.example.grooveix.ui.media

import androidx.lifecycle.LiveData

class MusicRepository(private val musicRoom: MusicDao) {

    val loadTracks: LiveData<List<Track>> = musicRoom.getSongListOrderByTitle()

    suspend fun insertTrack(track: Track) {
        musicRoom.insert(track)
    }

//    suspend fun insertLyrics(lyrics: String) {
//        musicRoom.insertLyrics(lyrics)
//    }

    suspend fun deleteTrack(track: Track) {
        musicRoom.delete(track)
    }

    suspend fun updateTrack(track: Track) {
        musicRoom.update(track)
    }

//    suspend fun getLyricsById(lyrics: String): String? = musicRoom.getLyricsById(lyrics)

    suspend fun getTrackById(trackId: Long): Track? = musicRoom.getTrackById(trackId)
}
