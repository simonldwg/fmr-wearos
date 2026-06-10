package com.simonludwig.fitnessmusicrecommender.exercise

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.ExerciseUpdate
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.simonludwig.fitnessmusicrecommender.messages.WearMessage
import com.simonludwig.fitnessmusicrecommender.messages.WearMessageManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.simonludwig.fitnessmusicrecommender.R
import com.simonludwig.fitnessmusicrecommender.presentation.MainActivity


@AndroidEntryPoint
class ExerciseService : LifecycleService() {

    @Inject
    lateinit var exerciseManager: ExerciseManager

    @Inject
    lateinit var wearMessageManager: WearMessageManager

    @Inject
    lateinit var exerciseState: ExerciseState

    override fun onCreate() {
        super.onCreate()
        Log.d(LOG_TAG, "Starting exercise service")
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        lifecycleScope.launch {
            exerciseManager.prepareExercise()
            exerciseManager.startExercise()
            collectHeartRate()
            wearMessageManager.sendPhoneMessage(WearMessage.ExerciseStarted)
        }
    }

    private fun collectHeartRate() {
        lifecycleScope.launch {
            exerciseManager.exerciseUpdateFlow
                .catch { e -> Log.e(LOG_TAG, "Error while receiving exercise updates", e) }
                .collect { update -> processUpdate(update) }
        }
    }

    private fun processUpdate(update: ExerciseUpdate) {
        val hrSamples = update.latestMetrics.getData(DataType.HEART_RATE_BPM)

        hrSamples.lastOrNull()?.let { sample ->
            val bpm = sample.value.toInt()

            exerciseState.updateHeartRate(bpm)

            lifecycleScope.launch {
                wearMessageManager.sendPhoneMessage(WearMessage.HeartRate(bpm))
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(LOG_TAG, "Stopping exercise service")

        lifecycleScope.launch {
            exerciseManager.stopExercise()
            wearMessageManager.sendPhoneMessage(WearMessage.ExerciseStopped)
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    private fun buildNotification() : Notification {
        // We want to open the MainActivity whenever the user taps on the notification
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_content))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "exercise_channel"

        const val LOG_TAG = "ExerciseService"

        fun start(context: Context) {
            Intent(context.applicationContext, ExerciseService::class.java).also {
                context.startForegroundService(it)
            }
        }

        fun stop(context: Context) {
            Intent(context.applicationContext, ExerciseService::class.java).also {
                context.stopService(it)
            }
        }
    }
}
