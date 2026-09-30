package com.example.restreminder

import java.util.concurrent.TimeUnit

/**
 * State machine for the rest-reminder timer.
 * One round = WORK + SHORT_REST. After [Settings.cycles] rounds, LONG_REST.
 * After LONG_REST, restart from WORK with cycles reset to 0.
 */
sealed class TimerState(val durationMillis: Long) {
    object Idle : TimerState(0L)
    class Work(durationMillis: Long) : TimerState(durationMillis)
    class ShortRest(durationMillis: Long) : TimerState(durationMillis)
    class LongRest(durationMillis: Long) : TimerState(durationMillis)

    val name: String
        get() = when (this) {
            Idle -> "IDLE"
            is Work -> "WORK"
            is ShortRest -> "SHORT_REST"
            is LongRest -> "LONG_REST"
        }

    val label: String
        get() = when (this) {
            Idle -> "空闲"
            is Work -> "工作中"
            is ShortRest -> "短休息"
            is LongRest -> "长休息"
        }

    companion object {
        fun fromName(name: String, durationMillis: Long): TimerState = when (name) {
            "WORK" -> Work(durationMillis)
            "SHORT_REST" -> ShortRest(durationMillis)
            "LONG_REST" -> LongRest(durationMillis)
            else -> Idle
        }
    }
}

/**
 * Format a milliseconds duration as mm:ss.
 */
fun formatCountdown(millis: Long): String {
    val totalSec = TimeUnit.MILLISECONDS.toSeconds(millis).coerceAtLeast(0L)
    val m = totalSec / 60
    val s = totalSec % 60
    return "%02d:%02d".format(m, s)
}
