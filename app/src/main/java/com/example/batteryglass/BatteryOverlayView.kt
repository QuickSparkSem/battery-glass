package com.example.batteryglass

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.sin

class BatteryOverlayView(context: Context) : View(context) {

    // Palette Colori
    enum class Palette { CLASSIC, MINIMAL, CYBERPUNK, PASTEL }

    var palette: Palette = Palette.CLASSIC
        set(value) {
            field = value
            invalidate()
        }

    var edgePaddingPx: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    var waveEnabled: Boolean = true
        set(value) {
            field = value
            if (value) startWaveAnimation() else stopWaveAnimation()
            invalidate()
        }

    var lowBatteryPulseEnabled: Boolean = true
        set(value) {
            field = value
            checkLowBatteryPulse()
            invalidate()
        }

    var hideOnFullEnabled: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    var batteryLevel: Int = 100
        set(value) {
            field = value.coerceIn(0, 100)
            checkLowBatteryPulse()
            invalidate()
        }

    var strokeWidthPx: Float = 12f
        set(value) {
            field = value
            paintLeft.strokeWidth = value
            paintRight.strokeWidth = value
            invalidate()
        }

    var isCharging: Boolean = false
        set(value) {
            field = value
            if (value && animEnabled) {
                startChargingAnimation()
            } else {
                stopChargingAnimation()
            }
            checkLowBatteryPulse()
            invalidate()
        }

    var animEnabled: Boolean = true
        set(value) {
            field = value
            if (isCharging && value) {
                startChargingAnimation()
            } else {
                stopChargingAnimation()
            }
            invalidate()
        }

    // Oggetti di disegno riciclati (Zero allocazioni in onDraw)
    private val paintLeft = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val paintRight = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val wavePathLeft = Path()
    private val wavePathRight = Path()

    private var pulseAlpha: Int = 255
    private var lowBatteryAlpha: Int = 255
    private var wavePhase: Float = 0f

    private var chargingAnimator: ValueAnimator? = null
    private var lowBatteryAnimator: ValueAnimator? = null
    private var waveAnimator: ValueAnimator? = null

    init {
        strokeWidthPx = 12f
        startWaveAnimation()
    }

    private fun checkLowBatteryPulse() {
        if (batteryLevel <= 15 && !isCharging && lowBatteryPulseEnabled) {
            startLowBatteryPulse()
        } else {
            stopLowBatteryPulse()
        }
    }

    private fun blendColor(startColor: Int, endColor: Int, fraction: Float): Int {
        val f = fraction.coerceIn(0f, 1f)
        val r = (Color.red(startColor) + f * (Color.red(endColor) - Color.red(startColor))).toInt()
        val g = (Color.green(startColor) + f * (Color.green(endColor) - Color.green(startColor))).toInt()
        val b = (Color.blue(startColor) + f * (Color.blue(endColor) - Color.blue(startColor))).toInt()
        return Color.rgb(r, g, b)
    }

    private fun getBatteryColor(level: Int): Int {
        val fraction = level / 100f
        return when (palette) {
            Palette.CLASSIC -> {
                if (fraction <= 0.5f) blendColor(Color.RED, Color.YELLOW, fraction / 0.5f)
                else blendColor(Color.YELLOW, Color.GREEN, (fraction - 0.5f) / 0.5f)
            }
            Palette.MINIMAL -> {
                blendColor(Color.parseColor("#444444"), Color.WHITE, fraction)
            }
            Palette.CYBERPUNK -> {
                if (fraction <= 0.5f) blendColor(Color.parseColor("#FF007F"), Color.parseColor("#8A2BE2"), fraction / 0.5f)
                else blendColor(Color.parseColor("#8A2BE2"), Color.parseColor("#00F5FF"), (fraction - 0.5f) / 0.5f)
            }
            Palette.PASTEL -> {
                if (fraction <= 0.5f) blendColor(Color.parseColor("#FF8B8B"), Color.parseColor("#FFE699"), fraction / 0.5f)
                else blendColor(Color.parseColor("#FFE699"), Color.parseColor("#98FFB3"), (fraction - 0.5f) / 0.5f)
            }
        }
    }

    private fun startChargingAnimation() {
        if (chargingAnimator?.isRunning == true) return
        chargingAnimator = ValueAnimator.ofInt(100, 255).apply {
            duration = 1000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = LinearInterpolator()
            addUpdateListener { anim ->
                pulseAlpha = anim.animatedValue as Int
                invalidate()
            }
            start()
        }
    }

    private fun stopChargingAnimation() {
        chargingAnimator?.cancel()
        chargingAnimator = null
        pulseAlpha = 255
    }

    private fun startLowBatteryPulse() {
        if (lowBatteryAnimator?.isRunning == true) return
        lowBatteryAnimator = ValueAnimator.ofInt(80, 255).apply {
            duration = 1200
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = LinearInterpolator()
            addUpdateListener { anim ->
                lowBatteryAlpha = anim.animatedValue as Int
                invalidate()
            }
            start()
        }
    }

    private fun stopLowBatteryPulse() {
        lowBatteryAnimator?.cancel()
        lowBatteryAnimator = null
        lowBatteryAlpha = 255
    }

    private fun startWaveAnimation() {
        if (waveAnimator?.isRunning == true) return
        waveAnimator = ValueAnimator.ofFloat(0f, (2 * Math.PI).toFloat()).apply {
            duration = 1500
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { anim ->
                wavePhase = anim.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun stopWaveAnimation() {
        waveAnimator?.cancel()
        waveAnimator = null
        wavePhase = 0f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Opzione: Nascondi se al 100% e non in carica
        if (hideOnFullEnabled && batteryLevel == 100 && !isCharging) {
            return
        }

        val currentColor = getBatteryColor(batteryLevel)
        var alpha = if (isCharging && animEnabled) pulseAlpha else 255
        if (batteryLevel <= 15 && !isCharging && lowBatteryPulseEnabled) {
            alpha = lowBatteryAlpha
        }

        paintLeft.color = currentColor
        paintLeft.alpha = alpha
        paintRight.color = currentColor
        paintRight.alpha = alpha

        val h = height.toFloat()
        val fillHeight = h * (batteryLevel / 100f)
        val startY = h
        val stopY = h - fillHeight

        val xLeft = (strokeWidthPx / 2f) + edgePaddingPx
        val xRight = width - (strokeWidthPx / 2f) - edgePaddingPx

        if (waveEnabled && fillHeight > 10f) {
            // Effetto Onda Liquida sulla superficie superiore
            val waveAmplitude = 8f
            
            wavePathLeft.reset()
            wavePathLeft.moveTo(xLeft, startY)
            var y = startY
            while (y >= stopY) {
                val waveOffset = sin((y * 0.02f) + wavePhase) * waveAmplitude
                wavePathLeft.lineTo(xLeft + waveOffset, y)
                y -= 10f
            }
            canvas.drawPath(wavePathLeft, paintLeft)

            wavePathRight.reset()
            wavePathRight.moveTo(xRight, startY)
            y = startY
            while (y >= stopY) {
                val waveOffset = sin((y * 0.02f) + wavePhase + 1.5f) * waveAmplitude
                wavePathRight.lineTo(xRight + waveOffset, y)
                y -= 10f
            }
            canvas.drawPath(wavePathRight, paintRight)

        } else {
            // Linee diritte standard
            canvas.drawLine(xLeft, startY, xLeft, stopY, paintLeft)
            canvas.drawLine(xRight, startY, xRight, stopY, paintRight)
        }
    }
}
