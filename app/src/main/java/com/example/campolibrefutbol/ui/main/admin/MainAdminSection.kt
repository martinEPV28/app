package com.example.campolibrefutbol.ui.main.admin

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.campolibrefutbol.AppReservation
import com.example.campolibrefutbol.AppUser
import com.example.campolibrefutbol.SQLiteUserHelper
import com.example.campolibrefutbol.dateDaysFromToday
import com.example.campolibrefutbol.exportReservationsToCsv
import com.example.campolibrefutbol.exportUsersToCsv
import com.example.campolibrefutbol.formatDate
import com.example.campolibrefutbol.formatDateTime
import com.example.campolibrefutbol.parseDateStringToMillis
import com.example.campolibrefutbol.shareCsvFile
import com.example.campolibrefutbol.showSavedCsvMessage
import com.example.campolibrefutbol.ui.main.common.HighlightCard
import com.example.campolibrefutbol.ui.main.common.SectionTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementSection(
    loggedUserEmail: String?,
    isAdminUser: Boolean,
    users: List<AppUser>,
    reservations: List<AppReservation>,
    onToggleAdmin: (String, Boolean) -> Unit,
    onDeleteUser: (String) -> Unit,
    onDeleteReservation: (Long) -> Unit,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    var userPendingDelete by remember { mutableStateOf<AppUser?>(null) }
    var reservationPendingDelete by remember { mutableStateOf<AppReservation?>(null) }
    var userPendingAdminChange by remember { mutableStateOf<AppUser?>(null) }
    var userSearch by rememberSaveable { mutableStateOf("") }
    var reservationSearch by rememberSaveable { mutableStateOf("") }
    var reservationDateFrom by rememberSaveable { mutableStateOf("") }
    var reservationDateTo by rememberSaveable { mutableStateOf("") }
    var userSortExpanded by remember { mutableStateOf(false) }
    var userSort by rememberSaveable { mutableStateOf("Correo A-Z") }
    var reservationFromPickerOpen by remember { mutableStateOf(false) }
    var reservationToPickerOpen by remember { mutableStateOf(false) }
    var reservationSortExpanded by remember { mutableStateOf(false) }
    var reservationSort by rememberSaveable { mutableStateOf("Más recientes") }
    var exportSectionExpanded by rememberSaveable { mutableStateOf(true) }
    var usersSectionExpanded by rememberSaveable { mutableStateOf(true) }
    var reservationsSectionExpanded by rememberSaveable { mutableStateOf(true) }
    val reservationFromPickerState = rememberDatePickerState(
        initialSelectedDateMillis = parseDateStringToMillis(reservationDateFrom)
    )
    val reservationToPickerState = rememberDatePickerState(
        initialSelectedDateMillis = parseDateStringToMillis(reservationDateTo)
    )
    val reservationSortOptions = listOf(
        "Más recientes",
        "Más antiguas",
        "Fecha reservada asc",
        "Fecha reservada desc"
    )
    val userSortOptions = listOf(
        "Correo A-Z",
        "Correo Z-A",
        "Más recientes",
        "Más antiguos"
    )
    val filteredUsers = remember(users, userSearch, userSort) {
        val query = userSearch.trim().lowercase()
        val baseUsers = if (query.isBlank()) {
            users
        } else {
            users.filter { user ->
                user.email.lowercase().contains(query)
            }
        }

        when (userSort) {
            "Correo Z-A" -> baseUsers.sortedByDescending { it.email.lowercase() }
            "Más recientes" -> baseUsers.sortedByDescending { it.createdAt }
            "Más antiguos" -> baseUsers.sortedBy { it.createdAt }
            else -> baseUsers.sortedBy { it.email.lowercase() }
        }
    }
    val filteredReservations = remember(
        reservations,
        reservationSearch,
        reservationDateFrom,
        reservationDateTo,
        reservationSort
    ) {
        val query = reservationSearch.trim().lowercase()
        val fromMillis = parseDateStringToMillis(reservationDateFrom.trim())
        val toMillis = parseDateStringToMillis(reservationDateTo.trim())

        val filtered = reservations.filter { reservation ->
            val matchesQuery = if (query.isBlank()) {
                true
            } else {
                reservation.canchaNombre.lowercase().contains(query) ||
                    reservation.usuarioEmail.lowercase().contains(query) ||
                    reservation.fecha.lowercase().contains(query) ||
                    reservation.hora.lowercase().contains(query)
            }

            val reservationDateMillis = parseDateStringToMillis(reservation.fecha)
            val matchesFrom = fromMillis == null || (reservationDateMillis != null && reservationDateMillis >= fromMillis)
            val matchesTo = toMillis == null || (reservationDateMillis != null && reservationDateMillis <= toMillis)

            matchesQuery && matchesFrom && matchesTo
        }

        when (reservationSort) {
            "Más antiguas" -> filtered.sortedBy { it.createdAt }
            "Fecha reservada asc" -> filtered.sortedBy { parseDateStringToMillis(it.fecha) ?: Long.MAX_VALUE }
            "Fecha reservada desc" -> filtered.sortedByDescending { parseDateStringToMillis(it.fecha) ?: Long.MIN_VALUE }
            else -> filtered.sortedByDescending { it.createdAt }
        }
    }
    val adminUsersCount = users.count { it.isAdmin }
    val lockedUsersCount = users.count { it.lockedUntil > System.currentTimeMillis() }
    val lockedUsersColor = when {
        lockedUsersCount == 0 -> Color(0xFF2E7D32)
        lockedUsersCount <= 2 -> Color(0xFFF9A825)
        else -> MaterialTheme.colorScheme.error
    }

    if (!isAdminUser) {
        HighlightCard(
            title = "Acceso restringido",
            description = "Solo el usuario administrador puede ver la administración de usuarios y reservas."
        )
        return
    }

    SectionTitle(
        title = "Administración de usuario",
        subtitle = "Aquí puedes ver los usuarios y también las reservas guardadas en SQLite."
    )

    ElevatedCard(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Usuarios: ${users.size} (Admin: $adminUsersCount)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Bloqueados: $lockedUsersCount",
                style = MaterialTheme.typography.bodyMedium,
                color = lockedUsersColor
            )
        }
    }

    ElevatedCard(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Reservas: ${reservations.size}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Filtradas: ${filteredReservations.size}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(onClick = onRefresh, modifier = Modifier.weight(1f)) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Actualizar"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Actualizar lista")
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        TextButton(
            onClick = {
                userSearch = ""
                reservationSearch = ""
                reservationDateFrom = ""
                reservationDateTo = ""
                userSort = "Correo A-Z"
                userSortExpanded = false
                reservationSort = "Más recientes"
                reservationSortExpanded = false
                Toast.makeText(context, "Filtros restablecidos", Toast.LENGTH_SHORT).show()
            }
        ) {
            Text("Limpiar filtros")
        }
        TextButton(
            onClick = {
                exportSectionExpanded = true
                usersSectionExpanded = true
                reservationsSectionExpanded = true
            }
        ) {
            Text("Expandir todo")
        }
        TextButton(
            onClick = {
                exportSectionExpanded = false
                usersSectionExpanded = false
                reservationsSectionExpanded = false
            }
        ) {
            Text("Contraer todo")
        }
    }

    ElevatedCard(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { exportSectionExpanded = !exportSectionExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Exportación",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Guarda o comparte archivos CSV de usuarios y reservas filtradas.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = if (exportSectionExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (exportSectionExpanded) "Contraer exportación" else "Expandir exportación"
                )
            }

            AnimatedVisibility(visible = exportSectionExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val file = exportUsersToCsv(context, filteredUsers)
                                showSavedCsvMessage(context, file, "usuarios")
                            },
                            enabled = filteredUsers.isNotEmpty(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = "Guardar usuarios"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Guardar usuarios")
                        }
                        OutlinedButton(
                            onClick = {
                                val file = exportReservationsToCsv(context, filteredReservations)
                                showSavedCsvMessage(context, file, "reservas")
                            },
                            enabled = filteredReservations.isNotEmpty(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = "Guardar reservas"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Guardar reservas")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val file = exportUsersToCsv(context, filteredUsers)
                                if (file != null) {
                                    shareCsvFile(context, file, "Compartir usuarios CSV")
                                } else {
                                    Toast.makeText(context, "No se pudo exportar usuarios", Toast.LENGTH_LONG).show()
                                }
                            },
                            enabled = filteredUsers.isNotEmpty(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Compartir usuarios"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Compartir usuarios")
                        }
                        OutlinedButton(
                            onClick = {
                                val file = exportReservationsToCsv(context, filteredReservations)
                                if (file != null) {
                                    shareCsvFile(context, file, "Compartir reservas CSV")
                                } else {
                                    Toast.makeText(context, "No se pudo exportar reservas", Toast.LENGTH_LONG).show()
                                }
                            },
                            enabled = filteredReservations.isNotEmpty(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Compartir reservas"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Compartir reservas")
                        }
                    }
                }
            }
        }
    }

    ElevatedCard(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { usersSectionExpanded = !usersSectionExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Usuarios registrados",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Mostrando ${filteredUsers.size} de ${users.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = if (usersSectionExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (usersSectionExpanded) "Contraer usuarios" else "Expandir usuarios"
                )
            }

            AnimatedVisibility(visible = usersSectionExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = userSearch,
                        onValueChange = { userSearch = it },
                        label = { Text("Buscar usuario por correo") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { userSortExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Orden usuarios: $userSort")
                                Icon(
                                    imageVector = Icons.Default.ExpandMore,
                                    contentDescription = "Ordenar usuarios"
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = userSortExpanded,
                            onDismissRequest = { userSortExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.92f)
                        ) {
                            userSortOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        userSort = option
                                        userSortExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    if (filteredUsers.isEmpty()) {
                        HighlightCard(
                            title = if (users.isEmpty()) "Sin usuarios registrados" else "Sin coincidencias",
                            description = if (users.isEmpty()) {
                                "Todavía no hay cuentas guardadas en SQLite."
                            } else {
                                "No hay usuarios que coincidan con la búsqueda actual."
                            }
                        )
                    } else {
                        filteredUsers.forEach { user ->
                            ElevatedCard(
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = user.email,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (user.isAdmin) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Security,
                                                    contentDescription = "Administrador",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = "Admin",
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "Creado: ${formatDateTime(user.createdAt)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = if (user.lockedUntil > System.currentTimeMillis()) "Estado: bloqueado temporalmente" else "Estado: activo",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Intentos fallidos acumulados: ${user.failedAttempts}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (user.email != loggedUserEmail && user.email != SQLiteUserHelper.DEFAULT_ADMIN_EMAIL) {
                                            OutlinedButton(
                                                onClick = { userPendingAdminChange = user },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(if (user.isAdmin) "Quitar admin" else "Dar admin")
                                            }
                                            OutlinedButton(
                                                onClick = { userPendingDelete = user },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Eliminar")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    ElevatedCard(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { reservationsSectionExpanded = !reservationsSectionExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Reservas guardadas",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Mostrando ${filteredReservations.size} de ${reservations.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = if (reservationsSectionExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (reservationsSectionExpanded) "Contraer reservas" else "Expandir reservas"
                )
            }

            AnimatedVisibility(visible = reservationsSectionExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = reservationSearch,
                        onValueChange = { reservationSearch = it },
                        label = { Text("Buscar reserva por cancha, usuario, fecha u hora") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { reservationFromPickerOpen = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (reservationDateFrom.isBlank()) "Desde" else "Desde: $reservationDateFrom")
                        }
                        OutlinedButton(
                            onClick = { reservationToPickerOpen = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (reservationDateTo.isBlank()) "Hasta" else "Hasta: $reservationDateTo")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val today = dateDaysFromToday(0)
                                reservationDateFrom = today
                                reservationDateTo = today
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Hoy")
                        }
                        OutlinedButton(
                            onClick = {
                                reservationDateFrom = dateDaysFromToday(0)
                                reservationDateTo = dateDaysFromToday(7)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Próximos 7 días")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                reservationDateFrom = ""
                                reservationDateTo = ""
                            }
                        ) {
                            Text("Limpiar rango")
                        }
                    }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { reservationSortExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Orden: $reservationSort")
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = "Ordenar reservas"
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = reservationSortExpanded,
                            onDismissRequest = { reservationSortExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.92f)
                        ) {
                            reservationSortOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        reservationSort = option
                                        reservationSortExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    if (filteredReservations.isEmpty()) {
                        HighlightCard(
                            title = if (reservations.isEmpty()) "Sin reservas" else "Sin coincidencias",
                            description = if (reservations.isEmpty()) {
                                "Todavía no hay reservas guardadas en la base de datos local."
                            } else {
                                "No hay reservas que coincidan con la búsqueda actual."
                            }
                        )
                    } else {
                        filteredReservations.forEach { reservation ->
                            ElevatedCard(
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
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
                                        text = "Usuario: ${reservation.usuarioEmail}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "Tipo: ${reservation.canchaTipo}  Precio: ${reservation.precio}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "Guardada: ${formatDateTime(reservation.createdAt)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    OutlinedButton(
                                        onClick = { reservationPendingDelete = reservation },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Eliminar reserva")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    userPendingDelete?.let { user ->
        AlertDialog(
            onDismissRequest = { userPendingDelete = null },
            title = { Text("Eliminar usuario") },
            text = { Text("¿Seguro que deseas eliminar a ${user.email}? También se eliminarán sus reservas locales.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteUser(user.email)
                        userPendingDelete = null
                    }
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { userPendingDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    userPendingAdminChange?.let { user ->
        val makeAdmin = !user.isAdmin
        AlertDialog(
            onDismissRequest = { userPendingAdminChange = null },
            title = { Text(if (makeAdmin) "Dar privilegio admin" else "Quitar privilegio admin") },
            text = {
                Text(
                    if (makeAdmin) {
                        "¿Deseas convertir a ${user.email} en administrador?"
                    } else {
                        "¿Deseas quitar los privilegios de administrador a ${user.email}?"
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onToggleAdmin(user.email, makeAdmin)
                        userPendingAdminChange = null
                    }
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { userPendingAdminChange = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    reservationPendingDelete?.let { reservation ->
        AlertDialog(
            onDismissRequest = { reservationPendingDelete = null },
            title = { Text("Eliminar reserva") },
            text = { Text("¿Deseas eliminar la reserva de ${reservation.canchaNombre} para ${reservation.usuarioEmail}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteReservation(reservation.id)
                        reservationPendingDelete = null
                    }
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { reservationPendingDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (reservationFromPickerOpen) {
        DatePickerDialog(
            onDismissRequest = { reservationFromPickerOpen = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        reservationDateFrom = reservationFromPickerState.selectedDateMillis?.let(::formatDate).orEmpty()
                        reservationFromPickerOpen = false
                    }
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { reservationFromPickerOpen = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = reservationFromPickerState)
        }
    }

    if (reservationToPickerOpen) {
        DatePickerDialog(
            onDismissRequest = { reservationToPickerOpen = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        reservationDateTo = reservationToPickerState.selectedDateMillis?.let(::formatDate).orEmpty()
                        reservationToPickerOpen = false
                    }
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { reservationToPickerOpen = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = reservationToPickerState)
        }
    }
}
