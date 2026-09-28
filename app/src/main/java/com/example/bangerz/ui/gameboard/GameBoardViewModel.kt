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
    val name: String
)

data class GameBoardUiState(
    val players: List<PlayerSlot> = emptyList(),
    val availableSongs: List<SongEntity> = emptyList(),
    val currentSong: SongEntity? = null
)

class GameBoardViewModel(
    private val repository: SongRepository,
    playerCount: Int
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        GameBoardUiState(
            players = (1..playerCount).map { PlayerSlot(id = it, name = "J$it") }
        )
    )
    val uiState: StateFlow<GameBoardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.songs.collect { songs ->
                _uiState.value = _uiState.value.copy(availableSongs = songs)
            }
        }
        refreshSongs()   // ← nueva línea
    }

    fun refreshSongs() {
        viewModelScope.launch {
            repository.refreshSongs()
        }
    }

    fun playRandomSong(): SongEntity? {
        val pool = _uiState.value.availableSongs
        if (pool.isEmpty()) return null

        val lastSongId = _uiState.value.currentSong?.id
        val candidates = if (pool.size > 1) {
            pool.filter { it.id != lastSongId }
        } else {
            pool
        }

        val song = candidates.random(kotlin.random.Random(System.nanoTime()))
        _uiState.value = _uiState.value.copy(currentSong = song)
        return song
    }
}