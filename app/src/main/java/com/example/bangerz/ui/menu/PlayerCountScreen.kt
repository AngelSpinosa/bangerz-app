package com.example.bangerz.ui.menu

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val MIN_PLAYERS = 3
private const val MAX_PLAYERS = 6

@Composable
fun PlayerCountScreen(
    onAccept: (Int) -> Unit
) {
    var playerCount by remember { mutableIntStateOf(MIN_PLAYERS) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Bangerz",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text("Selecciona una cantidad de jugadores")
        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedIconButton(
                onClick = { if (playerCount > MIN_PLAYERS) playerCount-- },
                enabled = playerCount > MIN_PLAYERS
            ) {
                Text("−")
            }
            Text(
                text = "$playerCount",
                fontSize = 20.sp,
                modifier = Modifier
                    .padding(horizontal = 20.dp)
            )
            OutlinedIconButton(
                onClick = { if (playerCount < MAX_PLAYERS) playerCount++ },
                enabled = playerCount < MAX_PLAYERS
            ) {
                Text("+")
            }
        }

        Spacer(Modifier.height(24.dp))
        Button(onClick = { onAccept(playerCount) }) {
            Text("Aceptar")
        }
    }
}