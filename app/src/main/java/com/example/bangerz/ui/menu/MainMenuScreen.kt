package com.example.bangerz.ui.menu

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bangerz.R
import com.example.bangerz.ui.components.BangerzBackground

@Composable
fun MainMenuScreen(
    onPlayClick: () -> Unit
) {
    BangerzBackground {
        Image(
            painter = painterResource(id = R.drawable.logo_icono),
            contentDescription = "Logo Bangerz",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 48.dp)
                .width(220.dp)
        )

        OutlinedButton(
            onClick = onPlayClick,
            modifier = Modifier.align(Alignment.Center),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color.White),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Black,
                contentColor = Color.White
            ),
            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Jugar",
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}