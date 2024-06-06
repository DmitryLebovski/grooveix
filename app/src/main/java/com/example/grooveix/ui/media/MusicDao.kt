package com.example.grooveix.ui.media

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.grooveix.ui.media.entity.Playlist
import com.example.grooveix.ui.media.entity.PlaylistTrackCrossRef
import com.example.grooveix.ui.media.entity.Track

@Dao
interface MusicDao {

    @Delete
    suspend fun delete(track: Track)

    @Query("SELECT * from grooveix_library ORDER BY :choice")
    fun getTrackListOrderBy(choice: String): LiveData<List<Track>>

    @Query("SELECT * from grooveix_library ORDER BY track_title")
    fun getTrackListOrderByTitle(): LiveData<List<Track>>

    @Query("SELECT * from grooveix_library ORDER BY track_artist")
    fun getTrackListOrderByArtist(): LiveData<List<Track>>

    @Query("SELECT * FROM grooveix_library WHERE track_title LIKE :search OR track_artist LIKE :search OR track_album LIKE :search")
    fun getTracksLikeSearch(search: String): List<Track>

    @Query("SELECT gl.trackId,gl.track_track, gl.track_title, gl.track_artist, gl.track_album, gl.track_album_id FROM playlist_tracks pt JOIN grooveix_library gl ON gl.trackId = pt.trackId WHERE playlistId = :playlistId")
    fun getTracksInPlaylist(playlistId: Long): List<Track>

    @Query("SELECT * FROM playlist WHERE playlistId = :playlistId")
    fun getPlaylistInfo(playlistId: Long): Playlist

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylist(playlist: Playlist)

    @Query("DELETE FROM playlist WHERE playlistId = :playlistId")
    fun deletePlaylist(playlistId: Long)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updatePlaylist(playlist: Playlist)

    @Query("SELECT * FROM playlist")
    fun getAllPlaylists(): LiveData<List<Playlist>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addTrackToPlaylist(crossRef: PlaylistTrackCrossRef)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long)

    @Query("SELECT * FROM grooveix_library WHERE trackId = :trackId")
    suspend fun getTrackById(trackId: Long): Track?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(track: Track)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(track: Track)

}
