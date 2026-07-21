package com.galaga.game.engine

import com.galaga.game.model.*
import kotlin.math.min

data class Explosion(
    val x: Float,
    val y: Float,
    val timer: Float = 0.5f
) {
    val progress: Float get() = (0.5f - timer) / 0.5f
    val alpha: Float get() = 1f - progress
    val radius: Float get() = 5f + 45f * progress
}

enum class DiveType { STRAIGHT, S_CURVE, MULTI_SEGMENT }

class GameEngine {
    private var bulletIdCounter = 0L
    private var powerUpIdCounter = 0L

    fun createPlayingState(
        level: Int,
        canvasWidth: Float,
        canvasHeight: Float,
        score: Int = 0,
        lives: Int = 3
    ): GameState.Playing {
        bulletIdCounter = 0L
        powerUpIdCounter = 0L
        val player = Player(
            x = canvasWidth / 2f,
            y = canvasHeight * 0.93f,
            lives = lives
        )
        val enemies = createFormation(level, canvasWidth, canvasHeight)
        return GameState.Playing(
            player = player,
            enemies = enemies,
            playerBullets = emptyList(),
            enemyBullets = emptyList(),
            explosions = emptyList(),
            powerUps = emptyList(),
            score = score,
            level = level,
            stageState = StageState.Entering
        )
    }

    private fun createFormation(level: Int, canvasWidth: Float, canvasHeight: Float): List<Enemy> {
        val rows = (2 + (level - 1) / 2).coerceAtMost(5)
        val cols = (6 + (level - 1) % 3 * 2).coerceAtMost(10)
        val marginX = canvasWidth * 0.06f
        val spacingX = (canvasWidth - 2f * marginX) / (cols - 1).coerceAtLeast(1)
        val spacingY = canvasHeight * 0.038f
        val startY = canvasHeight * 0.02f

        var idCounter = 0
        return buildList {
            for (row in 0 until rows) {
                for (col in 0 until cols) {
                    val formationX = marginX + col * spacingX
                    val formationY = startY + row * spacingY
                    val isBossRow = level % 2 == 0 && row == 0
                    val isBossCell = isBossRow && (col == cols / 2 || (cols >= 8 && col == cols / 2 - 1))
                    val type = when {
                        isBossCell -> EnemyType.BOSS
                        row < 2 && level >= 3 -> EnemyType.GOEI
                        else -> EnemyType.ZAKO
                    }
                    val extraHealth = if (type == EnemyType.BOSS) level / 4 else 0
                    add(
                        Enemy(
                            id = idCounter++,
                            type = type,
                            x = formationX,
                            y = -100f,
                            formationCol = col,
                            formationRow = row,
                            formationTargetX = formationX,
                            formationTargetY = formationY,
                            state = EnemyState.Entering(),
                            health = type.maxHealth + extraHealth,
                            shootCooldown = (2f + GameRandom.nextFloat() * 3f) / (1f + level * 0.05f),
                            animTimer = 0f
                        )
                    )
                }
            }
        }
    }

    fun update(
        state: GameState,
        deltaTime: Float,
        canvasWidth: Float,
        canvasHeight: Float
    ): GameState {
        return when (state) {
            is GameState.LevelIntro -> updateLevelIntro(state, deltaTime, canvasWidth, canvasHeight)
            is GameState.Playing -> updatePlaying(state, deltaTime, canvasWidth, canvasHeight)
            else -> state
        }
    }

    private fun updateLevelIntro(
        state: GameState.LevelIntro,
        deltaTime: Float,
        canvasWidth: Float,
        canvasHeight: Float
    ): GameState {
        val remaining = state.remainingTime - deltaTime
        if (remaining <= 0f) {
            return createPlayingState(
                level = state.level,
                canvasWidth = canvasWidth,
                canvasHeight = canvasHeight,
                score = state.score,
                lives = state.lives
            )
        }
        return state.copy(remainingTime = remaining)
    }

    private fun updatePlaying(
        state: GameState.Playing,
        deltaTime: Float,
        canvasWidth: Float,
        canvasHeight: Float
    ): GameState {
        var player = state.player
        var enemies = state.enemies
        var playerBullets = state.playerBullets
        var enemyBullets = state.enemyBullets
        var explosions = state.explosions
        var powerUps = state.powerUps
        var score = state.score
        val frameCount = state.frameCount + 1
        var stageState = state.stageState

        // 1. Player timers
        player = player.updateTimers(deltaTime)

        // 2. Auto-shoot según el powerLevel del jugador
        if (player.canShoot()) {
            val shootY = player.y - player.height / 2f
            val newBullets = when (player.powerLevel) {
                1 -> listOf(
                    Bullet(id = bulletIdCounter++, x = player.x, y = shootY, vx = 0f, vy = -980f, isPlayerBullet = true)
                )
                2 -> listOf(
                    Bullet(id = bulletIdCounter++, x = player.x - 20f, y = shootY, vx = 0f, vy = -980f, isPlayerBullet = true),
                    Bullet(id = bulletIdCounter++, x = player.x + 20f, y = shootY, vx = 0f, vy = -980f, isPlayerBullet = true)
                )
                else -> listOf(
                    Bullet(id = bulletIdCounter++, x = player.x - 22f, y = shootY, vx = -140f, vy = -950f, isPlayerBullet = true),
                    Bullet(id = bulletIdCounter++, x = player.x, y = shootY - 4f, vx = 0f, vy = -1000f, isPlayerBullet = true),
                    Bullet(id = bulletIdCounter++, x = player.x + 22f, y = shootY, vx = 140f, vy = -950f, isPlayerBullet = true)
                )
            }
            playerBullets = playerBullets + newBullets
            player = player.resetShootCooldown()
        }

        // 3. Update proyectiles y PowerUps (remover fuera de pantalla)
        val margin = 100f
        playerBullets = playerBullets.map { it.update(deltaTime) }.filter { b ->
            b.y > -margin && b.y < canvasHeight + margin &&
            b.x > -margin && b.x < canvasWidth + margin
        }
        enemyBullets = enemyBullets.map { it.update(deltaTime) }.filter { b ->
            b.y > -margin && b.y < canvasHeight + margin &&
            b.x > -margin && b.x < canvasWidth + margin
        }
        powerUps = powerUps.map { it.update(deltaTime) }.filter { p ->
            p.y < canvasHeight + margin
        }

        // 4. Update explosiones
        explosions = explosions.mapNotNull { e ->
            val nt = e.timer - deltaTime
            if (nt <= 0f) null else e.copy(timer = nt)
        }

        // 5. Update enemigos
        val entranceDuration = (2.0f / (1f + state.level * 0.02f)).coerceAtLeast(1.0f)
        enemies = enemies.map { enemy ->
            when (val s = enemy.state) {
                is EnemyState.Entering -> {
                    val delay = enemy.formationRow * 0.12f + enemy.formationCol * 0.03f
                    val raw = frameCount * (1f / 60f) / entranceDuration - delay
                    val progress = raw.coerceIn(0f, 1f)
                    val pos = entrancePosition(progress, enemy.formationTargetX, enemy.formationTargetY, canvasWidth, canvasHeight)
                    if (progress >= 1f) {
                        enemy.copy(x = pos.x, y = pos.y, state = EnemyState.Formation)
                    } else {
                        enemy.copy(x = pos.x, y = pos.y, state = EnemyState.Entering(progress))
                    }
                }

                is EnemyState.Formation -> {
                    val bob = formationBob(frameCount, enemy.formationCol, enemy.formationRow)
                    val cd = (enemy.shootCooldown - deltaTime).coerceAtLeast(0f)
                    enemy.copy(
                        x = enemy.formationTargetX + bob.x,
                        y = enemy.formationTargetY + bob.y,
                        animTimer = enemy.animTimer + deltaTime,
                        shootCooldown = cd
                    )
                }

                is EnemyState.Diving -> {
                    val duration = if (s.diveType == DiveType.MULTI_SEGMENT) 1.8f else 1.1f
                    val np = (s.progress + deltaTime / duration).coerceAtMost(1f)
                    val pos = when (s.diveType) {
                        DiveType.STRAIGHT -> straightDive(np, s.startX, s.startY, s.targetX, s.targetY)
                        DiveType.S_CURVE -> sCurveDive(np, s.startX, s.startY, s.targetX, s.targetY)
                        DiveType.MULTI_SEGMENT -> multiSegmentDive(np, s.startX, s.startY, s.targetX, canvasHeight)
                    }

                    var hasShot = s.hasShot
                    if (!hasShot && np >= 0.45f) {
                        val eb = Bullet(
                            id = bulletIdCounter++,
                            x = pos.x,
                            y = pos.y + enemy.height / 2f,
                            vx = 0f,
                            vy = 400f + state.level * 20f,
                            isPlayerBullet = false
                        )
                        enemyBullets = enemyBullets + eb
                        hasShot = true
                    }

                    if (np >= 1f) {
                        if (GameRandom.nextFloat() < 0.6f) {
                            enemy.copy(
                                x = pos.x, y = pos.y,
                                state = EnemyState.Returning(startX = pos.x, startY = pos.y)
                            )
                        } else {
                            enemy.copy(state = EnemyState.Destroyed())
                        }
                    } else {
                        enemy.copy(
                            x = pos.x, y = pos.y,
                            state = s.copy(progress = np, hasShot = hasShot)
                        )
                    }
                }

                is EnemyState.Returning -> {
                    val np = (s.progress + deltaTime / 1.4f).coerceAtMost(1f)
                    val x = lerp(s.startX, enemy.formationTargetX, np)
                    val y = lerp(s.startY, enemy.formationTargetY, np)
                    if (np >= 1f) {
                        enemy.copy(
                            x = enemy.formationTargetX, y = enemy.formationTargetY,
                            state = EnemyState.Formation,
                            shootCooldown = 1.2f + GameRandom.nextFloat() * 2.5f
                        )
                    } else {
                        enemy.copy(x = x, y = y, state = s.copy(progress = np))
                    }
                }

                is EnemyState.Destroyed -> {
                    val nt = s.timer - deltaTime
                    if (nt <= 0f) enemy.copy(state = s.copy(timer = 0f))
                    else enemy.copy(state = s.copy(timer = nt))
                }
            }
        }.filter { e -> !(e.state is EnemyState.Destroyed && e.state.timer <= 0f) }

        // 6. Stage state transition
        if (stageState is StageState.Entering) {
            val anyEntering = enemies.any { it.state is EnemyState.Entering }
            if (!anyEntering) stageState = StageState.Formation
        }

        // 7. Dive decision
        if (stageState is StageState.Formation) {
            val diving = enemies.count { it.state is EnemyState.Diving }
            val maxDivers = (2 + state.level / 3).coerceAtMost(6)
            if (diving < maxDivers) {
                val candidates = enemies.filter { it.state is EnemyState.Formation }
                if (candidates.isNotEmpty()) {
                    val diveRate = (0.005f * state.level * deltaTime * 60f).coerceAtMost(0.25f)
                    if (GameRandom.nextFloat() < diveRate) {
                        val diver = candidates[(GameRandom.nextFloat() * candidates.size).toInt().coerceAtMost(candidates.size - 1)]
                        val spread = canvasWidth * 0.15f
                        val targetX = when (diver.type) {
                            EnemyType.GOEI -> player.x
                            EnemyType.ZAKO -> player.x + (GameRandom.nextFloat() - 0.5f) * spread * 2f
                            EnemyType.BOSS -> player.x
                        }
                        enemies = enemies.map {
                            if (it.id == diver.id) {
                                it.copy(state = EnemyState.Diving(
                                    diveType = when (it.type) {
                                        EnemyType.ZAKO -> DiveType.STRAIGHT
                                        EnemyType.GOEI -> DiveType.S_CURVE
                                        EnemyType.BOSS -> DiveType.MULTI_SEGMENT
                                    },
                                    startX = it.x,
                                    startY = it.y,
                                    targetX = targetX,
                                    targetY = player.y + player.height / 2f
                                ))
                            } else it
                        }
                    }
                }
            }
        }

        // 8. Colisión: Disparos del jugador vs Enemigos y Spawn de Power-Ups
        val bulletsToRemove = mutableSetOf<Long>()
        val enemiesHit = mutableSetOf<Int>()

        for (bullet in playerBullets) {
            if (bullet.id in bulletsToRemove) continue
            for (enemy in enemies) {
                if (!enemy.isAlive) continue
                if (checkCollision(
                        bullet.x, bullet.y, bullet.width, bullet.height,
                        enemy.x, enemy.y, enemy.width, enemy.height,
                        0.1f, 0.1f
                    )
                ) {
                    bulletsToRemove.add(bullet.id)
                    enemiesHit.add(enemy.id)
                    break
                }
            }
        }

        playerBullets = playerBullets.filter { it.id !in bulletsToRemove }

        for (eid in enemiesHit) {
            enemies = enemies.map { e ->
                if (e.id == eid) {
                    val nh = e.health - 1
                    if (nh <= 0) {
                        explosions = explosions + Explosion(x = e.x, y = e.y)
                        score += e.type.scoreValue

                        // Spawn de Power-Up (35% de probabilidad al destruir enemigo)
                        if (GameRandom.nextFloat() < 0.35f) {
                            val roll = GameRandom.nextFloat()
                            val pType = when {
                                roll < 0.50f -> PowerUpType.WEAPON_UPGRADE
                                roll < 0.85f -> PowerUpType.SHIELD
                                else -> PowerUpType.EXTRA_LIFE
                            }
                            powerUps = powerUps + PowerUp(
                                id = powerUpIdCounter++,
                                x = e.x,
                                y = e.y,
                                type = pType
                            )
                        }

                        e.copy(state = EnemyState.Destroyed())
                    } else {
                        e.copy(health = nh)
                    }
                } else e
            }
        }

        // 9. Colisión: Jugador vs Power-Ups
        val powerUpsToRemove = mutableSetOf<Long>()
        for (p in powerUps) {
            if (checkCollision(
                    p.x, p.y, p.width, p.height,
                    player.x, player.y, player.width, player.height,
                    0.2f, 0.2f
                )
            ) {
                powerUpsToRemove.add(p.id)
                player = when (p.type) {
                    PowerUpType.WEAPON_UPGRADE -> player.upgradePower()
                    PowerUpType.SHIELD -> player.activateShield()
                    PowerUpType.EXTRA_LIFE -> player.addLife()
                }
            }
        }
        powerUps = powerUps.filter { it.id !in powerUpsToRemove }

        // 10. Colisión: Disparos enemigos vs Jugador
        var playerWasHit = false
        for (bullet in enemyBullets) {
            if (playerWasHit) break
            if (player.isInvulnerable) continue
            if (checkCollision(
                    bullet.x, bullet.y, bullet.width, bullet.height,
                    player.x, player.y, player.width, player.height,
                    0.2f, 0.2f
                )
            ) {
                player = player.takeHit()
                explosions = explosions + Explosion(x = player.x, y = player.y)
                enemyBullets = enemyBullets.filter { it.id != bullet.id }
                playerWasHit = true
            }
        }

        // 11. Colisión: Enemigos vs Jugador
        if (!player.isInvulnerable && !playerWasHit) {
            for (enemy in enemies) {
                if (!enemy.isAlive) continue
                if (checkCollision(
                        enemy.x, enemy.y, enemy.width, enemy.height,
                        player.x, player.y, player.width, player.height,
                        0.15f, 0.15f
                    )
                ) {
                    player = player.takeHit()
                    explosions = explosions + Explosion(x = player.x, y = player.y)
                    break
                }
            }
        }

        // 12. Verificar Game Over
        if (player.lives <= 0) {
            return GameState.GameOver(score = score, level = state.level)
        }

        // 13. Verificar avance de nivel (Soporte infinito para +100 niveles)
        val aliveEnemies = enemies.filter { it.isAlive }
        if (aliveEnemies.isEmpty() && stageState is StageState.Formation) {
            val nextLevel = state.level + 1
            return GameState.LevelIntro(
                level = nextLevel,
                remainingTime = 2.5f,
                lives = player.lives,
                score = score
            )
        }

        return state.copy(
            player = player,
            enemies = enemies,
            playerBullets = playerBullets,
            enemyBullets = enemyBullets,
            explosions = explosions,
            powerUps = powerUps,
            score = score,
            stageState = stageState,
            frameCount = frameCount
        )
    }
}
