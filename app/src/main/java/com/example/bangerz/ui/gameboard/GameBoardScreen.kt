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
import com.example.bangerz.ui.components.BangerzButton
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton


// --- Tamaños (ajusta aquí si quieres más grande o más pequeño) ---
private val CARD_MAX = 64.dp          // tarjeta con pocas cartas en la línea
private val CARD_MIN = 30.dp          // tarjeta con muchas cartas
private val PLAY_SIZE = 72.dp
private val AVATAR_SIZE = 46.dp
private val TOKEN_SIZE = 24.dp
private val TOKEN_GAP = 6.dp
private val GROUP_HALF_HEIGHT = (TOKEN_SIZE + TOKEN_GAP + AVATAR_SIZE) / 2
private const val SLOT_RATIO = 0.27f   // ancho del hueco respecto a la tarjeta
private const val MARKER_RATIO = 0.55f // ancho del marcador de ficha respecto a la tarjeta

/** Tamaño de tarjeta para que la línea de tiempo quepa en `available`. */
private fun fitCardSize(cardUnits: Float, slotCount: Int, available: Dp, maxSize: Dp): Dp {
    val units = cardUnits + slotCount * SLOT_RATIO
    return (available / units).coerceAtMost(maxSize).coerceAtLeast(CARD_MIN)
}

@Composable
fun GameBoardScreen(
    gameBoardViewModel: GameBoardViewModel,
    playerViewModel: PlayerViewModel,
    onExitToMenu: () -> Unit
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

    // --- Estado del arrastre (coordenadas de pantalla) ---
    var dragPointer by remember { mutableStateOf<Offset?>(null) }
    var timelineBounds by remember { mutableStateOf<Rect?>(null) }
    val cardCenters = remember { mutableStateMapOf<Int, Float>() }
    var showBuyDialog by remember { mutableStateOf(false) }

    val currentPlayer = uiState.players.getOrNull(uiState.currentPlayerIndex)

    /** Hueco (0..n) bajo el dedo, o null si está fuera de la línea del jugador en turno. */
    fun slotAt(pointer: Offset): Int? {
        val bounds = timelineBounds ?: return null
        val timeline = currentPlayer?.timeline ?: return null
        if (!bounds.inflate(60f).contains(pointer)) return null
        return timeline.count { (cardCenters[it.songId] ?: Float.MAX_VALUE) < pointer.x }
    }

    val hoverSlot = dragPointer?.let { slotAt(it) }
    val onDragMove: (Offset) -> Unit = { dragPointer = it }

    /** Al soltar: la tarjeta del turno o la ficha de un oponente, según quién arrastra. */
    fun dropHandler(playerId: Int): (Offset?) -> Unit = { pointer ->
        pointer?.let { slotAt(it) }?.let { slot ->
            if (playerId == currentPlayer?.id) gameBoardViewModel.placeTurnCard(slot)
            else gameBoardViewModel.placeOpponentToken(playerId, slot)
        }
        dragPointer = null
    }

    val turnCardPlaced = uiState.placements.isNotEmpty()
    val turnCardLocked = uiState.placements.any { !it.isTurnPlayer }

    BangerzBackground(background = R.drawable.bg_board) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val radiusX = maxWidth / 2 - 62.dp
            val radiusY = maxHeight / 2 - 46.dp
            val playerCount = uiState.players.size

            // Espacio vertical libre entre el botón central y las fichas del jugador en turno
            val gapTop = PLAY_SIZE / 2
            val gapBottom = radiusY - GROUP_HALF_HEIGHT
            val maxCardSize = (gapBottom - gapTop - 6.dp)
                .coerceAtMost(CARD_MAX).coerceAtLeast(CARD_MIN)
            val timelineY = (gapTop + gapBottom) / 2
            val yFactor = timelineY.value / radiusY.value

            // Tarjeta del jugador en turno: se achica según crece su línea
            // (cuenta la tarjeta nueva y los marcadores de fichas de los oponentes)
            val turnCards = currentPlayer?.timeline?.size ?: 1
            val turnCardSize = fitCardSize(
                cardUnits = turnCards + 1 + (playerCount - 1).coerceAtLeast(0) * MARKER_RATIO,
                slotCount = turnCards + 1,
                available = maxWidth * 0.6f,
                maxSize = maxCardSize
            )

            uiState.players.forEachIndexed { index, player ->
                val isCurrent = index == uiState.currentPlayerIndex
                // El jugador en turno siempre abajo (90°), el resto en sentido horario
                val seat = (index - uiState.currentPlayerIndex + playerCount) % playerCount
                val angleDeg = 90.0 + (360.0 / playerCount) * seat
                val angle = Math.toRadians(angleDeg)
                val dx = cos(angle)
                val dy = sin(angle)
                val color = playerColor(player.id)

                val canDragToken = !isCurrent && turnCardPlaced &&
                        player.tokens > 0 && uiState.placements.none { it.playerId == player.id }

                val confirm: @Composable () -> Unit = {
                    ConfirmButton(
                        enabled = uiState.placements.isNotEmpty() && uiState.resolution == null,
                        onClick = { gameBoardViewModel.resolveTurn() }
                    )
                }
                val buy: @Composable () -> Unit = {
                    BuyCardButton(
                        enabled = uiState.currentSong != null &&
                                uiState.resolution == null &&
                                uiState.placements.none { !it.isTurnPlayer },
                        onClick = { showBuyDialog = true }
                    )
                }

                PlayerWithTokens(
                    player = player,
                    color = color,
                    dx = dx,
                    dy = dy,
                    avatarExtra = if (isCurrent) confirm else null,
                    avatarExtraStart = if (isCurrent) buy else null,
                    dragEnabled = canDragToken,
                    onDragMove = onDragMove,
                    onDragEnd = dropHandler(player.id),
                    modifier = Modifier.offset(
                        x = (radiusX.value * dx).dp,
                        y = (radiusY.value * dy).dp
                    )
                )

                val timelineModifier = Modifier.offset(
                    x = (radiusX.value * dx * 0.67).dp,
                    y = (radiusY.value * dy * yFactor).dp
                )

                if (isCurrent) {
                    CurrentTimeline(
                        cards = player.timeline,
                        placements = uiState.placements,
                        players = uiState.players,
                        hoverSlot = hoverSlot,
                        color = color,
                        cardSize = turnCardSize,
                        onBounds = { timelineBounds = it },
                        onCardCenter = { id, x -> cardCenters[id] = x },
                        turnCard = {
                            if (turnCardLocked) {
                                CardBackView(size = turnCardSize)
                            } else {
                                DragSource(
                                    onDragMove = onDragMove,
                                    onDragEnd = dropHandler(player.id)
                                ) { CardBackView(size = turnCardSize) }
                            }
                        },
                        modifier = timelineModifier
                    )
                } else {
                    // Línea de un oponente: tarjetas más chicas cuanto más larga sea
                    val n = player.timeline.size.coerceAtLeast(1)
                    val available = if (abs(dx) > abs(dy)) maxHeight * 0.6f else maxWidth * 0.4f
                    val oppSize = ((available - 4.dp * (n - 1)) / n)
                        .coerceAtMost(maxCardSize).coerceAtLeast(CARD_MIN)

                    Row(
                        modifier = timelineModifier.rotate((angleDeg - 90).toFloat()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        player.timeline.forEach { SongCardView(card = it, size = oppSize) }
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
            if (uiState.currentSong != null && uiState.placements.isEmpty() && uiState.resolution == null) {
                currentPlayer?.let {
                    DragSource(
                        onDragMove = onDragMove,
                        onDragEnd = dropHandler(it.id),
                        modifier = Modifier.offset(x = PLAY_SIZE / 2 + turnCardSize / 2 + 10.dp)
                    ) { CardBackView(size = turnCardSize) }
                }
            }
            uiState.resolution?.let { res ->
                ResolutionOverlay(
                    resolution = res,
                    canAwardToken = (currentPlayer?.tokens ?: MAX_TOKENS) < MAX_TOKENS,
                    onAwardToken = { gameBoardViewModel.awardToken() },
                    onNext = {
                        gameBoardViewModel.nextTurn()
                        playerViewModel.stop()
                    },
                    onExit = {
                        playerViewModel.stop()
                        onExitToMenu()
                    }
                )
            }

            if (showBuyDialog && currentPlayer != null) {
                val hasEnough = currentPlayer.tokens >= BUY_COST
                AlertDialog(
                    onDismissRequest = { showBuyDialog = false },
                    containerColor = Color(0xFF0A0A0A),
                    titleContentColor = Color.White,
                    textContentColor = Color.White,
                    title = {
                        Text(if (hasEnough) "¿Adivinar?" else "Fichas insuficientes")
                    },
                    text = {
                        Text(
                            if (hasEnough)
                                "Gastarás $BUY_COST fichas BANG para añadir esta canción a tu línea de tiempo sin adivinar su año. ¿Continuar?"
                            else
                                "Necesitas $BUY_COST fichas BANG y solo tienes ${currentPlayer.tokens}."
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            if (hasEnough) gameBoardViewModel.buyCard()
                            showBuyDialog = false
                        }) {
                            Text(
                                if (hasEnough) "Sí, gastar $BUY_COST fichas" else "Entendido",
                                color = Color.White
                            )
                        }
                    },
                    dismissButton = if (hasEnough) {
                        {
                            TextButton(onClick = { showBuyDialog = false }) {
                                Text("Cancelar", color = Color.White)
                            }
                        }
                    } else null
                )
            }
        }
    }
}

/** Línea de tiempo del jugador en turno: huecos entre tarjetas + lo que hay en la cola. */
@Composable
private fun CurrentTimeline(
    cards: List<SongCard>,
    placements: List<Placement>,
    players: List<PlayerSlot>,
    hoverSlot: Int?,
    color: Color,
    cardSize: Dp,
    onBounds: (Rect) -> Unit,
    onCardCenter: (Int, Float) -> Unit,
    turnCard: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val slotWidth = cardSize * SLOT_RATIO
    val markerSize = (cardSize * MARKER_RATIO).coerceAtLeast(24.dp)

    Row(
        modifier = modifier.onGloballyPositioned { onBounds(it.boundsInRoot()) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0..cards.size) {
            Box(
                modifier = Modifier.width(slotWidth).height(cardSize),
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

            val queueIndex = placements.indexOfFirst { it.slotIndex == i }
            if (queueIndex >= 0) {
                val placement = placements[queueIndex]
                if (placement.isTurnPlayer) {
                    turnCard()
                } else {
                    val owner = players.firstOrNull { it.id == placement.playerId }
                    OpponentTokenMarker(
                        color = playerColor(placement.playerId),
                        priority = queueIndex,
                        label = owner?.name.orEmpty(),
                        size = markerSize
                    )
                }
            }

            if (i < cards.size) {
                val card = cards[i]
                Box(
                    Modifier.onGloballyPositioned {
                        onCardCenter(card.songId, it.boundsInRoot().center.x)
                    }
                ) { SongCardView(card = card, size = cardSize) }
            }
        }
    }
}

@Composable
private fun OpponentTokenMarker(color: Color, priority: Int, label: String, size: Dp) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(size).clip(CircleShape).background(color),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$priority",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (color.luminance() > 0.5f) Color.Black else Color.White
            )
        }
        Text(label, color = Color.White, fontSize = 10.sp)
    }
}

/** Contenedor arrastrable genérico (tarjeta o ficha). Reporta la posición del dedo en pantalla. */
@Composable
private fun DragSource(
    onDragMove: (Offset) -> Unit,
    onDragEnd: (Offset?) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val currentOnMove by rememberUpdatedState(onDragMove)
    val currentOnEnd by rememberUpdatedState(onDragEnd)
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
                        currentOnMove(pointer)
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        dragOffset += amount
                        pointer += amount
                        currentOnMove(pointer)
                    },
                    onDragEnd = {
                        dragging = false
                        dragOffset = Offset.Zero
                        currentOnEnd(pointer)
                    },
                    onDragCancel = {
                        dragging = false
                        dragOffset = Offset.Zero
                        currentOnEnd(null)
                    }
                )
            }
    ) { content() }
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
private fun BuyCardButton(enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = if (enabled) 1f else 0.5f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = "Adivinar",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false
        )
    }
}

/** Avatar + fichas. Si dragEnabled, la primera ficha se puede arrastrar a la línea del turno. */
@Composable
private fun PlayerWithTokens(
    player: PlayerSlot,
    color: Color,
    dx: Double,
    dy: Double,
    avatarExtra: (@Composable () -> Unit)?,
    avatarExtraStart: (@Composable () -> Unit)? = null,
    dragEnabled: Boolean,
    onDragMove: (Offset) -> Unit,
    onDragEnd: (Offset?) -> Unit,
    modifier: Modifier = Modifier
) {
    val avatar: @Composable () -> Unit = {
        Box(contentAlignment = Alignment.Center) {
            PlayerAvatar(player, color)
            if (avatarExtra != null) {
                Box(Modifier.align(Alignment.CenterEnd).offset(x = 72.dp)) { avatarExtra() }
            }
            if (avatarExtraStart != null) {
                Box(Modifier.align(Alignment.CenterStart).offset(x = (-72).dp)) { avatarExtraStart() }
            }
        }
    }

    val tokens: @Composable () -> Unit = {
        repeat(player.tokens) { i ->
            if (i == 0 && dragEnabled) {
                DragSource(onDragMove = onDragMove, onDragEnd = onDragEnd) { TokenChip(color) }
            } else {
                TokenChip(color)
            }
        }
    }

    if (abs(dy) >= abs(dx)) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(TOKEN_GAP)
        ) {
            val row: @Composable () -> Unit = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { tokens() }
            }
            if (dy < 0) { avatar(); row() } else { row(); avatar() }
        }
    } else {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val column: @Composable () -> Unit = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { tokens() }
            }
            if (dx < 0) { avatar(); column() } else { column(); avatar() }
        }
    }
}

@Composable
private fun PlayerAvatar(player: PlayerSlot, color: Color) {
    Box(
        modifier = Modifier.size(AVATAR_SIZE).clip(CircleShape).background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = player.name,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = if (color.luminance() > 0.5f) Color.Black else Color.White
        )
    }
}

@Composable
private fun TokenChip(color: Color) {
    Box(Modifier.size(TOKEN_SIZE).clip(CircleShape).background(color))
}

@Composable
private fun CenterPlayButton(isPlaying: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .size(PLAY_SIZE)
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
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
private fun ResolutionOverlay(
    resolution: TurnResolution,
    canAwardToken: Boolean,
    onAwardToken: () -> Unit,
    onNext: () -> Unit,
    onExit: () -> Unit
) {
    val smallPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    val gameOver = resolution.gameWinnerId != null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(20f)
            .background(Color.Black.copy(alpha = 0.7f))
            .pointerInput(Unit) { detectTapGestures { } },   // bloquea toques al tablero
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SongCardView(card = resolution.song, size = 120.dp)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = resolution.message,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 260.dp)
                )
                Spacer(Modifier.height(16.dp))
                if (gameOver) {
                    BangerzButton(
                        text = "Volver al menú",
                        onClick = onExit,
                        fontSize = 14.sp,
                        contentPadding = smallPadding
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        BangerzButton(
                            text = when {
                                resolution.tokenAwarded -> "Ficha ganada ✓"
                                !canAwardToken -> "Fichas al máximo"
                                else -> "Ganar ficha"
                            },
                            onClick = onAwardToken,
                            enabled = !resolution.tokenAwarded && canAwardToken,
                            fontSize = 14.sp,
                            contentPadding = smallPadding
                        )
                        BangerzButton(
                            text = "Siguiente turno",
                            onClick = onNext,
                            fontSize = 14.sp,
                            contentPadding = smallPadding
                        )
                    }
                }
            }
        }
    }
}

private val playerColors = listOf(
    Color(0xFFFFE600), Color(0xFF1F1FFF), Color(0xFFFF0000),
    Color(0xFF00E640), Color(0xFFB000FF), Color(0xFFFF8A00)
)

private fun playerColor(playerId: Int): Color =
    playerColors[(playerId - 1) % playerColors.size]