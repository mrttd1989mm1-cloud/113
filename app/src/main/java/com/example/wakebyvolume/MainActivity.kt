package com.example.wakebyvolume

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private var isRunning = false
    private lateinit var btnToggle: Button
    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnToggle = findViewById(R.id.btnToggle)
        tvStatus = findViewById(R.id.tvStatus)
        val btnBattery = findViewById<Button>(R.id.btnBattery)

        requestNotificationPermissionIfNeeded()

        btnToggle.setOnClickListener { toggleService() }

        btnBattery.setOnClickListener {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, getString(R.string.battery_settings_error), Toast.LENGTH_SHORT).show()
            }
        }

        updateUi()
    }

    private fun toggleService() {
        isRunning = !isRunning
        val intent = Intent(this, WakeService::class.java)
        if (isRunning) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } else {
            stopService(intent)
        }
        updateUi()
    }

    private fun updateUi() {
        if (isRunning) {
            tvStatus.text = getString(R.string.status_running)
            btnToggle.text = getString(R.string.btn_stop)
        } else {
            tvStatus.text = getString(R.string.status_stopped)
            btnToggle.text = getString(R.string.btn_start)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001
                )
            }
        }
    }
}
