# Galaga — Jetpack Compose

Versión del clásico arcade Galaga desarrollada íntegramente en **Kotlin** con **Jetpack Compose** para Android. Sin librerías externas de juegos; todo el renderizado, game loop y detección de colisiones está implementado sobre las APIs estándar de Compose.

## Tecnologías

| Capa | Tecnología |
|------|-----------|
| Lenguaje | Kotlin 1.9 |
| UI | Jetpack Compose (Canvas, BoxWithConstraints, pointerInput) |
| Arquitectura | MVVM (ViewModel + StateFlow vía mutableStateOf) |
| Game loop | LaunchedEffect + withFrameNanos (aprox. 60 FPS) |
| Build | Gradle 8.2 / AGP 8.2 / Compose BOM 2024.02 |
| API mínima | Android 7.0 (API 24) |

## Arquitectura del proyecto

```
app/src/main/java/com/galaga/game/
├── MainActivity.kt           # Entry point, edge-to-edge, pantalla completa
├── model/
│   ├── GameState.kt          # Sealed class con Menu, Playing, Paused, GameOver, LevelIntro, Victory
│   ├── Player.kt             # Data class: posición, vidas, cooldown, invulnerabilidad
│   ├── Enemy.kt              # Data class: tipos (Zako, Goei, Boss), estados, salud
│   └── Bullet.kt             # Data class: posición, velocidad, tipo player/enemy
├── engine/
│   ├── GameEngine.kt         # Lógica del juego: formación, picadas, colisiones, oleadas
│   └── MovementPatterns.kt   # Curvas de movimiento (senoidales, S-curve, multi-segmento)
├── viewmodel/
│   └── GameViewModel.kt      # Estado del juego, entrada del usuario, loop de frames
└── ui/
    ├── GameScreen.kt         # Composable principal: Canvas + gestos + HUD + overlays
    └── theme/
        ├── Color.kt          # Paleta del juego
        └── Theme.kt          # Tema Material3 base
```

## Game Loop

El loop principal corre en un `LaunchedEffect` usando `withFrameNanos` (~60 fps). Cada frame:

1. **ViewModel** calcula el deltaTime
2. **GameEngine.update()** recibe el estado actual y devuelve un nuevo estado inmutable
3. **Canvas** se redibuja automáticamente al detectar el cambio de estado

El Canvas usa `onSizeChanged` para reportar sus dimensiones reales al engine, evitando coordenadas fijas.

## Mecánicas implementadas

- **Jugador**: movimiento libre 4 direcciones (drag), auto-fire cada 250 ms, 3 vidas, invulnerabilidad tras impacto. Nave con silueta detallada (fuselaje central, alas en V, cabina, motor) que rota según la dirección del movimiento
- **Enemigos**: entran con curva senoidal amortiguada, oscilan en formación, realizan picadas (recta Zako, curva-S Goei, multi-segmento Boss)
- **Oleadas**: 8 niveles con progresión de dificultad (más filas, más agresividad, Boss cada 3 niveles)
- **Colisiones**: AABB con factor shrink para hacer el juego más indulgente
- **Estados**: Menú → Nivel 1 → Jugando → Pausa → Game Over / Victoria

## Cómo compilar

1. Abrir el directorio raíz en **Android Studio**
2. Sincronizar Gradle
3. Ejecutar en dispositivo o emulador (API 24+)

```
./gradlew assembleDebug
```
