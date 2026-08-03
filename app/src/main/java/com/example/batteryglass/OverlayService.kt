package com.example.batteryglass

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.view.WindowManager
import androidx.core.app.NotificationCompat

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var batteryView: BatteryOverlayView
    private val CHANNEL_ID = "BatteryGlassOverlayChannel"

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            if (level != -1 && scale != -1) {
                val batteryPct = (level * 100 / scale.toFloat()).toInt()
                batteryView.batteryLevel = batteryPct
                batteryView.isCharging = isCharging
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Battery Glass Attivo")
            .setContentText("L'overlay della batteria è in esecuzione in background.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
        startForeground(1, notification)

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        batteryView = BatteryOverlayView(this)

        // FLAG_NOT_TOUCHABLE e FLAG_NOT_FOCUSABLE rendono l'app 100% passiva ai tocchi
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        windowManager.addView(batteryView, layoutParams)
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            batteryView.strokeWidthPx = it.getFloatExtra("STROKE_WIDTH", 1f)
            batteryView.edgePaddingPx = it.getFloatExtra("EDGE_PADDING", 0f)
            batteryView.palette = BatteryOverlayView.Palette.values()[it.getIntExtra("PALETTE", 0)]
            batteryView.animEnabled = it.getBooleanExtra("ANIM_ENABLED", true)
            batteryView.waveEnabled = it.getBooleanExtra("WAVE_ENABLED", true)
            batteryView.lowBatteryPulseEnabled = it.getBooleanExtra("PULSE_ENABLED", true)
            batteryView.hideOnFullEnabled = it.getBooleanExtra("HIDE_FULL", false)
            
            // Extra per Trasparenza e barre attive
            batteryView.userAlpha = it.getIntExtra("ALPHA", 255)
            batteryView.showTop = it.getBooleanExtra("SHOW_TOP", true)
            batteryView.showLeft = it.getBooleanExtra("SHOW_LEFT", true)
            batteryView.showRight = it.getBooleanExtra("SHOW_RIGHT", true)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(batteryReceiver)
        windowManager.removeView(batteryView)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Servizio Overlay Batteria",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
