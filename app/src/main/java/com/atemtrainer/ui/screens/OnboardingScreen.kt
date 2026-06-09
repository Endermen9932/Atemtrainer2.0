package com.atemtrainer.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingScreen(onFinish: (Int) -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    var baselineInput by remember { mutableStateOf("") }
    var isTesting by remember { mutableStateOf(false) }
    var testSeconds by remember { mutableIntStateOf(0) }

    LaunchedEffect(isTesting) {
        if (isTesting) {
            testSeconds = 0
            while (isTesting) {
                kotlinx.coroutines.delay(1000)
                testSeconds++
            }
        }
    }

    Scaffold { padding ->
        AnimatedContent(
            targetState = step,
            transitionSpec = { slideInHorizontally { it } togetherWith slideOutHorizontally { -it } },
            modifier = Modifier.fillMaxSize().padding(padding)
        ) { currentStep ->
            when (currentStep) {
                0 -> WelcomeStep(onNext = { step = 1 })
                1 -> BaselineStep(
                    isTesting = isTesting,
                    testSeconds = testSeconds,
                    baselineInput = baselineInput,
                    onBaselineChange = { baselineInput = it },
                    onStartTest = { isTesting = true },
                    onStopTest = {
                        isTesting = false
                        baselineInput = testSeconds.toString()
                    },
                    onFinish = {
                        val seconds = baselineInput.toIntOrNull() ?: 30
                        onFinish(seconds.coerceAtLeast(5))
                    }
                )
            }
        }
    }
}

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Air,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Willkommen beim\nAtemtrainer",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Trainiere täglich das Luft anhalten und steigere dich Schritt für Schritt. Das Training besteht aus 9 Runden, bei denen du die Luft so lange anhältst wie du kannst – mit kürzer werdenden Pausen dazwischen.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(40.dp))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
            Text("Los geht's")
        }
    }
}

@Composable
private fun BaselineStep(
    isTesting: Boolean,
    testSeconds: Int,
    baselineInput: String,
    onBaselineChange: (String) -> Unit,
    onStartTest: () -> Unit,
    onStopTest: () -> Unit,
    onFinish: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Baseline-Test",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Teste jetzt, wie lange du ohne Probleme die Luft anhalten kannst. Tippe auf \"Test starten\", atme tief ein und halte die Luft an. Tippe auf \"Stopp\" wenn du wieder ausatmest.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(32.dp))

        if (isTesting) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Luft anhalten!", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = formatTime(testSeconds),
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onStopTest,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Stopp")
            }
        } else {
            Button(onClick = onStartTest, modifier = Modifier.fillMaxWidth()) {
                Text("Test starten")
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Oder trage dein Ergebnis manuell ein:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = baselineInput,
                onValueChange = onBaselineChange,
                label = { Text("Sekunden") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onFinish,
                modifier = Modifier.fillMaxWidth(),
                enabled = baselineInput.toIntOrNull() != null && (baselineInput.toIntOrNull() ?: 0) >= 5
            ) {
                Text("Training beginnen")
            }
        }
    }
}

private fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return if (m > 0) "${m}m ${s.toString().padStart(2, '0')}s" else "${s}s"
}
