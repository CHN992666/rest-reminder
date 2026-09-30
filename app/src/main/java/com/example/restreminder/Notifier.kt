package com.example.restreminder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Builds the two notification channels and the notifications for this app.
 *  - timer-running (LOW): persistent foreground notification with live countdown.
 *  - state-alert (HIGH): one-shot alert when the phase changes (sound + vibration).
 */
object Notifier {
    private const val CHANNEL_RUNNING = "timer-running"
    private const val CHANNEL_ALERT = "state-alert"
    const val NOTIFICATION_ID = 1

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(NotificationManager::class.java) ?: return
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_RUNNING,
                    "计时运行",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "前台计时器实时倒计时"
                    setShowBadge(false)
                }
            )
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ALERT,
                    "状态提醒",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "工作/休息切换时的提示音与振动"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 200)
                }
            )
        }
    }

    fun buildForeground(context: Context, state: TimerState, remainingMillis: Long): Notification {
        val text = "${state.label} - 剩 ${formatCountdown(remainingMillis)}"
        val pi = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(context, CHANNEL_RUNNING)
            .setSmallIcon(R.drawable.ic_timer_icon)
            .setContentTitle("休息提醒")
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pi)
            .build()
    }

    fun alert(context: Context, title: String, text: String) {
        val nm = NotificationManagerCompat.from(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ALERT)
            .setSmallIcon(R.drawable.ic_timer_icon)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()
        // Vary the id so successive alerts replace each other within a small window.
        nm.notify((ALERT_ID_BASE + (System.currentTimeMillis() % 500).toInt()), notification)
    }

    private const val ALERT_ID_BASE = 1000
}
