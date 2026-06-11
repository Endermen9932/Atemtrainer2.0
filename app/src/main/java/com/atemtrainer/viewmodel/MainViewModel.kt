package com.atemtrainer.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.atemtrainer.data.database.AppDatabase
import com.atemtrainer.data.database.SessionEntity
import com.atemtrainer.data.datastore.UserPreferences
import com.atemtrainer.data.repository.TrainingRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

sealed class MainUiState {
    object Loading : MainUiState()
    data class Ready(
        val onboardingDone: Boolean,
        val baselineSeconds: Int,
        val currentTargetSeconds: Int,
        val showIncreasePrompt: Boolean,
        val sessions: List<SessionEntity>,
        val maxDuration: Int?,
        // Cue settings
        val cueType: Int,
        val cueLeadSeconds: Int,
        // Display settings
        val dimEnabled: Boolean,
        val dimDelaySeconds: Int,
    ) : MainUiState()
}

private data class PrefsSnapshot(
    val onboardingDone: Boolean,
    val baseline: Int,
    val target: Int,
    val lastTrainingDate: Long,
    val increasePromptDate: Long,
)

private data class SettingsSnapshot(
    val lastIncreaseDate: Long,
    val cueType: Int,
    val cueLeadSeconds: Int,
    val dimEnabled: Boolean,
    val dimDelaySeconds: Int,
)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.getInstance(app)
    private val prefs = UserPreferences(app)
    private val repo = TrainingRepository(db, prefs)

    private val prefsFlow: Flow<PrefsSnapshot> = combine(
        prefs.onboardingDone,
        prefs.baselineSeconds,
        prefs.currentTargetSeconds,
        prefs.lastTrainingDate,
        prefs.increasePromptDate
    ) { onboarding, baseline, target, lastTraining, increaseDate ->
        PrefsSnapshot(onboarding, baseline, target, lastTraining, increaseDate)
    }

    private val settingsFlow: Flow<SettingsSnapshot> = combine(
        prefs.lastIncreaseDate,
        prefs.cueType,
        prefs.cueLeadSeconds,
        prefs.dimEnabled,
        prefs.dimDelaySeconds
    ) { lastIncrease, cueType, cueLead, dimEnabled, dimDelay ->
        SettingsSnapshot(lastIncrease, cueType, cueLead, dimEnabled, dimDelay)
    }

    val uiState: StateFlow<MainUiState> = combine(
        prefsFlow,
        settingsFlow,
        repo.allSessions,
        repo.maxDuration
    ) { snap, settings, sessions, maxDur ->
        val today = LocalDate.now(ZoneId.systemDefault())

        val lastTrainingDay = if (snap.lastTrainingDate > 0L)
            Instant.ofEpochMilli(snap.lastTrainingDate).atZone(ZoneId.systemDefault()).toLocalDate()
        else null

        val increasePromptDay = if (snap.increasePromptDate > 0L)
            Instant.ofEpochMilli(snap.increasePromptDate).atZone(ZoneId.systemDefault()).toLocalDate()
        else null

        // Weekly progression: only prompt once ≥7 days have passed since the last increase.
        // Fall back to lastTrainingDate if no explicit increase date is set yet.
        val increaseRefDate = if (settings.lastIncreaseDate > 0L) settings.lastIncreaseDate
        else snap.lastTrainingDate
        val lastIncreaseDay = if (increaseRefDate > 0L)
            Instant.ofEpochMilli(increaseRefDate).atZone(ZoneId.systemDefault()).toLocalDate()
        else null
        val daysSinceIncrease = if (lastIncreaseDay != null)
            ChronoUnit.DAYS.between(lastIncreaseDay, today) else Long.MAX_VALUE

        val showPrompt = snap.onboardingDone &&
                lastTrainingDay != null &&
                lastTrainingDay.isBefore(today) &&
                increasePromptDay != today &&
                daysSinceIncrease >= 7

        MainUiState.Ready(
            onboardingDone = snap.onboardingDone,
            baselineSeconds = snap.baseline,
            currentTargetSeconds = snap.target,
            showIncreasePrompt = showPrompt,
            sessions = sessions,
            maxDuration = maxDur,
            cueType = settings.cueType,
            cueLeadSeconds = settings.cueLeadSeconds,
            dimEnabled = settings.dimEnabled,
            dimDelaySeconds = settings.dimDelaySeconds,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MainUiState.Loading)

    fun completeOnboarding(baselineSeconds: Int) = viewModelScope.launch {
        prefs.setBaseline(baselineSeconds)
        prefs.setCurrentTarget(baselineSeconds)
        prefs.setOnboardingDone(true)
        prefs.setLastIncreaseDate(System.currentTimeMillis())
    }

    fun acceptIncrease() = viewModelScope.launch {
        val current = prefs.currentTargetSeconds.first()
        prefs.setCurrentTarget(current + 5)
        prefs.setIncreasePromptDate(System.currentTimeMillis())
        prefs.setLastIncreaseDate(System.currentTimeMillis())
    }

    fun dismissIncrease() = viewModelScope.launch {
        prefs.setIncreasePromptDate(System.currentTimeMillis())
    }

    /** Called when user taps "Stopp" in round 1 of their first session.
     *  Permanently updates baseline and current target to the achieved value. */
    fun applyMeasuredBaseline(seconds: Int) = viewModelScope.launch {
        prefs.setBaseline(seconds)
        prefs.setCurrentTarget(seconds)
        prefs.setLastIncreaseDate(System.currentTimeMillis())
    }

    fun recordCompletedSession(targetSeconds: Int) = viewModelScope.launch {
        repo.insertSession(
            SessionEntity(
                date = System.currentTimeMillis(),
                targetSeconds = targetSeconds,
                completed = true
            )
        )
        prefs.setLastTrainingDate(System.currentTimeMillis())
    }

    fun setCueType(type: Int) = viewModelScope.launch { prefs.setCueType(type) }
    fun setCueLeadSeconds(seconds: Int) = viewModelScope.launch { prefs.setCueLeadSeconds(seconds) }
    fun setDimEnabled(enabled: Boolean) = viewModelScope.launch { prefs.setDimEnabled(enabled) }
    fun setDimDelaySeconds(seconds: Int) = viewModelScope.launch { prefs.setDimDelaySeconds(seconds) }

    fun exportData(context: Context, uri: Uri) = viewModelScope.launch {
        val sessions = repo.allSessions.first()
        val arr = JSONArray()
        sessions.forEach { s ->
            arr.put(JSONObject().apply {
                put("id", s.id)
                put("date", s.date)
                put("targetSeconds", s.targetSeconds)
                put("completed", s.completed)
                put("notes", s.notes)
            })
        }
        val json = JSONObject().apply {
            put("version", 1)
            put("sessions", arr)
        }
        context.contentResolver.openOutputStream(uri)?.use {
            it.write(json.toString(2).toByteArray())
        }
    }

    fun importData(context: Context, uri: Uri) = viewModelScope.launch {
        try {
            val text = BufferedReader(
                InputStreamReader(context.contentResolver.openInputStream(uri))
            ).readText()
            val json = JSONObject(text)
            val arr = json.getJSONArray("sessions")
            repo.deleteAll()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                repo.insertSession(
                    SessionEntity(
                        date = obj.getLong("date"),
                        targetSeconds = obj.getInt("targetSeconds"),
                        completed = obj.getBoolean("completed"),
                        notes = obj.optString("notes", "")
                    )
                )
            }
        } catch (_: Exception) {}
    }
}
