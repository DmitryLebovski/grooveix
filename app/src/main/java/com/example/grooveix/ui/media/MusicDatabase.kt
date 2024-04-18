package com.example.grooveix.ui.media

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.grooveix.ui.media.entity.Playlist
import com.example.grooveix.ui.media.entity.PlaylistTrackCrossRef
import com.example.grooveix.ui.media.entity.Track

@Database(entities = [Track::class, Playlist::class, PlaylistTrackCrossRef::class], version = 2, exportSchema = false)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun musicDao(): MusicDao

    companion object {
        @Volatile
        private var database: MusicDatabase? = null

        fun getDatabase(context: Context): MusicDatabase {
            database ?: kotlin.run {
                database = Room.databaseBuilder(context, MusicDatabase::class.java, "music_database")
//                    .fallbackToDestructiveMigration()
                    .build()
            }

            return database!!
        }
    }
}