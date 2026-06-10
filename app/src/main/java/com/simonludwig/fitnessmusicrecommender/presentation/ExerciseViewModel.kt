package com.simonludwig.fitnessmusicrecommender.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simonludwig.fitnessmusicrecommender.exercise.ExerciseService
import com.simonludwig.fitnessmusicrecommender.exercise.ExerciseState
import com.simonludwig.fitnessmusicrecommender.messages.WearMessage
import com.simonludwig.fitnessmusicrecommender.messages.WearMessageManager
import com.simonludwig.fitnessmusicrecommender.songs.SongState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ExerciseViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val exerciseState: ExerciseState,
    songState: SongState,
    private val messageManager: WearMessageManager
) : ViewModel() {

    val screenState: StateFlow<ExerciseScreenState> = combine(
        exerciseState.heartRate,
        songState.currentSong
    ) { bpm, song ->
        ExerciseScreenState(
            heartRate = bpm,
            currentSong = song?.let { "${it.title} – ${it.artist}" } ?: "..."
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(1_000), ExerciseScreenState())

    fun togglePlayPause() {
        viewModelScope.launch {
            messageManager.sendPhoneMessage(WearMessage.PlayPause)
        }
    }

    fun skipSong() {
        viewModelScope.launch {
            messageManager.sendPhoneMessage(WearMessage.NextSong)
        }
    }

    fun stopExercise() {
        exerciseState.setExerciseActive(false)
        ExerciseService.stop(context)
    }
}
