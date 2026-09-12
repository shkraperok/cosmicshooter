package com.example.cosmicshooterioy

/**
 * Класс Player — модель космического корабля.
 * Хранит позицию (x, y) и размер (width, height).
 */
data class Player(
    var x: Float,       // Позиция по горизонтали (центр корабля)
    var y: Float,       // Позиция по вертикали (центр корабля)
    val width: Float,   // Ширина корабля
    val height: Float   // Высота корабля
)