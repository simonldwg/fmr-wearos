package com.simonludwig.fitnessmusicrecommender.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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

                    // Check permissions
                    var hasPermission by remember {
                        mutableStateOf(
                            ContextCompat.checkSelfPermission(context, HEART_RATE_PERMISSION) ==
                                    PackageManager.PERMISSION_GRANTED
                        )
                    }

                    val permissionLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) { granted -> hasPermission = granted }

                    LaunchedEffect(exerciseActive) {
                        if (!hasPermission) permissionLauncher.launch(HEART_RATE_PERMISSION)
                    }

                    // If an exercise is active and permissions have been granted, show the
                    // ExerciseScreen, otherwise show the WaitingScreen
                    if (exerciseActive && hasPermission) {
                        val viewModel: ExerciseViewModel = hiltViewModel()
                        val state by viewModel.screenState.collectAsStateWithLifecycle()

                        if(useBasicExerciseScreen) {
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
    }
}
