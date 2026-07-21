package com.galaga.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
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
                            is GameState.Menu -> viewModel.startGame()
                            is GameState.Paused -> viewModel.resume()
                            is GameState.GameOver -> viewModel.startGame()
                            is GameState.Victory -> viewModel.startGame()
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
                is GameState.Playing -> drawPlaying(this, textMeasurer, gs, cw, ch, viewModel)
                is GameState.Paused -> {
                    drawPlaying(this, textMeasurer, gs.playingState, cw, ch, viewModel)
                    drawOverlay(this, textMeasurer, "PAUSA", "TOCA PARA CONTINUAR", cw, ch)
                }
                is GameState.GameOver -> drawGameOver(this, textMeasurer, gs, cw, ch)
                is GameState.LevelIntro -> drawLevelIntro(this, textMeasurer, gs, cw, ch)
                is GameState.Victory -> drawVictory(this, textMeasurer, gs, cw, ch)
            }
        }
    }
}

// ── Drawing helpers ─────────────────────────────────────────────

private fun drawPlaying(
    d: DrawScope, tm: androidx.compose.ui.text.TextMeasurer,
    s: GameState.Playing, cw: Float, ch: Float, vm: GameViewModel
) = with(d) {
    drawRect(BackgroundColor, size = size)
    for (star in vm.stars) drawCircle(Color.White.copy(alpha = star.alpha), star.size,
        Offset(star.x % cw, (star.y + s.frameCount * 0.5f) % ch))

    for (b in s.playerBullets) {
        drawRect(PlayerBulletColor, Offset(b.x - b.width / 2, b.y - b.height / 2), Size(b.width, b.height))
        drawRect(Color.White.copy(alpha = 0.6f), Offset(b.x - b.width / 4, b.y - b.height / 2), Size(b.width / 2, b.height))
    }
    for (b in s.enemyBullets) {
        drawCircle(EnemyBulletColor, b.width / 2, Offset(b.x, b.y))
        drawCircle(Color.Yellow.copy(alpha = 0.5f), b.width / 4, Offset(b.x, b.y))
    }
    for (ex in s.explosions) {
        drawCircle(Color(0xFFFFAA00).copy(alpha = ex.alpha * 0.6f), ex.radius * 1.5f, Offset(ex.x, ex.y))
        drawCircle(Color(0xFFFFFF44).copy(alpha = ex.alpha * 0.4f), ex.radius * 0.8f, Offset(ex.x, ex.y))
    }
    for (e in s.enemies) {
        if (e.state is EnemyState.Destroyed) {
            val st = e.state as EnemyState.Destroyed; val p = 1f - st.timer / 0.4f
            drawCircle(Color(0xFFFFAA00).copy(alpha = 1f - p), e.width / 2 + p * 25f, Offset(e.x, e.y))
            continue
        }
        drawEnemy(d, e)
    }
    val show = !s.player.isInvulnerable || (s.frameCount / 6L) % 2L == 0L
    if (show) drawShip(d, s.player, s.frameCount)
    drawHud(d, tm, s, cw)
}

private fun drawEnemy(d: DrawScope, e: Enemy) = with(d) {
    val c = when (e.type) { EnemyType.ZAKO -> EnemyZakoColor; EnemyType.GOEI -> EnemyGoeiColor; EnemyType.BOSS -> EnemyBossColor }
    val ex = e.x; val ey = e.y; val ew = e.width; val eh = e.height
    drawRoundRect(c, Offset(ex - ew / 2, ey - eh / 2), Size(ew, eh), CornerRadius(8f, 8f))
    drawRoundRect(c.copy(alpha = 0.4f), Offset(ex - ew / 4, ey - eh / 4), Size(ew / 2, eh / 2), CornerRadius(4f, 4f))
    when (e.type) {
        EnemyType.ZAKO -> {
            drawCircle(c.copy(alpha = 0.7f), ew * 0.3f, Offset(ex - ew * 0.6f, ey))
            drawCircle(c.copy(alpha = 0.7f), ew * 0.3f, Offset(ex + ew * 0.6f, ey))
            drawCircle(Color.White, 4f, Offset(ex - 6f, ey - 4f)); drawCircle(Color.White, 4f, Offset(ex + 6f, ey - 4f))
        }
        EnemyType.GOEI -> {
            val w1 = Path().apply { moveTo(ex - ew / 2, ey - eh / 4); lineTo(ex - ew * 0.8f, ey - eh / 2); lineTo(ex - ew * 0.6f, ey); close() }
            val w2 = Path().apply { moveTo(ex + ew / 2, ey - eh / 4); lineTo(ex + ew * 0.8f, ey - eh / 2); lineTo(ex + ew * 0.6f, ey); close() }
            drawPath(w1, c.copy(alpha = 0.6f)); drawPath(w2, c.copy(alpha = 0.6f))
            drawCircle(Color.White, 5f, Offset(ex - 7f, ey - 3f)); drawCircle(Color.White, 5f, Offset(ex + 7f, ey - 3f))
            drawCircle(Color.Black, 2.5f, Offset(ex - 6f, ey - 3f)); drawCircle(Color.Black, 2.5f, Offset(ex + 6f, ey - 3f))
        }
        EnemyType.BOSS -> {
            drawRoundRect(c.copy(alpha = 0.8f), Offset(ex - ew * 0.7f, ey - eh * 0.4f), Size(ew * 1.4f, eh * 0.6f), CornerRadius(12f, 12f))
            for (i in 0 until 3) drawCircle(c, 8f, Offset(ex - 20f + i * 20f, ey - eh / 2 - 8f))
            drawCircle(Color.White, 7f, Offset(ex - 10f, ey - 5f)); drawCircle(Color.White, 7f, Offset(ex + 10f, ey - 5f))
            drawCircle(Color.Red, 4f, Offset(ex - 10f, ey - 5f)); drawCircle(Color.Red, 4f, Offset(ex + 10f, ey - 5f))
        }
    }
}

private fun drawShip(d: DrawScope, p: com.galaga.game.model.Player, frameCount: Long = 0L) = with(d) {
    val pw = p.width; val ph = p.height
    val cosA = cos(p.movementAngle)
    val sinA = sin(p.movementAngle)
    fun rot(lx: Float, ly: Float) = Offset(
        p.x + lx * cosA - ly * sinA,
        p.y + lx * sinA + ly * cosA
    )

    val fuselage = listOf(
        0f to -ph * 0.50f, pw * 0.12f to -ph * 0.38f, pw * 0.14f to -ph * 0.30f,
        pw * 0.10f to -ph * 0.20f, pw * 0.08f to -ph * 0.12f, pw * 0.11f to -ph * 0.02f,
        pw * 0.18f to ph * 0.04f
    )
    val wingRight = listOf(
        pw * 0.40f to ph * 0.06f, pw * 0.58f to ph * 0.10f,
        pw * 0.55f to ph * 0.14f, pw * 0.32f to ph * 0.18f
    )
    val engineRight = listOf(
        pw * 0.20f to ph * 0.26f, pw * 0.17f to ph * 0.38f,
        pw * 0.13f to ph * 0.44f, pw * 0.07f to ph * 0.48f
    )
    val centerBottom = listOf(0f to ph * 0.46f)
    val engineLeft = listOf(
        -pw * 0.07f to ph * 0.48f, -pw * 0.13f to ph * 0.44f,
        -pw * 0.17f to ph * 0.38f, -pw * 0.20f to ph * 0.26f
    )
    val wingLeft = listOf(
        -pw * 0.32f to ph * 0.18f, -pw * 0.55f to ph * 0.14f,
        -pw * 0.58f to ph * 0.10f, -pw * 0.40f to ph * 0.06f
    )
    val fuselageLeft = listOf(
        -pw * 0.18f to ph * 0.04f, -pw * 0.11f to -ph * 0.02f,
        -pw * 0.08f to -ph * 0.12f, -pw * 0.10f to -ph * 0.20f,
        -pw * 0.14f to -ph * 0.30f, -pw * 0.12f to -ph * 0.38f
    )

    val body = Path().apply {
        val all = fuselage + wingRight + engineRight + centerBottom + engineLeft + wingLeft + fuselageLeft
        all.forEachIndexed { i, (lx, ly) ->
            val o = rot(lx, ly)
            if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y)
        }
        close()
    }
    drawPath(body, color = PlayerColor)
    drawPath(body, color = Color.White.copy(alpha = 0.18f), style = Stroke(width = 1.2f))

    val cockpit = rot(0f, -ph * 0.18f)
    drawCircle(Color.Cyan.copy(alpha = 0.55f), pw * 0.09f, cockpit)
    drawCircle(Color.White.copy(alpha = 0.15f), pw * 0.06f, cockpit)

    val nose = rot(0f, -ph * 0.45f)
    drawCircle(Color.White.copy(alpha = 0.12f), pw * 0.04f, nose)

    val exhaustR = rot(pw * 0.10f, ph * 0.44f)
    val exhaustL = rot(-pw * 0.10f, ph * 0.44f)
    val t = frameCount * 0.15f
    val glowR = PlayerColor.copy(alpha = 0.5f + 0.3f * sin(t))
    val glowL = PlayerColor.copy(alpha = 0.5f + 0.3f * cos(t + 1f))
    drawCircle(glowR, pw * 0.10f, exhaustR)
    drawCircle(glowL, pw * 0.10f, exhaustL)
    drawCircle(Color.White.copy(alpha = 0.4f), pw * 0.05f, exhaustR)
    drawCircle(Color.White.copy(alpha = 0.4f), pw * 0.05f, exhaustL)
}

private fun drawHud(d: DrawScope, tm: androidx.compose.ui.text.TextMeasurer, s: GameState.Playing, cw: Float) = with(d) {
    val sty = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold)
    val sc = tm.measure(AnnotatedString("PUNTAJE: ${s.score}"), sty.copy(color = HudTextColor))
    drawText(textLayoutResult = sc, topLeft = Offset(12f, 8f))
    val lv = tm.measure(AnnotatedString("NIVEL ${s.level}"), sty.copy(color = HudAccentColor))
    drawText(textLayoutResult = lv, topLeft = Offset(cw / 2 - lv.size.width / 2, 8f))
    val li = tm.measure(AnnotatedString("VIDAS: ${s.player.lives}"), sty.copy(color = HudTextColor))
    drawText(textLayoutResult = li, topLeft = Offset(cw - li.size.width - 12f, 8f))
}

private fun drawMenu(d: DrawScope, tm: androidx.compose.ui.text.TextMeasurer, cw: Float, ch: Float) = with(d) {
    drawRect(BackgroundColor, size = size)
    for (i in 0..60) drawCircle(Color.White.copy(alpha = 0.3f), 1f,
        Offset(((i * 17 + 3) % cw.toInt()).toFloat(), ((i * 31 + 7) % ch.toInt()).toFloat()))
    val t = tm.measure(AnnotatedString("GALAGA"),
        TextStyle(MenuTitleColor, fontSize = 64.sp, fontWeight = FontWeight.Bold, letterSpacing = 8.sp))
    drawText(textLayoutResult = t, topLeft = Offset(cw / 2 - t.size.width / 2, ch * 0.25f))
    val s = tm.measure(AnnotatedString("TOCA PARA COMENZAR"),
        TextStyle(HudTextColor.copy(alpha = 0.8f), fontSize = 18.sp, letterSpacing = 2.sp))
    drawText(textLayoutResult = s, topLeft = Offset(cw / 2 - s.size.width / 2, ch * 0.55f))
    val c = tm.measure(AnnotatedString("Kotlin + Jetpack Compose"),
        TextStyle(HudTextColor.copy(alpha = 0.4f), fontSize = 12.sp))
    drawText(textLayoutResult = c, topLeft = Offset(cw / 2 - c.size.width / 2, ch * 0.65f))
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
