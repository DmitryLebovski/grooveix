package com.example.grooveix.ui.media

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "grooveix_library")
data class Song(
    @PrimaryKey val songId: Long,
    @ColumnInfo(name = "song_track") var track: Int,
    @ColumnInfo(name = "song_title") var title: String,
    @ColumnInfo(name = "song_artist") var artist: String,
    @ColumnInfo(name = "song_album") var album: String,
    @ColumnInfo(name = "song_album_id") val albumId: String,
    @ColumnInfo(name = "song_year") var year: String
) : Parcelable
