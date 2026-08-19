package com.maycode.neonrush.game

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF

class Player(private val screenWidth: Int, private val screenHeight: Int) {

    var x = screenWidth * 0.25f
    var y = screenHeight * 0.6f
    val width = 80f
    val height = 80f

    private var velocityY = 0f
    private val gravity = 1.8f
    private val jumpForce = -32f
    private val groundY = screenHeight * 0.75f

    var isAlive = true
    var hasShield = false
    private var shieldTimer = 0

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF00F5FF.toInt() // neon cyan
        style = Paint.Style.FILL
    }

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x8800F5FF.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 12f
    }

    private val shieldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xAAFF00E5.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 8f
    }

    fun jump() {
        if (y >= groundY - 5) { // solo desde el suelo
            velocityY = jumpForce
        }
    }

    fun update() {
        if (!isAlive) return

        velocityY += gravity
        y += velocityY

        // Suelo
        if (y > groundY) {
            y = groundY
            velocityY = 0f
        }

        // Techo
        if (y < 50f) {
            y = 50f
            velocityY = 0f
        }

        if (hasShield) {
            shieldTimer--
            if (shieldTimer <= 0) hasShield = false
        }
    }

    fun activateShield(durationFrames: Int = 180) {
        hasShield = true
        shieldTimer = durationFrames
    }

    fun getBounds(): RectF {
        return RectF(x - width / 2, y - height / 2, x + width / 2, y + height / 2)
    }

    fun draw(canvas: Canvas) {
        // Glow
        canvas.drawCircle(x, y, width / 2 + 8, glowPaint)

        // Cuerpo (círculo neón)
        canvas.drawCircle(x, y, width / 2, paint)

        // Escudo
        if (hasShield) {
            canvas.drawCircle(x, y, width / 2 + 20, shieldPaint)
        }
    }

    fun reset() {
        x = screenWidth * 0.25f
        y = groundY
        velocityY = 0f
        isAlive = true
        hasShield = false
        shieldTimer = 0
    }
}
