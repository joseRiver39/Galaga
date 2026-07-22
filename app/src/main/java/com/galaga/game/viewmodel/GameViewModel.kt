package com.galaga.game.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.galaga.game.audio.SoundManager
import com.galaga.game.data.SaveManager
import com.galaga.game.engine.GameEngine
import com.galaga.game.model.GameState
import kotlin.random.Random

data class Star(val x: Float, val y: Float, val size: Float, val alpha: Float)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    val soundManager = SoundManager(application)
    val saveManager = SaveManager(application)

    val hasSavedGame: Boolean get() = saveManager.hasSavedGame()

    var isSoundEnabled by mutableStateOf(true)
        private set

    private val _gameState = mutableStateOf<GameState>(GameState.Menu)
    var gameState: GameState
        get() = _gameState.value
        private set(value) {
            _gameState.value = value
            val isPlaying = value is GameState.Playing || value is GameState.LevelIntro
            soundManager.setGameState(isPlaying)

            if (value is GameState.LevelIntro) {
                saveManager.saveCheckpoint(value.level, value.score, value.lives, value.powerLevel)
            } else if (value is GameState.GameOver) {
                saveManager.clearSavedGame()
            }
        }

    private var canvasWidth = 1080f
    private var canvasHeight = 1920f
    private var lastFrameTime = 0L
    private val engine = GameEngine()

    val stars = List(80) {
        Star(
            x = Random.nextFloat() * 1200f,
            y = Random.nextFloat() * 2200f,
            size = 0.8f + Random.nextFloat() * 2.2f,
            alpha = 0.2f + Random.nextFloat() * 0.6f
        )
    }

    fun setCanvasSize(width: Float, height: Float) {
        canvasWidth = width
        canvasHeight = height
    }

    fun onFrame(frameTimeNanos: Long) {
        val current = gameState
        if (current !is GameState.Playing && current !is GameState.LevelIntro) {
            lastFrameTime = 0L
            return
        }
        val deltaTime = if (lastFrameTime == 0L) 1f / 60f
        else (frameTimeNanos - lastFrameTime) / 1_000_000_000f
        lastFrameTime = frameTimeNanos
        val clamped = deltaTime.coerceAtMost(0.05f)
        val result = engine.update(current, clamped, canvasWidth, canvasHeight)
        gameState = result.state
        result.events.forEach { soundManager.playEvent(it) }
    }

    fun onPlayerDrag(dx: Float, dy: Float) {
        val s = gameState
        if (s is GameState.Playing) {
            gameState = s.copy(player = s.player.move(dx, dy, canvasWidth, canvasHeight))
        }
    }

    fun startNewGame() {
        saveManager.clearSavedGame()
        gameState = GameState.LevelIntro(level = 1, remainingTime = 3f, lives = 3, score = 0, powerLevel = 1)
    }

    fun continueGame() {
        if (hasSavedGame) {
            gameState = GameState.LevelIntro(
                level = saveManager.getSavedLevel(),
                remainingTime = 3f,
                lives = saveManager.getSavedLives(),
                score = saveManager.getSavedScore(),
                powerLevel = saveManager.getSavedPowerLevel()
            )
        } else {
            startNewGame()
        }
    }

    fun pause() {
        val s = gameState
        if (s is GameState.Playing) {
            gameState = GameState.Paused(playingState = s)
        }
    }

    fun resume() {
        val s = gameState
        if (s is GameState.Paused) {
            gameState = s.playingState
        }
    }

    fun toggleSound() {
        isSoundEnabled = !isSoundEnabled
        soundManager.isEnabled = isSoundEnabled
    }

    override fun onCleared() {
        super.onCleared()
        soundManager.release()
    }
}
