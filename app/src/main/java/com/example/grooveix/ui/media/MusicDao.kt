package com.example.grooveix.ui.media

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface MusicDao {

    @Delete
    suspend fun delete(track: Track)

    @Query("SELECT * from grooveix_library ORDER BY track_title")
    fun getSongListOrderByTitle(): LiveData<List<Track>>

    @Query("SELECT * from grooveix_library ORDER BY track_artist")
    fun getSongListOrderByArtist(): LiveData<List<Track>>

    @Query("SELECT * FROM grooveix_library WHERE track_title LIKE :search OR track_artist LIKE :search OR track_album LIKE :search")
    suspend fun getSongListLikeSearch(search: String): List<Track>

    @Query("SELECT * FROM grooveix_library WHERE trackId = :trackId")
    suspend fun getTrackById(trackId: Long): Track?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(track: Track)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(track: Track)

//    @Query("SELECT * FROM grooveix_library WHERE track_lyrics = :lyrics")
//    suspend fun getLyricsById(lyrics: String): String?

//    @Insert(onConflict = OnConflictStrategy.IGNORE)
//    suspend fun insertLyrics(lyrics: String)
}
