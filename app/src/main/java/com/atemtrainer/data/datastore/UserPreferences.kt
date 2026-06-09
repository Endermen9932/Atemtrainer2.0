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
}

class UserPreferences(private val context: Context) {
    val onboardingDone: Flow<Boolean> = context.dataStore.data.map { it[PrefsKeys.ONBOARDING_DONE] ?: false }
    val baselineSeconds: Flow<Int> = context.dataStore.data.map { it[PrefsKeys.BASELINE_SECONDS] ?: 30 }
    val currentTargetSeconds: Flow<Int> = context.dataStore.data.map { it[PrefsKeys.CURRENT_TARGET_SECONDS] ?: 30 }
    val lastTrainingDate: Flow<Long> = context.dataStore.data.map { it[PrefsKeys.LAST_TRAINING_DATE] ?: 0L }
    val increasePromptDate: Flow<Long> = context.dataStore.data.map { it[PrefsKeys.INCREASE_PROMPT_DATE] ?: 0L }

    suspend fun setOnboardingDone(done: Boolean) = context.dataStore.edit { it[PrefsKeys.ONBOARDING_DONE] = done }
    suspend fun setBaseline(seconds: Int) = context.dataStore.edit { it[PrefsKeys.BASELINE_SECONDS] = seconds }
    suspend fun setCurrentTarget(seconds: Int) = context.dataStore.edit { it[PrefsKeys.CURRENT_TARGET_SECONDS] = seconds }
    suspend fun setLastTrainingDate(date: Long) = context.dataStore.edit { it[PrefsKeys.LAST_TRAINING_DATE] = date }
    suspend fun setIncreasePromptDate(date: Long) = context.dataStore.edit { it[PrefsKeys.INCREASE_PROMPT_DATE] = date }
}
