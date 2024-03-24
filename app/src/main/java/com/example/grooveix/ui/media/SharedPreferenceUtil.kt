package com.example.grooveix.ui.media

import android.content.SharedPreferences
import com.example.grooveix.ui.media.Constants.CURRENT_SONG_DURATION_KEY
import com.example.grooveix.ui.media.Constants.POSITION_KEY

object SharedPreferenceUtil {
    fun saveCurrentPosition(sharedPreferences: SharedPreferences, currentPosition: Int) {
        with(sharedPreferences.edit()) {
            putInt(CURRENT_SONG_DURATION_KEY, currentPosition)
            apply()
        }
    }

    fun getPosition(sharedPreferences: SharedPreferences): Int {
        return sharedPreferences.getInt(POSITION_KEY, 0)
    }
}