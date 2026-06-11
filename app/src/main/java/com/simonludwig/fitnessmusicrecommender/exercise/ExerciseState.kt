package com.simonludwig.fitnessmusicrecommender.exercise

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow


@Singleton
class ExerciseState @Inject constructor() {
    private val _isExerciseActive = MutableStateFlow(false)
    val isExerciseActive = _isExerciseActive.asStateFlow()

    private val _heartRate = MutableStateFlow<Int?>(null)
    val heartRate = _heartRate.asStateFlow()

    private val _useBasicExerciseScreen = MutableStateFlow(false)
    val useBasicExerciseScreen = _useBasicExerciseScreen.asStateFlow()


    fun activate() {
        _isExerciseActive.value = true
    }

    fun deactivateAndClear() {
        _isExerciseActive.value = false
        // reset value so the old HR value from the previous exercise does not get used during
        // the next exercise
        _heartRate.value = null
        _useBasicExerciseScreen.value = false
    }

    fun updateHeartRate(bpm: Int) {
        _heartRate.value = bpm
    }

    fun setUseBasicExerciseScreen(value: Boolean) {
        _useBasicExerciseScreen.value = value
    }

}
