package com.example.bangerz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.*
import com.example.bangerz.data.local.AppDatabase
import com.example.bangerz.data.remote.RetrofitClient
import com.example.bangerz.data.repository.SongRepository
import com.example.bangerz.ui.gameboard.GameBoardScreen
import com.example.bangerz.ui.gameboard.GameBoardViewModel
import com.example.bangerz.ui.gameboard.GameBoardViewModelFactory
import com.example.bangerz.ui.menu.MainMenuScreen
import com.example.bangerz.ui.menu.PlayerCountScreen
import com.example.bangerz.ui.player.PlayerViewModel
import com.example.bangerz.ui.theme.BangerzTheme
import androidx.lifecycle.viewmodel.compose.viewModel

private sealed class Screen {
    data object MainMenu : Screen()
    data object PlayerCount : Screen()
    data class GameBoard(val playerCount: Int) : Screen()
}

class MainActivity : ComponentActivity() {

    private val repository by lazy {
        SongRepository(
            apiService = RetrofitClient.apiService,
            database = AppDatabase.getInstance(applicationContext)
        )
    }

    private val playerViewModel: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BangerzTheme {
                var currentScreen by remember { mutableStateOf<Screen>(Screen.MainMenu) }

                when (val screen = currentScreen) {
                    is Screen.MainMenu -> MainMenuScreen(
                        onPlayClick = { currentScreen = Screen.PlayerCount }
                    )
                    is Screen.PlayerCount -> PlayerCountScreen(
                        onAccept = { count ->
                            currentScreen = Screen.GameBoard(count)
                        }
                    )
                    is Screen.GameBoard -> {
                        val gameBoardViewModel: GameBoardViewModel = viewModel(
                            factory = GameBoardViewModelFactory(repository, screen.playerCount)
                        )
                        GameBoardScreen(
                            gameBoardViewModel = gameBoardViewModel,
                            playerViewModel = playerViewModel
                        )
                    }
                }
            }
        }
    }
}