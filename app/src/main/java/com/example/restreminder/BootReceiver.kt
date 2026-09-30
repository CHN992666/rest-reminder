package com.example.restreminder

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * After device reboot, restart the timer service if it was running before
 * (i.e. the persisted state is not IDLE). This is a best-effort restore; some
 * OEM ROMs aggressively kill background services regardless.
 */
class BootReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val settings = Settings.get(context)
        if (settings.savedStateName == "IDLE") return
        // Starting a foreground service from BOOT_COMPLETED is allowed.
        val serviceIntent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(context, serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
