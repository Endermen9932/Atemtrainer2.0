package com.atemtrainer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.atemtrainer.viewmodel.MainUiState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: MainUiState,
    onStartTraining: () -> Unit,
    onNavigateStats: () -> Unit,
    onNavigateSettings: () -> Unit,
    onAcceptIncrease: () -> Unit,
    onDismissIncrease: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Atemtrainer") },
                actions = {
                    IconButton(onClick = onNavigateStats) {
                        Icon(Icons.Default.BarChart, contentDescription = "Statistik")
                    }
                    IconButton(onClick = onNavigateSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Einstellungen")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (state.showIncreasePrompt) {
                IncreasePromptCard(
                    currentSeconds = state.currentTargetSeconds,
                    onAccept = onAcceptIncrease,
                    onDismiss = onDismissIncrease
                )
                Spacer(Modifier.height(16.dp))
            }

            Spacer(Modifier.weight(1f))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(32.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Heutiges Ziel", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = formatDuration(state.currentTargetSeconds),
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "pro Runde · 9 Runden",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.height(8.dp))
                    val totalMin = (state.currentTargetSeconds * 9 + 540) / 60f
                    Text(
                        text = "Gesamtdauer ca. %.1f Min.".format(totalMin),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = onStartTraining,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Training starten", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(Modifier.height(16.dp))

            state.sessions.firstOrNull()?.let { last ->
                Text(
                    text = "Letztes Training: ${formatDate(last.date)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            state.maxDuration?.let { max ->
                Text(
                    text = "Persönlicher Rekord: ${formatDuration(max)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun IncreasePromptCard(currentSeconds: Int, onAccept: () -> Unit, onDismiss: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(8.dp))
                Text("Steigerung bereit!", style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Möchtest du heute 5 Sekunden draufpacken? ${formatDuration(currentSeconds)} → ${formatDuration(currentSeconds + 5)}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Nicht jetzt") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = onAccept) { Text("+5 Sekunden") }
            }
        }
    }
}

fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return if (m > 0) "${m}m ${s.toString().padStart(2, '0')}s" else "${s}s"
}

private fun formatDate(ms: Long): String {
    val date = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate()
    return DateTimeFormatter.ofPattern("dd.MM.yyyy").format(date)
}
