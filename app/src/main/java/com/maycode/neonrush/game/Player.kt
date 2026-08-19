package com.maycode.neonrush.game

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.sin

class Player(private val screenWidth: Int) {

    var x = screenWidth / 2f
    var y = 0f
    val radius = 38f

    var velocityY = 0f
    var velocityX = 0f
    private val gravity = 0.48f
    private val jumpForce = -17.8f
    private val springForce = -25f
    private val maxFallSpeed = 20f

    var isAlive = true
    var hasShield = false
    private var shieldTimer = 0

    private var pulseTime = 0f
    private val pulseSpeed = 0.18f

    private val trail = mutableListOf<Pair<Float, Float>>()

    // Colores de skin (se actualizan)
    private var bodyColor = 0xFF00F5FF.toInt()
    private var glowColor = 0x5500F5FF.toInt()

    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        style = Paint.Style.FILL
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val shieldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xAAFF00E5.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 7f
    }
    private val trailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    fun setSkin(body: Int, glow: Int) {
        bodyColor = body
        glowColor = glow
        bodyPaint.color = body
        glowPaint.color = glow
        trailPaint.color = (glow and 0x00FFFFFF) or 0x66000000
    }

    fun update() {
        if (!isAlive) return

        velocityY += gravity
        if (velocityY > maxFallSpeed) velocityY = maxFallSpeed

        y += velocityY
        x += velocityX
        velocityX *= 0.90f

        if (x < radius) { x = radius; velocityX = 0f }
        if (x > screenWidth - radius) { x = screenWidth - radius; velocityX = 0f }

        trail.add(0, x to y)
        if (trail.size > 14) trail.removeAt(trail.lastIndex)

        if (hasShield) {
            shieldTimer--
            if (shieldTimer <= 0) hasShield = false
        }

        pulseTime += pulseSpeed
    }

    fun jump(strong: Boolean = false) {
        velocityY = if (strong) springForce else jumpForce
    }

    fun moveLeft() { velocityX = -10.5f }
    fun moveRight() { velocityX = 10.5f }

    fun activateShield(frames: Int = 180) {
        hasShield = true
        shieldTimer = frames
    }

    fun getBounds(): RectF =
        RectF(x - radius * 0.8f, y - radius * 0.8f, x + radius * 0.8f, y + radius * 0.8f)

    fun draw(canvas: Canvas) {
        val pulse = 1f + 0.12f * sin(pulseTime).toFloat()

        trail.forEachIndexed { i, (tx, ty) ->
            val alpha = (170 * (1f - i / 14f)).toInt().coerceIn(0, 170)
            trailPaint.alpha = alpha
            canvas.drawCircle(tx, ty, radius * (0.55f - i * 0.025f) * pulse, trailPaint)
        }

        canvas.drawCircle(x, y, (radius + 16f) * pulse, glowPaint)
        canvas.drawCircle(x, y, radius * pulse, bodyPaint)
        canvas.drawCircle(x, y, radius * 0.36f * pulse, corePaint)

        if (hasShield) {
            canvas.drawCircle(x, y, (radius + 20f) * pulse, shieldPaint)
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
        pulseTime = 0f
    }
}
