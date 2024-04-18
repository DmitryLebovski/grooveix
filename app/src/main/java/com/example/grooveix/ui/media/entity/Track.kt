package com.example.grooveix.ui.media.entity

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "grooveix_library")
data class Track(
    @PrimaryKey val trackId: Long,
    @ColumnInfo(name = "track_track") var track: Int,
    @ColumnInfo(name = "track_title") var title: String,
    @ColumnInfo(name = "track_artist") var artist: String,
    @ColumnInfo(name = "track_album") var album: String,
    @ColumnInfo(name = "track_album_id") val albumId: String,
    ) : Parcelable