package com.maycode.neonrush.game

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF

class Player(private val screenWidth: Int) {

    var x = screenWidth / 2f
    var y = 0f
    val radius = 36f

    var velocityY = 0f
    var velocityX = 0f
    private val gravity = 0.55f
    private val jumpForce = -18.5f
    private val springForce = -26f
    private val maxFallSpeed = 22f

    var isAlive = true
    var hasShield = false
    private var shieldTimer = 0

    // Trail effect
    private val trail = mutableListOf<Pair<Float, Float>>()

    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF00F5FF.toInt()
        style = Paint.Style.FILL
    }
    private val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        style = Paint.Style.FILL
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x5500F5FF.toInt()
        style = Paint.Style.FILL
    }
    private val shieldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xAAFF00E5.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 7f
    }
    private val trailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x6600F5FF.toInt()
        style = Paint.Style.FILL
    }

    fun update() {
        if (!isAlive) return

        velocityY += gravity
        if (velocityY > maxFallSpeed) velocityY = maxFallSpeed

        y += velocityY
        x += velocityX

        // Soft friction on horizontal
        velocityX *= 0.92f

        // Keep inside screen horizontally
        if (x < radius) {
            x = radius
            velocityX = 0f
        }
        if (x > screenWidth - radius) {
            x = screenWidth - radius
            velocityX = 0f
        }

        // Trail
        trail.add(0, x to y)
        if (trail.size > 12) trail.removeAt(trail.lastIndex)

        if (hasShield) {
            shieldTimer--
            if (shieldTimer <= 0) hasShield = false
        }
    }

    fun jump(strong: Boolean = false) {
        velocityY = if (strong) springForce else jumpForce
    }

    fun moveLeft() {
        velocityX = -9f
    }

    fun moveRight() {
        velocityX = 9f
    }

    fun activateShield(frames: Int = 150) {
        hasShield = true
        shieldTimer = frames
    }

    fun getBounds(): RectF {
        return RectF(x - radius * 0.75f, y - radius * 0.75f, x + radius * 0.75f, y + radius * 0.75f)
    }

    fun draw(canvas: Canvas) {
        // Trail
        trail.forEachIndexed { i, (tx, ty) ->
            val alpha = (180 * (1f - i / 12f)).toInt().coerceIn(0, 180)
            trailPaint.alpha = alpha
            canvas.drawCircle(tx, ty, radius * (0.6f - i * 0.03f), trailPaint)
        }

        // Outer glow
        canvas.drawCircle(x, y, radius + 14f, glowPaint)

        // Body
        canvas.drawCircle(x, y, radius, bodyPaint)

        // Core
        canvas.drawCircle(x, y, radius * 0.38f, corePaint)

        // Shield
        if (hasShield) {
            canvas.drawCircle(x, y, radius + 18f, shieldPaint)
        }
    }

    fun reset(startY: Float) {
        x = screenWidth / 2f
        y = startY
        velocityY = 0f
        velocityX = 0f
        isAlive = true
        hasShield = false
        shieldTimer = 0
        trail.clear()
    }
}
