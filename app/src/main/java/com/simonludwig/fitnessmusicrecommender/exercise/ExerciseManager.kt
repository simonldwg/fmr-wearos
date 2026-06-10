package com.simonludwig.fitnessmusicrecommender.exercise

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.health.services.client.ExerciseUpdateCallback
import androidx.health.services.client.HealthServices
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.BatchingMode
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DataTypeAvailability
import androidx.health.services.client.data.ExerciseConfig
import androidx.health.services.client.data.ExerciseLapSummary
import androidx.health.services.client.data.ExerciseTrackedStatus
import androidx.health.services.client.data.ExerciseType
import androidx.health.services.client.data.ExerciseUpdate
import androidx.health.services.client.data.WarmUpConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.guava.await

@Singleton
class ExerciseManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val exerciseClient = HealthServices.getClient(context).exerciseClient

    suspend fun supportsHeartRate(): Boolean {
        val capabilities = exerciseClient.getCapabilitiesAsync().await()

        if (ExerciseType.WORKOUT in capabilities.supportedExerciseTypes) {
            val dataTypes = capabilities.getExerciseTypeCapabilities(ExerciseType.WORKOUT).supportedDataTypes
            return DataType.HEART_RATE_BPM in dataTypes
        }

        return false
    }

    @SuppressLint("RestrictedApi")
    private suspend fun endActiveExerciseIfExists() {
        val currentExerciseInfo = exerciseClient.getCurrentExerciseInfoAsync().await()

        // If an exercise is already in progress, terminate it
        @Suppress("WrongConstant")
        if (currentExerciseInfo.exerciseTrackedStatus == ExerciseTrackedStatus.OWNED_EXERCISE_IN_PROGRESS) {
            exerciseClient.endExerciseAsync().await()
        }
    }

    suspend fun startExercise() {
        endActiveExerciseIfExists()

        // The batching rate needs to be increased to receive heart rate updates more frequently
        // while the device is sleeping
        val batchingModeOverrides = mutableSetOf<BatchingMode>()

        // Firstly, check if receiving a heart rate update roughly every 5 seconds is supported
        val capabilities = exerciseClient.getCapabilitiesAsync().await()

        if(BatchingMode.HEART_RATE_5_SECONDS in capabilities.supportedBatchingModeOverrides) {
            // If the increased rate is supported, override the default HR batching mode
            batchingModeOverrides.add(BatchingMode.HEART_RATE_5_SECONDS)
        }

        val config = ExerciseConfig(
            exerciseType = ExerciseType.WORKOUT,
            dataTypes = setOf(
                DataType.HEART_RATE_BPM
            ),
            isAutoPauseAndResumeEnabled = false,
            isGpsEnabled = false,
            batchingModeOverrides = batchingModeOverrides
        )
        exerciseClient.startExerciseAsync(config).await()
        Log.d(LOG_TAG, "Exercise started")
    }

    suspend fun prepareExercise() {
        endActiveExerciseIfExists()

        Log.d(LOG_TAG, "Preparing an exercise")
        val warmUpConfig =
            WarmUpConfig(
                exerciseType = ExerciseType.WORKOUT,
                dataTypes = setOf(
                    DataType.HEART_RATE_BPM
                )
            )
        exerciseClient.prepareExerciseAsync(warmUpConfig).await()
    }

    suspend fun stopExercise() {
        exerciseClient.endExerciseAsync().await()
    }

    val exerciseUpdateFlow = callbackFlow {
        val callback =
            object : ExerciseUpdateCallback {
                override fun onExerciseUpdateReceived(update: ExerciseUpdate) {
                    trySend(update)
                }

                override fun onLapSummaryReceived(lapSummary: ExerciseLapSummary) {}

                override fun onRegistered() {
                    Log.d(LOG_TAG, "Update callback registered")
                }

                override fun onRegistrationFailed(throwable: Throwable) {
                    close(throwable)
                }

                override fun onAvailabilityChanged(
                    dataType: DataType<*, *>,
                    availability: Availability
                ) {
                    val status = when (availability) {
                        DataTypeAvailability.AVAILABLE -> "Sensor available..."
                        DataTypeAvailability.ACQUIRING -> "Acquiring sensor..."
                        DataTypeAvailability.UNAVAILABLE -> "Sensor unavailable!"
                        DataTypeAvailability.UNAVAILABLE_DEVICE_OFF_BODY -> "Sensor unavailable: device off body!"
                        else -> "Unknown: $availability"
                    }
                    Log.d(LOG_TAG, "[$dataType] availability: $status")
                }
            }

        exerciseClient.setUpdateCallback(callback)

        awaitClose {
            exerciseClient.clearUpdateCallbackAsync(callback)
        }
    }


    companion object {
        const val LOG_TAG = "ExerciseManager"
    }
}
