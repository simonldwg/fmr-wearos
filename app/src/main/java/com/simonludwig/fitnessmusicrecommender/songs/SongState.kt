package com.simonludwig.fitnessmusicrecommender.songs

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Song(val title: String, val artist: String)

@Singleton
class SongState @Inject constructor() {
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong = _currentSong.asStateFlow()


    fun updateSong(title: String, artist: String) {
        _currentSong.value = Song(title, artist)
    }

}
