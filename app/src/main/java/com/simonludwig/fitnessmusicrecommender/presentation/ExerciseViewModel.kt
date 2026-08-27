package com.simonludwig.fitnessmusicrecommender.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simonludwig.fitnessmusicrecommender.exercise.ExerciseController
import com.simonludwig.fitnessmusicrecommender.exercise.ExerciseState
import com.simonludwig.fitnessmusicrecommender.messages.WearMessage
import com.simonludwig.fitnessmusicrecommender.messages.WearMessageManager
import com.simonludwig.fitnessmusicrecommender.songs.SongState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ExerciseViewModel @Inject constructor(
    private val exerciseController: ExerciseController,
    exerciseState: ExerciseState,
    private val songState: SongState,
    private val messageManager: WearMessageManager
) : ViewModel() {

    // tracks whether a song skip is pending, i.e. the user has tapped "skip" but a new song has not
    // been received from the phone yet
    private val isSkipPending = MutableStateFlow(false)
    private var skipTimeoutJob: Job? = null

    val screenState: StateFlow<ExerciseScreenState> = combine(
        exerciseState.heartRate,
        songState.currentSong,
        isSkipPending
    ) { bpm, song, skipPending ->
        ExerciseScreenState(
            heartRate = bpm,
            currentSong = song?.let { "${it.title} – ${it.artist}" } ?: "...",
            // if a current song exists and no skip is pending, the skip button will be enabled
            isSkipEnabled = song != null && !skipPending
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(1_000), ExerciseScreenState())

    init {
        // reset skip lock when a new song is received
        viewModelScope.launch {
            songState.currentSong
                .drop(1) // ignore initial value while the viewmodel is initialized
                .collect {
                    skipTimeoutJob?.cancel()
                    isSkipPending.value = false
                }
        }
    }

    fun togglePlayPause() {
        viewModelScope.launch {
            messageManager.sendPhoneMessage(WearMessage.PlayPause)
        }
    }

    fun skipSong() {
        if (isSkipPending.value) return

        isSkipPending.value = true
        viewModelScope.launch {
            messageManager.sendPhoneMessage(WearMessage.NextSong)
        }

        // timeout in case no new song is received by the phone -> re-enable the skip button after
        // 10 seconds
        skipTimeoutJob?.cancel()
        skipTimeoutJob = viewModelScope.launch {
            delay(10_000)
            isSkipPending.value = false
        }
    }

    fun stopExercise() {
        exerciseController.stop()
    }
}
