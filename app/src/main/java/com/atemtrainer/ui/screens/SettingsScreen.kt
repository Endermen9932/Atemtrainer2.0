package com.atemtrainer.ui.screens

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.atemtrainer.viewmodel.MainUiState
import com.atemtrainer.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: MainUiState.Ready,
    viewModel: MainViewModel,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var showResetDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri -> viewModel.exportData(context, uri) }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri -> viewModel.importData(context, uri) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Einstellungen") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            // ── Training ─────────────────────────────────────────────────────────
            Text(
                "Training",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))

            // Cue type selector
            Text(
                "Vor-Reiz vor neuer Runde",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            val cueLabels = listOf("Aus", "Vibration", "Ton", "Beides")
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                cueLabels.forEachIndexed { idx, label ->
                    SegmentedButton(
                        selected = state.cueType == idx,
                        onClick = { viewModel.setCueType(idx) },
                        shape = SegmentedButtonDefaults.itemShape(index = idx, count = cueLabels.size)
                    ) {
                        Text(label, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            if (state.cueType != 0) {
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Vorlaufzeit: ${state.cueLeadSeconds} Sek",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }
                Slider(
                    value = state.cueLeadSeconds.toFloat(),
                    onValueChange = { viewModel.setCueLeadSeconds(it.toInt()) },
                    valueRange = 1f..15f,
                    steps = 13,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            HorizontalDivider()

            // Display / wake settings
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Display dimmen", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Display wird nach Inaktivität gedimmt (geht nicht aus)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = state.dimEnabled,
                    onCheckedChange = { viewModel.setDimEnabled(it) }
                )
            }

            if (state.dimEnabled) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Dimmen nach: ${state.dimDelaySeconds} Sek",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }
                Slider(
                    value = state.dimDelaySeconds.toFloat(),
                    onValueChange = { viewModel.setDimDelaySeconds(it.toInt()) },
                    valueRange = 5f..60f,
                    steps = 10,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            // ── Daten ─────────────────────────────────────────────────────────────
            Text(
                "Daten",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))

            ListItem(
                headlineContent = { Text("Daten exportieren") },
                supportingContent = { Text("Alle Sessions als JSON speichern") },
                leadingContent = { Icon(Icons.Default.Upload, contentDescription = null) },
                modifier = Modifier.clickable {
                    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "application/json"
                        putExtra(Intent.EXTRA_TITLE, "atemtrainer_export.json")
                    }
                    exportLauncher.launch(intent)
                }
            )

            HorizontalDivider()

            ListItem(
                headlineContent = { Text("Daten importieren") },
                supportingContent = { Text("Sessions aus JSON-Datei laden (bestehende Daten werden überschrieben)") },
                leadingContent = { Icon(Icons.Default.Download, contentDescription = null) },
                modifier = Modifier.clickable {
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "application/json"
                    }
                    importLauncher.launch(intent)
                }
            )

            HorizontalDivider()

            ListItem(
                headlineContent = {
                    Text(
                        "Alle Daten löschen",
                        color = MaterialTheme.colorScheme.error
                    )
                },
                supportingContent = { Text("Sessions und Einstellungen zurücksetzen") },
                modifier = Modifier.clickable { showResetDialog = true }
            )
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Alle Daten löschen?") },
            text = {
                Text("Diese Aktion kann nicht rückgängig gemacht werden. Alle Sessions und Einstellungen werden gelöscht.")
            },
            confirmButton = {
                TextButton(
                    onClick = { showResetDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Löschen") }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Abbrechen") }
            }
        )
    }
}
