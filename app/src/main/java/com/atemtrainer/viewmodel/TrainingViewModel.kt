package com.atemtrainer.viewmodel

import android.app.Application
import android.media.AudioManager
import android.media.ToneGenerator
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

enum class TrainingPhase { IDLE, PREP, HOLD, REST, DONE }

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
    private val toneGenerator: ToneGenerator? = try {
        ToneGenerator(AudioManager.STREAM_MUSIC, 80)
    } catch (_: Exception) { null }

    // Config stored at session start
    private var cueType: Int = 0       // 0=Off, 1=Vibration, 2=Tone, 3=Both
    private var cueLeadSec: Int = 0
    private var isFirstSession: Boolean = false

    fun start(
        targetSeconds: Int,
        cueType: Int = 0,
        cueLeadSeconds: Int = 0,
        firstSession: Boolean = false
    ) {
        this.cueType = cueType
        this.cueLeadSec = cueLeadSeconds
        this.isFirstSession = firstSession

        val hasCue = cueType != 0 && cueLeadSeconds > 0
        if (hasCue) {
            _state.value = TrainingState(
                phase = TrainingPhase.PREP,
                currentRound = 1,
                totalRounds = 9,
                secondsLeft = cueLeadSeconds,
                targetSeconds = targetSeconds,
                restDuration = 0,
            )
            playCue()
        } else {
            _state.value = TrainingState(
                phase = TrainingPhase.HOLD,
                currentRound = 1,
                totalRounds = 9,
                secondsLeft = targetSeconds,
                targetSeconds = targetSeconds,
                restDuration = 0,
            )
        }
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
                    when (s.phase) {
                        TrainingPhase.PREP -> {
                            // PREP countdown finished → start HOLD
                            _state.value = s.copy(
                                phase = TrainingPhase.HOLD,
                                secondsLeft = s.targetSeconds,
                            )
                        }
                        TrainingPhase.HOLD -> {
                            vibrate()
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
                                val hasCue = cueType != 0 && cueLeadSec > 0
                                if (hasCue) {
                                    _state.value = s.copy(
                                        phase = TrainingPhase.PREP,
                                        currentRound = nextRound,
                                        secondsLeft = cueLeadSec,
                                    )
                                    playCue()
                                } else {
                                    _state.value = s.copy(
                                        phase = TrainingPhase.HOLD,
                                        currentRound = nextRound,
                                        secondsLeft = s.targetSeconds,
                                    )
                                }
                            } else {
                                vibrate()
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

    /** Stop the current hold early (round 1, first session).
     *  Records elapsed time as the new target and transitions to REST. */
    fun stopHoldEarly() {
        val s = _state.value
        if (s.phase != TrainingPhase.HOLD) return
        val elapsed = (s.targetSeconds - s.secondsLeft).coerceAtLeast(5)
        vibrate()
        val roundIdx = s.currentRound - 1
        val restSec = if (roundIdx < REST_DURATIONS.size) REST_DURATIONS[roundIdx] else 15
        _state.value = s.copy(
            phase = TrainingPhase.REST,
            targetSeconds = elapsed,
            secondsLeft = restSec,
            restDuration = restSec,
        )
    }

    /** Exposes the current effective target (may have been adjusted via stopHoldEarly). */
    val effectiveTargetSeconds: Int get() = _state.value.targetSeconds

    fun cancel() {
        timerJob?.cancel()
        _state.value = TrainingState()
    }

    private fun playCue() {
        if (cueType == 1 || cueType == 3) vibrate()
        if (cueType == 2 || cueType == 3) {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 300)
            } catch (_: Exception) {}
        }
    }

    private fun vibrate() {
        vibrator?.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        try { toneGenerator?.release() } catch (_: Exception) {}
    }
}
