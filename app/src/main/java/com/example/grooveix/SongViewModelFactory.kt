package com.zakariya.mzmusicplayer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.grooveix.SongViewModel
import com.example.grooveix.repository.SongRepository

@Suppress("UNCHECKED_CAST")
class SongViewModelFactory(private val repository: SongRepository) :
    ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SongViewModel(repository) as T
    }
}