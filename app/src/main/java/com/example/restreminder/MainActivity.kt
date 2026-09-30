package com.example.restreminder

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.restreminder.databinding.ActivityMainBinding

/**
 * Single-activity UI: four number inputs for the configurable durations plus a
 * Start/Stop button. On Android 13+ it also requests POST_NOTIFICATIONS so the
 * foreground service and alerts can actually surface.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var settings: Settings

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                Toast.makeText(
                    this,
                    "未授予通知权限，提醒将不会出现",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = Settings.get(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadSettingsIntoUi()

        binding.buttonStart.setOnClickListener {
            saveSettingsFromUi()
            requestNotificationPermissionIfNeeded()
            TimerService.start(this)
            Toast.makeText(this, "已开始计时", Toast.LENGTH_SHORT).show()
        }
        binding.buttonStop.setOnClickListener {
            TimerService.stop(this)
            Toast.makeText(this, "已停止", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh inputs in case the service updated persisted state.
        loadSettingsIntoUi()
    }

    private fun loadSettingsIntoUi() {
        binding.editWorkMinutes.setText(settings.workMinutes.toString())
        binding.editShortRestSeconds.setText(settings.shortRestSeconds.toString())
        binding.editLongRestMinutes.setText(settings.longRestMinutes.toString())
        binding.editCycles.setText(settings.cycles.toString())
    }

    private fun saveSettingsFromUi() {
        settings.workMinutes = parseOrNull(binding.editWorkMinutes.text) ?: settings.workMinutes
        settings.shortRestSeconds = parseOrNull(binding.editShortRestSeconds.text) ?: settings.shortRestSeconds
        settings.longRestMinutes = parseOrNull(binding.editLongRestMinutes.text) ?: settings.longRestMinutes
        settings.cycles = parseOrNull(binding.editCycles.text) ?: settings.cycles
    }

    private fun parseOrNull(text: CharSequence): Int? =
        text.toString().trim().toIntOrNull()?.takeIf { it > 0 }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
