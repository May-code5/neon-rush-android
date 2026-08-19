package com.maycode.neonrush.game

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF

class Coin(var x: Float, var y: Float) {

    private val radius = 18f

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFD700.toInt()
        style = Paint.Style.FILL
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x66FFD700.toInt()
        style = Paint.Style.FILL
    }
    private val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFF0.toInt()
        style = Paint.Style.FILL
    }

    fun getBounds(): RectF {
        return RectF(x - radius, y - radius, x + radius, y + radius)
    }

    fun draw(canvas: Canvas) {
        canvas.drawCircle(x, y, radius + 8f, glowPaint)
        canvas.drawCircle(x, y, radius, paint)
        canvas.drawCircle(x, y, radius * 0.4f, corePaint)
    }
}
