package com.example.grooveix.ui.media.entity

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.ForeignKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "playlist_tracks",
    primaryKeys = ["playlistId", "trackId"],
    foreignKeys = [
        ForeignKey(entity = Playlist::class, parentColumns = ["playlistId"], childColumns = ["playlistId"]),
        ForeignKey(entity = Track::class, parentColumns = ["trackId"], childColumns = ["trackId"])
    ])
data class PlaylistTrackCrossRef(
    val playlistId: Long,
    val trackId: Long
) : Parcelable

