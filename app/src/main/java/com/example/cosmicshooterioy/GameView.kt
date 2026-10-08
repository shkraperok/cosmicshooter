package com.example.cosmicshooterioy

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlin.random.Random

// Класс для звезд (параллакс фон)
data class Star(
    var x: Float,
    var y: Float,
    var size: Float,
    var speed: Float
)

class GameView(context: Context, attrs: AttributeSet?) :
    SurfaceView(context, attrs), Runnable {

    // =========================
    // ОБЪЕКТЫ ИГРЫ
    // =========================

    private val player = Player(
        x = 0f,
        y = 0f,
        width = 80f,
        height = 100f
    )

    private val enemies = mutableListOf<Enemy>()
    private val bullets = mutableListOf<Bullet>()
    private val stars = mutableListOf<Star>()


    // =========================
    // ПАРАМЕТРЫ ИГРЫ
    // =========================
    private var score = 0

    private var lives = 3
    private var maxLives = 3

    private var gameOver = false
    private var isPaused = false

    private var enemySpawnCounter = 0


    // =========================
    // НАСТРАИВАЕМЫЕ ПАРАМЕТРЫ
    // =========================

    private var ENEMY_SPAWN_DELAY = 20
    private var BULLET_SPEED = 20f
    private var PLAYER_SPEED = 20f
    private var ACCURACY_RADIUS = 10f

    private var SHOOT_DELAY = 300L


    // =========================
    // СТАТИСТИКА
    // =========================

    private var totalEnemiesSpawned = 0
    private var totalEnemiesKilled = 0


    // =========================
    // АВТОСТРЕЛЬБА
    // =========================

    private var lastShotTime = 0L


    // =========================
    // КНОПКИ
    // =========================

    private val pauseButtonRect = RectF()

    private val restartButtonRect = RectF()

    private val backButtonRect = RectF()


    private val pauseButtonPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        isAntiAlias = true
    }


    // =========================
    // КИСТИ
    // =========================

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
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
    }

    private val buttonPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val buttonTextPaint = Paint().apply {
        color = Color.BLACK
        textSize = 32f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
    }

    private val healthBarBgPaint = Paint().apply {
        color = Color.DKGRAY
        style = Paint.Style.FILL
    }

    private val healthBarPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.FILL
    }

    private val starPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }


    // =========================
    // УПРАВЛЕНИЕ
    // =========================

    private var pointerId = -1

    private var isTouching = false

    private var touchX = 0f
    private var touchY = 0f


    // =========================
    // ИГРОВОЙ ПОТОК
    // =========================

    private var thread: Thread? = null

    private var isRunning = false


    // =========================
    // ИНИЦИАЛИЗАЦИЯ
    // =========================

    init {

        val prefs = context.getSharedPreferences(
            "GamePrefs",
            Context.MODE_PRIVATE
        )

        val difficulty =
            prefs.getString("difficulty", "Normal") ?: "Normal"

        val fireRateLvl =
            prefs.getInt("upgrade_fireRate", 0)

        val shieldLvl =
            prefs.getInt("upgrade_shield", 0)

        val accuracyLvl =
            prefs.getInt("upgrade_accuracy", 0)


        // =========================
        // СЛОЖНОСТЬ
        // =========================

        when (difficulty) {

            "Easy" -> {
                ENEMY_SPAWN_DELAY = 40
                PLAYER_SPEED = 25f
            }

            "Hard" -> {
                ENEMY_SPAWN_DELAY = 10
                PLAYER_SPEED = 15f
            }

            else -> {
                ENEMY_SPAWN_DELAY = 20
                PLAYER_SPEED = 20f
            }
        }


        // =========================
        // УЛУЧШЕНИЯ
        // =========================

        BULLET_SPEED =
            20f + (fireRateLvl * 5f)

        maxLives =
            3 + shieldLvl

        lives =
            maxLives

        ACCURACY_RADIUS =
            10f + (accuracyLvl * 5f)

        SHOOT_DELAY =
            300L - (fireRateLvl * 30L)

        if (SHOOT_DELAY < 100L) {
            SHOOT_DELAY = 100L
        }


        // =========================
        // ЗВЕЗДЫ
        // =========================

        initStars()


        // =========================
        // НАЧАЛЬНАЯ ПОЗИЦИЯ ИГРОКА
        // =========================

        post {

            player.x =
                width / 2f

            player.y =
                height - 200f

            updateButtonPositions()
        }


        // =========================
        // SURFACE HOLDER
        // =========================

        holder.addCallback(
            object : SurfaceHolder.Callback {

                override fun surfaceCreated(
                    holder: SurfaceHolder
                ) {
                    startGame()
                }


                override fun surfaceChanged(
                    holder: SurfaceHolder,
                    format: Int,
                    w: Int,
                    h: Int
                ) {
                    updateButtonPositions()
                }


                override fun surfaceDestroyed(
                    holder: SurfaceHolder
                ) {
                    stopGame()
                }
            }
        )
    }


    // =========================
    // ЗВЕЗДНЫЙ ФОН
    // =========================

    private fun initStars() {

        stars.clear()

        for (i in 0..100) {

            stars.add(
                Star(
                    x = Random.nextFloat() * 1000,
                    y = Random.nextFloat() * 2000,
                    size = Random.nextFloat() * 3 + 1,
                    speed = Random.nextFloat() * 3 + 1
                )
            )
        }
    }


    // =========================
    // ЗАПУСК ИГРЫ
    // =========================

    private fun startGame() {

        if (thread == null) {

            isRunning = true

            resetGame()

            thread = Thread(this)

            thread?.start()
        }
    }


    // =========================
    // НАЧАТЬ ИГРУ ЗАНОВО
    // =========================

    private fun resetGame() {

        score = 0

        val prefs =
            context.getSharedPreferences(
                "GamePrefs",
                Context.MODE_PRIVATE
            )

        maxLives =
            3 + prefs.getInt(
                "upgrade_shield",
                0
            )

        lives =
            maxLives

        gameOver = false

        isPaused = false

        enemySpawnCounter = 0

        totalEnemiesSpawned = 0

        totalEnemiesKilled = 0

        enemies.clear()

        bullets.clear()

        player.x =
            width / 2f

        player.y =
            height - 200f

        updateButtonPositions()

        lastShotTime =
            System.currentTimeMillis()
    }


    // =========================
    // ПОЗИЦИИ КНОПОК
    // =========================

    private fun updateButtonPositions() {

        // -------------------------
        // КНОПКА ПАУЗЫ
        // -------------------------

        val pauseSize = 100f

        val pauseLeft =
            (width - pauseSize) / 2f

        pauseButtonRect.set(
            pauseLeft,
            20f,
            pauseLeft + pauseSize,
            20f + pauseSize
        )


        // -------------------------
        // КНОПКИ GAME OVER
        // -------------------------

        val buttonWidth =
            minOf(
                320f,
                width - 80f
            )

        val buttonHeight = 80f

        val centerX =
            width / 2f


        // -------------------------
        // НАЧАТЬ ЗАНОВО
        // -------------------------

        val restartTop =
            height / 2f + 190f

        restartButtonRect.set(
            centerX - buttonWidth / 2f,
            restartTop,
            centerX + buttonWidth / 2f,
            restartTop + buttonHeight
        )


        // -------------------------
        // НАЗАД
        // -------------------------

        val backTop =
            restartTop +
                    buttonHeight +
                    20f

        backButtonRect.set(
            centerX - buttonWidth / 2f,
            backTop,
            centerX + buttonWidth / 2f,
            backTop + buttonHeight
        )
    }


    // =========================
    // ОСТАНОВКА ИГРЫ
    // =========================

    private fun stopGame() {

        isRunning = false

        thread?.join()

        thread = null

        saveProgress()
    }


    // =========================
    // ИГРОВОЙ ЦИКЛ
    // =========================

    override fun run() {

        while (isRunning) {

            if (!isPaused) {
                update()
            }

            draw()

            try {

                Thread.sleep(16)

            } catch (e: InterruptedException) {

                e.printStackTrace()
            }
        }
    }


    // =========================
    // ОБНОВЛЕНИЕ ИГРЫ
    // =========================

    private fun update() {

        if (gameOver) {
            return
        }


        // -------------------------
        // АВТОСТРЕЛЬБА
        // -------------------------

        val currentTime =
            System.currentTimeMillis()

        if (
            currentTime - lastShotTime >
            SHOOT_DELAY
        ) {

            shoot()

            lastShotTime =
                currentTime
        }


        // -------------------------
        // ДВИЖЕНИЕ ЗВЕЗД
        // -------------------------

        for (star in stars) {

            star.y += star.speed

            if (star.y > height) {

                star.y = 0f

                star.x =
                    Random.nextFloat() * width
            }
        }


        // -------------------------
        // ДВИЖЕНИЕ ИГРОКА
        // -------------------------

        if (isTouching) {

            val dx =
                touchX - player.x

            val dy =
                touchY - player.y

            val dist =
                Math.hypot(
                    dx.toDouble(),
                    dy.toDouble()
                ).toFloat()

            if (dist > 5f) {

                player.x +=
                    (dx / dist) * PLAYER_SPEED

                player.y +=
                    (dy / dist) * PLAYER_SPEED
            }
        }


        // -------------------------
        // ГРАНИЦЫ ЭКРАНА
        // -------------------------

        val hw =
            player.width / 2f

        val hh =
            player.height / 2f

        player.x =
            player.x.coerceIn(
                hw,
                width - hw
            )

        player.y =
            player.y.coerceIn(
                hh,
                height - hh
            )


        // -------------------------
        // ПУЛИ
        // -------------------------

        bullets.removeAll {

            it.y -= BULLET_SPEED

            it.y < 0
        }


        // -------------------------
        // ВРАГИ
        // -------------------------

        val missed =
            mutableListOf<Enemy>()

        enemies.forEach {

            it.y += it.speed

            if (
                it.y - it.size / 2 >
                height
            ) {
                missed.add(it)
            }
        }


        if (missed.isNotEmpty()) {

            lives -= missed.size

            if (lives <= 0) {

                lives = 0

                gameOver = true

                isTouching = false
            }
        }

        enemies.removeAll(missed)


        // -------------------------
        // СТОЛКНОВЕНИЯ
        // -------------------------

        val bToRemove =
            mutableListOf<Bullet>()

        val eToRemove =
            mutableListOf<Enemy>()


        for (b in bullets) {

            for (e in enemies) {

                val d =
                    Math.hypot(
                        (b.x - e.x).toDouble(),
                        (b.y - e.y).toDouble()
                    ).toFloat()


                if (
                    d <
                    (
                            10f +
                                    e.size / 2 +
                                    ACCURACY_RADIUS
                            )
                ) {

                    bToRemove.add(b)

                    eToRemove.add(e)

                    score++

                    totalEnemiesKilled++

                    break
                }
            }
        }


        bullets.removeAll(bToRemove)

        enemies.removeAll(eToRemove)


        // -------------------------
        // СПАВН ВРАГОВ
        // -------------------------

        enemySpawnCounter++

        if (
            enemySpawnCounter >=
            ENEMY_SPAWN_DELAY
        ) {

            spawnEnemy()

            enemySpawnCounter = 0
        }
    }


    // =========================
    // ОТРИСОВКА
    // =========================

    private fun draw() {

        val canvas =
            holder.lockCanvas()
                ?: return


        try {

            canvas.drawColor(Color.BLACK)


            // -------------------------
            // ЗВЕЗДЫ
            // -------------------------

            for (star in stars) {

                canvas.drawCircle(
                    star.x,
                    star.y,
                    star.size,
                    starPaint
                )
            }


            // -------------------------
            // ИГРОК
            // -------------------------

            val hw =
                player.width / 2f

            val hh =
                player.height / 2f


            canvas.drawRoundRect(
                player.x - hw,
                player.y - hh,
                player.x + hw,
                player.y + hh,
                20f,
                20f,
                playerPaint
            )


            val playerLightPaint =
                Paint().apply {

                    color = Color.CYAN

                    style =
                        Paint.Style.FILL
                }


            canvas.drawCircle(
                player.x,
                player.y - hh * 0.3f,
                15f,
                playerLightPaint
            )


            // -------------------------
            // ВРАГИ
            // -------------------------

            enemies.forEach {

                canvas.drawCircle(
                    it.x,
                    it.y,
                    it.size / 2,
                    enemyPaint
                )
            }


            // -------------------------
            // ПУЛИ
            // -------------------------

            bullets.forEach {

                canvas.drawCircle(
                    it.x,
                    it.y,
                    10f,
                    bulletPaint
                )
            }


            // -------------------------
            // КНОПКА ПАУЗЫ
            // -------------------------

            canvas.drawRoundRect(
                pauseButtonRect,
                10f,
                10f,
                pauseButtonPaint
            )


            val pauseTextPaint =
                Paint().apply {

                    color = Color.BLACK

                    textSize = 40f

                    textAlign =
                        Paint.Align.CENTER

                    typeface =
                        Typeface.DEFAULT_BOLD
                }


            canvas.drawText(
                "||",
                pauseButtonRect.centerX(),
                pauseButtonRect.centerY() + 15f,
                pauseTextPaint
            )


            // -------------------------
            // СЧЁТ
            // -------------------------

            canvas.drawText(
                "Счёт: $score",
                30f,
                80f,
                textPaint
            )


            // -------------------------
            // ШКАЛА ЗДОРОВЬЯ
            // -------------------------

            val healthBarWidth = 300f

            val healthBarHeight = 30f

            val healthBarX = 30f

            val healthBarY = 110f


            canvas.drawRoundRect(
                healthBarX,
                healthBarY,
                healthBarX + healthBarWidth,
                healthBarY + healthBarHeight,
                10f,
                10f,
                healthBarBgPaint
            )


            val healthPercent =
                lives.toFloat() / maxLives


            val healthFillWidth =
                healthBarWidth * healthPercent


            val healthColor =
                when {

                    healthPercent > 0.6f ->
                        Color.GREEN

                    healthPercent > 0.3f ->
                        Color.YELLOW

                    else ->
                        Color.RED
                }


            healthBarPaint.color =
                healthColor


            canvas.drawRoundRect(
                healthBarX,
                healthBarY,
                healthBarX + healthFillWidth,
                healthBarY + healthBarHeight,
                10f,
                10f,
                healthBarPaint
            )


            canvas.drawText(
                "$lives / $maxLives",
                healthBarX +
                        healthBarWidth +
                        20f,
                healthBarY + 22f,
                textPaint
            )


            // -------------------------
            // ТОЧНОСТЬ
            // -------------------------

            val acc =
                if (totalEnemiesSpawned > 0) {

                    (
                            totalEnemiesKilled.toFloat() /
                                    totalEnemiesSpawned *
                                    100
                            ).toInt()

                } else {
                    100
                }


            canvas.drawText(
                "Точность: $acc%",
                30f,
                200f,
                textPaint
            )


            // =========================
            // ПАУЗА
            // =========================

            if (
                isPaused &&
                !gameOver
            ) {

                canvas.drawColor(
                    Color.argb(
                        128,
                        0,
                        0,
                        0
                    )
                )


                val pausePaint =
                    Paint().apply {

                        color = Color.WHITE

                        textSize = 100f

                        textAlign =
                            Paint.Align.CENTER

                        typeface =
                            Typeface.DEFAULT_BOLD
                    }


                canvas.drawText(
                    "PAUSED",
                    width / 2f,
                    height / 2f,
                    pausePaint
                )


                canvas.drawText(
                    "Нажмите для продолжения",
                    width / 2f,
                    height / 2f + 100f,
                    textPaint
                )
            }


            // =========================
            // GAME OVER
            // =========================

            if (gameOver) {

                canvas.drawColor(
                    Color.argb(
                        180,
                        0,
                        0,
                        0
                    )
                )


                // GAME OVER

                canvas.drawText(
                    "GAME OVER",
                    width / 2f,
                    height / 2f,
                    gameOverPaint
                )


                // СЧЁТ

                val scorePaint =
                    Paint(textPaint).apply {

                        textAlign =
                            Paint.Align.CENTER
                    }


                canvas.drawText(
                    "Счёт: $score",
                    width / 2f,
                    height / 2f + 80f,
                    scorePaint
                )


                // -------------------------
                // НАЧАТЬ ЗАНОВО
                // -------------------------

                canvas.drawRoundRect(
                    restartButtonRect,
                    16f,
                    16f,
                    buttonPaint
                )


                canvas.drawText(
                    "НАЧАТЬ ЗАНОВО",
                    restartButtonRect.centerX(),
                    restartButtonRect.centerY() + 11f,
                    buttonTextPaint
                )


                // -------------------------
                // НАЗАД
                // -------------------------

                canvas.drawRoundRect(
                    backButtonRect,
                    16f,
                    16f,
                    buttonPaint
                )


                canvas.drawText(
                    "НАЗАД",
                    backButtonRect.centerX(),
                    backButtonRect.centerY() + 11f,
                    buttonTextPaint
                )
            }

        } finally {

            holder.unlockCanvasAndPost(canvas)
        }
    }


    // =========================
    // СОЗДАНИЕ ВРАГА
    // =========================

    private fun spawnEnemy() {

        val x =
            Random.nextFloat() *
                    (width - 100f) +
                    50f


        val size =
            listOf(
                40f,
                60f,
                90f
            ).random()


        val speed =
            when (size) {

                40f ->
                    Random.nextFloat() * 10f + 15f

                60f ->
                    Random.nextFloat() * 10f + 8f

                90f ->
                    Random.nextFloat() * 5f + 4f

                else ->
                    10f
            }


        totalEnemiesSpawned++


        enemies.add(
            Enemy(
                x,
                -50f,
                size,
                speed
            )
        )
    }


    // =========================
    // СТРЕЛЬБА
    // =========================

    private fun shoot() {

        if (
            !gameOver &&
            !isPaused
        ) {

            bullets.add(
                Bullet(
                    player.x,
                    player.y -
                            player.height / 2f
                )
            )
        }
    }


    // =========================
    // СОХРАНЕНИЕ ПРОГРЕССА
    // =========================

    private fun saveProgress() {

        val prefs =
            context.getSharedPreferences(
                "GamePrefs",
                Context.MODE_PRIVATE
            )


        // Рекорд

        val high =
            prefs.getInt(
                "highScore",
                0
            )


        if (score > high) {

            prefs.edit()
                .putInt(
                    "highScore",
                    score
                )
                .apply()
        }


        // Монеты

        val coins =
            prefs.getInt(
                "coins",
                0
            )


        prefs.edit()
            .putInt(
                "coins",
                coins + (score / 10)
            )
            .apply()
    }


    // =========================
    // ОБРАБОТКА КАСАНИЙ
    // =========================

    override fun onTouchEvent(
        event: MotionEvent
    ): Boolean {

        val action =
            event.actionMasked

        val index =
            event.actionIndex


        val x =
            event.getX(index)

        val y =
            event.getY(index)


        when (action) {


            // =========================
            // НАЖАТИЕ
            // =========================

            MotionEvent.ACTION_DOWN -> {


                // -------------------------
                // ПАУЗА
                // -------------------------

                if (
                    pauseButtonRect.contains(x, y) &&
                    !gameOver
                ) {

                    isPaused =
                        !isPaused

                    isTouching = false

                    return true
                }


                // =========================
                // GAME OVER
                // =========================

                if (gameOver) {


                    // -------------------------
                    // НАЧАТЬ ЗАНОВО
                    // -------------------------

                    if (
                        restartButtonRect.contains(
                            x,
                            y
                        )
                    ) {

                        resetGame()

                        return true
                    }


                    // -------------------------
                    // НАЗАД
                    // -------------------------

                    if (
                        backButtonRect.contains(
                            x,
                            y
                        )
                    ) {

                        isRunning = false

                        (context as? android.app.Activity)
                            ?.finish()

                        return true
                    }


                    return true
                }


                // =========================
                // ПАУЗА
                // =========================

                if (isPaused) {

                    isPaused = false

                    return true
                }


                // =========================
                // УПРАВЛЕНИЕ КОРАБЛЁМ
                // =========================

                pointerId =
                    event.getPointerId(index)

                touchX = x

                touchY = y

                isTouching = true
            }


            // =========================
            // ДВИЖЕНИЕ
            // =========================

            MotionEvent.ACTION_MOVE -> {

                if (
                    !gameOver &&
                    !isPaused
                ) {

                    for (
                    i in 0 until event.pointerCount
                    ) {

                        if (
                            event.getPointerId(i) ==
                            pointerId
                        ) {

                            touchX =
                                event.getX(i)

                            touchY =
                                event.getY(i)
                        }
                    }
                }
            }


            // =========================
            // ОТПУСКАНИЕ
            // =========================

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_POINTER_UP -> {

                if (
                    event.getPointerId(index) ==
                    pointerId
                ) {

                    isTouching = false

                    pointerId = -1
                }
            }


            // =========================
            // ОТМЕНА
            // =========================

            MotionEvent.ACTION_CANCEL -> {

                isTouching = false

                pointerId = -1
            }
        }


        return true
    }
}