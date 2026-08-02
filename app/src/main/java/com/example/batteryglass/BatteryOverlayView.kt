package com.example.batteryglass

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import android.view.animation.LinearInterpolator

class BatteryOverlayView(context: Context) : View(context) {

    private val paintLeft = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintRight = Paint(Paint.ANTI_ALIAS_FLAG)
    private val evaluator = ArgbEvaluator()

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
        paintLeft.style = Paint.Style.STROKE
        paintLeft.strokeCap = Paint.Cap.ROUND

        paintRight.style = Paint.Style.STROKE
        paintRight.strokeCap = Paint.Cap.ROUND

        strokeWidthPx = 12f
    }

    private fun getBatteryColor(level: Int): Int {
        val fraction = level / 100f
        return if (fraction <= 0.5f) {
            // Da Rosso (0%) a Giallo (50%)
            val subFraction = fraction / 0.5f
            evaluator.evaluate(subFraction, Color.RED, Color.YELLOW) as Int
        } else {
            // Da Giallo (50%) a Verde (100%)
            val subFraction = (fraction - 0.5f) / 0.5f
            evaluator.evaluate(subFraction, Color.YELLOW, Color.GREEN) as Int
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

        // Linea sinistra (Parete bicchiere SX)
        canvas.drawLine(offset, startY, offset, stopY, paintLeft)

        // Linea destra (Parete bicchiere DX)
        canvas.drawLine(width - offset, startY, width - offset, stopY, paintRight)
    }
}
