package com.example.bangerz.ui.gameboard

import com.example.bangerz.data.local.SongEntity

data class SongCard(
    val songId: Int,
    val title: String,
    val artist: String,
    val year: Int
)

fun SongEntity.toCard() = SongCard(
    songId = id,
    title = title,
    artist = artist,
    year = year
)