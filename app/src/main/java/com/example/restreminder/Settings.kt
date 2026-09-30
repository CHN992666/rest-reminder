package com.example.restreminder

import android.content.Context
import android.content.SharedPreferences

/**
 * Wrapper around SharedPreferences for the four configurable values, plus the
 * persisted running state used by [TimerService] to survive process death.
 *
 * Defaults follow the 20-20-20 eye-care rule: 20 min work, 20 sec short rest.
 */
class Settings private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var workMinutes: Int
        get() = prefs.getInt(KEY_WORK_MIN, 20)
        set(value) = prefs.edit().putInt(KEY_WORK_MIN, value.coerceIn(1, 180)).apply()

    var shortRestSeconds: Int
        get() = prefs.getInt(KEY_SHORT_REST_SEC, 20)
        set(value) = prefs.edit().putInt(KEY_SHORT_REST_SEC, value.coerceIn(1, 3600)).apply()

    var longRestMinutes: Int
        get() = prefs.getInt(KEY_LONG_REST_MIN, 5)
        set(value) = prefs.edit().putInt(KEY_LONG_REST_MIN, value.coerceIn(1, 60)).apply()

    var cycles: Int
        get() = prefs.getInt(KEY_CYCLES, 4)
        set(value) = prefs.edit().putInt(KEY_CYCLES, value.coerceIn(1, 12)).apply()

    var savedStateName: String
        get() = prefs.getString(KEY_STATE_NAME, "IDLE") ?: "IDLE"
        set(value) = prefs.edit().putString(KEY_STATE_NAME, value).apply()

    var savedRemainingMillis: Long
        get() = prefs.getLong(KEY_REMAINING_MS, 0L)
        set(value) = prefs.edit().putLong(KEY_REMAINING_MS, value).apply()

    var savedCompletedCycles: Int
        get() = prefs.getInt(KEY_COMPLETED_CYCLES, 0)
        set(value) = prefs.edit().putInt(KEY_COMPLETED_CYCLES, value).apply()

    fun clearSaved() {
        prefs.edit()
            .putString(KEY_STATE_NAME, "IDLE")
            .putLong(KEY_REMAINING_MS, 0L)
            .putInt(KEY_COMPLETED_CYCLES, 0)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "rest_reminder_prefs"
        private const val KEY_WORK_MIN = "work_minutes"
        private const val KEY_SHORT_REST_SEC = "short_rest_seconds"
        private const val KEY_LONG_REST_MIN = "long_rest_minutes"
        private const val KEY_CYCLES = "cycles"
        private const val KEY_STATE_NAME = "saved_state_name"
        private const val KEY_REMAINING_MS = "saved_remaining_ms"
        private const val KEY_COMPLETED_CYCLES = "saved_completed_cycles"

        @Volatile
        private var instance: Settings? = null

        fun get(context: Context): Settings =
            instance ?: synchronized(this) {
                instance ?: Settings(context).also { instance = it }
            }
    }
}
