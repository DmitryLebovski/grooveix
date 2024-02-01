package com.example.grooveix.ui.media

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface MusicRoom {

    @Delete
    suspend fun delete(track: Track)

    @Query("SELECT * from grooveix_library ORDER BY track_title")
    fun getSongsOrderByTitle(): LiveData<List<Track>>

    @Query("SELECT * from grooveix_library ORDER BY track_artist")
    fun getSongsOrderByArtist(): LiveData<List<Track>>

    @Query("SELECT * FROM grooveix_library WHERE track_title LIKE :search OR track_artist LIKE :search OR track_album LIKE :search")
    suspend fun getSongsLikeSearch(search: String): List<Track>

    @Query("SELECT * FROM grooveix_library WHERE trackId = :trackId")
    suspend fun getSongById(trackId: Long): Track?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(track: Track)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(track: Track)
}
