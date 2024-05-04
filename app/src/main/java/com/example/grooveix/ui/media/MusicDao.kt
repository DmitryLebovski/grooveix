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
    fun getSongListOrderBy(choice: String): LiveData<List<Track>>

    @Query("SELECT * from grooveix_library ORDER BY track_title")
    fun getSongListOrderByTitle(): LiveData<List<Track>>

    @Query("SELECT * from grooveix_library ORDER BY track_artist")
    fun getSongListOrderByArtist(): LiveData<List<Track>>

    @Query("SELECT * FROM grooveix_library WHERE track_title LIKE :search OR track_artist LIKE :search OR track_album LIKE :search")
    suspend fun getTracksLikeSearch(search: String): List<Track>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylist(playlist: Playlist)

    @Delete
    suspend fun deletePlaylist(playlist: Playlist)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updatePlaylist(playlist: Playlist)

    @Query("SELECT * FROM playlist")
    fun getAllPlaylists(): LiveData<List<Playlist>>

    @Insert
    suspend fun addTrackToPlaylist(crossRef: PlaylistTrackCrossRef)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long)

    @Query("SELECT * FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun getTracksInPlaylist(playlistId: Long): List<PlaylistTrackCrossRef>

    @Query("SELECT * FROM grooveix_library WHERE trackId = :trackId")
    suspend fun getTrackById(trackId: Long): Track?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(track: Track)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(track: Track)
}
