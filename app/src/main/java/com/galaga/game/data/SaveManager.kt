package com.galaga.game.data

import android.content.Context
import android.content.SharedPreferences
import com.galaga.game.model.GameState

class SaveManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("galaga_save", Context.MODE_PRIVATE)

    fun hasSavedGame(): Boolean {
        return prefs.getBoolean("has_saved_game", false)
    }

    fun saveCheckpoint(level: Int, score: Int, lives: Int, powerLevel: Int) {
        prefs.edit()
            .putBoolean("has_saved_game", true)
            .putInt("level", level)
            .putInt("score", score)
            .putInt("lives", lives)
            .putInt("powerLevel", powerLevel)
            .apply()
    }

    fun clearSavedGame() {
        prefs.edit().clear().apply()
    }

    fun getSavedLevel(): Int = prefs.getInt("level", 1)
    fun getSavedScore(): Int = prefs.getInt("score", 0)
    fun getSavedLives(): Int = prefs.getInt("lives", 3)
    fun getSavedPowerLevel(): Int = prefs.getInt("powerLevel", 1)
}
