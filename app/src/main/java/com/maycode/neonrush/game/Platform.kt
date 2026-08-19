package com.maycode.neonrush.game

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import kotlin.random.Random

enum class PlatformType {
    NORMAL, MOVING, BREAKABLE, SPRING
}

class Platform(
    var x: Float,
    var y: Float,
    val width: Float,
    val type: PlatformType = PlatformType.NORMAL
) {
    var speedX = if (type == PlatformType.MOVING) (if (Random.nextBoolean()) 3f else -3f) else 0f
    var broken = false
    private val height = 22f

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = when (type) {
            PlatformType.NORMAL -> 0xFF00F5FF.toInt()
            PlatformType.MOVING -> 0xFFFF00E5.toInt()
            PlatformType.BREAKABLE -> 0xFFFF6B35.toInt()
            PlatformType.SPRING -> 0xFF39FF14.toInt()
        }
    }

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        color = paint.color
        alpha = 90
    }

    fun update(screenWidth: Int) {
        if (type == PlatformType.MOVING && !broken) {
            x += speedX
            if (x < 40f || x + width > screenWidth - 40f) {
                speedX = -speedX
            }
        }
    }

    fun getBounds(): RectF = RectF(x, y, x + width, y + height)

    fun draw(canvas: Canvas) {
        if (broken) return
        val rect = getBounds()
        canvas.drawRoundRect(rect, 12f, 12f, glowPaint)
        canvas.drawRoundRect(rect, 10f, 10f, paint)

        // Small shine line
        val shine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x55FFFFFF
            strokeWidth = 3f
        }
        canvas.drawLine(x + 12f, y + 6f, x + width - 12f, y + 6f, shine)
    }
}
