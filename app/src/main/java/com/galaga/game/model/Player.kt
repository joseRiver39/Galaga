package com.galaga.game.model

import kotlin.math.abs
import kotlin.math.atan2

data class Player(
    val x: Float,
    val y: Float,
    val width: Float = 56f,
    val height: Float = 60f,
    val lives: Int = 3,
    val invulnerabilityTimer: Float = 0f,
    val shootCooldown: Float = 0f,
    val isInvulnerable: Boolean = false,
    val movementAngle: Float = 0f
) {
    companion object {
        const val INVULNERABILITY_DURATION = 2.5f
        const val SHOOT_COOLDOWN = 0.25f
    }

    fun move(dx: Float, dy: Float, canvasWidth: Float, canvasHeight: Float): Player {
        val newX = (x + dx).coerceIn(width / 2, canvasWidth - width / 2)
        val newY = (y + dy).coerceIn(height / 2, canvasHeight - height / 2)
        val angle = if (abs(dx) > 0.5f || abs(dy) > 0.5f) {
            atan2(dx, -dy)
        } else movementAngle
        return copy(x = newX, y = newY, movementAngle = angle)
    }

    fun updateTimers(deltaTime: Float): Player {
        var p = this
        if (isInvulnerable) {
            p = p.copy(invulnerabilityTimer = p.invulnerabilityTimer - deltaTime)
            if (p.invulnerabilityTimer <= 0f) {
                p = p.copy(isInvulnerable = false, invulnerabilityTimer = 0f)
            }
        }
        p = p.copy(shootCooldown = (p.shootCooldown - deltaTime).coerceAtLeast(0f))
        return p
    }

    fun canShoot(): Boolean = shootCooldown <= 0f

    fun resetShootCooldown(): Player = copy(shootCooldown = SHOOT_COOLDOWN)

    fun takeHit(): Player {
        if (isInvulnerable) return this
        return copy(
            lives = lives - 1,
            isInvulnerable = true,
            invulnerabilityTimer = INVULNERABILITY_DURATION
        )
    }
}
