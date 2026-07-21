package com.galaga.game

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.galaga.game.ui.GameScreen
import com.galaga.game.ui.theme.GalagaTheme
import com.galaga.game.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()
        setContent {
            GalagaTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    val viewModel: GameViewModel = viewModel()
                    GameScreen(viewModel = viewModel)
                }
            }
        }
    }
}
