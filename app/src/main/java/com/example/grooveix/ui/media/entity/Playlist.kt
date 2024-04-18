package com.example.grooveix.ui.media.entity

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "playlist")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val playlistId: Long,
    @ColumnInfo(name = "playlist_name") val name: String,
    @ColumnInfo(name = "playlist_artwork") val artwork: String
) : Parcelable
