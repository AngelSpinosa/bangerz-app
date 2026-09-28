package com.example.bangerz.data.remote

import retrofit2.http.GET
import retrofit2.http.Path

interface ApiService {
    @GET("songs")
    suspend fun getSongs(): List<SongDto>

    @GET("songs/{id}")
    suspend fun getSongById(@Path("id") id: Int): SongDto

    @GET("health")
    suspend fun healthCheck(): Map<String, Any>
}