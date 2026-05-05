package com.example.campolibrefutbol

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.campolibrefutbol.ui.main.home.HomeSection
import com.example.campolibrefutbol.ui.main.reservations.AssignmentSection
import com.example.campolibrefutbol.ui.main.reservations.MyReservationsSection
import com.example.campolibrefutbol.ui.main.navigation.RightMenuDrawer
import com.example.campolibrefutbol.ui.main.admin.UserManagementSection
import com.example.campolibrefutbol.ui.theme.CampoLibreFutbolTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class Cancha(
    val id: String,
    val nombre: String,
    val tipo: String,
    val descripcion: String,
    val precio: String
)

enum class AppSection(val titulo: String) {
    HOME("Inicio"),
    ASSIGN("Asignar cancha"),
    MY_RESERVATIONS("Mis reservas"),
    USERS("Administración de usuario")
}

data class MenuItemData(
    val section: AppSection,
    val label: String,
    val icon: ImageVector
)

private val canchasDemo = listOf(
    Cancha("libertadores", "Cancha Libertadores", "Fútbol 5", "Césped sintético y luces LED", "$35.000"),
    Cancha("sudamericana", "Cancha Sudamericana", "Fútbol 7", "Espacio techado para jugar de noche", "$48.000"),
    Cancha("campeones", "Cancha Campeones", "Fútbol 8", "Ideal para torneos y partidos largos", "$55.000")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val loggedUserEmail = intent.getStringExtra("user_email")
        val isAdminUser = intent.getBooleanExtra("is_admin", false)
        val userDb = SQLiteUserHelper(this)

        enableEdgeToEdge()
        setContent {
            CampoLibreFutbolTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ReservaScreen(
                        modifier = Modifier.padding(innerPadding),
                        loggedUserEmail = loggedUserEmail,
                        isAdminUser = isAdminUser,
                        loadUsers = { userDb.getAllUsers() },
                        loadReservations = { userDb.getAllReservations() },
                        loadReservationsByUser = { email -> userDb.getReservationsByUser(email) },
                        updateUserAdmin = { email, isAdmin -> userDb.updateUserAdmin(email, isAdmin) },
                        deleteUser = { email -> userDb.deleteUser(email) },
                        deleteReservation = { id -> userDb.deleteReservation(id) },
                        saveReservation = { cancha, fecha, hora, email ->
                            userDb.saveReservation(cancha, fecha, hora, email)
                        },
                        onLogout = {
                            startActivity(Intent(this, LoginActivity::class.java))
                            finish()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservaScreen(
    modifier: Modifier = Modifier,
    loggedUserEmail: String? = null,
    isAdminUser: Boolean = false,
    loadUsers: () -> List<AppUser> = { emptyList() },
    loadReservations: () -> List<AppReservation> = { emptyList() },
    loadReservationsByUser: (String) -> List<AppReservation> = { emptyList() },
    updateUserAdmin: (String, Boolean) -> Boolean = { _, _ -> false },
    deleteUser: (String) -> Boolean = { false },
    deleteReservation: (Long) -> Boolean = { false },
    saveReservation: (Cancha, String, String, String?) -> Long = { _, _, _, _ -> -1L },
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val hourRanges = remember { generateHourRanges() }
    val menuItems = remember(isAdminUser) {
        buildList {
            add(MenuItemData(AppSection.HOME, "Inicio", Icons.Default.Home))
            add(MenuItemData(AppSection.ASSIGN, "Asignar cancha", Icons.Default.SportsSoccer))
            add(MenuItemData(AppSection.MY_RESERVATIONS, "Mis reservas", Icons.Default.AccessTime))
            if (isAdminUser) {
                add(MenuItemData(AppSection.USERS, "Administración de usuario", Icons.Default.Groups))
            }
        }
    }
    var currentSection by remember { mutableStateOf(AppSection.HOME) }
    var isMenuOpen by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var selectedHourRange by remember { mutableStateOf<String?>(null) }
    var canchasDisponibles by remember { mutableStateOf<List<Cancha>>(emptyList()) }
    var canchaReservandoId by remember { mutableStateOf<String?>(null) }
    var reservaConfirmada by remember { mutableStateOf<String?>(null) }
    var users by remember { mutableStateOf(loadUsers()) }
    var reservations by remember { mutableStateOf(loadReservations()) }
    var userReservations by remember {
        mutableStateOf(loggedUserEmail?.let(loadReservationsByUser).orEmpty())
    }
    var showReservationDialog by remember { mutableStateOf(false) }
    var reservationDetails by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    val selectedDate = remember(selectedDateMillis) { formatDate(selectedDateMillis) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)

    fun resetAssignmentView() {
        selectedDateMillis = System.currentTimeMillis()
        selectedHourRange = null
        canchasDisponibles = emptyList()
        canchaReservandoId = null
        reservaConfirmada = null
    }

    fun refreshUserReservations() {
        userReservations = loggedUserEmail?.let(loadReservationsByUser).orEmpty()
    }

    fun openSection(section: AppSection) {
        when (section) {
            AppSection.HOME -> currentSection = AppSection.HOME
            AppSection.ASSIGN -> {
                resetAssignmentView()
                currentSection = AppSection.ASSIGN
            }
            AppSection.MY_RESERVATIONS -> {
                refreshUserReservations()
                currentSection = AppSection.MY_RESERVATIONS
            }
            AppSection.USERS -> {
                if (isAdminUser) {
                    users = loadUsers()
                    reservations = loadReservations()
                    currentSection = AppSection.USERS
                } else {
                    Toast.makeText(context, "Solo el usuario administrador puede ver esta sección", Toast.LENGTH_SHORT).show()
                    currentSection = AppSection.HOME
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HeaderCard(
                loggedUserEmail = loggedUserEmail,
                isAdminUser = isAdminUser,
                currentSection = currentSection,
                onMenuClick = { isMenuOpen = true }
            )

            when (currentSection) {
                AppSection.HOME -> HomeSection(
                    loggedUserEmail = loggedUserEmail,
                    totalCanchas = canchasDemo.size,
                    totalUsers = users.size,
                    totalReservations = reservations.size,
                    totalMyReservations = userReservations.size,
                    canManageUsers = isAdminUser,
                    onGoAssign = { openSection(AppSection.ASSIGN) },
                    onGoMyReservations = { openSection(AppSection.MY_RESERVATIONS) },
                    onGoUsers = { openSection(AppSection.USERS) }
                )

                AppSection.ASSIGN -> AssignmentSection(
                    selectedDate = selectedDate,
                    selectedHourRange = selectedHourRange,
                    hourRanges = hourRanges,
                    canchasDisponibles = canchasDisponibles,
                    canchaReservandoId = canchaReservandoId,
                    reservaConfirmada = reservaConfirmada,
                    onOpenCalendar = { showDatePicker = true },
                    onSelectHour = {
                        selectedHourRange = it
                        reservaConfirmada = null
                    },
                    onBuscar = {
                        if (selectedHourRange == null) {
                            Toast.makeText(context, "Selecciona una franja horaria", Toast.LENGTH_SHORT).show()
                        } else {
                            reservations = loadReservations()
                            canchasDisponibles = obtenerCanchasDisponibles(
                                fecha = selectedDate,
                                hora = selectedHourRange!!,
                                reservations = reservations
                            )
                            reservaConfirmada = null
                        }
                    },
                    onReservar = { cancha ->
                        val horario = selectedHourRange ?: return@AssignmentSection
                        canchaReservandoId = cancha.id
                        val reservationId = saveReservation(cancha, selectedDate, horario, loggedUserEmail)
                        if (reservationId == -1L) {
                            canchaReservandoId = null
                            Toast.makeText(
                                context,
                                "Ese horario ya fue reservado para esa cancha",
                                Toast.LENGTH_LONG
                            ).show()
                            return@AssignmentSection
                        }

                        syncReservationToFirebase(
                            cancha = cancha,
                            fecha = selectedDate,
                            hora = horario,
                            localUserEmail = loggedUserEmail,
                            onSuccess = {
                                canchaReservandoId = null
                                reservaConfirmada = "Reservaste ${cancha.nombre} para el $selectedDate en $horario."
                                reservations = loadReservations()
                                refreshUserReservations()
                                canchasDisponibles = canchasDisponibles.filter { it.id != cancha.id }
                                reservationDetails = mapOf(
                                    "Cancha" to cancha.nombre,
                                    "Fecha" to selectedDate,
                                    "Hora" to horario,
                                    "Correo" to (loggedUserEmail ?: "N/A"),
                                    "Estado" to "Sincronizado con Firebase"
                                )
                                showReservationDialog = true
                                Toast.makeText(context, "✓ Reserva guardada con éxito", Toast.LENGTH_SHORT).show()
                            },
                            onFailure = {
                                canchaReservandoId = null
                                reservaConfirmada = "Reservaste ${cancha.nombre} para el $selectedDate en $horario."
                                reservations = loadReservations()
                                refreshUserReservations()
                                canchasDisponibles = canchasDisponibles.filter { it.id != cancha.id }
                                reservationDetails = mapOf(
                                    "Cancha" to cancha.nombre,
                                    "Fecha" to selectedDate,
                                    "Hora" to horario,
                                    "Correo" to (loggedUserEmail ?: "N/A"),
                                    "Estado" to "Guardado localmente"
                                )
                                showReservationDialog = true
                                Toast.makeText(context, "✓ Reserva guardada localmente", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                )

                AppSection.USERS -> UserManagementSection(
                    loggedUserEmail = loggedUserEmail,
                    isAdminUser = isAdminUser,
                    users = users,
                    reservations = reservations,
                    onToggleAdmin = { email, makeAdmin ->
                        val ok = updateUserAdmin(email, makeAdmin)
                        if (ok) {
                            users = loadUsers()
                            Toast.makeText(
                                context,
                                if (makeAdmin) "Usuario promovido a admin" else "Privilegio admin removido",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(context, "No se pudo actualizar el rol", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onDeleteUser = { email ->
                        val ok = deleteUser(email)
                        if (ok) {
                            users = loadUsers()
                            reservations = loadReservations()
                            refreshUserReservations()
                            Toast.makeText(context, "Usuario eliminado", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "No se puede eliminar ese usuario", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onDeleteReservation = { id ->
                        val ok = deleteReservation(id)
                        if (ok) {
                            reservations = loadReservations()
                            refreshUserReservations()
                            Toast.makeText(context, "Reserva eliminada", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "No se pudo eliminar la reserva", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onRefresh = {
                        users = loadUsers()
                        reservations = loadReservations()
                        refreshUserReservations()
                    }
                )

                AppSection.MY_RESERVATIONS -> MyReservationsSection(
                    loggedUserEmail = loggedUserEmail,
                    reservations = userReservations,
                    onCancelReservation = { id ->
                        val ok = deleteReservation(id)
                        if (ok) {
                            reservations = loadReservations()
                            refreshUserReservations()
                            Toast.makeText(context, "Reserva cancelada", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "No se pudo cancelar la reserva", Toast.LENGTH_SHORT).show()
                        }
                    },
                    formatReservationDateTime = ::formatDateTime
                )
            }
        }

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            selectedDateMillis = datePickerState.selectedDateMillis ?: selectedDateMillis
                            reservaConfirmada = null
                            showDatePicker = false
                        }
                    ) {
                        Text("Aceptar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Cancelar")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        if (showReservationDialog) {
            AlertDialog(
                onDismissRequest = { showReservationDialog = false },
                title = { Text("✓ Reserva Guardada") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        reservationDetails.forEach { entry ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${entry.key}:",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = entry.value,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showReservationDialog = false }) {
                        Text("OK")
                    }
                }
            )
        }

        RightMenuDrawer(
            visible = isMenuOpen,
            currentSection = currentSection,
            loggedUserEmail = loggedUserEmail,
            menuItems = menuItems,
            onDismiss = { isMenuOpen = false },
            onSelectSection = {
                openSection(it)
                isMenuOpen = false
            },
            onLogout = {
                isMenuOpen = false
                onLogout()
            }
        )
    }
}

@Composable
private fun HeaderCard(
    loggedUserEmail: String?,
    isAdminUser: Boolean,
    currentSection: AppSection,
    onMenuClick: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Usuario",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column {
                        Text(
                            text = " Campo Libre Fútbol",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = loggedUserEmail ?: "Sin usuario activo",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (isAdminUser) {
                            Text(
                                text = "Perfil administrador",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                IconButton(onClick = onMenuClick) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Abrir menú"
                    )
                }
            }

            Text(
                text = "Sección actual: ${currentSection.titulo}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun generateHourRanges(): List<String> {
    return (10 until 22).map { startHour ->
        String.format(Locale.getDefault(), "%02d:00 - %02d:00", startHour, startHour + 1)
    }
}

fun formatDate(timeInMillis: Long): String {
    return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(timeInMillis))
}

fun parseDateStringToMillis(dateText: String): Long? {
    if (dateText.isBlank()) return null
    return try {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
            isLenient = false
        }.parse(dateText)?.time
    } catch (_: Exception) {
        null
    }
}

fun dateDaysFromToday(days: Int): String {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_YEAR, days)
    return formatDate(calendar.timeInMillis)
}

fun exportUsersToCsv(context: Context, users: List<AppUser>): File? {
    return runCatching {
        val directory = File(context.getExternalFilesDir(null), "exports").apply { mkdirs() }
        val file = File(directory, "usuarios_${System.currentTimeMillis()}.csv")
        val csv = buildString {
            appendLine("email,creado,intentos_fallidos,bloqueado_hasta,es_admin")
            users.forEach { user ->
                appendLine(
                    listOf(
                        user.email,
                        formatDateTime(user.createdAt),
                        user.failedAttempts.toString(),
                        if (user.lockedUntil > 0) formatDateTime(user.lockedUntil) else "",
                        if (user.isAdmin) "si" else "no"
                    ).joinToString(",") { escapeCsvValue(it) }
                )
            }
        }
        file.writeText(csv)
        file
    }.getOrNull()
}

fun exportReservationsToCsv(context: Context, reservations: List<AppReservation>): File? {
    return runCatching {
        val directory = File(context.getExternalFilesDir(null), "exports").apply { mkdirs() }
        val file = File(directory, "reservas_${System.currentTimeMillis()}.csv")
        val csv = buildString {
            appendLine("id,cancha,tipo,fecha,hora,usuario,precio,guardada")
            reservations.forEach { reservation ->
                appendLine(
                    listOf(
                        reservation.id.toString(),
                        reservation.canchaNombre,
                        reservation.canchaTipo,
                        reservation.fecha,
                        reservation.hora,
                        reservation.usuarioEmail,
                        reservation.precio,
                        formatDateTime(reservation.createdAt)
                    ).joinToString(",") { escapeCsvValue(it) }
                )
            }
        }
        file.writeText(csv)
        file
    }.getOrNull()
}

fun shareCsvFile(context: Context, file: File, chooserTitle: String) {
    runCatching {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, chooserTitle))
    }.onFailure {
        Toast.makeText(context, "No se pudo compartir el archivo", Toast.LENGTH_LONG).show()
    }
}

fun showSavedCsvMessage(context: Context, file: File?, label: String) {
    Toast.makeText(
        context,
        if (file != null) {
            "CSV de $label guardado: ${file.name}"
        } else {
            "No se pudo guardar el CSV de $label"
        },
        Toast.LENGTH_LONG
    ).show()
}

private fun escapeCsvValue(value: String): String {
    val escaped = value.replace("\"", "\"\"")
    return "\"$escaped\""
}

fun formatDateTime(timeInMillis: Long): String {
    return SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(timeInMillis))
}

private fun buildBlockedSlots(): Map<String, Set<String>> {
    fun dayPlus(offset: Int): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, offset)
        return formatDate(calendar.timeInMillis)
    }

    return mapOf(
        "libertadores" to setOf("${dayPlus(0)}|18:00 - 19:00", "${dayPlus(2)}|20:00 - 21:00"),
        "sudamericana" to setOf("${dayPlus(1)}|14:00 - 15:00", "${dayPlus(3)}|19:00 - 20:00"),
        "campeones" to setOf("${dayPlus(1)}|10:00 - 11:00", "${dayPlus(4)}|21:00 - 22:00")
    )
}

private fun obtenerCanchasDisponibles(
    fecha: String,
    hora: String,
    reservations: List<AppReservation>
): List<Cancha> {
    val slot = "$fecha|$hora"
    val horariosBloqueadosPorCancha = buildBlockedSlots()
    val canchaIdsReservadas = reservations
        .asSequence()
        .filter { it.fecha == fecha && it.hora == hora }
        .mapNotNull { reservation ->
            canchasDemo.firstOrNull { cancha -> cancha.nombre == reservation.canchaNombre }?.id
        }
        .toSet()

    return canchasDemo.filter { cancha ->
        val bloqueados = horariosBloqueadosPorCancha[cancha.id].orEmpty()
        slot !in bloqueados && cancha.id !in canchaIdsReservadas
    }
}

private fun syncReservationToFirebase(
    cancha: Cancha,
    fecha: String,
    hora: String,
    localUserEmail: String?,
    onSuccess: () -> Unit,
    onFailure: () -> Unit
) {
    val user = try {
        FirebaseAuth.getInstance().currentUser
    } catch (_: IllegalStateException) {
        onFailure()
        return
    }

    val reserva = hashMapOf(
        "canchaId" to cancha.id,
        "canchaNombre" to cancha.nombre,
        "canchaTipo" to cancha.tipo,
        "fecha" to fecha,
        "hora" to hora,
        "precio" to cancha.precio,
        "usuarioId" to (user?.uid ?: "anonimo"),
        "usuarioEmail" to (localUserEmail ?: user?.email ?: "invitado"),
        "timestamp" to System.currentTimeMillis()
    )

    try {
        FirebaseFirestore
            .getInstance()
            .collection("reservas")
            .add(reserva)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure() }
    } catch (_: IllegalStateException) {
        onFailure()
    }
}

@Preview(showBackground = true, name = "Main Completa")
@Composable
private fun PreviewReserva() {
    CampoLibreFutbolTheme {
        ReservaScreen(
            loggedUserEmail = "admin@campolibre.com",
            isAdminUser = true,
            loadUsers = {
                listOf(
                    AppUser("admin@campolibre.com", System.currentTimeMillis(), 0, 0, true),
                    AppUser("jugador@campolibre.com", System.currentTimeMillis(), 1, 0, false)
                )
            },
            loadReservations = {
                listOf(
                    AppReservation(1, "Cancha Libertadores", "Fútbol 5", formatDate(System.currentTimeMillis()), "18:00 - 19:00", "jugador@campolibre.com", "$35.000", System.currentTimeMillis())
                )
            }
        )
    }
}

@Preview(showBackground = true, name = "Inicio")
@Composable
private fun PreviewHomeSection() {
    CampoLibreFutbolTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HeaderCard(
                loggedUserEmail = "demo@campolibre.com",
                isAdminUser = true,
                currentSection = AppSection.HOME,
                onMenuClick = {}
            )
            HomeSection(
                loggedUserEmail = "demo@campolibre.com",
                totalCanchas = canchasDemo.size,
                totalUsers = 12,
                totalReservations = 8,
                totalMyReservations = 3,
                canManageUsers = true,
                onGoAssign = {},
                onGoMyReservations = {},
                onGoUsers = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Asignar Cancha")
@Composable
private fun PreviewAssignmentSection() {
    CampoLibreFutbolTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HeaderCard(
                loggedUserEmail = "demo@campolibre.com",
                isAdminUser = false,
                currentSection = AppSection.ASSIGN,
                onMenuClick = {}
            )
            AssignmentSection(
                selectedDate = formatDate(System.currentTimeMillis()),
                selectedHourRange = "18:00 - 19:00",
                hourRanges = generateHourRanges(),
                canchasDisponibles = canchasDemo,
                canchaReservandoId = null,
                reservaConfirmada = "Reservaste Cancha Libertadores para hoy en 18:00 - 19:00.",
                onOpenCalendar = {},
                onSelectHour = {},
                onBuscar = {},
                onReservar = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Administración")
@Composable
private fun PreviewUserManagementSection() {
    CampoLibreFutbolTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HeaderCard(
                loggedUserEmail = "admin@campolibre.com",
                isAdminUser = true,
                currentSection = AppSection.USERS,
                onMenuClick = {}
            )
            UserManagementSection(
                loggedUserEmail = "admin@campolibre.com",
                isAdminUser = true,
                users = listOf(
                    AppUser("admin@campolibre.com", System.currentTimeMillis(), 0, 0, true),
                    AppUser("jugador1@campolibre.com", System.currentTimeMillis() - 86_400_000, 1, 0, false)
                ),
                reservations = listOf(
                    AppReservation(1, "Cancha Libertadores", "Fútbol 5", formatDate(System.currentTimeMillis()), "18:00 - 19:00", "jugador1@campolibre.com", "$35.000", System.currentTimeMillis())
                ),
                onToggleAdmin = { _, _ -> },
                onDeleteUser = {},
                onDeleteReservation = {},
                onRefresh = {}
            )
        }
    }
}
