package com.example.bangerz.ui.gameboard

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bangerz.R
import com.example.bangerz.ui.components.BangerzBackground
import com.example.bangerz.ui.player.PlayerViewModel
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private const val INITIAL_TOKENS = 2 // TODO: leerlo del estado de cada jugador

@Composable
fun GameBoardScreen(
    gameBoardViewModel: GameBoardViewModel,
    playerViewModel: PlayerViewModel
) {
    val uiState by gameBoardViewModel.uiState.collectAsState()
    val playerUiState by playerViewModel.uiState.collectAsState()

    // El tablero es horizontal: rotamos al entrar y volvemos a vertical al salir
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val activity = context as? Activity
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    BangerzBackground(background = R.drawable.bg_board) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Elipse: aprovecha el ancho de la pantalla horizontal
            val radiusX = maxWidth / 2 - 56.dp
            val radiusY = maxHeight / 2 - 56.dp
            val playerCount = uiState.players.size

            uiState.players.forEachIndexed { index, player ->
                val angle = (-90.0 + (360.0 / playerCount) * index) * (Math.PI / 180.0)
                val dx = cos(angle)
                val dy = sin(angle)

                PlayerWithTokens(
                    player = player,
                    tokens = INITIAL_TOKENS,
                    dx = dx,
                    dy = dy,
                    modifier = Modifier.offset(
                        x = (radiusX.value * dx).dp,
                        y = (radiusY.value * dy).dp
                    )
                )
            }

            CenterPlayButton(
                isPlaying = playerUiState.isPlaying,
                onClick = {
                    if (playerUiState.currentSongId != null) {
                        playerViewModel.togglePlayPause()
                    } else {
                        val song = gameBoardViewModel.playRandomSong()
                        song?.let { playerViewModel.playSong(it.id, it.audioUrl) }
                    }
                }
            )
        }
    }
}

/** Avatar + fichas, con las fichas siempre del lado que mira al centro. */
@Composable
private fun PlayerWithTokens(
    player: PlayerSlot,
    tokens: Int,
    dx: Double,
    dy: Double,
    modifier: Modifier = Modifier
) {
    val avatar: @Composable () -> Unit = { PlayerAvatar(player) }
    val tokenRow: @Composable () -> Unit = {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(tokens) { TokenChip() }
        }
    }
    val tokenColumn: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(tokens) { TokenChip() }
        }
    }

    if (abs(dy) >= abs(dx)) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (dy < 0) { avatar(); tokenRow() } else { tokenRow(); avatar() }
        }
    } else {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (dx < 0) { avatar(); tokenColumn() } else { tokenColumn(); avatar() }
        }
    }
}

@Composable
private fun PlayerAvatar(player: PlayerSlot) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = player.name,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = Color.White
        )
    }
}

@Composable
private fun TokenChip() {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(Color(0xFFE50914))
    )
}

@Composable
private fun CenterPlayButton(
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0A0A0A))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = if (isPlaying) "Pausar" else "Reproducir canción aleatoria",
            tint = Color.White,
            modifier = Modifier.size(36.dp)
        )
    }
}