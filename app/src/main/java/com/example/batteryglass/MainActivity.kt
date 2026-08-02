package com.example.batteryglass

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial

class MainActivity : AppCompatActivity() {

    private var isServiceRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val prefs = getSharedPreferences("battery_glass_prefs", Context.MODE_PRIVATE)

        val btnPermission = findViewById<Button>(R.id.btnOverlayPermission)
        val spinnerPalette = findViewById<Spinner>(R.id.spinnerPalette)
        val tvThickness = findViewById<TextView>(R.id.tvThickness)
        val sbThickness = findViewById<SeekBar>(R.id.sbThickness)
        val tvPadding = findViewById<TextView>(R.id.tvPadding)
        val sbPadding = findViewById<SeekBar>(R.id.sbPadding)
        val switchAnim = findViewById<SwitchMaterial>(R.id.switchAnimation)
        val switchWave = findViewById<SwitchMaterial>(R.id.switchWave)
        val switchPulse = findViewById<SwitchMaterial>(R.id.switchPulse)
        val switchHideFull = findViewById<SwitchMaterial>(R.id.switchHideFull)
        val btnToggleService = findViewById<Button>(R.id.btnToggleService)

        var currentThickness = prefs.getFloat("stroke_width", 1f)
        var currentPadding = prefs.getFloat("edge_padding", 0f)
        var currentPalette = prefs.getInt("palette", 0)
        var animEnabled = prefs.getBoolean("anim_enabled", true)
        var waveEnabled = prefs.getBoolean("wave_enabled", true)
        var pulseEnabled = prefs.getBoolean("pulse_enabled", true)
        var hideFullEnabled = prefs.getBoolean("hide_full_enabled", false)

        val palettes = arrayOf("Classico (Spettro 50 Sfumature)", "Minimal Monocromatico", "Cyberpunk Neon", "Pastello Soft")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, palettes)
        spinnerPalette.adapter = adapter
        spinnerPalette.setSelection(currentPalette)

        sbThickness.progress = currentThickness.toInt()
        tvThickness.text = "Spessore linea: ${currentThickness.toInt()} dp"
        sbPadding.progress = currentPadding.toInt()
        tvPadding.text = "Distanza dai bordi (Bordi curvi): ${currentPadding.toInt()} dp"

        switchAnim.isChecked = animEnabled
        switchWave.isChecked = waveEnabled
        switchPulse.isChecked = pulseEnabled
        switchHideFull.isChecked = hideFullEnabled

        btnPermission.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                startActivity(intent)
            } else {
                Toast.makeText(this, "Permesso già concesso!", Toast.LENGTH_SHORT).show()
            }
        }

        spinnerPalette.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentPalette = position
                prefs.edit().putInt("palette", currentPalette).apply()
                updateService()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        sbThickness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = progress.coerceAtLeast(1) // Permette lo spessore fino a 1 pixel/dp
                tvThickness.text = "Spessore linea: $value dp"
                currentThickness = value.toFloat()
                prefs.edit().putFloat("stroke_width", currentThickness).apply()
                updateService()
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })

        sbPadding.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvPadding.text = "Distanza dai bordi (Bordi curvi): $progress dp"
                currentPadding = progress.toFloat()
                prefs.edit().putFloat("edge_padding", currentPadding).apply()
                updateService()
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })

        switchAnim.setOnCheckedChangeListener { _, isChecked ->
            animEnabled = isChecked
            prefs.edit().putBoolean("anim_enabled", animEnabled).apply()
            updateService()
        }

        switchWave.setOnCheckedChangeListener { _, isChecked ->
            waveEnabled = isChecked
            prefs.edit().putBoolean("wave_enabled", waveEnabled).apply()
            updateService()
        }

        switchPulse.setOnCheckedChangeListener { _, isChecked ->
            pulseEnabled = isChecked
            prefs.edit().putBoolean("pulse_enabled", pulseEnabled).apply()
            updateService()
        }

        switchHideFull.setOnCheckedChangeListener { _, isChecked ->
            hideFullEnabled = isChecked
            prefs.edit().putBoolean("hide_full_enabled", hideFullEnabled).apply()
            updateService()
        }

        btnToggleService.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Concedi prima il permesso di sovraimpressione!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isServiceRunning) {
                startOverlayService()
                btnToggleService.text = "Stop Sovraimpressione"
                isServiceRunning = true
            } else {
                stopService(Intent(this, OverlayService::class.java))
                btnToggleService.text = "Avvia Sovraimpressione"
                isServiceRunning = false
            }
        }
    }

    private fun startOverlayService() {
        val density = resources.displayMetrics.density
        val prefs = getSharedPreferences("battery_glass_prefs", Context.MODE_PRIVATE)
        val intent = Intent(this, OverlayService::class.java).apply {
            putExtra("STROKE_WIDTH", prefs.getFloat("stroke_width", 1f) * density)
            putExtra("EDGE_PADDING", prefs.getFloat("edge_padding", 0f) * density)
            putExtra("PALETTE", prefs.getInt("palette", 0))
            putExtra("ANIM_ENABLED", prefs.getBoolean("anim_enabled", true))
            putExtra("WAVE_ENABLED", prefs.getBoolean("wave_enabled", true))
            putExtra("PULSE_ENABLED", prefs.getBoolean("pulse_enabled", true))
            putExtra("HIDE_FULL", prefs.getBoolean("hide_full_enabled", false))
        }
        startForegroundService(intent)
    }

    private fun updateService() {
        if (isServiceRunning) {
            startOverlayService()
        }
    }
}
