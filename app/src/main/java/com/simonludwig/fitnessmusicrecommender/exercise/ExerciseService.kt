package com.simonludwig.fitnessmusicrecommender.exercise

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.ExerciseUpdate
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
import com.simonludwig.fitnessmusicrecommender.messages.WearMessage
import com.simonludwig.fitnessmusicrecommender.messages.WearMessageManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.simonludwig.fitnessmusicrecommender.R
import com.simonludwig.fitnessmusicrecommender.presentation.MainActivity
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.ExperimentalTime
import kotlin.time.toKotlinInstant


@AndroidEntryPoint
class ExerciseService : LifecycleService() {

    @Inject
    lateinit var exerciseClientManager: ExerciseClientManager

    @Inject
    lateinit var wearMessageManager: WearMessageManager

    @Inject
    lateinit var exerciseState: ExerciseState

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(LOG_TAG, "Starting exercise service")
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, postOngoingActivityAndBuildNotification())

        if (exerciseState.disablePowerOptimization.value) {
            acquireWakeLock()
        }

        lifecycleScope.launch {
            exerciseClientManager.prepareExercise()
            exerciseClientManager.startExercise()
            collectUpdates()
            wearMessageManager.sendPhoneMessage(WearMessage.ExerciseStarted)
        }
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "$LOG_TAG:ExerciseWakeLock"
        ).apply {
            // acquire the wake lock for a maximum of 1h
            acquire(3600000L)
        }
        Log.d(LOG_TAG, "WakeLock acquired")
    }


    private fun collectUpdates() {
        lifecycleScope.launch {
            exerciseClientManager.exerciseUpdateFlow
                .catch { e -> Log.e(LOG_TAG, "Error while receiving exercise updates", e) }
                .collect { update -> processUpdate(update) }
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun processUpdate(update: ExerciseUpdate) {

        update.latestMetrics.getData(DataType.HEART_RATE_BPM).lastOrNull()?.let { sample ->
            val bpm = sample.value.toInt()
            exerciseState.updateHeartRate(bpm)
            lifecycleScope.launch {
                wearMessageManager.sendPhoneMessage(WearMessage.HeartRate(bpm))
            }
        }

        update.latestMetrics.getData(DataType.STEPS_PER_MINUTE).lastOrNull()?.let { sample ->
            lifecycleScope.launch {
                wearMessageManager.sendPhoneMessage(WearMessage.StepsPerMinute(sample.value))
            }
        }

        update.latestMetrics.getData(DataType.STEPS_PER_MINUTE_STATS)?.let { stats ->
            lifecycleScope.launch {
                wearMessageManager.sendPhoneMessage(
                    WearMessage.StepsPerMinuteStats(
                        min = stats.min,
                        max = stats.max,
                        average = stats.average,
                        startTime = stats.start.toKotlinInstant(),
                        endTime = stats.end.toKotlinInstant()
                    )
                )
            }
        }

        update.latestMetrics.getData(DataType.STEPS_TOTAL)?.let { cumulative ->
            lifecycleScope.launch {
                wearMessageManager.sendPhoneMessage(WearMessage.RunningStepsTotal(cumulative.total))
            }
        }
    }

    override fun onDestroy() {
        Log.d(LOG_TAG, "Stopping exercise service")

        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null

        runBlocking {
            withTimeoutOrNull(3000) {
                exerciseClientManager.stopExercise()
                wearMessageManager.sendPhoneMessage(WearMessage.ExerciseStopped)
            }
        }

        super.onDestroy()
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

    private fun buildOngoingActivity(notificationBuilder: NotificationCompat.Builder, intent: PendingIntent) {
        val runStartTime = SystemClock.elapsedRealtime()

        val ongoingActivityStatus = Status.Builder()
            .addTemplate(applicationContext.getString(R.string.ongoing_activity_status_content))
            .addPart("time", Status.StopwatchPart(runStartTime))
            .build()

        // create ongoing activity
        val ongoingActivity =
            OngoingActivity.Builder(applicationContext, NOTIFICATION_ID, notificationBuilder)
                .setStaticIcon(R.drawable.ic_fmr_notification)
                .setAnimatedIcon(R.drawable.ic_fmr_notification)
                .setTouchIntent(intent)
                .setStatus(ongoingActivityStatus)
                .build()

        ongoingActivity.apply(applicationContext)

    }

    private fun postOngoingActivityAndBuildNotification() : Notification {
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

        val notificationBuilder =  NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_content))
            .setSmallIcon(R.drawable.ic_fmr_notification)
            .setOngoing(true)
            .setContentIntent(pendingIntent)

        buildOngoingActivity(notificationBuilder, pendingIntent)
        return notificationBuilder.build()
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
