package com.example.bangerz.ui.menu

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bangerz.ui.components.BangerzBackground
import com.example.bangerz.ui.components.BangerzButton

private const val MIN_PLAYERS = 3
private const val MAX_PLAYERS = 6

@Composable
fun PlayerCountScreen(
    onAccept: (Int) -> Unit
) {
    var playerCount by remember { mutableIntStateOf(MIN_PLAYERS) }

    BangerzBackground {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Bangerz",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(8.dp))
            Text("Selecciona una cantidad de jugadores", color = Color.White)
            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedIconButton(
                    onClick = { if (playerCount > MIN_PLAYERS) playerCount-- },
                    enabled = playerCount > MIN_PLAYERS,
                    border = BorderStroke(1.dp, Color.White),
                    colors = IconButtonDefaults.outlinedIconButtonColors(
                        containerColor = Color.Black,
                        contentColor = Color.White,
                        disabledContainerColor = Color.Black.copy(alpha = 0.5f),
                        disabledContentColor = Color.White.copy(alpha = 0.4f)
                    )
                ) { Text("−") }

                Text(
                    text = "$playerCount",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                OutlinedIconButton(
                    onClick = { if (playerCount < MAX_PLAYERS) playerCount++ },
                    enabled = playerCount < MAX_PLAYERS,
                    border = BorderStroke(1.dp, Color.White),
                    colors = IconButtonDefaults.outlinedIconButtonColors(
                        containerColor = Color.Black,
                        contentColor = Color.White,
                        disabledContainerColor = Color.Black.copy(alpha = 0.5f),
                        disabledContentColor = Color.White.copy(alpha = 0.4f)
                    )
                ) { Text("+") }
            }

            Spacer(Modifier.height(24.dp))
            BangerzButton(text = "Aceptar", onClick = { onAccept(playerCount) })
        }
    }
}