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
import java.time.LocalDate
import java.time.ZoneId

data class MainUiState(
    val onboardingDone: Boolean = false,
    val baselineSeconds: Int = 30,
    val currentTargetSeconds: Int = 30,
    val showIncreasePrompt: Boolean = false,
    val sessions: List<SessionEntity> = emptyList(),
    val maxDuration: Int? = null,
)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.getInstance(app)
    private val prefs = UserPreferences(app)
    private val repo = TrainingRepository(db, prefs)

    val uiState: StateFlow<MainUiState> = combine(
        prefs.onboardingDone,
        prefs.baselineSeconds,
        prefs.currentTargetSeconds,
        prefs.lastTrainingDate,
        prefs.increasePromptDate,
        repo.allSessions,
        repo.maxDuration
    ) { values ->
        val onboardingDone = values[0] as Boolean
        val baseline = values[1] as Int
        val target = values[2] as Int
        val lastTraining = values[3] as Long
        val increasePromptDate = values[4] as Long
        @Suppress("UNCHECKED_CAST")
        val sessions = values[5] as List<SessionEntity>
        val maxDur = values[6] as Int?

        val today = LocalDate.now(ZoneId.systemDefault())
        val lastTrainingDay = if (lastTraining > 0L)
            java.time.Instant.ofEpochMilli(lastTraining).atZone(ZoneId.systemDefault()).toLocalDate()
        else null
        val increasePromptDay = if (increasePromptDate > 0L)
            java.time.Instant.ofEpochMilli(increasePromptDate).atZone(ZoneId.systemDefault()).toLocalDate()
        else null

        val showPrompt = onboardingDone &&
                lastTrainingDay != null &&
                lastTrainingDay.isBefore(today) &&
                increasePromptDay != today

        MainUiState(
            onboardingDone = onboardingDone,
            baselineSeconds = baseline,
            currentTargetSeconds = target,
            showIncreasePrompt = showPrompt,
            sessions = sessions,
            maxDuration = maxDur,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MainUiState())

    fun completeOnboarding(baselineSeconds: Int) = viewModelScope.launch {
        prefs.setBaseline(baselineSeconds)
        prefs.setCurrentTarget(baselineSeconds)
        prefs.setOnboardingDone(true)
    }

    fun acceptIncrease() = viewModelScope.launch {
        val current = prefs.currentTargetSeconds.first()
        prefs.setCurrentTarget(current + 5)
        prefs.setIncreasePromptDate(System.currentTimeMillis())
    }

    fun dismissIncrease() = viewModelScope.launch {
        prefs.setIncreasePromptDate(System.currentTimeMillis())
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
        context.contentResolver.openOutputStream(uri)?.use { it.write(json.toString(2).toByteArray()) }
    }

    fun importData(context: Context, uri: Uri) = viewModelScope.launch {
        try {
            val text = BufferedReader(InputStreamReader(context.contentResolver.openInputStream(uri))).readText()
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
