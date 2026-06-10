package com.simonludwig.fitnessmusicrecommender.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.FilledIconButton
import androidx.wear.compose.material3.Icon
import com.simonludwig.fitnessmusicrecommender.R



@Composable
fun BasicExerciseScreen(
    onSkipSong: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        FilledIconButton(
            onClick = onSkipSong,
            modifier = Modifier
                .fillMaxSize()
                .padding(25.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_skipnext),
                contentDescription = "Next song",
                modifier = Modifier.size(45.dp)
            )
        }
    }
}
