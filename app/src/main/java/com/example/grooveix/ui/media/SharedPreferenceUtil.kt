package com.example.grooveix.ui.media

import android.content.SharedPreferences

object SharedPreferenceUtil {
    fun saveCurrent(sharedPreferences: SharedPreferences, value: Int) {
        with(sharedPreferences.edit()) {
            putInt("playlistID", value)
            apply()
        }
    }

    fun getValue(sharedPreferences: SharedPreferences): Int {
        return sharedPreferences.getInt("playlistID", 0)
    }
}