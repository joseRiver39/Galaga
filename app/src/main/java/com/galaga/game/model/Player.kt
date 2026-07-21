package com.galaga.game.model

import kotlin.math.abs
import kotlin.math.atan2

data class Player(
    val x: Float,
    val y: Float,
    val width: Float = 84f,
    val height: Float = 90f,
    val lives: Int = 3,
    val invulnerabilityTimer: Float = 0f,
    val shootCooldown: Float = 0f,
    val isInvulnerable: Boolean = false,
    val movementAngle: Float = 0f,
    val powerLevel: Int = 1,
    val hasShield: Boolean = false
) {
    companion object {
        const val INVULNERABILITY_DURATION = 2.5f
        const val SHOOT_COOLDOWN = 0.22f
    }

    fun move(dx: Float, dy: Float, canvasWidth: Float, canvasHeight: Float): Player {
        val newX = (x + dx).coerceIn(width / 2, canvasWidth - width / 2)
        val newY = (y + dy).coerceIn(height / 2, canvasHeight - height / 2 - 48f)
        return copy(x = newX, y = newY, movementAngle = 0f)
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
        if (hasShield) {
            return copy(
                hasShield = false,
                isInvulnerable = true,
                invulnerabilityTimer = 1.0f
            )
        }
        val nextPower = (powerLevel - 1).coerceAtLeast(1)
        return copy(
            lives = lives - 1,
            powerLevel = nextPower,
            isInvulnerable = true,
            invulnerabilityTimer = INVULNERABILITY_DURATION
        )
    }

    fun upgradePower(): Player = copy(powerLevel = (powerLevel + 1).coerceAtMost(3))

    fun activateShield(): Player = copy(hasShield = true)

    fun addLife(): Player = copy(lives = lives + 1)
}
