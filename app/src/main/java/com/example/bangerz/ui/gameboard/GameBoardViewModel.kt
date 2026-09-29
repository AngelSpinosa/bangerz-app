package com.example.bangerz.ui.gameboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bangerz.data.local.SongEntity
import com.example.bangerz.data.repository.SongRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PlayerSlot(
    val id: Int,
    val name: String,
    val tokens: Int = 2,
    val timeline: List<SongCard> = emptyList()   // siempre ordenada por año
)

data class GameBoardUiState(
    val players: List<PlayerSlot> = emptyList(),
    val currentPlayerIndex: Int = 0,
    val availableSongs: List<SongEntity> = emptyList(),
    val usedSongIds: Set<Int> = emptySet(),
    val currentSong: SongEntity? = null,
    val pendingIndex: Int? = null        // posición donde el jugador dejó la tarjeta (null = aún sin colocar)
)

class GameBoardViewModel(
    private val repository: SongRepository,
    private val playerCount: Int
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameBoardUiState())
    val uiState: StateFlow<GameBoardUiState> = _uiState.asStateFlow()

    private var gameStarted = false

    init {
        viewModelScope.launch {
            repository.songs.collect { songs ->
                _uiState.value = _uiState.value.copy(availableSongs = songs)
                startGameIfReady(songs)
            }
        }
        refreshSongs()
    }

    fun refreshSongs() {
        viewModelScope.launch { repository.refreshSongs() }
    }

    /** Se ejecuta una sola vez, cuando Room ya tiene canciones suficientes. */
    private fun startGameIfReady(songs: List<SongEntity>) {
        if (gameStarted || songs.size <= playerCount) return
        gameStarted = true

        // 1. Orden de turnos aleatorio (los nombres J1..Jn se conservan)
        val shuffledPlayers = (1..playerCount)
            .map { PlayerSlot(id = it, name = "J$it") }
            .shuffled()

        // 2. Una canción distinta y aleatoria por jugador
        val initialSongs = songs.shuffled().take(playerCount)

        val playersWithCards = shuffledPlayers.mapIndexed { i, player ->
            player.copy(timeline = listOf(initialSongs[i].toCard()))
        }

        _uiState.value = _uiState.value.copy(
            players = playersWithCards,
            currentPlayerIndex = 0,
            usedSongIds = initialSongs.map { it.id }.toSet()
        )
    }

    fun playRandomSong(): SongEntity? {
        val state = _uiState.value
        val song = state.availableSongs
            .filter { it.id !in state.usedSongIds }   // excluye repartidas y ya jugadas
            .randomOrNull() ?: return null

        _uiState.value = state.copy(
            currentSong = song,
            usedSongIds = state.usedSongIds + song.id,
            pendingIndex = null
        )
        return song
    }

    fun placePendingCard(index: Int) {
        _uiState.value = _uiState.value.copy(pendingIndex = index)
    }
}