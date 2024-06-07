package com.example.grooveix

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.example.grooveix.ui.media.MusicDao
import com.example.grooveix.ui.media.MusicDatabase
import com.example.grooveix.ui.media.entity.Playlist
import com.example.grooveix.ui.media.entity.PlaylistTrackCrossRef
import com.example.grooveix.ui.media.entity.Track
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class MusicDaoTest {

    private lateinit var database: MusicDatabase
    private lateinit var musicDao: MusicDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MusicDatabase::class.java
        ).allowMainThreadQueries().build()

        musicDao = database.musicDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertTrackAndRetrieve() = runBlocking {
        val track = Track(1, 1, "Song Title", "Artist", "Album", "AlbumId")
        musicDao.insert(track)

        val retrievedTrack = musicDao.getTrackById(1)
        assertThat(retrievedTrack).isEqualTo(track)
    }

    @Test
    fun insertPlaylistAndRetrieve() = runBlocking {
        val playlist = Playlist(1, "My Playlist", "artwork.png")
        musicDao.insertPlaylist(playlist)

        val retrievedPlaylist = musicDao.getPlaylistInfo(1)
        assertThat(retrievedPlaylist).isEqualTo(playlist)
    }

    @Test
    fun addTrackToPlaylist() = runBlocking {
        val track = Track(1, 1, "Song Title", "Artist", "Album", "AlbumId")
        val playlist = Playlist(1, "My Playlist", "artwork.png")
        musicDao.insert(track)
        musicDao.insertPlaylist(playlist)
        musicDao.addTrackToPlaylist(PlaylistTrackCrossRef(1, 1))

        val tracksInPlaylist = musicDao.getTracksInPlaylist(1)
        assertThat(tracksInPlaylist).contains(track)
    }
}
