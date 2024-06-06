package com.example.grooveix.ui.media

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.grooveix.ui.media.entity.Playlist
import com.example.grooveix.ui.media.entity.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MusicRepository
    val loadTracks: LiveData<List<Track>>
    val loadTracksArtist: LiveData<List<Track>>
    val loadPlaylists: LiveData<List<Playlist>>

    init {
        val musicRoom = MusicDatabase.getDatabase(application).musicDao()
        repository = MusicRepository(musicRoom)
        loadTracks = repository.loadTracks
        loadTracksArtist = repository.loadTracksArtist
        loadPlaylists = repository.loadPlaylist
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

    fun insertPlaylist(playlist: Playlist) = viewModelScope.launch(Dispatchers.IO) {
        repository.insertPlaylist(playlist)
    }

    fun deletePlaylist(playlistId: Long) = viewModelScope.launch(Dispatchers.IO) {
        repository.deletePlaylist(playlistId)
    }

    fun addTrackToPlaylist(playlistId: Long, trackId: Long) = viewModelScope.launch(Dispatchers.IO) {
        repository.addTrackToPlaylist(playlistId, trackId)
    }

    fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) = viewModelScope.launch(Dispatchers.IO) {
        repository.removeTrackFromPlaylist(playlistId, trackId)
    }

    fun getTracksInPlaylist(playlistId: Long): List<Track> {
        return repository.getTracksInPlaylist(playlistId)
    }

    fun getPlaylistInfo(playlistId: Long): Playlist {
        return repository.getPlaylistInfo(playlistId)
    }

    fun getTracksLikeSearch(query: String): List<Track> {
        return repository.getTracksLikeSearch(query)
    }
}
