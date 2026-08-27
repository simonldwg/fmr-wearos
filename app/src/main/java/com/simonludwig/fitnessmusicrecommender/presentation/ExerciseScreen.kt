package com.simonludwig.fitnessmusicrecommender.presentation

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.FilledIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.simonludwig.fitnessmusicrecommender.R


private fun heartRateToString(
    state: ExerciseScreenState
): String {
    // Return a string of the number if a heart rate has been reported, otherwise return "--"
    return state.heartRate?.let { "$it" } ?: "--"
}

@Composable
private fun SongTitle(song: String) {
    Text(
        text = song,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        maxLines = 1,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .basicMarquee()
    )
}

@Composable
fun ExerciseScreen(
    state: ExerciseScreenState,
    onPlayPauseToggled: () -> Unit,
    onSkipSong: () -> Unit,
    onStopExercise: () -> Unit
) {


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_heartrate),
                contentDescription = "Heart rate",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.heart_rate, heartRateToString(state)),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }

        SongTitle(state.currentSong)

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            FilledIconButton(
                onClick = onPlayPauseToggled,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_playpause),
                    contentDescription = "Play/Pause"
                )
            }
            FilledIconButton(
                onClick = onSkipSong,
                modifier = Modifier.size(38.dp),
                enabled = state.isSkipEnabled
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_skipnext),
                    contentDescription = "Next song"
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onStopExercise,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            ),
            modifier = Modifier
                .padding(top = 8.dp)
                .defaultMinSize(minHeight = 10.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
        ) {
            Text(
                text = stringResource(R.string.stop_exercise),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }

}
