package com.simonludwig.fitnessmusicrecommender.exercise

import android.content.Context
import com.simonludwig.fitnessmusicrecommender.songs.SongState
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExerciseController @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val exerciseState: ExerciseState,
    private val songState: SongState
) {
    fun start(useBasicExerciseScreen: Boolean = false) {
        exerciseState.activate()
        exerciseState.setUseBasicExerciseScreen(useBasicExerciseScreen)
        ExerciseService.start(context)
    }

    fun stop() {
        ExerciseService.stop(context)
        songState.clear()
        exerciseState.deactivateAndClear()
    }
}
