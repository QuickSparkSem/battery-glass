package com.example.batteryglass

import android.animation.ValueAnimator
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.sin

class BatteryOverlayView(context: Context) : View(context) {

    enum class Palette { CLASSIC, MINIMAL, CYBERPUNK, PASTEL }

    var palette: Palette = Palette.CLASSIC
        set(value) { field = value; invalidate() }

    var edgePaddingPx: Float = 0f
        set(value) { field = value; invalidate() }

    var userAlpha: Int = 255
        set(value) { field = value.coerceIn(0, 255); invalidate() }

    var showTop: Boolean = true
        set(value) { field = value; updateAnimators(); invalidate() }

    var showLeft: Boolean = true
        set(value) { field = value; updateAnimators(); invalidate() }

    var showRight: Boolean = true
        set(value) { field = value; updateAnimators(); invalidate() }

    var topBarMode: Int = 0 // 0=L->R, 1=R->L, 2=Center->Out, 3=BlackHole
        set(value) { field = value; invalidate() }

    var waveEnabled: Boolean = true
        set(value) { field = value; updateAnimators(); invalidate() }

    var lowBatteryPulseEnabled: Boolean = true
        set(value) { field = value; updateAnimators(); invalidate() }

    var hideOnFullEnabled: Boolean = false
        set(value) { field = value; updateAnimators(); invalidate() }

    var isScreenOn: Boolean = true
        set(value) { field = value; updateAnimators() }

    var batteryLevel: Int = 100
        set(value) {
            field = value.coerceIn(0, 100)
            updateAnimators()
            invalidate()
        }

    var strokeWidthPx: Float = 1f
        set(value) {
            field = value.coerceAtLeast(1f)
            paint.strokeWidth = field
            invalidate()
        }

    var isCharging: Boolean = false
        set(value) {
            field = value
            updateAnimators()
            invalidate()
        }

    var animEnabled: Boolean = true
        set(value) {
            field = value
            updateAnimators()
            invalidate()
        }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 1f
    }

    private val wavePath = Path()
    private val hsvArray = floatArrayOf(0f, 0.95f, 1.0f)

    private var pulseAlpha: Int = 255
    private var lowBatteryAlpha: Int = 255
    private var wavePhase: Float = 0f

    private var chargingAnimator: ValueAnimator? = null
    private var lowBatteryAnimator: ValueAnimator? = null
    private var waveAnimator: ValueAnimator? = null

    fun applyPreferences(prefs: SharedPreferences, density: Float) {
        strokeWidthPx = prefs.getFloat("stroke_width", 2f).coerceAtLeast(1f) * density
        edgePaddingPx = prefs.getFloat("edge_padding", 0f).coerceAtLeast(0f) * density
        userAlpha = (prefs.getInt("alpha_pct", 100).coerceIn(10, 100) * 255) / 100
        showTop = prefs.getBoolean("show_top", true)
        topBarMode = prefs.getInt("top_bar_mode", 0).coerceIn(0, 3)
        showLeft = prefs.getBoolean("show_left", true)
        showRight = prefs.getBoolean("show_right", true)
        val paletteIndex = prefs.getInt("palette", 0)
        palette = Palette.values().getOrElse(paletteIndex) { Palette.CLASSIC }
        animEnabled = prefs.getBoolean("anim_enabled", true)
        waveEnabled = prefs.getBoolean("wave_enabled", true)
        lowBatteryPulseEnabled = prefs.getBoolean("pulse_enabled", true)
        hideOnFullEnabled = prefs.getBoolean("hide_full_enabled", false)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateAnimators()
    }

    override fun onDetachedFromWindow() {
        stopAllAnimators()
        super.onDetachedFromWindow()
    }

    private fun isHiddenByFullCharge(): Boolean =
        hideOnFullEnabled && batteryLevel == 100 && !isCharging

    private fun updateAnimators() {
        val hasVisibleBar = showLeft || showRight || showTop
        if (!isAttachedToWindow || !isScreenOn || !hasVisibleBar || isHiddenByFullCharge()) {
            stopAllAnimators()
            return
        }

        if (waveEnabled) startWaveAnimation() else stopWaveAnimation()

        if (isCharging && animEnabled) {
            startChargingAnimation()
        } else {
            stopChargingAnimation()
        }

        if (batteryLevel <= 15 && !isCharging && lowBatteryPulseEnabled) {
            startLowBatteryPulse()
        } else {
            stopLowBatteryPulse()
        }
    }

    private fun stopAllAnimators() {
        stopWaveAnimation()
        stopChargingAnimation()
        stopLowBatteryPulse()
    }

    private fun blendColor(startColor: Int, endColor: Int, fraction: Float): Int {
        val f = fraction.coerceIn(0f, 1f)
        val r = (Color.red(startColor) + f * (Color.red(endColor) - Color.red(startColor))).toInt()
        val g = (Color.green(startColor) + f * (Color.green(endColor) - Color.green(startColor))).toInt()
        val b = (Color.blue(startColor) + f * (Color.blue(endColor) - Color.blue(startColor))).toInt()
        return Color.rgb(r, g, b)
    }

    private fun getBatteryColor(level: Int): Int {
        val stepLevel = (level / 2) * 2
        val fraction = stepLevel / 100f
        return when (palette) {
            Palette.CLASSIC -> {
                hsvArray[0] = 120f * fraction
                Color.HSVToColor(hsvArray)
            }
            Palette.MINIMAL -> blendColor(Color.parseColor("#444444"), Color.WHITE, fraction)
            Palette.CYBERPUNK -> if (fraction <= 0.5f) {
                blendColor(Color.parseColor("#FF007F"), Color.parseColor("#8A2BE2"), fraction / 0.5f)
            } else {
                blendColor(Color.parseColor("#8A2BE2"), Color.parseColor("#00F5FF"), (fraction - 0.5f) / 0.5f)
            }
            Palette.PASTEL -> if (fraction <= 0.5f) {
                blendColor(Color.parseColor("#FF8B8B"), Color.parseColor("#FFE699"), fraction / 0.5f)
            } else {
                blendColor(Color.parseColor("#FFE699"), Color.parseColor("#98FFB3"), (fraction - 0.5f) / 0.5f)
            }
        }
    }

    private fun startChargingAnimation() {
        if (chargingAnimator?.isRunning == true) return
        chargingAnimator = ValueAnimator.ofInt(100, 255).apply {
            duration = 1000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
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
            duration = 1600
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
        if (isHiddenByFullCharge()) return
        if (!showLeft && !showRight && !showTop) return

        var currentAlpha = if (isCharging && animEnabled) pulseAlpha else 255
        if (batteryLevel <= 15 && !isCharging && lowBatteryPulseEnabled) {
            currentAlpha = lowBatteryAlpha
        }

        paint.color = getBatteryColor(batteryLevel)
        paint.alpha = ((currentAlpha * userAlpha) / 255).coerceIn(0, 255)

        val h = height.toFloat()
        val w = width.toFloat()
        val pct = batteryLevel / 100f
        val fillHeight = h * pct
        val stopY = h - fillHeight
        val halfStroke = strokeWidthPx / 2f
        val xLeft = halfStroke + edgePaddingPx
        val xRight = w - halfStroke - edgePaddingPx
        val topY = halfStroke
        val waveAmplitude = (strokeWidthPx * 0.4f).coerceAtLeast(2f)

        if (showLeft && fillHeight > 1f) {
            drawVerticalBar(canvas, xLeft, h, stopY, phaseOffset = 0f, waveAmplitude)
        }

        if (showRight && fillHeight > 1f) {
            drawVerticalBar(canvas, xRight, h, stopY, phaseOffset = 2f, waveAmplitude)
        }

        if (showTop && pct > 0f) {
            val totalW = (xRight - xLeft).coerceAtLeast(0f)
            val centerX = xLeft + totalW / 2f
            when (topBarMode) {
                0 -> drawTopSegment(canvas, xLeft, xLeft + totalW * pct, topY, waveAmplitude)
                1 -> drawTopSegment(canvas, xRight - totalW * pct, xRight, topY, waveAmplitude)
                2 -> {
                    val halfW = (totalW * pct) / 2f
                    drawTopSegment(canvas, centerX - halfW, centerX + halfW, topY, waveAmplitude)
                }
                3 -> {
                    val emptyHalfW = (totalW * (1f - pct)) / 2f
                    drawTopSegment(canvas, xLeft, centerX - emptyHalfW, topY, waveAmplitude)
                    drawTopSegment(canvas, centerX + emptyHalfW, xRight, topY, waveAmplitude)
                }
            }
        }
    }

    private fun drawVerticalBar(
        canvas: Canvas,
        x: Float,
        startY: Float,
        stopY: Float,
        phaseOffset: Float,
        amplitude: Float
    ) {
        if (waveEnabled && (startY - stopY) > 14f) {
            wavePath.reset()
            wavePath.moveTo(x, startY)
            var y = startY - 14f
            while (y > stopY) {
                val wobble = sin(y * 0.015f + wavePhase * 1.2f + phaseOffset) * 0.6f +
                        sin(y * 0.03f - wavePhase * 0.8f + phaseOffset * 0.5f) * 0.4f
                wavePath.lineTo(x + wobble * amplitude, y)
                y -= 14f
            }
            wavePath.lineTo(x, stopY)
            canvas.drawPath(wavePath, paint)
        } else {
            canvas.drawLine(x, startY, x, stopY, paint)
        }
    }

    private fun drawTopSegment(
        canvas: Canvas,
        startX: Float,
        stopX: Float,
        topY: Float,
        amplitude: Float
    ) {
        if (stopX - startX < 1f) return
        if (waveEnabled && (stopX - startX) > 14f) {
            wavePath.reset()
            wavePath.moveTo(startX, topY)
            var x = startX + 14f
            while (x < stopX) {
                val wobble = sin(x * 0.015f + wavePhase * 1.2f) * 0.6f +
                        sin(x * 0.03f - wavePhase * 0.8f) * 0.4f
                wavePath.lineTo(x, topY + wobble * amplitude)
                x += 14f
            }
            wavePath.lineTo(stopX, topY)
            canvas.drawPath(wavePath, paint)
        } else {
            canvas.drawLine(startX, topY, stopX, topY, paint)
        }
    }
}
