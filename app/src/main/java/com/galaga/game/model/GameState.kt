package com.galaga.game.model

import com.galaga.game.engine.Explosion

sealed class GameState {

    /** Menú principal del juego */
    data object Menu : GameState()

    /** Juego activo */
    data class Playing(
        val player: Player,
        val enemies: List<Enemy>,
        val playerBullets: List<Bullet>,
        val enemyBullets: List<Bullet>,
        val explosions: List<Explosion>,
        val powerUps: List<PowerUp> = emptyList(),
        val score: Int,
        val level: Int,
        val stageState: StageState,
        val frameCount: Long = 0,
        val screenShake: Float = 0f
    ) : GameState()

    /** Pausa */
    data class Paused(val playingState: Playing) : GameState()

    /** Game Over */
    data class GameOver(val score: Int, val level: Int) : GameState()

    /** Transición entre niveles (cuenta regresiva) */
    data class LevelIntro(
        val level: Int,
        val remainingTime: Float = 3f,
        val lives: Int = 3,
        val score: Int = 0,
        val powerLevel: Int = 1
    ) : GameState()

    /** Victoria */
    data class Victory(val score: Int) : GameState()
}

/** Estado del escenario actual */
sealed class StageState {
    /** Enemigos entrando a formación */
    data object Entering : StageState()
    /** Enemigos en formación, atacando */
    data object Formation : StageState()
    /** Jefe aproximándose */
    data object BossApproaching : StageState()
}
