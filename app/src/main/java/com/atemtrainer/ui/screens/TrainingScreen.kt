package com.atemtrainer.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.atemtrainer.viewmodel.TrainingPhase
import com.atemtrainer.viewmodel.TrainingState
import com.atemtrainer.viewmodel.TrainingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingScreen(
    targetSeconds: Int,
    viewModel: TrainingViewModel,
    onDone: () -> Unit,
    onCancel: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.start(targetSeconds)
    }

    LaunchedEffect(state.phase) {
        if (state.phase == TrainingPhase.DONE) onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Training") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.cancel()
                        onCancel()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedContent(
                targetState = state.phase,
                transitionSpec = { fadeIn() togetherWith fadeOut() }
            ) { phase ->
                when (phase) {
                    TrainingPhase.IDLE -> CircularProgressIndicator()
                    TrainingPhase.HOLD -> HoldPhase(state)
                    TrainingPhase.REST -> RestPhase(state)
                    TrainingPhase.DONE -> DonePhase()
                }
            }
        }
    }
}

@Composable
private fun HoldPhase(state: TrainingState) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "scale"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Runde ${state.currentRound} / ${state.totalRounds}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Luft anhalten!",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(200.dp).scale(scale)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = state.secondsLeft.toString(),
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("Sek", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        Spacer(Modifier.height(32.dp))
        LinearProgressIndicator(
            progress = { 1f - state.secondsLeft.toFloat() / state.targetSeconds },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun RestPhase(state: TrainingState) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Runde ${state.currentRound} abgeschlossen!",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Pause",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(200.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = state.secondsLeft.toString(),
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text("Sek", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        Spacer(Modifier.height(32.dp))
        Text(
            text = "Nächste Runde: ${state.currentRound + 1} / ${state.totalRounds}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        LinearProgressIndicator(
            progress = { 1f - state.secondsLeft.toFloat() / state.restDuration.coerceAtLeast(1) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun DonePhase() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Training abgeschlossen!",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Sehr gut gemacht!",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
