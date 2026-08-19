package com.maycode.neonrush.game

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import kotlin.random.Random

class Obstacle(
    private val screenWidth: Int,
    private val screenHeight: Int,
    var speed: Float
) {
    var x = screenWidth + 100f
    val width = 60f + Random.nextFloat() * 40f
    val height = 80f + Random.nextFloat() * 120f
    val y = screenHeight * 0.75f - height / 2 // apoyado en el "suelo"

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFF2D95.toInt() // neon pink
        style = Paint.Style.FILL
    }

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFF00E5.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }

    fun update() {
        x -= speed
    }

    fun isOffScreen(): Boolean = x + width < 0

    fun getBounds(): RectF {
        return RectF(x, y - height / 2, x + width, y + height / 2)
    }

    fun draw(canvas: Canvas) {
        val rect = getBounds()
        canvas.drawRoundRect(rect, 12f, 12f, paint)
        canvas.drawRoundRect(rect, 12f, 12f, strokePaint)
    }
}
