package com.simonludwig.fitnessmusicrecommender.messages

import android.content.Intent
import com.simonludwig.fitnessmusicrecommender.exercise.ExerciseService
import com.simonludwig.fitnessmusicrecommender.exercise.ExerciseState
import com.simonludwig.fitnessmusicrecommender.presentation.MainActivity
import com.simonludwig.fitnessmusicrecommender.songs.SongState
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class WearListenerService : BaseListenerService() {

    @Inject
    lateinit var exerciseState: ExerciseState

    @Inject
    lateinit var songState: SongState

    override fun processMessage(message: WearMessage) {
        when (message) {
            is WearMessage.CurrentSong -> updateCurrentSong(message.title, message.artist)
            WearMessage.StartExercise -> startExercise()
            WearMessage.StopExercise -> stopExercise()
            WearMessage.ExerciseStopped -> closeApp()
            else -> throw IllegalArgumentException("Message type not supported")
        }
    }

    private fun startExercise() {
        ExerciseService.start(exerciseState, applicationContext)

        // Show app / activity if it is not currently active
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)

    }

    private fun stopExercise() {
        ExerciseService.stop(exerciseState, applicationContext)
    }

    private fun updateCurrentSong(title: String, artist: String) {
        songState.updateSong(title, artist)
    }

    private fun closeApp() {
        // TODO
    }
}
