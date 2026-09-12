package com.example.cosmicshooterioy

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.example.cosmicshooterioy.Bullet
import com.example.cosmicshooterioy.Enemy
import com.example.cosmicshooterioy.Player
import kotlin.random.Random

class GameView(context: Context, attrs: AttributeSet?) :
    SurfaceView(context, attrs),
    Runnable {

    // ============================================================
    // ИГРОВЫЕ ОБЪЕКТЫ (НЕ ТРОГАТЬ)
    // ============================================================

    private val player = Player(
        x = 0f,
        y = 0f,
        width = 80f,
        height = 100f
    )

    private val enemies = mutableListOf<Enemy>()
    private val bullets = mutableListOf<Bullet>()

    // ============================================================
    // ПАРАМЕТРЫ ИГРЫ (ИЗМЕНЕНО ДЛЯ ЗАДАНИЯ 1)
    // ============================================================

    private var score = 0

    // ЗАДАНИЕ 2: Счётчик жизней
    private var lives = 3

    private var gameOver = false
    private var enemySpawnCounter = 0

    private val ENEMY_SPAWN_DELAY = 20

    // ЗАДАНИЕ 6: Переменные для подсчета процента сбитых врагов
    private var totalEnemiesSpawned = 0
    private var totalEnemiesKilled = 0

    // ============================================================
    // КИСТИ ДЛЯ РИСОВАНИЯ
    // ============================================================

    private val playerPaint = Paint().apply {
        color = Color.BLUE
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val enemyPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val bulletPaint = Paint().apply {
        color = Color.YELLOW
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 50f
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
    }

    private val gameOverPaint = Paint().apply {
        color = Color.RED
        textSize = 80f
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
    }


    // ============================================================
    // УПРАВЛЕНИЕ КАСАНИЯМИ (НЕ ТРОГАТЬ)
    // ============================================================

    private var pointerId = -1
    private var isTouching = false
    private var touchX = 0f
    private var touchY = 0f

    // ============================================================
    // ПОТОК ДЛЯ ИГРОВОГО ЦИКЛА
    // ============================================================

    private var thread: Thread? = null
    private var isRunning = false

    // ============================================================
    // ИНИЦИАЛИЗАЦИЯ
    // ============================================================

    init {
        post {
            player.x = width / 2f
            player.y = height - 200f
        }

        holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                startGame()
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                stopGame()
            }
        })
    }

    // ============================================================
    // МЕТОДЫ ЖИЗНЕННОГО ЦИКЛА
    // ============================================================

    private fun startGame() {
        if (thread == null) {
            isRunning = true
            resetGame() // Сбрасываем игру при каждом старте потока
            thread = Thread(this)
            thread?.start()
        }
    }

    // ЗАДАНИЕ 3: Метод для полного перезапуска игры
    private fun resetGame() {
        score = 0
        lives = 3
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
    }

    // ============================================================
    // ИГРОВОЙ ЦИКЛ
    // ============================================================

    override fun run() {
        while (isRunning) {
            update()
            draw()
            try {
                Thread.sleep(16)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    private fun update() {
        if (gameOver) return

        // Движение корабля
        if (isTouching) {
            val dx = touchX - player.x
            val dy = touchY - player.y
            val distance = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
            if (distance > 5f) {
                val speed = 20f
                player.x += (dx / distance) * speed
                player.y += (dy / distance) * speed
            }
        }

        // Границы корабля
        val halfWidth = player.width / 2f
        val halfHeight = player.height / 2f
        if (player.x - halfWidth < 0) player.x = halfWidth
        if (player.x + halfWidth > width) player.x = width - halfWidth
        if (player.y - halfHeight < 0) player.y = halfHeight
        if (player.y + halfHeight > height) player.y = height - halfHeight

        // Пули
        val bulletsToRemove = mutableListOf<Bullet>()
        for (bullet in bullets) {
            bullet.y -= 20f
            if (bullet.y < 0) {
                bulletsToRemove.add(bullet)
            }
        }
        bullets.removeAll(bulletsToRemove)

        // Враги
        val enemiesToRemove = mutableListOf<Enemy>()
        for (enemy in enemies) {
            enemy.y += enemy.speed
            if (enemy.y - enemy.size / 2 > height) {
                enemiesToRemove.add(enemy)
            }
        }

        if (enemiesToRemove.isNotEmpty()) {
            lives -= enemiesToRemove.size
            if (lives <= 0) {
                lives = 0
                gameOver = true
            }
        }
        enemies.removeAll(enemiesToRemove)

        // Столкновения
        val bulletsHit = mutableListOf<Bullet>()
        val enemiesHit = mutableListOf<Enemy>()
        for (bullet in bullets) {
            for (enemy in enemies) {
                val distance = Math.hypot(
                    (bullet.x - enemy.x).toDouble(),
                    (bullet.y - enemy.y).toDouble()
                ).toFloat()
                if (distance < 10f + enemy.size / 2) {
                    bulletsHit.add(bullet)
                    enemiesHit.add(enemy)
                    score++
                    totalEnemiesKilled++
                }
            }
        }
        bullets.removeAll(bulletsHit)
        enemies.removeAll(enemiesHit)

        // Создание врагов
        enemySpawnCounter++
        if (enemySpawnCounter >= ENEMY_SPAWN_DELAY) {
            spawnEnemy()
            enemySpawnCounter = 0
        }
    }

    private fun draw() {
        val holder = holder ?: return
        val canvas = holder.lockCanvas() ?: return

        canvas.drawColor(Color.BLACK)

        // Корабль
        val halfWidth = player.width / 2f
        val halfHeight = player.height / 2f
        canvas.drawRoundRect(
            player.x - halfWidth,
            player.y - halfHeight,
            player.x + halfWidth,
            player.y + halfHeight,
            20f, 20f,
            playerPaint
        )
        val cockpitPaint = Paint().apply {
            color = Color.CYAN
            style = Paint.Style.FILL
        }
        canvas.drawCircle(player.x, player.y - halfHeight * 0.3f, 15f, cockpitPaint)

        // Враги
        for (enemy in enemies) {
            canvas.drawCircle(enemy.x, enemy.y, enemy.size / 2, enemyPaint)
        }

        // Пули
        for (bullet in bullets) {
            canvas.drawCircle(bullet.x, bullet.y, 10f, bulletPaint)
        }

        // Интерфейс
        canvas.drawText("Счёт: $score", 30f, 80f, textPaint)

        canvas.drawText("Жизни: $lives", 30f, 140f, textPaint)

        val accuracy = if (totalEnemiesSpawned > 0) {
            (totalEnemiesKilled.toFloat() / totalEnemiesSpawned.toFloat() * 100).toInt()
        } else {
            100
        }
        canvas.drawText("Точность: $accuracy%", 30f, 200f, textPaint)

        // Game Over
        if (gameOver) {
            canvas.drawText("GAME OVER", width / 2f - 220f, height / 2f, gameOverPaint)

            canvas.drawText("Нажмите для рестарта", width / 2f - 280f, height / 2f + 100f, textPaint)
        }

        holder.unlockCanvasAndPost(canvas)
    }

    // ============================================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // ============================================================
    private fun spawnEnemy() {
        val x = Random.nextFloat() * (width - 100f) + 50f
        val y = -50f

        val sizeOptions = listOf(40f, 60f, 90f)
        val size = sizeOptions.random()

        val speed = when (size) {
            40f -> Random.nextFloat() * 10f + 15f  // Скорость 15-25
            60f -> Random.nextFloat() * 10f + 8f   // Скорость 8-18
            90f -> Random.nextFloat() * 5f + 4f    // Скорость 4-9
            else -> 10f
        }
        totalEnemiesSpawned++

        enemies.add(Enemy(x, y, size, speed))
    }
    private fun shoot() {
        if (!gameOver) {
            bullets.add(Bullet(player.x, player.y))
        }
    }


    // ============================================================
    // ОБРАБОТКА КАСАНИЙ
    // ============================================================

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.actionMasked
        val index = event.actionIndex

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                if (gameOver) {
                    resetGame()
                }

                pointerId = event.getPointerId(index)
                touchX = event.getX(index)
                touchY = event.getY(index)
                isTouching = true
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                if (!gameOver) {
                    shoot()
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (!gameOver) {
                    for (i in 0 until event.pointerCount) {
                        val id = event.getPointerId(i)
                        if (id == pointerId) {
                            touchX = event.getX(i)
                            touchY = event.getY(i)
                        }
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                isTouching = false
            }

            MotionEvent.ACTION_POINTER_UP -> {
                val id = event.getPointerId(index)
                if (id == pointerId) {
                    isTouching = false
                }
            }
        }
        return true
    }
}