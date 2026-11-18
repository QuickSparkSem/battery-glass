package com.example.batteryglass

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.BatteryManager
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.switchmaterial.SwitchMaterial

class MainActivity : AppCompatActivity() {

    private lateinit var cardPermission: MaterialCardView
    private lateinit var tvStatusSubtitle: TextView
    private lateinit var btnToggleService: MaterialButton

    private val tickIntervals = intArrayOf(0, 1, 2, 3, 5, 10, 20, 25)
    private val tickIntervalLabels = arrayOf(
        "Disattivato",
        "Ogni 1%",
        "Ogni 2%",
        "Ogni 3%",
        "Ogni 5%",
        "Ogni 10%",
        "Ogni 20%",
        "Ogni 25%"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val prefs = getSharedPreferences(OverlayService.PREFS_NAME, Context.MODE_PRIVATE)

        cardPermission = findViewById(R.id.cardPermission)
        tvStatusSubtitle = findViewById(R.id.tvStatusSubtitle)
        btnToggleService = findViewById(R.id.btnToggleService)

        val btnPermission = findViewById<MaterialButton>(R.id.btnOverlayPermission)
        val spinnerPalette = findViewById<Spinner>(R.id.spinnerPalette)
        val tvAlpha = findViewById<TextView>(R.id.tvAlpha)
        val sbAlpha = findViewById<SeekBar>(R.id.sbAlpha)

        val tvTickInterval = findViewById<TextView>(R.id.tvTickInterval)
        val sbTickInterval = findViewById<SeekBar>(R.id.sbTickInterval)
        val layoutTickOptions = findViewById<View>(R.id.layoutTickOptions)
        val tvTickGap = findViewById<TextView>(R.id.tvTickGap)
        val sbTickGap = findViewById<SeekBar>(R.id.sbTickGap)
        val spinnerErosionMode = findViewById<Spinner>(R.id.spinnerErosionMode)

        val tvThickness = findViewById<TextView>(R.id.tvThickness)
        val sbThickness = findViewById<SeekBar>(R.id.sbThickness)
        val tvPadding = findViewById<TextView>(R.id.tvPadding)
        val sbPadding = findViewById<SeekBar>(R.id.sbPadding)

        val switchTopBar = findViewById<SwitchMaterial>(R.id.switchTopBar)
        val layoutTopMode = findViewById<View>(R.id.layoutTopMode)
        val spinnerTopMode = findViewById<Spinner>(R.id.spinnerTopMode)
        val switchLeftBar = findViewById<SwitchMaterial>(R.id.switchLeftBar)
        val switchRightBar = findViewById<SwitchMaterial>(R.id.switchRightBar)

        val switchWave = findViewById<SwitchMaterial>(R.id.switchWave)
        val switchAnim = findViewById<SwitchMaterial>(R.id.switchAnimation)
        val switchPulse = findViewById<SwitchMaterial>(R.id.switchPulse)
        val switchHideFull = findViewById<SwitchMaterial>(R.id.switchHideFull)

        val palettes = arrayOf(
            "Classico (Spettro 50 sfumature)",
            "Minimal Monocromatico",
            "Cyberpunk Neon",
            "Pastello Soft"
        )
        spinnerPalette.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, palettes)
        spinnerPalette.setSelection(prefs.getInt("palette", 0).coerceIn(0, palettes.lastIndex))

        val erosionModes = arrayOf(
            "Nero → Bianco (si schiarisce consumandosi)",
            "Bianco → Nero (si scurisce consumandosi)",
            "Colore barra → Bianco",
            "Colore barra → Nero",
            "Disattivata (stesso colore della barra)"
        )
        spinnerErosionMode.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, erosionModes)
        spinnerErosionMode.setSelection(prefs.getInt("erosion_mode", 0).coerceIn(0, erosionModes.lastIndex))

        val topModes = arrayOf(
            "Sinistra → Destra",
            "Destra → Sinistra",
            "Centro → Esterni",
            "Buco Nero (Erosione Notch)"
        )
        spinnerTopMode.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, topModes)
        spinnerTopMode.setSelection(prefs.getInt("top_bar_mode", 0).coerceIn(0, topModes.lastIndex))

        val savedAlpha = prefs.getInt("alpha_pct", 100).coerceIn(10, 100)
        sbAlpha.progress = savedAlpha - 10
        tvAlpha.text = "$savedAlpha%"

        val savedInterval = prefs.getInt("tick_interval_pct", 10)
        val intervalIndex = tickIntervals.indexOf(savedInterval).let { if (it >= 0) it else 5 }
        sbTickInterval.max = tickIntervals.lastIndex
        sbTickInterval.progress = intervalIndex
        tvTickInterval.text = tickIntervalLabels[intervalIndex]
        layoutTickOptions.visibility = if (tickIntervals[intervalIndex] > 0) View.VISIBLE else View.GONE

        val savedGap = prefs.getFloat("tick_gap_dp", 3f).toInt().coerceIn(1, 20)
        sbTickGap.progress = savedGap - 1
        tvTickGap.text = "$savedGap dp"

        val savedThickness = prefs.getFloat("stroke_width", 2f).toInt().coerceIn(1, 50)
        sbThickness.progress = savedThickness - 1
        tvThickness.text = "$savedThickness dp"

        val savedPadding = prefs.getFloat("edge_padding", 0f).toInt().coerceIn(0, 100)
        sbPadding.progress = savedPadding
        tvPadding.text = "$savedPadding dp"

        val showTop = prefs.getBoolean("show_top", true)
        switchTopBar.isChecked = showTop
        layoutTopMode.visibility = if (showTop) View.VISIBLE else View.GONE

        switchLeftBar.isChecked = prefs.getBoolean("show_left", true)
        switchRightBar.isChecked = prefs.getBoolean("show_right", true)
        switchWave.isChecked = prefs.getBoolean("wave_enabled", true)
        switchAnim.isChecked = prefs.getBoolean("anim_enabled", true)
        switchPulse.isChecked = prefs.getBoolean("pulse_enabled", true)
        switchHideFull.isChecked = prefs.getBoolean("hide_full_enabled", false)

        btnPermission.setOnClickListener {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }

        spinnerPalette.onItemSelectedListener = onItemSelected { pos ->
            prefs.edit().putInt("palette", pos).apply()
        }

        spinnerErosionMode.onItemSelectedListener = onItemSelected { pos ->
            prefs.edit().putInt("erosion_mode", pos).apply()
        }

        spinnerTopMode.onItemSelectedListener = onItemSelected { pos ->
            prefs.edit().putInt("top_bar_mode", pos).apply()
        }

        sbAlpha.setOnSeekBarChangeListener(onSeekChanged { progress ->
            val pct = progress + 10
            tvAlpha.text = "$pct%"
            prefs.edit().putInt("alpha_pct", pct).apply()
        })

        sbTickInterval.setOnSeekBarChangeListener(onSeekChanged { progress ->
            val idx = progress.coerceIn(0, tickIntervals.lastIndex)
            val intervalPct = tickIntervals[idx]
            tvTickInterval.text = tickIntervalLabels[idx]
            layoutTickOptions.visibility = if (intervalPct > 0) View.VISIBLE else View.GONE
            prefs.edit().putInt("tick_interval_pct", intervalPct).apply()
        })

        sbTickGap.setOnSeekBarChangeListener(onSeekChanged { progress ->
            val dp = progress + 1
            tvTickGap.text = "$dp dp"
            prefs.edit().putFloat("tick_gap_dp", dp.toFloat()).apply()
        })

        sbThickness.setOnSeekBarChangeListener(onSeekChanged { progress ->
            val dp = progress + 1
            tvThickness.text = "$dp dp"
            prefs.edit().putFloat("stroke_width", dp.toFloat()).apply()
        })

        sbPadding.setOnSeekBarChangeListener(onSeekChanged { progress ->
            tvPadding.text = "$progress dp"
            prefs.edit().putFloat("edge_padding", progress.toFloat()).apply()
        })

        switchTopBar.setOnCheckedChangeListener { _, isChecked ->
            layoutTopMode.visibility = if (isChecked) View.VISIBLE else View.GONE
            prefs.edit().putBoolean("show_top", isChecked).apply()
        }

        switchLeftBar.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("show_left", isChecked).apply()
        }

        switchRightBar.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("show_right", isChecked).apply()
        }

        switchWave.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("wave_enabled", isChecked).apply()
        }

        switchAnim.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("anim_enabled", isChecked).apply()
        }

        switchPulse.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("pulse_enabled", isChecked).apply()
        }

        switchHideFull.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("hide_full_enabled", isChecked).apply()
        }

        btnToggleService.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Concedi prima il permesso di sovraimpressione", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val serviceIntent = Intent(this, OverlayService::class.java)
            if (!OverlayService.isRunning) {
                ContextCompat.startForegroundService(this, serviceIntent)
                updateServiceUi(running = true)
            } else {
                stopService(serviceIntent)
                updateServiceUi(running = false)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val hasPermission = Settings.canDrawOverlays(this)
        cardPermission.visibility = if (hasPermission) View.GONE else View.VISIBLE
        updateServiceUi(OverlayService.isRunning)
    }

    private fun updateServiceUi(running: Boolean) {
        val batteryPct = readCurrentBatteryPct()
        val stateLabel = if (running) "Overlay attivo" else "Overlay spento"
        tvStatusSubtitle.text = "$stateLabel • Batteria $batteryPct%"
        btnToggleService.text = if (running) "Ferma Sovraimpressione" else "Avvia Sovraimpressione"
    }

    private fun readCurrentBatteryPct(): Int {
        val batteryStatus = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level >= 0 && scale > 0) ((level * 100f) / scale).toInt() else 100
    }

    private fun onItemSelected(action: (Int) -> Unit) = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) = action(position)
        override fun onNothingSelected(parent: AdapterView<*>?) {}
    }

    private fun onSeekChanged(action: (Int) -> Unit) = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            if (fromUser) action(progress)
        }
        override fun onStartTrackingTouch(seekBar: SeekBar?) {}
        override fun onStopTrackingTouch(seekBar: SeekBar?) {}
    }
}
