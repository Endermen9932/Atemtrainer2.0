package com.atemtrainer.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.atemtrainer.data.database.SessionEntity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    sessions: List<SessionEntity>,
    maxDuration: Int?,
    onBack: () -> Unit,
) {
    val completed = remember(sessions) { sessions.filter { it.completed }.sortedBy { it.date } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistik") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Sessions gesamt",
                        value = completed.size.toString()
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Pers. Rekord",
                        value = maxDuration?.let { formatDuration(it) } ?: "-",
                        showTrophy = true
                    )
                }
            }

            if (completed.size >= 2) {
                item {
                    ProgressChartCard(sessions = completed)
                }
            } else if (completed.size == 1) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Mach noch mindestens eine weitere Session, um deinen Fortschritt als Grafik zu sehen.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            item {
                Text("Letzte Sessions", style = MaterialTheme.typography.titleMedium)
            }

            if (completed.isEmpty()) {
                item {
                    Text(
                        "Noch keine abgeschlossenen Sessions.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(completed.reversed().take(50)) { session ->
                    SessionRow(session)
                }
            }
        }
    }
}

@Composable
private fun ProgressChartCard(sessions: List<SessionEntity>) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Fortschritt", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                val values = sessions.map { it.targetSeconds.toFloat() }
                val minVal = values.min()
                val maxVal = values.max()
                val range = (maxVal - minVal).coerceAtLeast(1f)
                val w = size.width
                val h = size.height
                val padding = 16f

                // Grid lines
                repeat(4) { i ->
                    val y = padding + (h - 2 * padding) * i / 3f
                    drawLine(
                        color = surfaceVariantColor,
                        start = Offset(padding, y),
                        end = Offset(w - padding, y),
                        strokeWidth = 1f
                    )
                }

                // Line path
                val path = Path()
                values.forEachIndexed { idx, v ->
                    val x = padding + (w - 2 * padding) * idx / (values.size - 1).coerceAtLeast(1).toFloat()
                    val y = h - padding - (v - minVal) / range * (h - 2 * padding)
                    if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, primaryColor, style = Stroke(width = 3f))

                // Dots
                values.forEachIndexed { idx, v ->
                    val x = padding + (w - 2 * padding) * idx / (values.size - 1).coerceAtLeast(1).toFloat()
                    val y = h - padding - (v - minVal) / range * (h - 2 * padding)
                    drawCircle(primaryColor, radius = 5f, center = Offset(x, y))
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    showTrophy: Boolean = false
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (showTrophy) {
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary
                )
            }
            Text(
                title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun SessionRow(session: SessionEntity) {
    val date = remember(session.date) {
        Instant.ofEpochMilli(session.date).atZone(ZoneId.systemDefault()).toLocalDate()
    }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                DateTimeFormatter.ofPattern("dd.MM.yyyy").format(date),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                formatDuration(session.targetSeconds),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
