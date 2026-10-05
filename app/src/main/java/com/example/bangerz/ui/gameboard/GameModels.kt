package com.example.bangerz.ui.gameboard

import com.example.bangerz.data.local.SongEntity

data class SongCard(
    val songId: Int,
    val title: String,
    val artist: String,
    val year: Int
)

const val MAX_TOKENS = 5
const val WIN_CARDS = 10   // tarjetas en la línea de tiempo para ganar (cuenta la inicial)
const val BUY_COST = 3   // fichas BANG para quedarte la canción sin adivinar
data class TurnResolution(
    val song: SongCard,
    val winnerId: Int?,      // null = nadie acertó
    val message: String,
    val tokenAwarded: Boolean = false,   // el jugador en turno ya reclamó su ficha
    val gameWinnerId: Int? = null        // != null = la partida terminó
)

fun SongEntity.toCard() = SongCard(
    songId = id,
    title = title,
    artist = artist,
    year = year
)