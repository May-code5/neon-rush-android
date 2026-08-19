package com.maycode.neonrush.game

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import kotlin.random.Random

class Coin(
    private val screenWidth: Int,
    private val screenHeight: Int,
    var speed: Float
) {
    var x = screenWidth + 50f
    var y = screenHeight * (0.3f + Random.nextFloat() * 0.4f)
    val radius = 25f

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFF00.toInt() // neon yellow
        style = Paint.Style.FILL
    }

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x88FFFF00.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 10f
    }

    fun update() {
        x -= speed
    }

    fun isOffScreen(): Boolean = x + radius < 0

    fun getBounds(): RectF {
        return RectF(x - radius, y - radius, x + radius, y + radius)
    }

    fun draw(canvas: Canvas) {
        canvas.drawCircle(x, y, radius + 6, glowPaint)
        canvas.drawCircle(x, y, radius, paint)
    }
}
