package com.example.bangerz.ui.gameboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bangerz.ui.player.PlayerViewModel
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GameBoardScreen(
    gameBoardViewModel: GameBoardViewModel,
    playerViewModel: PlayerViewModel
) {
    val uiState by gameBoardViewModel.uiState.collectAsState()
    val playerUiState by playerViewModel.uiState.collectAsState()

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val radius = minOf(maxWidth, maxHeight) / 2 - 48.dp
        val playerCount = uiState.players.size

        uiState.players.forEachIndexed { index, player ->
            val angle = (-90.0 + (360.0 / playerCount) * index) * (Math.PI / 180.0)
            val offsetX = (radius.value * cos(angle)).dp
            val offsetY = (radius.value * sin(angle)).dp

            PlayerAvatar(
                player = player,
                modifier = Modifier.offset(x = offsetX, y = offsetY)
            )
        }

        CenterPlayButton(
            isPlaying = playerUiState.isPlaying,
            onClick = {
                if (playerUiState.currentSongId != null) {
                    // Ya hay una canción activa: solo pausa/reanuda
                    playerViewModel.togglePlayPause()
                } else {
                    // No hay canción activa: elige una nueva al azar
                    val song = gameBoardViewModel.playRandomSong()
                    song?.let {
                        playerViewModel.playSong(it.id, it.audioUrl)
                    }
                }
            }
        )
    }
}

@Composable
private fun PlayerAvatar(
    player: PlayerSlot,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = player.name,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun CenterPlayButton(
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(96.dp)
            .clip(CircleShape)
            .background(Color.Black)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = if (isPlaying) "Pausar" else "Reproducir canción aleatoria",
            tint = Color.White,
            modifier = Modifier.size(40.dp)
        )
    }
}