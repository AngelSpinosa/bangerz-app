package com.example.bangerz.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey
    val id: Int,
    val title: String,
    val artist: String,
    val year: Int,
    val duration: Int,
    val genre: String,
    val coverUrl: String?,
    val audioUrl: String
)