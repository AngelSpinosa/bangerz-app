package com.example.bangerz.ui.gameboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.bangerz.data.repository.SongRepository

class GameBoardViewModelFactory(
    private val repository: SongRepository,
    private val playerCount: Int
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GameBoardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GameBoardViewModel(repository, playerCount) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}