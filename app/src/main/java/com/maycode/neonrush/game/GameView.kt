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
    private val obstacles = mutableListOf<Obstacle>()
    private val coins = mutableListOf<Coin>()
    private val lock = Any()

    private var score = 0
    private var coinsCollected = 0
    private var distance = 0f
    private var gameSpeed = 12f
    private var spawnTimer = 0
    private var coinSpawnTimer = 0

    private val groundPaint = Paint().apply {
        color = 0xFF1A1A2E.toInt()
        style = Paint.Style.FILL
    }
    private val linePaint = Paint().apply {
        color = 0xFF00F5FF.toInt()
        strokeWidth = 4f
        alpha = 80
    }

    private var screenW = 0
    private var screenH = 0

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
            player = Player(screenW, screenH)
        }
        surfaceReady = true
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        surfaceReady = false
        stop()
    }

    fun startGame() {
        if (isRunning) return
        isRunning = true
        isPaused = false
        thread = Thread(this, "GameThread")
        thread?.start()
    }

    fun pause() {
        isPaused = true
    }

    fun resume() {
        isPaused = false
    }

    fun stop() {
        isRunning = false
        try {
            thread?.join(800)
        } catch (_: InterruptedException) {}
        thread = null
    }

    fun revive() {
        synchronized(lock) {
            player?.let {
                it.isAlive = true
                it.activateShield(120)
            }
            obstacles.clear()
        }
    }

    override fun run() {
        while (isRunning) {
            if (isPaused || !surfaceReady || screenW <= 0) {
                try { Thread.sleep(40) } catch (_: InterruptedException) {}
                continue
            }

            update()
            drawFrame()

            try { Thread.sleep(16) } catch (_: InterruptedException) {}
        }
    }

    private fun update() {
        val p = player ?: return
        if (!p.isAlive) return

        synchronized(lock) {
            p.update()

            distance += gameSpeed * 0.1f
            score = (distance / 10).toInt()
            gameSpeed = 12f + (distance / 800f).coerceAtMost(18f)

            spawnTimer++
            if (spawnTimer > (70 - gameSpeed.toInt()).coerceAtLeast(35)) {
                obstacles.add(Obstacle(screenW, screenH, gameSpeed))
                spawnTimer = 0
            }

            coinSpawnTimer++
            if (coinSpawnTimer > 50) {
                if (Random.nextFloat() > 0.4f) {
                    coins.add(Coin(screenW, screenH, gameSpeed))
                }
                coinSpawnTimer = 0
            }

            val obsIt = obstacles.iterator()
            while (obsIt.hasNext()) {
                val obs = obsIt.next()
                obs.speed = gameSpeed
                obs.update()
                if (obs.isOffScreen()) {
                    obsIt.remove()
                } else if (rectsIntersect(p.getBounds(), obs.getBounds())) {
                    if (p.hasShield) {
                        obsIt.remove()
                    } else {
                        p.isAlive = false
                        listener?.onGameOver(score, coinsCollected)
                        return
                    }
                }
            }

            val coinIt = coins.iterator()
            while (coinIt.hasNext()) {
                val coin = coinIt.next()
                coin.speed = gameSpeed
                coin.update()
                if (coin.isOffScreen()) {
                    coinIt.remove()
                } else if (rectsIntersect(p.getBounds(), coin.getBounds())) {
                    coinsCollected++
                    coinIt.remove()
                }
            }
        }

        listener?.onScoreChanged(score, coinsCollected)
    }

    private fun drawFrame() {
        if (!surfaceReady || screenW <= 0) return
        val canvas: Canvas = try {
            holder.lockCanvas() ?: return
        } catch (_: Exception) {
            return
        }

        try {
            canvas.drawColor(0xFF0A0A12.toInt())

            val groundY = screenH * 0.75f
            canvas.drawRect(0f, groundY, screenW.toFloat(), screenH.toFloat(), groundPaint)

            for (i in 0..8) {
                val lx = ((i * 150f + distance * 2) % (screenW + 100)) - 50
                canvas.drawLine(lx, groundY, lx + 80, groundY, linePaint)
            }

            synchronized(lock) {
                player?.draw(canvas)
                obstacles.forEach { it.draw(canvas) }
                coins.forEach { it.draw(canvas) }
            }
        } catch (_: Exception) {
            // ignore draw errors
        } finally {
            try {
                holder.unlockCanvasAndPost(canvas)
            } catch (_: Exception) {}
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            synchronized(lock) {
                val p = player
                if (p != null && p.isAlive) {
                    p.jump()
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
