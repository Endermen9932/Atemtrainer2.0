package com.atemtrainer.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

object PrefsKeys {
    val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
    val BASELINE_SECONDS = intPreferencesKey("baseline_seconds")
    val CURRENT_TARGET_SECONDS = intPreferencesKey("current_target_seconds")
    val LAST_TRAINING_DATE = longPreferencesKey("last_training_date")
    val INCREASE_PROMPT_DATE = longPreferencesKey("increase_prompt_date")
    val LAST_INCREASE_DATE = longPreferencesKey("last_increase_date")

    // Cue settings (0=Off, 1=Vibration, 2=Tone, 3=Both)
    val CUE_TYPE = intPreferencesKey("cue_type")
    val CUE_LEAD_SECONDS = intPreferencesKey("cue_lead_seconds")

    // Display / wake settings
    val DIM_ENABLED = booleanPreferencesKey("dim_enabled")
    val DIM_DELAY_SECONDS = intPreferencesKey("dim_delay_seconds")
}

class UserPreferences(private val context: Context) {
    val onboardingDone: Flow<Boolean> = context.dataStore.data.map { it[PrefsKeys.ONBOARDING_DONE] ?: false }
    val baselineSeconds: Flow<Int> = context.dataStore.data.map { it[PrefsKeys.BASELINE_SECONDS] ?: 30 }
    val currentTargetSeconds: Flow<Int> = context.dataStore.data.map { it[PrefsKeys.CURRENT_TARGET_SECONDS] ?: 30 }
    val lastTrainingDate: Flow<Long> = context.dataStore.data.map { it[PrefsKeys.LAST_TRAINING_DATE] ?: 0L }
    val increasePromptDate: Flow<Long> = context.dataStore.data.map { it[PrefsKeys.INCREASE_PROMPT_DATE] ?: 0L }
    val lastIncreaseDate: Flow<Long> = context.dataStore.data.map { it[PrefsKeys.LAST_INCREASE_DATE] ?: 0L }
    val cueType: Flow<Int> = context.dataStore.data.map { it[PrefsKeys.CUE_TYPE] ?: 1 }
    val cueLeadSeconds: Flow<Int> = context.dataStore.data.map { it[PrefsKeys.CUE_LEAD_SECONDS] ?: 5 }
    val dimEnabled: Flow<Boolean> = context.dataStore.data.map { it[PrefsKeys.DIM_ENABLED] ?: false }
    val dimDelaySeconds: Flow<Int> = context.dataStore.data.map { it[PrefsKeys.DIM_DELAY_SECONDS] ?: 15 }

    suspend fun setOnboardingDone(done: Boolean) = context.dataStore.edit { it[PrefsKeys.ONBOARDING_DONE] = done }
    suspend fun setBaseline(seconds: Int) = context.dataStore.edit { it[PrefsKeys.BASELINE_SECONDS] = seconds }
    suspend fun setCurrentTarget(seconds: Int) = context.dataStore.edit { it[PrefsKeys.CURRENT_TARGET_SECONDS] = seconds }
    suspend fun setLastTrainingDate(date: Long) = context.dataStore.edit { it[PrefsKeys.LAST_TRAINING_DATE] = date }
    suspend fun setIncreasePromptDate(date: Long) = context.dataStore.edit { it[PrefsKeys.INCREASE_PROMPT_DATE] = date }
    suspend fun setLastIncreaseDate(date: Long) = context.dataStore.edit { it[PrefsKeys.LAST_INCREASE_DATE] = date }
    suspend fun setCueType(type: Int) = context.dataStore.edit { it[PrefsKeys.CUE_TYPE] = type }
    suspend fun setCueLeadSeconds(seconds: Int) = context.dataStore.edit { it[PrefsKeys.CUE_LEAD_SECONDS] = seconds }
    suspend fun setDimEnabled(enabled: Boolean) = context.dataStore.edit { it[PrefsKeys.DIM_ENABLED] = enabled }
    suspend fun setDimDelaySeconds(seconds: Int) = context.dataStore.edit { it[PrefsKeys.DIM_DELAY_SECONDS] = seconds }
}
