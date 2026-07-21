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

    fun createPlayingState(
        level: Int,
        canvasWidth: Float,
        canvasHeight: Float,
        score: Int = 0,
        lives: Int = 3
    ): GameState.Playing {
        bulletIdCounter = 0L
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
            score = score,
            level = level,
            stageState = StageState.Entering
        )
    }

    private fun createFormation(level: Int, canvasWidth: Float, canvasHeight: Float): List<Enemy> {
        val rows = min(2 + (level - 1), 5)
        val cols = 6
        val marginX = canvasWidth * 0.08f
        val spacingX = (canvasWidth - 2f * marginX) / (cols - 1).coerceAtLeast(1)
        val spacingY = canvasHeight * 0.035f
        val startY = canvasHeight * 0.02f

        var idCounter = 0
        return buildList {
            for (row in 0 until rows) {
                for (col in 0 until cols) {
                    val formationX = marginX + col * spacingX
                    val formationY = startY + row * spacingY
                    val isBossRow = level % 3 == 0 && row == 0
                    val type = when {
                        isBossRow && col == cols / 2 -> EnemyType.BOSS
                        isBossRow -> EnemyType.ZAKO
                        level >= 4 && row < 2 -> EnemyType.GOEI
                        level >= 7 && row < 3 -> EnemyType.GOEI
                        else -> EnemyType.ZAKO
                    }
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
                            health = type.maxHealth,
                            shootCooldown = 2f + GameRandom.nextFloat() * 3f,
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
        var score = state.score
        val frameCount = state.frameCount + 1
        var stageState = state.stageState

        // 1. Player timers (invulnerability, shoot cooldown)
        player = player.updateTimers(deltaTime)

        // 2. Auto-shoot
        if (player.canShoot()) {
            val bullet = Bullet(
                id = bulletIdCounter++,
                x = player.x,
                y = player.y - player.height / 2f,
                vx = 0f,
                vy = -950f,
                isPlayerBullet = true
            )
            playerBullets = playerBullets + bullet
            player = player.resetShootCooldown()
        }

        // 3. Update bullets and remove off-screen
        val margin = 100f
        playerBullets = playerBullets.map { it.update(deltaTime) }.filter { b ->
            b.y > -margin && b.y < canvasHeight + margin &&
            b.x > -margin && b.x < canvasWidth + margin
        }
        enemyBullets = enemyBullets.map { it.update(deltaTime) }.filter { b ->
            b.y > -margin && b.y < canvasHeight + margin &&
            b.x > -margin && b.x < canvasWidth + margin
        }

        // 4. Update explosions
        explosions = explosions.mapNotNull { e ->
            val nt = e.timer - deltaTime
            if (nt <= 0f) null else e.copy(timer = nt)
        }

        // 5. Update enemies
        val entranceDuration = 2.0f
        enemies = enemies.map { enemy ->
            when (val s = enemy.state) {
                is EnemyState.Entering -> {
                    val delay = enemy.formationRow * 0.15f + enemy.formationCol * 0.04f
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
                    val duration = if (s.diveType == DiveType.MULTI_SEGMENT) 2.0f else 1.2f
                    val np = (s.progress + deltaTime / duration).coerceAtMost(1f)
                    val pos = when (s.diveType) {
                        DiveType.STRAIGHT -> straightDive(np, s.startX, s.startY, s.targetX, s.targetY)
                        DiveType.S_CURVE -> sCurveDive(np, s.startX, s.startY, s.targetX, s.targetY)
                        DiveType.MULTI_SEGMENT -> multiSegmentDive(np, s.startX, s.startY, s.targetX, canvasHeight)
                    }

                    // Shoot once during dive at progress ~0.45
                    var hasShot = s.hasShot
                    if (!hasShot && np >= 0.45f) {
                        val eb = Bullet(
                            id = bulletIdCounter++,
                            x = pos.x,
                            y = pos.y + enemy.height / 2f,
                            vx = 0f,
                            vy = 380f + state.level * 40f,
                            isPlayerBullet = false
                        )
                        enemyBullets = enemyBullets + eb
                        hasShot = true
                    }

                    if (np >= 1f) {
                        if (GameRandom.nextFloat() < 0.5f) {
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
                    val np = (s.progress + deltaTime / 1.5f).coerceAtMost(1f)
                    val x = lerp(s.startX, enemy.formationTargetX, np)
                    val y = lerp(s.startY, enemy.formationTargetY, np)
                    if (np >= 1f) {
                        enemy.copy(
                            x = enemy.formationTargetX, y = enemy.formationTargetY,
                            state = EnemyState.Formation,
                            shootCooldown = 1.5f + GameRandom.nextFloat() * 3f
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

        // 7. Dive decision (only in Formation stage)
        if (stageState is StageState.Formation) {
            val diving = enemies.count { it.state is EnemyState.Diving }
            val maxDivers = (1 + state.level / 2).coerceAtMost(4)
            if (diving < maxDivers) {
                val candidates = enemies.filter { it.state is EnemyState.Formation }
                if (candidates.isNotEmpty()) {
                    val diveRate = 0.004f * state.level * deltaTime * 60f
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

        // 8. Collision: player bullets vs enemies
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
                        e.copy(state = EnemyState.Destroyed())
                    } else {
                        e.copy(health = nh)
                    }
                } else e
            }
        }

        // 9. Collision: enemy bullets vs player
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

        // 10. Collision: enemies vs player
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

        // 11. Check game over
        if (player.lives <= 0) {
            return GameState.GameOver(score = score, level = state.level)
        }

        // 12. Check level complete
        val aliveEnemies = enemies.filter { it.isAlive }
        if (aliveEnemies.isEmpty() && stageState is StageState.Formation) {
            val nextLevel = state.level + 1
            return if (nextLevel > 8) {
                GameState.Victory(score = score)
            } else {
                GameState.LevelIntro(
                    level = nextLevel,
                    remainingTime = 3f,
                    lives = player.lives,
                    score = score
                )
            }
        }

        return state.copy(
            player = player,
            enemies = enemies,
            playerBullets = playerBullets,
            enemyBullets = enemyBullets,
            explosions = explosions,
            score = score,
            stageState = stageState,
            frameCount = frameCount
        )
    }
}
