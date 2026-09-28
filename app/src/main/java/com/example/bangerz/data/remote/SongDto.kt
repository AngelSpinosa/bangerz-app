package com.example.bangerz.data.remote

import com.google.gson.annotations.SerializedName

data class SongDto(
    val id: Int,
    val title: String,
    val artist: String,
    val year: Int,
    val duration: Int,          // segundos
    val genre: String,
    @SerializedName("coverUrl")
    val coverUrl: String?,      // puede venir null
    @SerializedName("audioUrl")
    val audioUrl: String
)