package com.example.batteryglass

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial

class MainActivity : AppCompatActivity() {

    private var isServiceRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate()
        setContentView(R.layout.activity_main)

        val prefs = getSharedPreferences("battery_glass_prefs", Context.MODE_PRIVATE)

        val btnPermission = findViewById<Button>(R.id.btnOverlayPermission)
        val tvThickness = findViewById<TextView>(R.id.tvThickness)
        val sbThickness = findViewById<SeekBar>(R.id.sbThickness)
        val switchAnim = findViewById<SwitchMaterial>(R.id.switchAnimation)
        val btnToggleService = findViewById<Button>(R.id.btnToggleService)

        var currentThickness = prefs.getFloat("stroke_width", 12f)
        var animEnabled = prefs.getBoolean("anim_enabled", true)

        sbThickness.progress = currentThickness.toInt()
        tvThickness.text = "Spessore della linea: ${currentThickness.toInt()} dp"
        switchAnim.isChecked = animEnabled

        btnPermission.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            } else {
                Toast.makeText(this, "Permesso già concesso!", Toast.LENGTH_SHORT).show()
            }
        }

        sbThickness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = progress.coerceAtLeast(2)
                tvThickness.text = "Spessore della linea: $value dp"
                currentThickness = value.toFloat()
                prefs.edit().putFloat("stroke_width", currentThickness).apply()
                updateService(currentThickness, animEnabled)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        switchAnim.setOnCheckedChangeListener { _, isChecked ->
            animEnabled = isChecked
            prefs.edit().putBoolean("anim_enabled", animEnabled).apply()
            updateService(currentThickness, animEnabled)
        }

        btnToggleService.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Concedi prima il permesso di sovraimpressione!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isServiceRunning) {
                val intent = Intent(this, OverlayService::class.java).apply {
                    putExtra("STROKE_WIDTH", currentThickness * resources.displayMetrics.density)
                    putExtra("ANIM_ENABLED", animEnabled)
                }
                startForegroundService(intent)
                btnToggleService.text = "Stop Sovraimpressione"
                isServiceRunning = true
            } else {
                stopService(Intent(this, OverlayService::class.java))
                btnToggleService.text = "Avvia Sovraimpressione"
                isServiceRunning = false
            }
        }
    }

    private fun updateService(thicknessDp: Float, anim: Boolean) {
        if (isServiceRunning) {
            val intent = Intent(this, OverlayService::class.java).apply {
                putExtra("STROKE_WIDTH", thicknessDp * resources.displayMetrics.density)
                putExtra("ANIM_ENABLED", anim)
            }
            startForegroundService(intent)
        }
    }
}
