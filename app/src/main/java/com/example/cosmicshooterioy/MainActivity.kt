package com.example.cosmicshooterioy

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButtonToggleGroup

class MainActivity : AppCompatActivity() {

    private lateinit var difficultyGroup: MaterialButtonToggleGroup
    private lateinit var coinsText: TextView
    private lateinit var highScoreText: TextView
    private lateinit var prefs: android.content.SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("GamePrefs", MODE_PRIVATE)

        // Инициализация UI
        coinsText = findViewById(R.id.coinsText)
        highScoreText = findViewById(R.id.highScoreText)
        difficultyGroup = findViewById(R.id.difficultyGroup)

        updateUI()

        // Восстановление выбранной сложности
        when (prefs.getString("difficulty", "Normal")) {
            "Easy" -> difficultyGroup.check(R.id.btnEasy)
            "Hard" -> difficultyGroup.check(R.id.btnHard)
            else -> difficultyGroup.check(R.id.btnNormal)
        }

        // Сохранение сложности при переключении
        difficultyGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val selected = when (checkedId) {
                    R.id.btnEasy -> "Easy"
                    R.id.btnHard -> "Hard"
                    else -> "Normal"
                }
                prefs.edit().putString("difficulty", selected).apply()
            }
        }

        // Навигация
        findViewById<Button>(R.id.btnPlay).setOnClickListener { start<GameActivity>() }
        findViewById<Button>(R.id.btnShop).setOnClickListener { start<ShopActivity>() }
        findViewById<Button>(R.id.btnLeaderboard).setOnClickListener { start<LeaderboardActivity>() }
    }

    private fun updateUI() {
        coinsText.text = "💰 ${prefs.getInt("coins", 0)}"
        highScoreText.text = "🏆 Рекорд: ${prefs.getInt("highScore", 0)}"
    }
}

// Extension function для чистого запуска Activity
inline fun <reified T : Activity> Context.start() {
    startActivity(Intent(this, T::class.java))
}