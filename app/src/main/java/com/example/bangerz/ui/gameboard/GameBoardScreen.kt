package com.example.bangerz.ui.gameboard

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.bangerz.R
import com.example.bangerz.ui.components.BangerzBackground
import com.example.bangerz.ui.components.CardBackView
import com.example.bangerz.ui.components.SongCardView
import com.example.bangerz.ui.player.PlayerViewModel
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private val CARD_SIZE = 52.dp
private val SLOT_WIDTH = 14.dp

@Composable
fun GameBoardScreen(
    gameBoardViewModel: GameBoardViewModel,
    playerViewModel: PlayerViewModel
) {
    val uiState by gameBoardViewModel.uiState.collectAsState()
    val playerUiState by playerViewModel.uiState.collectAsState()

    val context = LocalContext.current
    DisposableEffect(Unit) {
        val activity = context as? Activity
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    // --- Estado del arrastre (posiciones en coordenadas de pantalla) ---
    var dragPointer by remember { mutableStateOf<Offset?>(null) }
    var timelineBounds by remember { mutableStateOf<Rect?>(null) }
    val cardCenters = remember { mutableStateMapOf<Int, Float>() }

    val currentPlayer = uiState.players.getOrNull(uiState.currentPlayerIndex)

    /** Índice de hueco (0..n) sobre el que está el dedo, o null si está fuera de la línea. */
    fun slotAt(pointer: Offset): Int? {
        val bounds = timelineBounds ?: return null
        val timeline = currentPlayer?.timeline ?: return null
        if (!bounds.inflate(60f).contains(pointer)) return null
        return timeline.count { (cardCenters[it.songId] ?: Float.MAX_VALUE) < pointer.x }
    }

    val hoverSlot = dragPointer?.let { slotAt(it) }
    val onDragMove: (Offset) -> Unit = { dragPointer = it }
    val onDragEnd: (Offset?) -> Unit = { pointer ->
        pointer?.let { slotAt(it) }?.let { gameBoardViewModel.placePendingCard(it) }
        dragPointer = null
    }

    BangerzBackground(background = R.drawable.bg_board) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val radiusX = maxWidth / 2 - 56.dp
            val radiusY = maxHeight / 2 - 56.dp
            val playerCount = uiState.players.size

            uiState.players.forEachIndexed { index, player ->
                val isCurrent = index == uiState.currentPlayerIndex
                // El jugador en turno siempre se sienta abajo (90°), el resto en sentido horario
                val seat = (index - uiState.currentPlayerIndex + playerCount) % playerCount
                val angleDeg = 90.0 + (360.0 / playerCount) * seat
                val angle = Math.toRadians(angleDeg)
                val dx = cos(angle)
                val dy = sin(angle)
                val color = playerColor(player.id)

                val confirm: @Composable () -> Unit = {
                    ConfirmButton(
                        enabled = uiState.pendingIndex != null,
                        onClick = { /* TODO: siguiente paso (reacciones de otros jugadores + verificación) */ }
                    )
                }

                PlayerWithTokens(
                    player = player,
                    color = color,
                    dx = dx,
                    dy = dy,
                    avatarExtra = if (isCurrent) confirm else null,
                    modifier = Modifier.offset(
                        x = (radiusX.value * dx).dp,
                        y = (radiusY.value * dy).dp
                    )
                )

                val timelineModifier = Modifier.offset(
                    x = (radiusX.value * dx * 0.67).dp,
                    y = (radiusY.value * dy * 0.55).dp
                )

                if (isCurrent) {
                    CurrentTimeline(
                        cards = player.timeline,
                        pendingIndex = uiState.pendingIndex,
                        hoverSlot = hoverSlot,
                        color = color,
                        onBounds = { timelineBounds = it },
                        onCardCenter = { id, x -> cardCenters[id] = x },
                        pendingCard = {
                            DraggableBackCard(onDragMove = onDragMove, onDragEnd = onDragEnd)
                        },
                        modifier = timelineModifier
                    )
                } else {
                    Row(
                        modifier = timelineModifier.rotate((angleDeg - 90).toFloat()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        player.timeline.forEach { SongCardView(card = it, size = CARD_SIZE) }
                    }
                }
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

            // Tarjeta volteada junto al botón central, lista para arrastrar
            if (uiState.currentSong != null && uiState.pendingIndex == null) {
                DraggableBackCard(
                    onDragMove = onDragMove,
                    onDragEnd = onDragEnd,
                    modifier = Modifier.offset(x = 76.dp)
                )
            }
        }
    }
}

/** Línea de tiempo del jugador en turno: huecos fijos entre tarjetas + tarjeta pendiente. */
@Composable
private fun CurrentTimeline(
    cards: List<SongCard>,
    pendingIndex: Int?,
    hoverSlot: Int?,
    color: Color,
    onBounds: (Rect) -> Unit,
    onCardCenter: (Int, Float) -> Unit,
    pendingCard: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.onGloballyPositioned { onBounds(it.boundsInRoot()) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0..cards.size) {
            Box(
                modifier = Modifier.width(SLOT_WIDTH).height(CARD_SIZE),
                contentAlignment = Alignment.Center
            ) {
                if (hoverSlot == i) {
                    Box(
                        Modifier
                            .width(4.dp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(2.dp))
                            .background(color)
                    )
                }
            }
            if (pendingIndex == i) pendingCard()
            if (i < cards.size) {
                val card = cards[i]
                Box(
                    Modifier.onGloballyPositioned {
                        onCardCenter(card.songId, it.boundsInRoot().center.x)
                    }
                ) {
                    SongCardView(card = card, size = CARD_SIZE)
                }
            }
        }
    }
}

/** Tarjeta volteada que se puede arrastrar; reporta la posición del dedo en pantalla. */
@Composable
private fun DraggableBackCard(
    onDragMove: (Offset) -> Unit,
    onDragEnd: (Offset?) -> Unit,
    modifier: Modifier = Modifier
) {
    var origin by remember { mutableStateOf(Offset.Zero) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var pointer by remember { mutableStateOf(Offset.Zero) }
    var dragging by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .onGloballyPositioned { origin = it.positionInRoot() }
            .zIndex(if (dragging) 10f else 0f)
            .graphicsLayer {
                translationX = dragOffset.x
                translationY = dragOffset.y
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { start ->
                        dragging = true
                        pointer = origin + start
                        onDragMove(pointer)
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        dragOffset += amount
                        pointer += amount
                        onDragMove(pointer)
                    },
                    onDragEnd = {
                        dragging = false
                        dragOffset = Offset.Zero
                        onDragEnd(pointer)
                    },
                    onDragCancel = {
                        dragging = false
                        dragOffset = Offset.Zero
                        onDragEnd(null)
                    }
                )
            }
    ) {
        CardBackView(size = CARD_SIZE)
    }
}

@Composable
private fun ConfirmButton(enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = if (enabled) 1f else 0.5f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = "Confirmar",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun PlayerWithTokens(
    player: PlayerSlot,
    color: Color,
    dx: Double,
    dy: Double,
    avatarExtra: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val avatar: @Composable () -> Unit = {
        Box(contentAlignment = Alignment.Center) {
            PlayerAvatar(player, color)
            if (avatarExtra != null) {
                // Se coloca a la derecha del avatar sin afectar su posición
                Box(Modifier.align(Alignment.CenterEnd).offset(x = 72.dp)) { avatarExtra() }
            }
        }
    }
    val tokenRow: @Composable () -> Unit = {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(player.tokens) { TokenChip(color) }
        }
    }
    val tokenColumn: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(player.tokens) { TokenChip(color) }
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
private fun PlayerAvatar(player: PlayerSlot, color: Color) {
    Box(
        modifier = Modifier.size(40.dp).clip(CircleShape).background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = player.name,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = if (color.luminance() > 0.5f) Color.Black else Color.White
        )
    }
}

@Composable
private fun TokenChip(color: Color) {
    Box(Modifier.size(20.dp).clip(CircleShape).background(color))
}

@Composable
private fun CenterPlayButton(isPlaying: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(shape)
            .background(Color(0xFF0A0A0A))
            .border(1.dp, Color.White, shape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = if (isPlaying) "Pausar" else "Reproducir",
            tint = Color.White,
            modifier = Modifier.size(36.dp)
        )
    }
}

private val playerColors = listOf(
    Color(0xFFFFE600), Color(0xFF1F1FFF), Color(0xFFFF0000),
    Color(0xFF00E640), Color(0xFFB000FF), Color(0xFFFF8A00)
)

private fun playerColor(playerId: Int): Color =
    playerColors[(playerId - 1) % playerColors.size]