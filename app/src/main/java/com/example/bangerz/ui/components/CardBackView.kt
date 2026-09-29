package com.example.bangerz.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.bangerz.R

@Composable
fun CardBackView(
    size: Dp = 52.dp,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(size * 0.18f)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(Color(0xFF111111))
            .border(1.dp, Color.White, shape),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.icono_bangerz),
            contentDescription = "Tarjeta oculta",
            colorFilter = ColorFilter.tint(Color.White),
            modifier = Modifier.size(size * 0.6f)
        )
    }
}