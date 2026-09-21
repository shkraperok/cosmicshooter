package com.example.cosmicshooterioy

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlin.random.Random

class GameView(context: Context, attrs: AttributeSet?) : SurfaceView(context, attrs), Runnable {

    // Объекты
    private val player = Player(x = 0f, y = 0f, width = 80f, height = 100f)
    private val enemies = mutableListOf<Enemy>()
    private val bullets = mutableListOf<Bullet>()

    // Параметры игры (динамические)
    private var score = 0
    private var lives = 3
    private var gameOver = false
    private var enemySpawnCounter = 0

    // Настраиваемые параметры
    private var ENEMY_SPAWN_DELAY = 20
    private var BULLET_SPEED = 20f
    private var PLAYER_SPEED = 20f
    private var ACCURACY_RADIUS = 10f

    // Статистика
    private var totalEnemiesSpawned = 0
    private var totalEnemiesKilled = 0

    // Кисти
    private val playerPaint = Paint().apply { color = Color.BLUE; style = Paint.Style.FILL; isAntiAlias = true }
    private val enemyPaint = Paint().apply { color = Color.RED; style = Paint.Style.FILL; isAntiAlias = true }
    private val bulletPaint = Paint().apply { color = Color.YELLOW; style = Paint.Style.FILL; isAntiAlias = true }
    private val textPaint = Paint().apply { color = Color.WHITE; textSize = 50f; isAntiAlias = true; typeface = Typeface.DEFAULT_BOLD }
    private val gameOverPaint = Paint().apply { color = Color.RED; textSize = 80f; isAntiAlias = true; typeface = Typeface.DEFAULT_BOLD }

    // Управление
    private var pointerId = -1
    private var isTouching = false
    private var touchX = 0f
    private var touchY = 0f
    private var thread: Thread? = null
    private var isRunning = false

    init {
        // ЗАГРУЗКА НАСТРОЕК ИЗ МЕНЮ
        val prefs = context.getSharedPreferences("GamePrefs", Context.MODE_PRIVATE)
        val difficulty = prefs.getString("difficulty", "Normal") ?: "Normal"
        val fireRateLvl = prefs.getInt("upgrade_fireRate", 0)
        val shieldLvl = prefs.getInt("upgrade_shield", 0)
        val accuracyLvl = prefs.getInt("upgrade_accuracy", 0)

        // Применение сложности
        when (difficulty) {
            "Easy" -> { ENEMY_SPAWN_DELAY = 40; PLAYER_SPEED = 25f }
            "Hard" -> { ENEMY_SPAWN_DELAY = 10; PLAYER_SPEED = 15f }
            else -> { ENEMY_SPAWN_DELAY = 20; PLAYER_SPEED = 20f }
        }

        // Применение улучшений
        BULLET_SPEED = 20f + (fireRateLvl * 5f)
        lives = 3 + shieldLvl
        ACCURACY_RADIUS = 10f + (accuracyLvl * 5f)

        post {
            player.x = width / 2f
            player.y = height - 200f
        }

        holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) { startGame() }
            override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {}
            override fun surfaceDestroyed(holder: SurfaceHolder) { stopGame() }
        })
    }

    private fun startGame() {
        if (thread == null) {
            isRunning = true
            resetGame()
            thread = Thread(this).apply { start() }
        }
    }

    private fun resetGame() {
        score = 0
        val prefs = context.getSharedPreferences("GamePrefs", Context.MODE_PRIVATE)
        lives = 3 + prefs.getInt("upgrade_shield", 0)
        gameOver = false
        enemySpawnCounter = 0
        totalEnemiesSpawned = 0
        totalEnemiesKilled = 0
        enemies.clear()
        bullets.clear()
        player.x = width / 2f
        player.y = height - 200f
    }

    private fun stopGame() {
        isRunning = false
        thread?.join()
        thread = null
        saveProgress()
    }

    override fun run() {
        while (isRunning) {
            update()
            draw()
            try { Thread.sleep(16) } catch (e: InterruptedException) { e.printStackTrace() }
        }
    }

    private fun update() {
        if (gameOver) return

        // Движение игрока
        if (isTouching) {
            val dx = touchX - player.x
            val dy = touchY - player.y
            val dist = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
            if (dist > 5f) {
                player.x += (dx / dist) * PLAYER_SPEED
                player.y += (dy / dist) * PLAYER_SPEED
            }
        }

        // Границы
        val hw = player.width / 2f; val hh = player.height / 2f
        player.x = player.x.coerceIn(hw, width - hw)
        player.y = player.y.coerceIn(hh, height - hh)

        // Пули
        bullets.removeAll { it.y -= BULLET_SPEED; it.y < 0 }

        // Враги
        val missed = mutableListOf<Enemy>()
        enemies.forEach {
            it.y += it.speed
            if (it.y - it.size / 2 > height) missed.add(it)
        }
        if (missed.isNotEmpty()) {
            lives -= missed.size
            if (lives <= 0) { lives = 0; gameOver = true }
        }
        enemies.removeAll(missed)

        // Столкновения
        val bToRemove = mutableListOf<Bullet>()
        val eToRemove = mutableListOf<Enemy>()
        for (b in bullets) {
            for (e in enemies) {
                val d = Math.hypot((b.x - e.x).toDouble(), (b.y - e.y).toDouble()).toFloat()
                if (d < (10f + e.size / 2 + ACCURACY_RADIUS)) {
                    bToRemove.add(b); eToRemove.add(e)
                    score++; totalEnemiesKilled++
                    break
                }
            }
        }
        bullets.removeAll(bToRemove); enemies.removeAll(eToRemove)

        // Спавн
        enemySpawnCounter++
        if (enemySpawnCounter >= ENEMY_SPAWN_DELAY) { spawnEnemy(); enemySpawnCounter = 0 }
    }

    private fun draw() {
        val canvas = holder.lockCanvas() ?: return
        canvas.drawColor(Color.BLACK)

        // Игрок
        val hw = player.width / 2f; val hh = player.height / 2f
        canvas.drawRoundRect(player.x - hw, player.y - hh, player.x + hw, player.y + hh, 20f, 20f, playerPaint)
        canvas.drawCircle(player.x, player.y - hh * 0.3f, 15f, Paint().apply { color = Color.CYAN; style = Paint.Style.FILL })

        // Объекты
        enemies.forEach { canvas.drawCircle(it.x, it.y, it.size / 2, enemyPaint) }
        bullets.forEach { canvas.drawCircle(it.x, it.y, 10f, bulletPaint) }

        // UI
        canvas.drawText("Счёт: $score", 30f, 80f, textPaint)
        canvas.drawText("Жизни: $lives", 30f, 140f, textPaint)
        val acc = if (totalEnemiesSpawned > 0) (totalEnemiesKilled.toFloat() / totalEnemiesSpawned * 100).toInt() else 100
        canvas.drawText("Точность: $acc%", 30f, 200f, textPaint)

        if (gameOver) {
            canvas.drawText("GAME OVER", width / 2f - 220f, height / 2f, gameOverPaint)
            canvas.drawText("Нажмите для рестарта", width / 2f - 280f, height / 2f + 100f, textPaint)
        }

        holder.unlockCanvasAndPost(canvas)
    }

    private fun spawnEnemy() {
        val x = Random.nextFloat() * (width - 100f) + 50f
        val size = listOf(40f, 60f, 90f).random()
        val speed = when (size) {
            40f -> Random.nextFloat() * 10f + 15f
            60f -> Random.nextFloat() * 10f + 8f
            90f -> Random.nextFloat() * 5f + 4f
            else -> 10f
        }
        totalEnemiesSpawned++
        enemies.add(Enemy(x, -50f, size, speed))
    }

    private fun shoot() { if (!gameOver) bullets.add(Bullet(player.x, player.y)) }

    private fun saveProgress() {
        val prefs = context.getSharedPreferences("GamePrefs", Context.MODE_PRIVATE)
        val high = prefs.getInt("highScore", 0)
        if (score > high) prefs.edit().putInt("highScore", score).apply()
        val coins = prefs.getInt("coins", 0)
        prefs.edit().putInt("coins", coins + (score / 10)).apply()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.actionMasked
        val index = event.actionIndex
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                if (gameOver) resetGame()
                pointerId = event.getPointerId(index)
                touchX = event.getX(index); touchY = event.getY(index); isTouching = true
            }
            MotionEvent.ACTION_POINTER_DOWN -> if (!gameOver) shoot()
            MotionEvent.ACTION_MOVE -> {
                if (!gameOver) for (i in 0 until event.pointerCount) {
                    if (event.getPointerId(i) == pointerId) {
                        touchX = event.getX(i); touchY = event.getY(i)
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                if (event.getPointerId(index) == pointerId) isTouching = false
            }
        }
        return true
    }
}