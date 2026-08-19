package com.maycode.neonrush.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.maycode.neonrush.SkinManager
import kotlin.random.Random

class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback, Runnable {

    interface GameListener {
        fun onScoreChanged(score: Int, coins: Int)
        fun onGameOver(finalScore: Int, coinsCollected: Int)
    }

    var listener: GameListener? = null

    private var thread: Thread? = null
    @Volatile private var isRunning = false
    @Volatile private var isPaused = false
    @Volatile private var surfaceReady = false

    private var player: Player? = null
    private val platforms = mutableListOf<Platform>()
    private val coins = mutableListOf<Coin>()
    private val lock = Any()

    private var score = 0
    private var coinsCollected = 0
    private var cameraY = 0f

    private var screenW = 0
    private var screenH = 0

    private var lastTouchX = 0f
    private var isTouching = false

    private val stars = mutableListOf<Triple<Float, Float, Float>>()
    private val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF88AADD.toInt() }

    init {
        holder.addCallback(this)
        isFocusable = true
    }

    override fun surfaceCreated(holder: SurfaceHolder) { surfaceReady = true }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        screenW = width
        screenH = height
        synchronized(lock) {
            player = Player(screenW)
            // Aplicar skin seleccionada
            val skin = SkinManager.getSelectedSkin(context)
            player?.setSkin(skin.color, skin.glowColor)
            player?.reset(screenH * 0.72f)
            generateInitialPlatforms()
            generateStars()
        }
        surfaceReady = true
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        surfaceReady = false
        stop()
    }

    private fun generateStars() {
        stars.clear()
        repeat(50) {
            stars.add(Triple(Random.nextFloat() * screenW, Random.nextFloat() * screenH * 4, Random.nextFloat() * 2.2f + 0.7f))
        }
    }

    private fun generateInitialPlatforms() {
        platforms.clear()
        platforms.add(Platform(screenW / 2f - 100f, screenH * 0.78f, 200f, PlatformType.NORMAL))

        var currentY = screenH * 0.78f
        repeat(18) {
            currentY -= Random.nextFloat() * 90f + 110f
            val width = Random.nextFloat() * 60f + 130f
            val x = Random.nextFloat() * (screenW - width - 60f) + 30f
            val type = when {
                Random.nextFloat() < 0.10f -> PlatformType.SPRING
                Random.nextFloat() < 0.15f -> PlatformType.MOVING
                Random.nextFloat() < 0.12f -> PlatformType.BREAKABLE
                else -> PlatformType.NORMAL
            }
            platforms.add(Platform(x, currentY, width, type))
        }
    }

    fun startGame() {
        if (isRunning) return
        isRunning = true
        isPaused = false
        thread = Thread(this, "NeonHopThread")
        thread?.start()
    }

    fun pause() { isPaused = true }
    fun resume() { isPaused = false }

    fun stop() {
        isRunning = false
        try { thread?.join(600) } catch (_: InterruptedException) {}
        thread = null
    }

    fun revive() {
        synchronized(lock) {
            player?.let {
                it.isAlive = true
                it.activateShield(200)
                it.velocityY = -16f
            }
        }
    }

    override fun run() {
        while (isRunning) {
            if (isPaused || !surfaceReady || screenW <= 0) {
                try { Thread.sleep(30) } catch (_: InterruptedException) {}
                continue
            }
            update()
            drawFrame()
            try { Thread.sleep(14) } catch (_: InterruptedException) {}
        }
    }

    private fun update() {
        val p = player ?: return
        if (!p.isAlive) return

        synchronized(lock) {
            p.update()

            val targetCamera = p.y - screenH * 0.5f
            if (targetCamera < cameraY) {
                cameraY += (targetCamera - cameraY) * 0.12f
            }

            val heightScore = ((-cameraY) / 35f).toInt()
            if (heightScore > score) score = heightScore

            platforms.forEach { it.update(screenW) }

            if (p.velocityY > 0) {
                val playerBounds = p.getBounds()
                for (plat in platforms) {
                    if (plat.broken) continue
                    val pb = plat.getBounds()
                    if (playerBounds.bottom >= pb.top - 8f &&
                        playerBounds.bottom <= pb.top + 35f &&
                        playerBounds.right > pb.left + 8f &&
                        playerBounds.left < pb.right - 8f) {

                        when (plat.type) {
                            PlatformType.BREAKABLE -> { plat.broken = true; p.jump(false) }
                            PlatformType.SPRING -> p.jump(true)
                            else -> p.jump(false)
                        }
                        break
                    }
                }
            }

            val coinIt = coins.iterator()
            while (coinIt.hasNext()) {
                val c = coinIt.next()
                if (rectsIntersect(p.getBounds(), c.getBounds())) {
                    coinsCollected++
                    coinIt.remove()
                }
            }

            val highest = platforms.minOfOrNull { it.y } ?: p.y
            if (highest > cameraY - 80) {
                val width = Random.nextFloat() * 50f + 130f
                val x = Random.nextFloat() * (screenW - width - 50f) + 25f
                val y = highest - Random.nextFloat() * 80f - 115f
                val type = when {
                    Random.nextFloat() < 0.11f -> PlatformType.SPRING
                    Random.nextFloat() < 0.16f -> PlatformType.MOVING
                    Random.nextFloat() < 0.13f -> PlatformType.BREAKABLE
                    else -> PlatformType.NORMAL
                }
                platforms.add(Platform(x, y, width, type))
                if (Random.nextFloat() < 0.5f) coins.add(Coin(x + width / 2, y - 45f))
            }

            platforms.removeAll { it.y > cameraY + screenH + 120 }
            coins.removeAll { it.y > cameraY + screenH + 120 }

            if (p.y > cameraY + screenH + 120) {
                p.isAlive = false
                listener?.onGameOver(score, coinsCollected)
            }
        }

        listener?.onScoreChanged(score, coinsCollected)
    }

    private fun drawFrame() {
        if (!surfaceReady || screenW <= 0) return
        val canvas = try { holder.lockCanvas() } catch (_: Exception) { null } ?: return

        try {
            canvas.drawColor(0xFF07070F.toInt())

            stars.forEach { (sx, sy, size) ->
                val drawY = (sy - cameraY * 0.25f) % (screenH + 50) - 25
                starPaint.alpha = (130 + size * 35).toInt().coerceIn(70, 210)
                canvas.drawCircle(sx, drawY, size, starPaint)
            }

            canvas.save()
            canvas.translate(0f, -cameraY)

            synchronized(lock) {
                platforms.forEach { it.draw(canvas) }
                coins.forEach { it.draw(canvas) }
                player?.draw(canvas)
            }

            canvas.restore()
        } catch (_: Exception) {
        } finally {
            try { holder.unlockCanvasAndPost(canvas) } catch (_: Exception) {}
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        synchronized(lock) {
            val p = player ?: return true
            if (!p.isAlive) return true

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastTouchX = event.x
                    isTouching = true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (isTouching) {
                        val deltaX = event.x - lastTouchX
                        if (deltaX < -4f) p.moveLeft()
                        else if (deltaX > 4f) p.moveRight()
                        lastTouchX = event.x
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> isTouching = false
            }
        }
        return true
    }

    private fun rectsIntersect(a: RectF, b: RectF): Boolean {
        return a.left < b.right && a.right > b.left && a.top < b.bottom && a.bottom > b.top
    }
}
