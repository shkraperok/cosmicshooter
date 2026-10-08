
package com.example.cosmicshooterioy

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class LeaderboardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_leaderboard)

        val recordsContainer = findViewById<LinearLayout>(R.id.recordsContainer)
        val btnBack = findViewById<Button>(R.id.btnBack)
        val prefs = getSharedPreferences("GamePrefs", MODE_PRIVATE)

        btnBack.setOnClickListener {
            finish()
        }

        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })

        val records = prefs.getStringSet("records", emptySet())
            ?.mapNotNull { it.toIntOrNull() }
            ?.sortedDescending()
            ?.take(5)
            ?: emptyList()

        recordsContainer.removeAllViews()

        if (records.isEmpty()) {
            val emptyText = TextView(this)
            emptyText.text = "Рекордов пока нет"
            emptyText.textSize = 22f
            emptyText.setPadding(20, 30, 20, 30)
            recordsContainer.addView(emptyText)
        } else {
            records.forEachIndexed { index, score ->
                val recordText = TextView(this)
                recordText.text = "${index + 1}. $score очков"
                recordText.textSize = 22f
                recordText.setPadding(20, 20, 20, 20)
                recordsContainer.addView(recordText)
            }
        }
    }
}