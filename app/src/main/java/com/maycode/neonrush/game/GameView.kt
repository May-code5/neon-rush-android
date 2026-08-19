package com.maycode.neonrush.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
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
    private var maxHeight = 0f
    private var cameraY = 0f

    private var screenW = 0
    private var screenH = 0

    // Background stars
    private val stars = mutableListOf<Triple<Float, Float, Float>>() // x, y, size

    private val bgPaint = Paint().apply { color = 0xFF07070F.toInt() }
    private val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF88AADD.toInt() }
    private val groundLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x2200F5FF
        strokeWidth = 2f
    }

    init {
        holder.addCallback(this)
        isFocusable = true
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        surfaceReady = true
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        screenW = width
        screenH = height
        synchronized(lock) {
            player = Player(screenW)
            player?.reset(screenH * 0.7f)
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
        repeat(60) {
            stars.add(Triple(
                Random.nextFloat() * screenW,
                Random.nextFloat() * screenH * 3,
                Random.nextFloat() * 2.5f + 0.8f
            ))
        }
    }

    private fun generateInitialPlatforms() {
        platforms.clear()
        // Starting platform
        platforms.add(Platform(screenW / 2f - 80f, screenH * 0.75f, 160f, PlatformType.NORMAL))

        var currentY = screenH * 0.75f
        repeat(14) {
            currentY -= Random.nextFloat() * 140f + 90f
            val width = Random.nextFloat() * 70f + 110f
            val x = Random.nextFloat() * (screenW - width - 80f) + 40f
            val type = when {
                Random.nextFloat() < 0.12f -> PlatformType.SPRING
                Random.nextFloat() < 0.18f -> PlatformType.MOVING
                Random.nextFloat() < 0.15f -> PlatformType.BREAKABLE
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
                it.activateShield(180)
                it.velocityY = -14f
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

            // Camera follows player upward
            val targetCamera = p.y - screenH * 0.45f
            if (targetCamera < cameraY) {
                cameraY = targetCamera
            }

            // Score based on height
            val heightScore = ((-cameraY) / 40f).toInt()
            if (heightScore > score) score = heightScore

            // Update platforms
            platforms.forEach { it.update(screenW) }

            // Collision with platforms (only when falling)
            if (p.velocityY > 0) {
                val playerBounds = p.getBounds()
                for (plat in platforms) {
                    if (plat.broken) continue
                    val pb = plat.getBounds()
                    if (playerBounds.bottom >= pb.top && playerBounds.bottom <= pb.top + 28f &&
                        playerBounds.right > pb.left + 10f && playerBounds.left < pb.right - 10f) {

                        when (plat.type) {
                            PlatformType.BREAKABLE -> {
                                plat.broken = true
                                p.jump(false)
                            }
                            PlatformType.SPRING -> p.jump(true)
                            else -> p.jump(false)
                        }
                        break
                    }
                }
            }

            // Coins
            val coinIt = coins.iterator()
            while (coinIt.hasNext()) {
                val c = coinIt.next()
                if (rectsIntersect(p.getBounds(), c.getBounds())) {
                    coinsCollected++
                    coinIt.remove()
                }
            }

            // Spawn new platforms above
            val highest = platforms.minOfOrNull { it.y } ?: p.y
            if (highest > cameraY - 100) {
                val width = Random.nextFloat() * 80f + 100f
                val x = Random.nextFloat() * (screenW - width - 60f) + 30f
                val y = highest - Random.nextFloat() * 130f - 100f
                val type = when {
                    Random.nextFloat() < 0.13f -> PlatformType.SPRING
                    Random.nextFloat() < 0.20f -> PlatformType.MOVING
                    Random.nextFloat() < 0.16f -> PlatformType.BREAKABLE
                    else -> PlatformType.NORMAL
                }
                platforms.add(Platform(x, y, width, type))

                // Chance to spawn coin
                if (Random.nextFloat() < 0.45f) {
                    coins.add(Coin(x + width / 2, y - 50f))
                }
            }

            // Remove off-screen platforms
            platforms.removeAll { it.y > cameraY + screenH + 100 }
            coins.removeAll { it.y > cameraY + screenH + 100 }

            // Death: fell below camera
            if (p.y > cameraY + screenH + 80) {
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

            // Stars (parallax)
            stars.forEach { (sx, sy, size) ->
                val drawY = (sy - cameraY * 0.3f) % (screenH + 40) - 20
                starPaint.alpha = (140 + size * 30).toInt().coerceIn(80, 220)
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
        if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
            synchronized(lock) {
                val p = player
                if (p != null && p.isAlive) {
                    if (event.x < screenW / 2f) p.moveLeft()
                    else p.moveRight()
                }
            }
            return true
        }
        return super.onTouchEvent(event)
    }

    private fun rectsIntersect(a: RectF, b: RectF): Boolean {
        return a.left < b.right && a.right > b.left && a.top < b.bottom && a.bottom > b.top
    }
}
