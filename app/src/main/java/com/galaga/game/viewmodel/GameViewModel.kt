package com.galaga.game.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.galaga.game.engine.GameEngine
import com.galaga.game.model.GameState
import kotlin.random.Random

data class Star(val x: Float, val y: Float, val size: Float, val alpha: Float)

class GameViewModel : ViewModel() {

    var gameState by mutableStateOf<GameState>(GameState.Menu)
        private set

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
        gameState = engine.update(current, clamped, canvasWidth, canvasHeight)
    }

    fun onPlayerDrag(dx: Float, dy: Float) {
        val s = gameState
        if (s is GameState.Playing) {
            gameState = s.copy(player = s.player.move(dx, dy, canvasWidth, canvasHeight))
        }
    }

    fun startGame() {
        gameState = GameState.LevelIntro(level = 1, remainingTime = 3f, lives = 3, score = 0)
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
}
