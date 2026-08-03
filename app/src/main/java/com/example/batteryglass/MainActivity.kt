package com.example.batteryglass

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.*
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
        val tvAlpha = findViewById<TextView>(R.id.tvAlpha)
        val sbAlpha = findViewById<SeekBar>(R.id.sbAlpha)

        val switchTopBar = findViewById<SwitchMaterial>(R.id.switchTopBar)
        val spinnerTopMode = findViewById<Spinner>(R.id.spinnerTopMode)
        val switchLeftBar = findViewById<SwitchMaterial>(R.id.switchLeftBar)
        val switchRightBar = findViewById<SwitchMaterial>(R.id.switchRightBar)
        
        val switchAnim = findViewById<SwitchMaterial>(R.id.switchAnimation)
        val switchWave = findViewById<SwitchMaterial>(R.id.switchWave)
        val switchPulse = findViewById<SwitchMaterial>(R.id.switchPulse)
        val switchHideFull = findViewById<SwitchMaterial>(R.id.switchHideFull)
        val btnToggleService = findViewById<Button>(R.id.btnToggleService)

        val palettes = arrayOf("Classico (Spettro 50 Sfumature)", "Minimal Monocromatico", "Cyberpunk Neon", "Pastello Soft")
        spinnerPalette.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, palettes)
        spinnerPalette.setSelection(prefs.getInt("palette", 0))

        val topModes = arrayOf("Sinistra ➡️ Destra", "Destra ➡️ Sinistra", "Centro ➡️ Esterni", "Buco Nero (Erosione Notch)")
        spinnerTopMode.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, topModes)
        spinnerTopMode.setSelection(prefs.getInt("top_bar_mode", 0))

        sbThickness.progress = prefs.getFloat("stroke_width", 1f).toInt()
        tvThickness.text = "Spessore linea: ${sbThickness.progress} dp"
        
        sbPadding.progress = prefs.getFloat("edge_padding", 0f).toInt()
        tvPadding.text = "Distanza dai bordi laterali: ${sbPadding.progress} dp"

        val currentAlphaPct = prefs.getInt("alpha_pct", 100)
        sbAlpha.progress = currentAlphaPct
        tvAlpha.text = "Trasparenza: $currentAlphaPct%"

        switchTopBar.isChecked = prefs.getBoolean("show_top", true)
        switchLeftBar.isChecked = prefs.getBoolean("show_left", true)
        switchRightBar.isChecked = prefs.getBoolean("show_right", true)

        switchAnim.isChecked = prefs.getBoolean("anim_enabled", true)
        switchWave.isChecked = prefs.getBoolean("wave_enabled", true)
        switchPulse.isChecked = prefs.getBoolean("pulse_enabled", true)
        switchHideFull.isChecked = prefs.getBoolean("hide_full_enabled", false)

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
                prefs.edit().putInt("palette", position).apply(); updateService()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spinnerTopMode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                prefs.edit().putInt("top_bar_mode", position).apply(); updateService()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        sbThickness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = progress.coerceAtLeast(1)
                tvThickness.text = "Spessore linea: $value dp"
                prefs.edit().putFloat("stroke_width", value.toFloat()).apply(); updateService()
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })

        sbPadding.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvPadding.text = "Distanza dai bordi laterali: $progress dp"
                prefs.edit().putFloat("edge_padding", progress.toFloat()).apply(); updateService()
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })

        sbAlpha.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val pct = progress.coerceAtLeast(10)
                tvAlpha.text = "Trasparenza: $pct%"
                prefs.edit().putInt("alpha_pct", pct).apply(); updateService()
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })

        val toggleListener = { buttonView: CompoundButton, isChecked: Boolean ->
            when (buttonView.id) {
                R.id.switchTopBar -> prefs.edit().putBoolean("show_top", isChecked).apply()
                R.id.switchLeftBar -> prefs.edit().putBoolean("show_left", isChecked).apply()
                R.id.switchRightBar -> prefs.edit().putBoolean("show_right", isChecked).apply()
                R.id.switchAnimation -> prefs.edit().putBoolean("anim_enabled", isChecked).apply()
                R.id.switchWave -> prefs.edit().putBoolean("wave_enabled", isChecked).apply()
                R.id.switchPulse -> prefs.edit().putBoolean("pulse_enabled", isChecked).apply()
                R.id.switchHideFull -> prefs.edit().putBoolean("hide_full_enabled", isChecked).apply()
            }
            updateService()
        }

        switchTopBar.setOnCheckedChangeListener(toggleListener)
        switchLeftBar.setOnCheckedChangeListener(toggleListener)
        switchRightBar.setOnCheckedChangeListener(toggleListener)
        switchAnim.setOnCheckedChangeListener(toggleListener)
        switchWave.setOnCheckedChangeListener(toggleListener)
        switchPulse.setOnCheckedChangeListener(toggleListener)
        switchHideFull.setOnCheckedChangeListener(toggleListener)

        btnToggleService.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Concedi prima il permesso!", Toast.LENGTH_SHORT).show()
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
        val alphaPct = prefs.getInt("alpha_pct", 100)
        val alpha255 = (alphaPct * 255) / 100

        val intent = Intent(this, OverlayService::class.java).apply {
            putExtra("STROKE_WIDTH", prefs.getFloat("stroke_width", 1f) * density)
            putExtra("EDGE_PADDING", prefs.getFloat("edge_padding", 0f) * density)
            putExtra("ALPHA", alpha255)
            putExtra("SHOW_TOP", prefs.getBoolean("show_top", true))
            putExtra("TOP_BAR_MODE", prefs.getInt("top_bar_mode", 0))
            putExtra("SHOW_LEFT", prefs.getBoolean("show_left", true))
            putExtra("SHOW_RIGHT", prefs.getBoolean("show_right", true))
            putExtra("PALETTE", prefs.getInt("palette", 0))
            putExtra("ANIM_ENABLED", prefs.getBoolean("anim_enabled", true))
            putExtra("WAVE_ENABLED", prefs.getBoolean("wave_enabled", true))
            putExtra("PULSE_ENABLED", prefs.getBoolean("pulse_enabled", true))
            putExtra("HIDE_FULL", prefs.getBoolean("hide_full_enabled", false))
        }
        startForegroundService(intent)
    }

    private fun updateService() {
        if (isServiceRunning) startOverlayService()
    }
}
