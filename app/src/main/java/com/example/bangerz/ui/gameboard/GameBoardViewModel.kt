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

data class Placement(
    val playerId: Int,
    val slotIndex: Int,          // huecos 0..n de la línea del jugador en turno
    val isTurnPlayer: Boolean
)

data class GameBoardUiState(
    val players: List<PlayerSlot> = emptyList(),
    val currentPlayerIndex: Int = 0,
    val availableSongs: List<SongEntity> = emptyList(),
    val usedSongIds: Set<Int> = emptySet(),
    val currentSong: SongEntity? = null,
    val placements: List<Placement> = emptyList(),   // la cola: el orden ES la prioridad
    val resolution: TurnResolution? = null
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
            placements = emptyList()
        )
        return song
    }

    /** El jugador en turno coloca (o mueve) su tarjeta. Siempre ocupa la posición 0 de la cola. */
    fun placeTurnCard(slot: Int) {
        val state = _uiState.value
        val current = state.players.getOrNull(state.currentPlayerIndex) ?: return
        // Si ya hay oponentes en la cola, la tarjeta queda bloqueada
        if (state.placements.any { !it.isTurnPlayer }) return
        _uiState.value = state.copy(
            placements = listOf(Placement(current.id, slot, isTurnPlayer = true))
        )
    }

    /** Un oponente coloca su ficha; se agrega al final de la cola. */
    fun placeOpponentToken(playerId: Int, slot: Int): Boolean {
        val state = _uiState.value
        val current = state.players.getOrNull(state.currentPlayerIndex) ?: return false
        val player = state.players.firstOrNull { it.id == playerId } ?: return false

        if (state.placements.isEmpty()) return false                       // el turno aún no colocó
        if (playerId == current.id) return false
        if (player.tokens <= 0) return false
        if (state.placements.any { it.playerId == playerId }) return false // ya colocó una
        if (state.placements.any { it.slotIndex == slot }) return false    // hueco ocupado

        _uiState.value = state.copy(
            placements = state.placements + Placement(playerId, slot, isTurnPlayer = false)
        )
        return true
    }

    /** ¿Es `slot` la posición correcta para la canción actual en la línea del turno? */
    private fun isCorrectSlot(slot: Int): Boolean {
        val state = _uiState.value
        val song = state.currentSong ?: return false
        val timeline = state.players.getOrNull(state.currentPlayerIndex)?.timeline ?: return false
        val leftOk = slot == 0 || timeline[slot - 1].year <= song.year
        val rightOk = slot == timeline.size || song.year <= timeline[slot].year
        return leftOk && rightOk
    }

    /** Recorre la cola en orden de prioridad; gana la primera colocación correcta (o ninguna). */
    fun findWinner(): Placement? =
        _uiState.value.placements.firstOrNull { isCorrectSlot(it.slotIndex) }

    /** Resuelve el encuentro: reparte tarjeta y descarta fichas según la cola de prioridad. */
    fun resolveTurn() {
        val state = _uiState.value
        if (state.resolution != null || state.placements.isEmpty()) return
        val song = state.currentSong ?: return
        val turnPlayer = state.players.getOrNull(state.currentPlayerIndex) ?: return

        val card = song.toCard()
        val winner = findWinner()
        val opponentIds = state.placements.filter { !it.isTurnPlayer }.map { it.playerId }.toSet()

        val updatedPlayers = state.players.map { p ->
            var np = p
            // Oponentes que pusieron ficha y no ganaron: pierden una ficha
            if (p.id in opponentIds && p.id != winner?.playerId) {
                np = np.copy(tokens = (np.tokens - 1).coerceAtLeast(0))
            }
            // El ganador (turno u oponente) se queda la tarjeta, ordenada por año
            if (winner != null && p.id == winner.playerId) {
                np = np.copy(timeline = (np.timeline + card).sortedBy { it.year })
            }
            np
        }

        val winnerName = state.players.firstOrNull { it.id == winner?.playerId }?.name
        val message = when {
            winner == null -> "Nadie acertó. Se descarta la canción."
            winner.isTurnPlayer -> "¡$winnerName acertó! Se queda la tarjeta."
            else -> "¡$winnerName le roba la tarjeta a ${turnPlayer.name}!"
        }

        _uiState.value = state.copy(
            players = updatedPlayers,
            placements = emptyList(),
            resolution = TurnResolution(card, winner?.playerId, message)
        )
    }

    /** Pasa al siguiente jugador y deja todo listo para una nueva canción. */
    fun nextTurn() {
        val state = _uiState.value
        if (state.players.isEmpty()) return
        _uiState.value = state.copy(
            currentPlayerIndex = (state.currentPlayerIndex + 1) % state.players.size,
            placements = emptyList(),
            currentSong = null,
            resolution = null
        )
    }
}