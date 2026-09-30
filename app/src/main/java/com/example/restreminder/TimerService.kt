package com.example.restreminder

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import java.util.concurrent.TimeUnit

/**
 * Foreground service that drives the rest-reminder state machine.
 *
 * State transitions (one round = WORK + SHORT_REST):
 *   WORK -> { cycles+1 < X ? SHORT_REST : LONG_REST }
 *   SHORT_REST -> WORK (cycles += 1)
 *   LONG_REST  -> WORK (cycles = 0)
 *
 * State + remaining + cycles are persisted to [Settings] so the service can
 * be restored after the process is killed.
 */
class TimerService : Service() {

    private lateinit var settings: Settings
    private var state: TimerState = TimerState.Idle
    private var completedCycles: Int = 0
    private var countdown: CountDownTimer? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        settings = Settings.get(this)
        Notifier.ensureChannels(this)
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "rest-reminder:timer")
            .apply { setReferenceCounted(false) }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                completedCycles = 0
                settings.savedCompletedCycles = 0
                enterWork()
            }
            ACTION_STOP -> stopAll()
            else -> restore()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        countdown?.cancel()
        wakeLock?.let { if (it.isHeld) it.release() }
        super.onDestroy()
    }

    // ---------- State transitions ----------

    private fun enterWork() {
        val dur = TimeUnit.MINUTES.toMillis(settings.workMinutes.toLong())
        enterState(TimerState.Work(dur))
    }

    private fun enterShortRest() {
        val dur = TimeUnit.SECONDS.toMillis(settings.shortRestSeconds.toLong())
        enterState(TimerState.ShortRest(dur))
    }

    private fun enterLongRest() {
        val dur = TimeUnit.MINUTES.toMillis(settings.longRestMinutes.toLong())
        enterState(TimerState.LongRest(dur))
    }

    private fun onPhaseFinished() {
        when (state) {
            is TimerState.Work -> {
                if (completedCycles + 1 >= settings.cycles) enterLongRest()
                else enterShortRest()
            }
            is TimerState.ShortRest -> {
                completedCycles += 1
                settings.savedCompletedCycles = completedCycles
                enterWork()
            }
            is TimerState.LongRest -> {
                completedCycles = 0
                settings.savedCompletedCycles = 0
                enterWork()
            }
            TimerState.Idle -> Unit
        }
    }

    private fun enterState(newState: TimerState, startMillis: Long) {
        countdown?.cancel()
        state = newState
        settings.savedStateName = newState.name
        settings.savedRemainingMillis = startMillis
        if (newState is TimerState.Idle) return
        if (wakeLock?.isHeld != true) {
            // Hold the CPU for up to (duration + 30s) so the timer can fire even
            // when the screen is off. Auto-renewed on each state transition.
            wakeLock?.acquire(startMillis + 30_000L)
        }
        val notification = Notifier.buildForeground(this, newState, startMillis)
        startInForeground(notification)
        announceEnter(newState)
        countdown = object : CountDownTimer(startMillis, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                settings.savedRemainingMillis = millisUntilFinished
                NotificationManagerCompat.from(this@TimerService)
                    .notify(
                        Notifier.NOTIFICATION_ID,
                        Notifier.buildForeground(this@TimerService, newState, millisUntilFinished)
                    )
            }

            override fun onFinish() {
                onPhaseFinished()
            }
        }.start()
    }

    private fun startInForeground(notification: android.app.Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+ requires the type to be passed at startForeground time.
            ServiceCompat.startForeground(
                this,
                Notifier.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(Notifier.NOTIFICATION_ID, notification)
        }
    }

    // ---------- Persistence / restore ----------

    private fun restore() {
        val name = settings.savedStateName
        if (name == "IDLE") { stopSelf(); return }
        completedCycles = settings.savedCompletedCycles
        val duration = when (name) {
            "WORK" -> TimeUnit.MINUTES.toMillis(settings.workMinutes.toLong())
            "SHORT_REST" -> TimeUnit.SECONDS.toMillis(settings.shortRestSeconds.toLong())
            "LONG_REST" -> TimeUnit.MINUTES.toMillis(settings.longRestMinutes.toLong())
            else -> { stopSelf(); return }
        }
        val remaining = settings.savedRemainingMillis.coerceIn(1L, duration)
        val restored = TimerState.fromName(name, duration)
        enterState(restored, startMillis = remaining)
    }

    private fun stopAll() {
        countdown?.cancel()
        state = TimerState.Idle
        settings.clearSaved()
        completedCycles = 0
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        wakeLock?.let { if (it.isHeld) it.release() }
        stopSelf()
    }

    // ---------- Helpers ----------

    private fun announceEnter(s: TimerState) {
        val (title, text) = when (s) {
            is TimerState.Work -> "开始工作" to "专注 $completedCycles/${settings.cycles} 轮"
            is TimerState.ShortRest -> "该休息了" to "短休息 ${settings.shortRestSeconds} 秒"
            is TimerState.LongRest -> "长休息时间" to "休息 ${settings.longRestMinutes} 分钟"
            TimerState.Idle -> return
        }
        Notifier.alert(this, title, text)
    }

    companion object {
        const val ACTION_START = "com.example.restreminder.START"
        const val ACTION_STOP = "com.example.restreminder.STOP"

        fun start(ctx: Context) {
            val intent = Intent(ctx, TimerService::class.java).apply { action = ACTION_START }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(intent)
            } else {
                ctx.startService(intent)
            }
        }

        fun stop(ctx: Context) {
            val intent = Intent(ctx, TimerService::class.java).apply { action = ACTION_STOP }
            ctx.startService(intent)
        }
    }
}
