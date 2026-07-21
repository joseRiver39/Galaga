package com.galaga.game.model

data class Bullet(
    val id: Long,
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val width: Float = 5f,
    val height: Float = 18f,
    val isPlayerBullet: Boolean
) {
    fun update(deltaTime: Float): Bullet = copy(x = x + vx * deltaTime, y = y + vy * deltaTime)
}
