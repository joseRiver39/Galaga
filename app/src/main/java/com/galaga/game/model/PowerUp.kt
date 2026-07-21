package com.galaga.game.model

enum class PowerUpType {
    WEAPON_UPGRADE,
    SHIELD,
    EXTRA_LIFE
}

data class PowerUp(
    val id: Long,
    val x: Float,
    val y: Float,
    val type: PowerUpType,
    val width: Float = 36f,
    val height: Float = 36f,
    val vy: Float = 140f
) {
    fun update(deltaTime: Float): PowerUp {
        return copy(y = y + vy * deltaTime)
    }
}
