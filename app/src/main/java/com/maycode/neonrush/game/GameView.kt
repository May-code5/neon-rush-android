package com.maycode.neonrush.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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

    private lateinit var player: Player
    private val obstacles = mutableListOf<Obstacle>()
    private val coins = mutableListOf<Coin>()

    private var score = 0
    private var coinsCollected = 0
    private var distance = 0f
    private var gameSpeed = 12f
    private var spawnTimer = 0
    private var coinSpawnTimer = 0

    private val bgPaint = Paint().apply { color = 0xFF0A0A12.toInt() }
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
        // Esperamos a surfaceChanged para tener medidas reales
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        screenW = width
        screenH = height
        player = Player(screenW, screenH)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        stop()
    }

    fun startGame() {
        if (isRunning) return
        isRunning = true
        isPaused = false
        thread = Thread(this)
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
            thread?.join(500)
        } catch (_: InterruptedException) {}
    }

    fun revive() {
        player.isAlive = true
        player.activateShield(120) // 2 segundos de invencibilidad
        obstacles.clear()
        // No reseteamos score ni monedas
    }

    override fun run() {
        var lastTime = System.nanoTime()
        while (isRunning) {
            if (isPaused) {
                try { Thread.sleep(50) } catch (_: InterruptedException) {}
                continue
            }

            val now = System.nanoTime()
            val delta = (now - lastTime) / 1_000_000_000f
            lastTime = now

            update(delta)
            draw()

            // ~60 FPS
            try { Thread.sleep(16) } catch (_: InterruptedException) {}
        }
    }

    private fun update(delta: Float) {
        if (!::player.isInitialized || !player.isAlive) return

        player.update()

        // Aumentar dificultad
        distance += gameSpeed * 0.1f
        score = (distance / 10).toInt()
        gameSpeed = 12f + (distance / 800f).coerceAtMost(18f)

        // Spawn obstáculos
        spawnTimer++
        if (spawnTimer > (70 - gameSpeed.toInt()).coerceAtLeast(35)) {
            obstacles.add(Obstacle(screenW, screenH, gameSpeed))
            spawnTimer = 0
        }

        // Spawn monedas
        coinSpawnTimer++
        if (coinSpawnTimer > 50) {
            if (Random.nextFloat() > 0.4f) {
                coins.add(Coin(screenW, screenH, gameSpeed))
            }
            coinSpawnTimer = 0
        }

        // Actualizar obstáculos
        val obsIterator = obstacles.iterator()
        while (obsIterator.hasNext()) {
            val obs = obsIterator.next()
            obs.speed = gameSpeed
            obs.update()
            if (obs.isOffScreen()) {
                obsIterator.remove()
            } else if (RectF_intersects(player.getBounds(), obs.getBounds())) {
                if (player.hasShield) {
                    // Choca pero no muere
                    obsIterator.remove()
                } else {
                    player.isAlive = false
                    listener?.onGameOver(score, coinsCollected)
                    return
                }
            }
        }

        // Actualizar monedas
        val coinIterator = coins.iterator()
        while (coinIterator.hasNext()) {
            val coin = coinIterator.next()
            coin.speed = gameSpeed
            coin.update()
            if (coin.isOffScreen()) {
                coinIterator.remove()
            } else if (RectF_intersects(player.getBounds(), coin.getBounds())) {
                coinsCollected++
                coinIterator.remove()
            }
        }

        listener?.onScoreChanged(score, coinsCollected)
    }

    private fun draw() {
        val canvas = holder.lockCanvas() ?: return
        try {
            // Fondo
            canvas.drawColor(0xFF0A0A12.toInt())

            // Suelo
            val groundY = screenH * 0.75f
            canvas.drawRect(0f, groundY, screenW.toFloat(), screenH.toFloat(), groundPaint)

            // Líneas de velocidad (efecto parallax simple)
            for (i in 0..8) {
                val lx = ((i * 150f + distance * 2) % (screenW + 100)) - 50
                canvas.drawLine(lx, groundY, lx + 80, groundY, linePaint)
            }

            if (::player.isInitialized) {
                player.draw(canvas)
            }

            obstacles.forEach { it.draw(canvas) }
            coins.forEach { it.draw(canvas) }

        } finally {
            holder.unlockCanvasAndPost(canvas)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN && ::player.isInitialized && player.isAlive) {
            player.jump()
            return true
        }
        return super.onTouchEvent(event)
    }

    private fun RectF_intersects(a: android.graphics.RectF, b: android.graphics.RectF): Boolean {
        return a.left < b.right && a.right > b.left && a.top < b.bottom && a.bottom > b.top
    }
}
