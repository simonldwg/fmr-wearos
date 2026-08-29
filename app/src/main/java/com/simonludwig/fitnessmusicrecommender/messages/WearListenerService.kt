package com.simonludwig.fitnessmusicrecommender.messages

import android.content.Intent
import com.simonludwig.fitnessmusicrecommender.exercise.ExerciseController
import com.simonludwig.fitnessmusicrecommender.presentation.MainActivity
import com.simonludwig.fitnessmusicrecommender.songs.SongState
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class WearListenerService : BaseListenerService() {

    @Inject
    lateinit var exerciseController: ExerciseController

    @Inject
    lateinit var songState: SongState

    override fun processMessage(message: WearMessage) {
        when (message) {
            is WearMessage.CurrentSong -> songState.updateSong(message.title, message.artist)
            is WearMessage.StartExercise -> startExercise(message.useBasicExerciseScreen, message.disablePowerOptimization)
            WearMessage.StopExercise -> exerciseController.stop()
            WearMessage.ExerciseStopped -> closeApp()
            else -> throw IllegalArgumentException("Message type not supported")
        }
    }

    private fun startExercise(useBasicExerciseScreen: Boolean, disablePowerOptimization: Boolean) {
        exerciseController.start(useBasicExerciseScreen, disablePowerOptimization)

        // Show app / activity if it is not currently active
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
    }

    private fun closeApp() {
        // TODO
    }
}
