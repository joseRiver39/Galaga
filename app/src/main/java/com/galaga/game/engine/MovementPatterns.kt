package com.galaga.game.engine

import androidx.compose.ui.geometry.Offset
import kotlin.math.cos
import kotlin.math.sin

/**
 * CURVA DE ENTRADA SINUSOIDAL
 *
 * Los enemigos entran desde arriba de la pantalla describiendo una
 * trayectoria sinusoidal que se amortigua hasta alcanzar su posición
 * final en la formación.
 *
 * Matemática:
 *   x(t) = formationX + A * (1 - t) * sin(2π * f * t)
 *   y(t) = -offset + (formationY + offset) * t
 *
 * donde:
 *   t ∈ [0,1] = progreso normalizado de la entrada
 *   A        = amplitud máxima de oscilación horizontal
 *   f        = frecuencia de oscilación (número de ciclos durante la entrada)
 *   (1 - t)  = factor de amortiguamiento: la oscilación se reduce
 *              linealmente hasta cero al llegar a la formación
 *   offset   = distancia vertical sobre el borde superior (start y)
 *
 * El efecto visual es de un "serpenteo" que se estabiliza al llegar
 * a la posición de formación, similar al movimiento de entrada en Galaga original.
 */
fun entrancePosition(
    progress: Float,
    formationX: Float,
    formationY: Float,
    canvasWidth: Float = 1080f,
    canvasHeight: Float = 2000f,
    frequency: Float = 2.5f
): Offset {
    val entranceAmplitude = canvasWidth * 0.13f
    val t = progress.coerceIn(0f, 1f)
    val dampening = 1f - t
    val startYOffset = canvasHeight * 0.04f
    val x = formationX + entranceAmplitude * dampening * sin(2f * kotlin.math.PI.toFloat() * frequency * t)
    val y = -startYOffset + (formationY + startYOffset) * t
    return Offset(x, y)
}

/**
 * OSCILACIÓN EN FORMACIÓN (BOBBING)
 *
 * Mientras están en formación, los enemigos se mecen suavemente
 * usando movimiento armónico simple independiente en cada eje.
 *
 * Matemática:
 *   dx = A_x * sin(ω_x * frame + φ_x)
 *   dy = A_y * cos(ω_y * frame + φ_y)
 *
 * donde:
 *   A_x, A_y = amplitudes de oscilación (píxeles)
 *   ω_x, ω_y = frecuencias angulares (radianes/frame)
 *   φ_x, φ_y = desfases basados en columna/fila para crear
 *              un efecto de "ola" en la formación
 *
 * Los desfases por columna y fila generan un efecto visual
 * orgánico donde los enemigos no se mueven todos al unísono.
 */
fun formationBob(
    frameCount: Long,
    col: Int,
    row: Int,
    amplitudeX: Float = 8f,
    amplitudeY: Float = 4f,
    speedX: Float = 0.003f,
    speedY: Float = 0.002f
): Offset {
    val phaseX = col * 0.8f + row * 0.3f
    val phaseY = col * 0.5f + row * 0.7f
    val dx = amplitudeX * sin(speedX * frameCount + phaseX)
    val dy = amplitudeY * cos(speedY * frameCount + phaseY)
    return Offset(dx, dy)
}

/**
 * PICADA RECTA (Zako - enemigo básico)
 *
 * Trayectoria lineal desde la formación hasta la posición objetivo
 * cerca del jugador.
 *
 * Matemática:
 *   x(t) = lerp(x_inicio, x_objetivo, t)
 *   y(t) = lerp(y_inicio, y_objetivo, t)
 *
 * Los Zako siguen una línea recta sin desviación lateral,
 * priorizando llegar rápido a la zona del jugador.
 */
fun straightDive(
    progress: Float,
    startX: Float,
    startY: Float,
    targetX: Float,
    targetY: Float
): Offset {
    val t = progress.coerceIn(0f, 1f)
    return Offset(lerp(startX, targetX, t), lerp(startY, targetY, t))
}

/**
 * PICADA EN CURVA S (Goei - enemigo intermedio)
 *
 * Trayectoria con oscilación lateral modulada que describe una
 * curva sinusoidal (forma de "S") mientras desciende.
 *
 * Matemática:
 *   x(t) = lerp(x_inicio, x_objetivo, t) + A * (1 - t) * sin(2π * f * t)
 *   y(t) = lerp(y_inicio, y_objetivo, t)
 *
 * donde:
 *   A        = 150px (amplitud lateral, mayor que en la entrada)
 *   f        = 1.5 ciclos (frecuencia más baja para curva más amplia)
 *   (1 - t)  = amortiguamiento: la oscilación se reduce al acercarse
 *
 * El Goei además apunta al jugador (targetX = player.x), por lo que
 * la curva S combina movimiento evasivo con puntería precisa.
 */
fun sCurveDive(
    progress: Float,
    startX: Float,
    startY: Float,
    targetX: Float,
    targetY: Float,
    amplitude: Float = 150f,
    frequency: Float = 1.5f
): Offset {
    val t = progress.coerceIn(0f, 1f)
    val modulation = 1f - t
    val x = lerp(startX, targetX, t) + amplitude * modulation * sin(2f * kotlin.math.PI.toFloat() * frequency * t)
    val y = lerp(startY, targetY, t)
    return Offset(x, y)
}

/**
 * PICADA MULTI-SEGMENTO (Boss)
 *
 * Trayectoria compuesta por 3 segmentos:
 *   1. (0% - 30%)  Caída vertical rápida desde la formación
 *   2. (30% - 70%) Barrido horizontal hacia la posición del jugador
 *   3. (70% - 100%) Ascenso de regreso a la formación
 *
 * Matemática: interpolación lineal por tramos (piecewise linear).
 * Cada segmento usa su propia función lerp con el progreso
 * re-mapeado al rango [0,1] del segmento.
 *
 * Este patrón recuerda al movimiento del jefe en el Galaga original,
 * donde desciende, barre la pantalla horizontalmente y regresa.
 */
fun multiSegmentDive(
    progress: Float,
    startX: Float,
    startY: Float,
    playerX: Float,
    canvasHeight: Float,
    sweepDepth: Float = 0f
): Offset {
    val sd = if (sweepDepth > 0f) sweepDepth else canvasHeight * 0.18f
    val t = progress.coerceIn(0f, 1f)
    return when {
        t < 0.3f -> {
            val p = t / 0.3f
            Offset(lerp(startX, startX, p), lerp(startY, startY + sd, p))
        }
        t < 0.7f -> {
            val p = (t - 0.3f) / 0.4f
            Offset(lerp(startX, playerX, p), startY + sd)
        }
        else -> {
            val p = (t - 0.7f) / 0.3f
            Offset(lerp(playerX, startX, p), lerp(startY + sd, startY, p))
        }
    }
}

/**
 * DETECCIÓN DE COLISIÓN (AABB - Axis-Aligned Bounding Box)
 *
 * Dos rectángulos alineados a los ejes colisionan si y solo si
 * se superponen simultáneamente en ambos ejes (X e Y).
 *
 * Matemática (Separating Axis Theorem para AABB):
 *   colisión = NO (a_izquierda > b_derecha O a_derecha < b_izquierda
 *                  O a_arriba > b_abajo O a_abajo < b_arriba)
 *
 * El factor shrink (0.0 - 1.0) reduce el área de colisión en cada
 * eje, haciendo el juego más indulgente. Por ejemplo shrink=0.2
 * reduce el bounding box al 60% del tamaño visual.
 */
fun checkCollision(
    ax: Float, ay: Float, aw: Float, ah: Float,
    bx: Float, by: Float, bw: Float, bh: Float,
    shrinkX: Float = 0.15f,
    shrinkY: Float = 0.15f
): Boolean {
    val aLeft = ax - aw / 2 + aw * shrinkX / 2
    val aRight = ax + aw / 2 - aw * shrinkX / 2
    val aTop = ay - ah / 2 + ah * shrinkY / 2
    val aBottom = ay + ah / 2 - ah * shrinkY / 2
    val bLeft = bx - bw / 2 + bw * shrinkX / 2
    val bRight = bx + bw / 2 - bw * shrinkX / 2
    val bTop = by - bh / 2 + bh * shrinkY / 2
    val bBottom = by + bh / 2 - bh * shrinkY / 2
    return aLeft < bRight && aRight > bLeft && aTop < bBottom && aBottom > bTop
}

fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

internal object GameRandom {
    private var seed = System.nanoTime()
    fun nextFloat(): Float {
        seed = seed * 1103515245L + 12345L
        return ((seed ushr 16).toInt() and 0x7FFF).toFloat() / 32767f
    }
}
