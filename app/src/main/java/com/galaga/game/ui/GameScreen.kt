package com.galaga.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import com.galaga.game.R
import com.galaga.game.model.*
import com.galaga.game.ui.theme.*
import com.galaga.game.viewmodel.GameViewModel
import kotlinx.coroutines.isActive
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.abs

@Composable
fun GameScreen(viewModel: GameViewModel) {
    val state by remember { derivedStateOf { viewModel.gameState } }
    val textMeasurer = rememberTextMeasurer()

    // Sprites de Naves del Jugador segun PowerLevel
    val shipLevel1 = ImageBitmap.imageResource(id = R.drawable.player_ship)
    val shipLevel2 = ImageBitmap.imageResource(id = R.drawable.player_ship_power2)
    val shipLevel3 = ImageBitmap.imageResource(id = R.drawable.player_ship_power3)

    // Sprites de Enemigos
    val enemyZakoImage = ImageBitmap.imageResource(id = R.drawable.enemy_zako)
    val enemyGoeiImage = ImageBitmap.imageResource(id = R.drawable.enemy_goei)
    val enemyBossImage = ImageBitmap.imageResource(id = R.drawable.enemy_boss)

    // Sprites de Power-Ups
    val powerupWeaponImage = ImageBitmap.imageResource(id = R.drawable.powerup_weapon)
    val powerupShieldImage = ImageBitmap.imageResource(id = R.drawable.powerup_shield)
    val powerupLifeImage   = ImageBitmap.imageResource(id = R.drawable.powerup_life)

    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameNanos { viewModel.onFrame(it) }
        }
    }

    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current

    LaunchedEffect(viewportSize) {
        if (viewportSize.width > 0 && viewportSize.height > 0) {
            with(density) {
                viewModel.setCanvasSize(
                    viewportSize.width.toFloat(),
                    viewportSize.height.toFloat()
                )
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val gs = state

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { viewportSize = it }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        viewModel.onPlayerDrag(dragAmount.x, dragAmount.y)
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures {
                        when (viewModel.gameState) {
                            is GameState.Menu -> {} // Handled by compose UI
                            is GameState.GameOver -> viewModel.startNewGame()
                            is GameState.Victory -> viewModel.startNewGame()
                            is GameState.Playing -> viewModel.pause()
                            else -> {}
                        }
                    }
                }
        ) {
            val cw = size.width
            val ch = size.height
            when (gs) {
                is GameState.Menu -> drawMenu(this, textMeasurer, cw, ch)
                is GameState.Playing -> drawPlaying(
                    this, textMeasurer, gs, cw, ch, viewModel,
                    shipLevel1, shipLevel2, shipLevel3,
                    enemyZakoImage, enemyGoeiImage, enemyBossImage,
                    powerupWeaponImage, powerupShieldImage, powerupLifeImage
                )
                is GameState.Paused -> {
                    drawPlaying(
                        this, textMeasurer, gs.playingState, cw, ch, viewModel,
                        shipLevel1, shipLevel2, shipLevel3,
                        enemyZakoImage, enemyGoeiImage, enemyBossImage,
                        powerupWeaponImage, powerupShieldImage, powerupLifeImage
                    )
                }
                is GameState.GameOver -> drawGameOver(this, textMeasurer, gs, cw, ch)
                is GameState.LevelIntro -> drawLevelIntro(this, textMeasurer, gs, cw, ch)
                is GameState.Victory -> drawVictory(this, textMeasurer, gs, cw, ch)
            }
        }

        if (gs is GameState.Paused) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.layout.Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    androidx.compose.foundation.text.BasicText(
                        text = "MENÚ DE CONFIGURACIÓN",
                        style = TextStyle(color = HudAccentColor, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(24.dp))
                    androidx.compose.foundation.text.BasicText(
                        text = if (viewModel.isSoundEnabled) "Sonido: ON 🔊" else "Sonido: OFF 🔇",
                        style = TextStyle(color = Color.White, fontSize = 24.sp),
                        modifier = Modifier
                            .clickable { viewModel.toggleSound() }
                            .padding(16.dp)
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(16.dp))
                    androidx.compose.foundation.text.BasicText(
                        text = "REANUDAR PARTIDA",
                        style = TextStyle(color = Color.White, fontSize = 20.sp),
                        modifier = Modifier
                            .clickable { viewModel.resume() }
                            .padding(16.dp)
                    )
                }
            }
        }

        if (gs is GameState.Menu) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 120.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                androidx.compose.foundation.layout.Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (viewModel.hasSavedGame) {
                        androidx.compose.foundation.text.BasicText(
                            text = "CONTINUAR PARTIDA",
                            style = TextStyle(color = Color(0xFF00E5FF), fontSize = 26.sp, fontWeight = FontWeight.Bold),
                            modifier = Modifier
                                .clickable { viewModel.continueGame() }
                                .padding(16.dp)
                        )
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(8.dp))
                    }
                    androidx.compose.foundation.text.BasicText(
                        text = "NUEVA PARTIDA",
                        style = TextStyle(color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold),
                        modifier = Modifier
                            .clickable { viewModel.startNewGame() }
                            .padding(16.dp)
                    )
                }
            }
        }
    }
}

// ── Drawing helpers ─────────────────────────────────────────────

private fun drawPlaying(
    d: DrawScope, tm: androidx.compose.ui.text.TextMeasurer,
    s: GameState.Playing, cw: Float, ch: Float, vm: GameViewModel,
    shipL1: ImageBitmap, shipL2: ImageBitmap, shipL3: ImageBitmap,
    zakoImg: ImageBitmap, goeiImg: ImageBitmap, bossImg: ImageBitmap,
    weaponImg: ImageBitmap, shieldImg: ImageBitmap, lifeImg: ImageBitmap
) = with(d) {
    drawRect(BackgroundColor, size = size)
    val shakeOffset = if (s.screenShake > 0) {
        Offset(
            (kotlin.random.Random.nextFloat() - 0.5f) * s.screenShake,
            (kotlin.random.Random.nextFloat() - 0.5f) * s.screenShake
        )
    } else Offset.Zero

    translate(left = shakeOffset.x, top = shakeOffset.y) {
        for (star in vm.stars) drawCircle(Color.White.copy(alpha = star.alpha), star.size,
            Offset(star.x % cw, (star.y + s.frameCount * star.size * 0.5f) % ch))

        // Disparos del jugador
        for (b in s.playerBullets) {
            val bColor = when (s.player.powerLevel) {
                1 -> PlayerBulletColor
                2 -> Color(0xFF00E5FF)
                else -> Color(0xFFFFD700)
            }
            drawRect(bColor, Offset(b.x - b.width / 2, b.y - b.height / 2), Size(b.width, b.height))
            drawRect(Color.White.copy(alpha = 0.8f), Offset(b.x - b.width / 4, b.y - b.height / 2), Size(b.width / 2, b.height))
        }
        // Disparos enemigos
        for (b in s.enemyBullets) {
            drawCircle(EnemyBulletColor, b.width / 2, Offset(b.x, b.y))
            drawCircle(Color.Yellow.copy(alpha = 0.5f), b.width / 4, Offset(b.x, b.y))
        }
        // Explosiones
        for (ex in s.explosions) {
            drawCircle(Color(0xFFFFAA00).copy(alpha = ex.alpha * 0.6f), ex.radius * 1.5f, Offset(ex.x, ex.y))
            drawCircle(Color(0xFFFFFF44).copy(alpha = ex.alpha * 0.4f), ex.radius * 0.8f, Offset(ex.x, ex.y))
        }
        // Power-Ups cayendo
        for (p in s.powerUps) {
            val pImg = when (p.type) {
                PowerUpType.WEAPON_UPGRADE -> weaponImg
                PowerUpType.SHIELD -> shieldImg
                PowerUpType.EXTRA_LIFE -> lifeImg
            }
            drawImage(
                image = pImg,
                dstOffset = IntOffset((p.x - p.width / 2).toInt(), (p.y - p.height / 2).toInt()),
                dstSize = androidx.compose.ui.unit.IntSize(p.width.toInt(), p.height.toInt())
            )
        }
        // Enemigos
        for (e in s.enemies) {
            if (e.state is EnemyState.Destroyed) {
                val st = e.state as EnemyState.Destroyed; val p = 1f - st.timer / 0.4f
                drawCircle(Color(0xFFFFAA00).copy(alpha = 1f - p), e.width / 2 + p * 25f, Offset(e.x, e.y))
                continue
            }
            drawEnemySprite(this, e, zakoImg, goeiImg, bossImg)
        }

        // Jugador (Evolución según powerLevel y Escudo)
        val show = !s.player.isInvulnerable || (s.frameCount / 6L) % 2L == 0L
        if (show) {
            val currentShipImg = when (s.player.powerLevel) {
                1 -> shipL1
                2 -> shipL2
                else -> shipL3
            }
            drawShip(this, s.player, currentShipImg, s.frameCount)
        }
    }

    drawHud(d, tm, s, cw, ch)

    if (s.stageState is StageState.Entering && s.level % 5 == 0) {
        val alpha = 0.1f + 0.2f * kotlin.math.abs(sin(s.frameCount * 0.05f))
        drawRect(Color.Red.copy(alpha = alpha), size = size)
        val text = tm.measure(AnnotatedString("WARNING"), TextStyle(Color.Red, fontSize = 64.sp, fontWeight = FontWeight.Bold, letterSpacing = 8.sp))
        drawText(textLayoutResult = text, topLeft = Offset(cw / 2 - text.size.width / 2, ch * 0.25f))
    }
}

private fun drawEnemySprite(
    d: DrawScope, e: Enemy,
    zakoImg: ImageBitmap, goeiImg: ImageBitmap, bossImg: ImageBitmap
) = with(d) {
    val img = when (e.type) {
        EnemyType.ZAKO -> zakoImg
        EnemyType.GOEI -> goeiImg
        EnemyType.BOSS -> bossImg
    }
    val ew = e.width
    val eh = e.height

    val colorFilter = if (e.hitFlashTimer > 0f) {
        androidx.compose.ui.graphics.ColorFilter.tint(Color.White, androidx.compose.ui.graphics.BlendMode.SrcAtop)
    } else null

    drawImage(
        image = img,
        dstOffset = IntOffset((e.x - ew / 2).toInt(), (e.y - eh / 2).toInt()),
        dstSize = androidx.compose.ui.unit.IntSize(ew.toInt(), eh.toInt()),
        colorFilter = colorFilter
    )
}

private fun drawShip(
    d: DrawScope, p: com.galaga.game.model.Player,
    shipImage: ImageBitmap, frameCount: Long = 0L
) = with(d) {
    val pw = p.width
    val ph = p.height

    // Aureola de Escudo protector animado si está activo
    if (p.hasShield) {
        val pulse = 2f * sin(frameCount * 0.1f)
        val sRadius = (pw.coerceAtLeast(ph) / 2f) + 12f + pulse
        drawCircle(
            color = Color(0xFF00E5FF).copy(alpha = 0.35f + 0.15f * sin(frameCount * 0.15f)),
            radius = sRadius,
            center = Offset(p.x, p.y)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.6f),
            radius = sRadius,
            center = Offset(p.x, p.y),
            style = Stroke(width = 2.5f)
        )
    }

    // Thruster glow animado detrás de la nave
    val exhaustR = Offset(p.x + pw * 0.15f, p.y + ph * 0.45f)
    val exhaustL = Offset(p.x - pw * 0.15f, p.y + ph * 0.45f)
    val t = frameCount * 0.15f
    val glowColor = when (p.powerLevel) {
        1 -> PlayerColor
        2 -> Color(0xFF00E5FF)
        else -> Color(0xFFFFD700)
    }
    val glowR = glowColor.copy(alpha = 0.5f + 0.3f * sin(t))
    val glowL = glowColor.copy(alpha = 0.5f + 0.3f * cos(t + 1f))
    drawCircle(glowR, pw * 0.12f, exhaustR)
    drawCircle(glowL, pw * 0.12f, exhaustL)
    drawCircle(Color.White.copy(alpha = 0.5f), pw * 0.05f, exhaustR)
    drawCircle(Color.White.copy(alpha = 0.5f), pw * 0.05f, exhaustL)

    // Renderizar la nave espacial según el nivel de poder
    drawImage(
        image = shipImage,
        dstOffset = IntOffset((p.x - pw / 2).toInt(), (p.y - ph / 2).toInt()),
        dstSize = androidx.compose.ui.unit.IntSize(pw.toInt(), ph.toInt())
    )
}

private fun drawHud(
    d: DrawScope, tm: androidx.compose.ui.text.TextMeasurer,
    s: GameState.Playing, cw: Float, ch: Float
) = with(d) {
    val barHeight = 44f
    val navBarOffset = 60f          // margen para la barra de navegación del dispositivo
    val barTop = ch - barHeight - navBarOffset

    // Fondo elegante y traslúcido para la barra HUD inferior
    drawRect(
        color = Color(0xFF0C0F20).copy(alpha = 0.90f),
        topLeft = Offset(0f, barTop),
        size = Size(cw, barHeight)
    )
    // Línea divisora superior brillante
    drawLine(
        color = Color(0xFF00E5FF).copy(alpha = 0.5f),
        start = Offset(0f, barTop),
        end = Offset(cw, barTop),
        strokeWidth = 2f
    )

    val textY = barTop + 12f
    val sty = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold)

    // Puntaje (Izquierda)
    val sc = tm.measure(AnnotatedString("PTS: ${s.score}"), sty.copy(color = HudTextColor))
    drawText(textLayoutResult = sc, topLeft = Offset(14f, textY))

    // Poder / Escudo (Centro-Izquierda)
    val pwrText = if (s.player.hasShield) "PWR L${s.player.powerLevel} [🛡️]" else "PWR L${s.player.powerLevel}"
    val pwrColor = when (s.player.powerLevel) {
        1 -> Color(0xFF00E5FF)
        2 -> Color(0xFF00FF88)
        else -> Color(0xFFFFD700)
    }
    val pwr = tm.measure(AnnotatedString(pwrText), sty.copy(color = pwrColor))
    drawText(textLayoutResult = pwr, topLeft = Offset(cw * 0.30f, textY))

    // Nivel (Centro-Derecha)
    val lv = tm.measure(AnnotatedString("NIVEL ${s.level}"), sty.copy(color = HudAccentColor))
    drawText(textLayoutResult = lv, topLeft = Offset(cw * 0.60f, textY))

    // Vidas (Derecha)
    val li = tm.measure(AnnotatedString("VIDAS: ${s.player.lives}"), sty.copy(color = HudTextColor))
    drawText(textLayoutResult = li, topLeft = Offset(cw - li.size.width - 14f, textY))
}

private fun drawMenu(d: DrawScope, tm: androidx.compose.ui.text.TextMeasurer, cw: Float, ch: Float) = with(d) {
    drawRect(BackgroundColor, size = size)
    // Parallax stars in menu too
    val time = System.currentTimeMillis() / 20f
    for (i in 0..60) {
        val size = 1f + (i % 3)
        drawCircle(Color.White.copy(alpha = 0.3f), size,
            Offset(((i * 17 + 3) % cw.toInt()).toFloat(), ((i * 31 + 7 + time * size) % ch.toInt()).toFloat()))
    }
    val t = tm.measure(AnnotatedString("GALAGA"),
        TextStyle(MenuTitleColor, fontSize = 64.sp, fontWeight = FontWeight.Bold, letterSpacing = 8.sp))
    drawText(textLayoutResult = t, topLeft = Offset(cw / 2 - t.size.width / 2, ch * 0.25f))
    val c = tm.measure(AnnotatedString("100+ Niveles • Power-Ups • Evolución de Nave"),
        TextStyle(HudTextColor.copy(alpha = 0.6f), fontSize = 12.sp))
    drawText(textLayoutResult = c, topLeft = Offset(cw / 2 - c.size.width / 2, ch * 0.45f))
}

private fun drawGameOver(d: DrawScope, tm: androidx.compose.ui.text.TextMeasurer, s: GameState.GameOver, cw: Float, ch: Float) = with(d) {
    drawRect(Color.Black.copy(alpha = 0.7f), size = size)
    val t = tm.measure(AnnotatedString("GAME OVER"), TextStyle(Color.Red, fontSize = 52.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp))
    drawText(textLayoutResult = t, topLeft = Offset(cw / 2 - t.size.width / 2, ch * 0.3f))
    val sc = tm.measure(AnnotatedString("PUNTUACION: ${s.score}"), TextStyle(HudTextColor, fontSize = 24.sp))
    drawText(textLayoutResult = sc, topLeft = Offset(cw / 2 - sc.size.width / 2, ch * 0.45f))
    val lv = tm.measure(AnnotatedString("NIVEL ALCANZADO: ${s.level}"), TextStyle(HudAccentColor, fontSize = 18.sp))
    drawText(textLayoutResult = lv, topLeft = Offset(cw / 2 - lv.size.width / 2, ch * 0.52f))
    val r = tm.measure(AnnotatedString("TOCA PARA REINICIAR"), TextStyle(HudTextColor.copy(alpha = 0.7f), fontSize = 16.sp))
    drawText(textLayoutResult = r, topLeft = Offset(cw / 2 - r.size.width / 2, ch * 0.65f))
}

private fun drawLevelIntro(d: DrawScope, tm: androidx.compose.ui.text.TextMeasurer, s: GameState.LevelIntro, cw: Float, ch: Float) = with(d) {
    drawRect(BackgroundColor, size = size)
    val t = tm.measure(AnnotatedString("NIVEL ${s.level}"), TextStyle(HudAccentColor, fontSize = 48.sp, fontWeight = FontWeight.Bold))
    drawText(textLayoutResult = t, topLeft = Offset(cw / 2 - t.size.width / 2, ch * 0.35f))
    val n = tm.measure(AnnotatedString("${(s.remainingTime.toInt() + 1).coerceAtMost(3)}"), TextStyle(HudTextColor, fontSize = 36.sp, fontWeight = FontWeight.Bold))
    drawText(textLayoutResult = n, topLeft = Offset(cw / 2 - n.size.width / 2, ch * 0.5f))
}

private fun drawVictory(d: DrawScope, tm: androidx.compose.ui.text.TextMeasurer, s: GameState.Victory, cw: Float, ch: Float) = with(d) {
    drawRect(Color.Black.copy(alpha = 0.7f), size = size)
    val t = tm.measure(AnnotatedString("VICTORIA"), TextStyle(MenuTitleColor, fontSize = 52.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp))
    drawText(textLayoutResult = t, topLeft = Offset(cw / 2 - t.size.width / 2, ch * 0.3f))
    val sc = tm.measure(AnnotatedString("PUNTUACION FINAL: ${s.score}"), TextStyle(HudTextColor, fontSize = 24.sp))
    drawText(textLayoutResult = sc, topLeft = Offset(cw / 2 - sc.size.width / 2, ch * 0.45f))
    val r = tm.measure(AnnotatedString("TOCA PARA JUGAR DE NUEVO"), TextStyle(HudTextColor.copy(alpha = 0.7f), fontSize = 16.sp))
    drawText(textLayoutResult = r, topLeft = Offset(cw / 2 - r.size.width / 2, ch * 0.6f))
}

private fun drawOverlay(d: DrawScope, tm: androidx.compose.ui.text.TextMeasurer, title: String, sub: String, cw: Float, ch: Float) = with(d) {
    drawRect(Color.Black.copy(alpha = 0.6f), size = size)
    val t = tm.measure(AnnotatedString(title), TextStyle(HudAccentColor, fontSize = 44.sp, fontWeight = FontWeight.Bold))
    drawText(textLayoutResult = t, topLeft = Offset(cw / 2 - t.size.width / 2, ch * 0.4f))
    val s = tm.measure(AnnotatedString(sub), TextStyle(HudTextColor.copy(alpha = 0.7f), fontSize = 16.sp))
    drawText(textLayoutResult = s, topLeft = Offset(cw / 2 - s.size.width / 2, ch * 0.5f))
}
