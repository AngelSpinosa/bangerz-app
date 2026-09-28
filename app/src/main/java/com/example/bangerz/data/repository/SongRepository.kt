package com.example.bangerz.data.repository

import com.example.bangerz.data.local.AppDatabase
import com.example.bangerz.data.local.SongEntity
import com.example.bangerz.data.remote.ApiService
import com.example.bangerz.data.remote.SongDto
import kotlinx.coroutines.flow.Flow

class SongRepository(
    private val apiService: ApiService,
    private val database: AppDatabase
) {
    val songs: Flow<List<SongEntity>> = database.songDao().getAllSongs()

    suspend fun refreshSongs(): Result<Unit> {
        return try {
            val remoteSongs = apiService.getSongs()
            val entities = remoteSongs.map { it.toEntity() }
            database.songDao().clearAll()      // ← borra lo viejo primero
            database.songDao().insertAll(entities)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun SongDto.toEntity(): SongEntity {
        return SongEntity(
            id = id,
            title = title,
            artist = artist,
            year = year,
            duration = duration,
            genre = genre,
            coverUrl = coverUrl,
            audioUrl = audioUrl
        )
    }
}