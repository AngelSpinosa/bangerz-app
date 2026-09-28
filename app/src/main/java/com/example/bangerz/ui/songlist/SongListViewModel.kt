package com.example.bangerz.ui.songlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bangerz.data.local.SongEntity
import com.example.bangerz.data.repository.SongRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class SongListUiState(
    val songs: List<SongEntity> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class SongListViewModel(
    private val repository: SongRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SongListUiState> = combine(
        repository.songs,
        _isLoading,
        _errorMessage
    ) { songs, isLoading, error ->
        SongListUiState(songs = songs, isLoading = isLoading, errorMessage = error)
    }.let { flow ->
        val state = MutableStateFlow(SongListUiState())
        viewModelScope.launch {
            flow.collect { state.value = it }
        }
        state
    }

    init {
        loadSongs()
    }

    fun loadSongs() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            repository.refreshSongs()
                .onFailure { e -> _errorMessage.value = e.message ?: "Error de conexión" }
            _isLoading.value = false
        }
    }
}