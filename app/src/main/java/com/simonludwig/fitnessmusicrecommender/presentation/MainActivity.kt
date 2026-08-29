package com.simonludwig.fitnessmusicrecommender.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.MaterialTheme
import com.simonludwig.fitnessmusicrecommender.exercise.ExerciseState
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var exerciseState: ExerciseState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AppScaffold {
                    val context = LocalContext.current
                    val exerciseActive by exerciseState.isExerciseActive
                        .collectAsStateWithLifecycle()
                    val useBasicExerciseScreen by exerciseState.useBasicExerciseScreen
                        .collectAsStateWithLifecycle()
                    val disablePowerOptimization by exerciseState.disablePowerOptimization
                        .collectAsStateWithLifecycle()

                    // let screen stay active if the user decided to disable power optimization
                    LaunchedEffect(disablePowerOptimization) {
                        if (disablePowerOptimization) {
                            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        } else {
                            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        }
                    }

                    // Check permissions
                    var hasPermissions by remember {
                        mutableStateOf(REQUIRED_PERMISSIONS.all { permission ->
                            ContextCompat.checkSelfPermission(context, permission) ==
                                    PackageManager.PERMISSION_GRANTED
                        })
                    }
                    val permissionLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestMultiplePermissions()
                    ) { results ->
                        hasPermissions = results.values.all { it }
                    }
                    LaunchedEffect(exerciseActive) {
                        if (!hasPermissions) permissionLauncher.launch(REQUIRED_PERMISSIONS)
                    }

                    // If an exercise is active and permissions have been granted, show the
                    // ExerciseScreen, otherwise show the WaitingScreen
                    if (exerciseActive && hasPermissions) {
                        val viewModel: ExerciseViewModel = hiltViewModel()
                        val state by viewModel.screenState.collectAsStateWithLifecycle()
                        if (useBasicExerciseScreen) {
                            BasicExerciseScreen(
                                state = state,
                                onSkipSong = viewModel::skipSong
                            )
                        } else {
                            ExerciseScreen(
                                state = state,
                                onPlayPauseToggled = viewModel::togglePlayPause,
                                onSkipSong = viewModel::skipSong,
                                onStopExercise = viewModel::stopExercise
                            )
                        }
                    } else {
                        WaitingScreen()
                    }
                }
            }
        }
    }

    companion object {
        val HEART_RATE_PERMISSION =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA)
                "android.permission.health.READ_HEART_RATE"
            else
                Manifest.permission.BODY_SENSORS

        val REQUIRED_PERMISSIONS: Array<String> = buildList {
            add(HEART_RATE_PERMISSION)
            add(Manifest.permission.ACTIVITY_RECOGNITION)
        }.toTypedArray()
    }
}