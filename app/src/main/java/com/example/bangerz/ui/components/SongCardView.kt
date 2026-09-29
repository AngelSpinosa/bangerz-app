package com.example.bangerz.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bangerz.ui.gameboard.SongCard

@Composable
fun SongCardView(
    card: SongCard,
    size: Dp = 88.dp,
    modifier: Modifier = Modifier
) {
    // Todo escala con el tamaño de la tarjeta
    val factor = size.value / 88f

    Column(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.18f))
            .background(Color.Black)
            .padding(size * 0.08f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = card.year.toString(),
            color = Color.White,
            fontSize = (22 * factor).sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(size * 0.04f))
        Text(
            text = card.title,
            color = Color.White,
            fontSize = (9 * factor).sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = card.artist,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = (8 * factor).sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}