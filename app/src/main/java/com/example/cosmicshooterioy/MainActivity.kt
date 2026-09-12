package com.example.cosmicshooterioy

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * Главная активность приложения.
 * Загружает разметку activity_main.xml, в которой находится GameView.
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}