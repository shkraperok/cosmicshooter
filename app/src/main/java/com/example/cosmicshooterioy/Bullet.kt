package com.example.cosmicshooterioy

/**
 * Класс Bullet — модель пули.
 * Хранит позицию (x, y) и скорость (speed).
 */
data class Bullet(
    var x: Float,       // Позиция по горизонтали (центр пули)
    var y: Float,       // Позиция по вертикали (центр пули)
    val speed: Float = 20f  // Скорость движения вверх
)