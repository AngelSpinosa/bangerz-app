package com.example.bangerz.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BangerzButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color.White),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Black,
            contentColor = Color.White
        ),
        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 12.dp)
    ) {
        Text(text = text, fontSize = 20.sp, fontWeight = FontWeight.Medium)
    }
}