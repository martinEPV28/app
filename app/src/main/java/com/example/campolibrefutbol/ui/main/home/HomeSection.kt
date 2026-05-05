package com.example.campolibrefutbol.ui.main.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.campolibrefutbol.ui.main.common.HighlightCard
import com.example.campolibrefutbol.ui.main.common.SectionTitle
import com.example.campolibrefutbol.ui.main.common.StatCard
import com.example.campolibrefutbol.ui.main.common.StatCardRow

@Composable
fun HomeSection(
    loggedUserEmail: String?,
    totalCanchas: Int,
    totalUsers: Int,
    totalReservations: Int,
    totalMyReservations: Int,
    canManageUsers: Boolean,
    onGoAssign: () -> Unit,
    onGoMyReservations: () -> Unit,
    onGoUsers: () -> Unit
) {
    SectionTitle(
        title = "Inicio",
        subtitle = "Accesos rapidos para reservar canchas y revisar la informacion guardada localmente."
    )

    HighlightCard(
        title = "Bienvenida",
        description = "Hola ${loggedUserEmail ?: "jugador"}."
    )

    StatCardRow(
        leftTitle = "Canchas",
        leftValue = totalCanchas.toString(),
        rightTitle = "Reservas",
        rightValue = totalReservations.toString()
    )

    StatCard(
        title = "Mis reservas",
        value = totalMyReservations.toString(),
        modifier = Modifier.fillMaxWidth()
    )

    if (canManageUsers) {
        StatCardRow(
            leftTitle = "Usuarios",
            leftValue = totalUsers.toString(),
            rightTitle = "Rol",
            rightValue = "Admin"
        )
    }

    ElevatedCard(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Que deseas hacer ahora?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Button(onClick = onGoAssign, modifier = Modifier.fillMaxWidth()) {
                Text("Asignar cancha")
            }
            OutlinedButton(onClick = onGoMyReservations, modifier = Modifier.fillMaxWidth()) {
                Text("Ver mis reservas")
            }
            if (canManageUsers) {
                OutlinedButton(onClick = onGoUsers, modifier = Modifier.fillMaxWidth()) {
                    Text("Administrar usuarios y reservas")
                }
            }
        }
    }
}
