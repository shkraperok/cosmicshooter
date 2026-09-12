package com.example.cosmicshooterioy

/**
 * Класс Enemy — модель врага.
 * Хранит позицию (x, y), размер (size) и скорость (speed).
 */
data class Enemy(
    var x: Float,       // Позиция по горизонтали (центр врага)
    var y: Float,       // Позиция по вертикали (центр врага)
    val size: Float,    // Размер врага (диаметр круга)
    val speed: Float = 5f  // Скорость движения вниз
)