package com.example.campolibrefutbol.ui.main.reservations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.campolibrefutbol.AppReservation
import com.example.campolibrefutbol.Cancha
import com.example.campolibrefutbol.ui.main.common.CanchaCard
import com.example.campolibrefutbol.ui.main.common.HighlightCard
import com.example.campolibrefutbol.ui.main.common.HourRangeSelector
import com.example.campolibrefutbol.ui.main.common.SectionTitle
import com.example.campolibrefutbol.ui.main.common.SelectorRow

@Composable
fun MyReservationsSection(
    loggedUserEmail: String?,
    reservations: List<AppReservation>,
    onCancelReservation: (Long) -> Unit,
    formatReservationDateTime: (Long) -> String
) {
    var reservationPendingCancel by remember { mutableStateOf<AppReservation?>(null) }

    SectionTitle(
        title = "Mis reservas",
        subtitle = "Aqui ves unicamente las reservas que hiciste con tu usuario."
    )

    if (loggedUserEmail.isNullOrBlank()) {
        HighlightCard(
            title = "Sin usuario activo",
            description = "Inicia sesion para ver tus reservas."
        )
        return
    }

    HighlightCard(
        title = "Usuario",
        description = "$loggedUserEmail - Total: ${reservations.size}"
    )

    if (reservations.isEmpty()) {
        HighlightCard(
            title = "Todavia no tienes reservas",
            description = "Ve a Asignar cancha para reservar un horario disponible."
        )
        return
    }

    reservations.forEach { reservation ->
        ElevatedCard(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = reservation.canchaNombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${reservation.fecha}  ${reservation.hora}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Tipo: ${reservation.canchaTipo}  Precio: ${reservation.precio}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Guardada: ${formatReservationDateTime(reservation.createdAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = { reservationPendingCancel = reservation },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancelar reserva")
                }
            }
        }
    }

    reservationPendingCancel?.let { reservation ->
        AlertDialog(
            onDismissRequest = { reservationPendingCancel = null },
            title = { Text("Cancelar reserva") },
            text = {
                Text(
                    "Deseas cancelar tu reserva de ${reservation.canchaNombre} para ${reservation.fecha} ${reservation.hora}?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onCancelReservation(reservation.id)
                        reservationPendingCancel = null
                    }
                ) {
                    Text("Cancelar reserva")
                }
            },
            dismissButton = {
                TextButton(onClick = { reservationPendingCancel = null }) {
                    Text("Volver")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignmentSection(
    selectedDate: String,
    selectedHourRange: String?,
    hourRanges: List<String>,
    canchasDisponibles: List<Cancha>,
    canchaReservandoId: String?,
    reservaConfirmada: String?,
    onOpenCalendar: () -> Unit,
    onSelectHour: (String) -> Unit,
    onBuscar: () -> Unit,
    onReservar: (Cancha) -> Unit
) {
    SectionTitle(
        title = "Asignar cancha",
        subtitle = "Cada vez que entres aqui la vista se reinicia. Selecciona una fecha y un rango horario de una hora entre las 10:00 y las 22:00."
    )

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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SelectorRow(
                icon = Icons.Default.CalendarMonth,
                title = "Fecha seleccionada",
                value = selectedDate,
                actionLabel = "Abrir calendario",
                onAction = onOpenCalendar
            )

            HourRangeSelector(
                selectedHourRange = selectedHourRange,
                hourRanges = hourRanges,
                onSelectHour = onSelectHour
            )

            Button(
                onClick = onBuscar,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Buscar canchas disponibles")
            }
        }
    }

    if (canchasDisponibles.isEmpty()) {
        HighlightCard(
            title = "Sin resultados todavia",
            description = "Elige una hora y presiona buscar para ver las canchas disponibles."
        )
    } else {
        Text(
            text = "Disponibles para $selectedDate${selectedHourRange?.let { " en $it" } ?: ""}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        canchasDisponibles.forEach { cancha ->
            CanchaCard(
                cancha = cancha,
                isLoading = canchaReservandoId == cancha.id,
                onReservar = { onReservar(cancha) }
            )
        }
    }

    reservaConfirmada?.let { mensaje ->
        ElevatedCard(
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Reserva confirmada",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = mensaje,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}
