package com.example.grooveix.ui.media

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Track::class], version = 1, exportSchema = false)
abstract class MusicDatabase : RoomDatabase() {

    abstract fun musicRoom(): MusicDao

    companion object {
        @Volatile
        private var database: MusicDatabase? = null

        fun getDatabase(context: Context): MusicDatabase {
            database ?: kotlin.run {
                database = Room.databaseBuilder(context, MusicDatabase::class.java, "grooveix_library")
                    .build()
            }

            return database!!
        }
    }
}
