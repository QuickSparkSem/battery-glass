package com.example.batteryglass

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import android.view.animation.LinearInterpolator

class BatteryOverlayView(context: Context) : View(context) {

    // Paint dichiarati a livello di classe e riutilizzati
    private val paintLeft = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val paintRight = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    var batteryLevel: Int = 100
        set(value) {
            field = value.coerceIn(0, 100)
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

    private var pulseAlpha: Int = 255
    private var animator: ValueAnimator? = null

    init {
        strokeWidthPx = 12f
    }

    // Interpolazione matematica RGB ad altissime prestazioni (senza creare oggetti ArgbEvaluator)
    private fun blendColor(startColor: Int, endColor: Int, fraction: Float): Int {
        val f = fraction.coerceIn(0f, 1f)
        val r = (Color.red(startColor) + f * (Color.red(endColor) - Color.red(startColor))).toInt()
        val g = (Color.green(startColor) + f * (Color.green(endColor) - Color.green(startColor))).toInt()
        val b = (Color.blue(startColor) + f * (Color.blue(endColor) - Color.blue(startColor))).toInt()
        return Color.rgb(r, g, b)
    }

    private fun getBatteryColor(level: Int): Int {
        val fraction = level / 100f
        return if (fraction <= 0.5f) {
            // Da Rosso (0%) a Giallo (50%)
            blendColor(Color.RED, Color.YELLOW, fraction / 0.5f)
        } else {
            // Da Giallo (50%) a Verde (100%)
            blendColor(Color.YELLOW, Color.GREEN, (fraction - 0.5f) / 0.5f)
        }
    }

    private fun startChargingAnimation() {
        if (animator?.isRunning == true) return
        animator = ValueAnimator.ofInt(100, 255).apply {
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
        animator?.cancel()
        animator = null
        pulseAlpha = 255
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val currentColor = getBatteryColor(batteryLevel)
        val alpha = if (isCharging && animEnabled) pulseAlpha else 255

        paintLeft.color = currentColor
        paintLeft.alpha = alpha
        paintRight.color = currentColor
        paintRight.alpha = alpha

        val h = height.toFloat()
        val fillHeight = h * (batteryLevel / 100f)
        val startY = h
        val stopY = h - fillHeight

        val offset = strokeWidthPx / 2f

        // Parete sinistra del bicchiere
        canvas.drawLine(offset, startY, offset, stopY, paintLeft)

        // Parete destra del bicchiere
        canvas.drawLine(width - offset, startY, width - offset, stopY, paintRight)
    }
}
