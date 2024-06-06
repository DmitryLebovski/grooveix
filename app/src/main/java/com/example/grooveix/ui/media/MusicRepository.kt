package com.example.grooveix.ui.media

import androidx.lifecycle.LiveData
import com.example.grooveix.ui.media.entity.Playlist
import com.example.grooveix.ui.media.entity.PlaylistTrackCrossRef
import com.example.grooveix.ui.media.entity.Track

class MusicRepository(private val musicRoom: MusicDao) {

    val loadTracks: LiveData<List<Track>> = musicRoom.getTrackListOrderByTitle()
    val loadTracksArtist: LiveData<List<Track>> = musicRoom.getTrackListOrderByArtist()
    val loadPlaylist: LiveData<List<Playlist>> = musicRoom.getAllPlaylists()


    suspend fun insertTrack(track: Track) {
        musicRoom.insert(track)
    }

    suspend fun deleteTrack(track: Track) {
        musicRoom.delete(track)
    }

    suspend fun updateTrack(track: Track) {
        musicRoom.update(track)
    }

    suspend fun getTrackById(trackId: Long): Track? = musicRoom.getTrackById(trackId)

    suspend fun insertPlaylist(playlist: Playlist) {
        musicRoom.insertPlaylist(playlist)
    }

    fun deletePlaylist(playlistId: Long) {
        musicRoom.deletePlaylist(playlistId)
    }

    suspend fun updatePlaylist(playlist: Playlist) {
        musicRoom.updatePlaylist(playlist)
    }

    fun getAllPlaylists(): LiveData<List<Playlist>> {
        return musicRoom.getAllPlaylists()
    }

    suspend fun addTrackToPlaylist(playlistId: Long, trackId: Long) {
        musicRoom.addTrackToPlaylist(PlaylistTrackCrossRef(playlistId, trackId))
    }

    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) {
        musicRoom.removeTrackFromPlaylist(playlistId, trackId)
    }

    fun getTracksInPlaylist(playlistId: Long): List<Track> {
        return musicRoom.getTracksInPlaylist(playlistId)
    }

    fun getPlaylistInfo(playlistId: Long): Playlist {
        return musicRoom.getPlaylistInfo(playlistId)
    }

    fun getTracksLikeSearch(query: String): List<Track> {
        return musicRoom.getTracksLikeSearch(query)
    }
}
