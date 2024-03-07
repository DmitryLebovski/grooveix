package com.example.grooveix.ui.media

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MusicRepository
    val loadTracks: LiveData<List<Track>>

    init {
        val musicRoom = MusicDatabase.getDatabase(application).musicDao()
        repository = MusicRepository(musicRoom)
        loadTracks = repository.loadTracks
    }

    fun deleteTrack(track: Track) = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteTrack(track)
    }

    fun insertTrack(track: Track) = viewModelScope.launch(Dispatchers.IO) {
        repository.insertTrack(track)
    }

    fun updateTrack(track: Track) = viewModelScope.launch(Dispatchers.IO) {
        repository.updateTrack(track)
    }

    suspend fun getTrackById(trackId: Long) : Track? = repository.getTrackById(trackId)
}
