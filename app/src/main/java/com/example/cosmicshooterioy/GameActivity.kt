package com.example.cosmicshooterioy

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class GameActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide() // Скрываем верхнюю панель для полного погружения
        setContentView(R.layout.activity_game)
    }
}