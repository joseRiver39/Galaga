package com.galaga.game.model

import com.galaga.game.engine.DiveType

enum class EnemyType(val scoreValue: Int, val maxHealth: Int) {
    ZAKO(100, 1),
    GOEI(200, 2),
    BOSS(1000, 8)
}

sealed class EnemyState {
    data class Entering(val progress: Float = 0f) : EnemyState()
    data object Formation : EnemyState()
    data class Diving(
        val diveType: DiveType,
        val startX: Float,
        val startY: Float,
        val targetX: Float,
        val targetY: Float,
        val progress: Float = 0f,
        val hasShot: Boolean = false
    ) : EnemyState()
    data class Returning(
        val startX: Float,
        val startY: Float,
        val progress: Float = 0f
    ) : EnemyState()
    data class Destroyed(val timer: Float = 0.4f) : EnemyState()
}

data class Enemy(
    val id: Int,
    val type: EnemyType,
    val x: Float,
    val y: Float,
    val formationCol: Int,
    val formationRow: Int,
    val formationTargetX: Float,
    val formationTargetY: Float,
    val state: EnemyState,
    val health: Int,
    val shootCooldown: Float,
    val animTimer: Float
) {
    val isAlive: Boolean get() = state !is EnemyState.Destroyed

    val width: Float get() = when (type) {
        EnemyType.ZAKO -> 48f
        EnemyType.GOEI -> 60f
        EnemyType.BOSS -> 90f
    }

    val height: Float get() = when (type) {
        EnemyType.ZAKO -> 48f
        EnemyType.GOEI -> 56f
        EnemyType.BOSS -> 80f
    }
}
