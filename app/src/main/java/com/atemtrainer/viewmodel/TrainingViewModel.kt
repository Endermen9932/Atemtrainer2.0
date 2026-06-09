package com.atemtrainer.viewmodel

import android.app.Application
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.content.getSystemService
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class TrainingPhase { IDLE, HOLD, REST, DONE }

data class TrainingState(
    val phase: TrainingPhase = TrainingPhase.IDLE,
    val currentRound: Int = 1,
    val totalRounds: Int = 9,
    val secondsLeft: Int = 0,
    val targetSeconds: Int = 30,
    val restDuration: Int = 0,
)

// Rest durations between rounds (in seconds): 120, 105, 90, 75, 60, 45, 30, 15
private val REST_DURATIONS = intArrayOf(120, 105, 90, 75, 60, 45, 30, 15)

class TrainingViewModel(app: Application) : AndroidViewModel(app) {
    private val _state = MutableStateFlow(TrainingState())
    val state: StateFlow<TrainingState> = _state

    private var timerJob: Job? = null
    private val vibrator: Vibrator? = app.getSystemService()

    fun start(targetSeconds: Int) {
        _state.value = TrainingState(
            phase = TrainingPhase.HOLD,
            currentRound = 1,
            totalRounds = 9,
            secondsLeft = targetSeconds,
            targetSeconds = targetSeconds,
            restDuration = 0,
        )
        runTimer()
    }

    private fun runTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val s = _state.value
                val next = s.secondsLeft - 1
                if (next > 0) {
                    _state.value = s.copy(secondsLeft = next)
                } else {
                    vibrate()
                    when (s.phase) {
                        TrainingPhase.HOLD -> {
                            val roundIdx = s.currentRound - 1
                            if (roundIdx < REST_DURATIONS.size) {
                                val restSec = REST_DURATIONS[roundIdx]
                                _state.value = s.copy(
                                    phase = TrainingPhase.REST,
                                    secondsLeft = restSec,
                                    restDuration = restSec,
                                )
                            } else {
                                _state.value = s.copy(phase = TrainingPhase.DONE, secondsLeft = 0)
                                timerJob?.cancel()
                                return@launch
                            }
                        }
                        TrainingPhase.REST -> {
                            val nextRound = s.currentRound + 1
                            if (nextRound <= s.totalRounds) {
                                _state.value = s.copy(
                                    phase = TrainingPhase.HOLD,
                                    currentRound = nextRound,
                                    secondsLeft = s.targetSeconds,
                                )
                            } else {
                                _state.value = s.copy(phase = TrainingPhase.DONE, secondsLeft = 0)
                                timerJob?.cancel()
                                return@launch
                            }
                        }
                        else -> return@launch
                    }
                }
            }
        }
    }

    fun cancel() {
        timerJob?.cancel()
        _state.value = TrainingState()
    }

    private fun vibrate() {
        vibrator?.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
