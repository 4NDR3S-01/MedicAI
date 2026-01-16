package com.example.medicai.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider
import android.widget.Toast
import com.example.medicai.data.models.Appointment
import com.example.medicai.data.models.AppointmentRequest
import com.example.medicai.viewmodel.AuthViewModel
import com.example.medicai.viewmodel.AppointmentViewModel
import com.example.medicai.sensors.LocationPickerDialog
import com.example.medicai.ui.theme.GradientStartSecondary
import com.example.medicai.ui.theme.GradientEndSecondary
import com.example.medicai.ui.theme.OnGradientLight
import com.example.medicai.ui.theme.UpdateSystemBars
import java.text.SimpleDateFormat
import java.util.*

/**
 * Pantalla Moderna de Gestión de Citas Médicas
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentsScreen(
    authViewModel: AuthViewModel,
    appointmentViewModel: AppointmentViewModel = viewModel(factory = AppointmentViewModel.Factory),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUser by authViewModel.currentUser.collectAsState()
    val appointments by appointmentViewModel.appointments.collectAsState()
    val isLoading by appointmentViewModel.isLoading.collectAsState()
    val error by appointmentViewModel.error.collectAsState()
    val successMessage by appointmentViewModel.successMessage.collectAsState()

    // Actualizar status bar con color del header (gradiente verde)
    UpdateSystemBars(
        statusBarColor = GradientStartSecondary,
        darkIcons = false
    )

    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var showEditDialog by rememberSaveable { mutableStateOf(false) }
    var appointmentToEdit by remember { mutableStateOf<Appointment?>(null) }
    var selectedFilter by rememberSaveable { mutableStateOf("Próximas") }

    /** ──────────────────────────── Carga inicial ───────────────────────────── */
    LaunchedEffect(currentUser) {
        currentUser?.id?.let(appointmentViewModel::loadAppointments)
    }

    /** ──────────────────────────── Notificaciones ─────────────────────────── */
    LaunchedEffect(successMessage) {
        successMessage?.let {
            Toast.makeText(context, "✅ $it", Toast.LENGTH_SHORT).show()
            appointmentViewModel.clearSuccessMessage()
        }
    }

    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, "❌ $it", Toast.LENGTH_LONG).show()
            appointmentViewModel.clearError()
        }
    }

    /** ─────────────────────────── Filtro de citas ─────────────────────────── */
    val today = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    val filteredAppointments = remember(appointments, selectedFilter) {
        when (selectedFilter) {
            "Próximas"     -> appointments.filter { it.status == "scheduled" && it.date >= today }
            "Pasadas"      -> appointments.filter { (it.date < today) || it.status == "completed" }
            "Completadas"  -> appointments.filter { it.status == "completed" }
            "Canceladas"   -> appointments.filter { it.status == "cancelled" }
            else           -> appointments
        }
    }

    /** ────────────────────────────── UI Base ──────────────────────────────── */
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(Modifier.fillMaxSize()) {

            ModernAppointmentHeader(
                totalCount = appointments.size,
                upcomingCount = appointments.count { it.status == "scheduled" && it.date >= today }
            )

            AppointmentFilterChips(
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it },
                counts = mapOf(
                    "Próximas"     to appointments.count { it.status == "scheduled" && it.date >= today },
                    "Completadas"  to appointments.count { it.status == "completed" },
                    "Pasadas"      to appointments.count { (it.date < today) || it.status == "completed" },
                    "Canceladas"   to appointments.count { it.status == "cancelled" }
                )
            )

            /** ────────────── Contenido dinámico según estado ────────────── */
            when {
                isLoading -> LoadingAppointmentsState()

                error != null -> com.example.medicai.ui.components.ErrorState(
                    message = error ?: "Error desconocido",
                    onRetry = { currentUser?.id?.let(appointmentViewModel::loadAppointments) },
                    modifier = Modifier.fillMaxSize()
                )

                filteredAppointments.isEmpty() -> EmptyAppointmentsState(
                    filter = selectedFilter,
                    onAddClick = { showAddDialog = true }
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 12.dp,
                        bottom = 100.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        filteredAppointments,
                        key = { it.id } // evita recreación de key en cada render
                    ) { appointment ->
                        AnimatedAppointmentCard(
                            appointment = appointment,
                            onEdit = {
                                appointmentToEdit = appointment
                                showEditDialog = true
                            },
                            onCancel = { currentUser?.id?.let { id -> appointmentViewModel.cancelAppointment(appointment.id, id) } },
                            onComplete = { currentUser?.id?.let { id -> appointmentViewModel.completeAppointment(appointment.id, id) } }
                        )
                    }
                }
            }
        }

        /** ───────────────────── FAB ───────────────────── */
        if (!isLoading && filteredAppointments.isNotEmpty()) {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp),
                containerColor = MaterialTheme.colorScheme.secondary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Agregar cita",
                    tint = MaterialTheme.colorScheme.onSecondary
                )
            }
        }
    }

    /** ────────────────────────── Diálogos ───────────────────────────── */
    if (showAddDialog) {
        currentUser?.let { user ->
            ModernAddAppointmentDialog(
                userId = user.id,
                onDismiss = { showAddDialog = false },
                onConfirm = {
                    appointmentViewModel.addAppointment(it) { showAddDialog = false }
                }
            )
        }
    }

    if (showEditDialog && appointmentToEdit != null) {
        ModernEditAppointmentDialog(
            appointment = appointmentToEdit!!,
            onDismiss = {
                showEditDialog = false
                appointmentToEdit = null
            },
            onConfirm = {
                appointmentViewModel.updateAppointment(appointmentToEdit!!.id, it) {
                    showEditDialog = false
                    appointmentToEdit = null
                }
            }
        )
    }
}


@Composable
private fun ModernAppointmentHeader(
    totalCount: Int,
    upcomingCount: Int
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        com.example.medicai.ui.theme.GradientStartSecondary,
                        com.example.medicai.ui.theme.GradientEndSecondary
                    )
                ),
                shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CalendarMonth,
                            contentDescription = null,
                            tint = OnGradientLight,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Mis Citas Médicas",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = OnGradientLight
                        )
                        Text(
                            text = "Gestiona tus consultas",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnGradientLight.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatChip(
                    modifier = Modifier.weight(1f),
                    label = "Total",
                    value = totalCount.toString(),
                    icon = Icons.Filled.EventNote,
                    color = MaterialTheme.colorScheme.secondary
                )
                StatChip(
                    modifier = Modifier.weight(1f),
                    label = "Próximas",
                    value = upcomingCount.toString(),
                    icon = Icons.Filled.Event,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
private fun StatChip(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Surface(
        modifier = modifier.wrapContentHeight(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium.copy(
                        platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                            includeFontPadding = false
                        )
                    ),
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall.copy(
                        platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                            includeFontPadding = false
                        )
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AppointmentFilterChips(
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    counts: Map<String, Int>
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf("Próximas", "Completadas", "Pasadas", "Canceladas").forEach { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = {
                    Text(
                        text = "$filter (${counts[filter] ?: 0})",
                        fontWeight = if (selectedFilter == filter) FontWeight.Bold else FontWeight.Normal
                    )
                },
                leadingIcon = if (selectedFilter == filter) {
                    {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.secondary
                )
            )
        }
    }
}

@Composable
private fun AnimatedAppointmentCard(
    appointment: Appointment,
    onEdit: () -> Unit,
    onCancel: () -> Unit,
    onComplete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val statusColor = when (appointment.status) {
        "scheduled" -> com.example.medicai.ui.theme.AppointmentScheduledLight
        "completed" -> com.example.medicai.ui.theme.AppointmentCompletedLight
        "cancelled" -> com.example.medicai.ui.theme.AppointmentCancelledLight
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val statusText = when (appointment.status) {
        "scheduled" -> "Programada"
        "completed" -> "Completada"
        "cancelled" -> "Cancelada"
        else -> "Desconocido"
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            .clip(RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp,
            hoveredElevation = 3.dp
        ),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
        ) {
            // Barra superior con color de estado
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                statusColor,
                                statusColor.copy(alpha = 0.6f)
                            )
                        ),
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    )
            )

            // Contenido principal
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Calendario visual
                Surface(
                    modifier = Modifier.size(72.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = appointment.date.split("-").getOrNull(2) ?: "??",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                        Text(
                            text = getMonthName(appointment.date),
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Información de la cita
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = appointment.doctor_name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = statusColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = statusText,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.MedicalServices,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = appointment.specialty,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.AccessTime,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = appointment.time,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = appointment.location,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Botón expandir
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (expanded) "Colapsar" else "Expandir",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Contenido expandido
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp)
                ) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    if (!appointment.notes.isNullOrBlank()) {
                        InfoSection(
                            icon = Icons.Filled.Notes,
                            title = "Notas",
                            content = appointment.notes
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Información completa de ubicación
                    InfoSection(
                        icon = Icons.Filled.LocationOn,
                        title = "Ubicación completa",
                        content = appointment.location
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Botones de acción según el estado
                    when (appointment.status) {
                        "scheduled" -> {
                            // Cita programada: Editar, Completar, Cancelar
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = onEdit,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Edit,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Editar")
                                    }

                                    Button(
                                        onClick = onComplete,
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                            contentColor = MaterialTheme.colorScheme.tertiary
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Completar")
                                    }
                                }

                                Button(
                                    onClick = onCancel,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Cancel,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Cancelar Cita")
                                }
                            }
                        }
                        "completed" -> {
                            // Cita completada: Solo editar
                            OutlinedButton(
                                onClick = onEdit,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Editar Información")
                            }
                        }
                        "cancelled" -> {
                            // Cita cancelada: Editar o eliminar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onEdit,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Editar")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoSection(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    content: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun LoadingAppointmentsState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                strokeWidth = 4.dp
            )
            Text(
                text = "Cargando citas...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyAppointmentsState(
    filter: String,
    onAddClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.EventNote,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
                )
            }

            Text(
                text = when (filter) {
                    "Próximas" -> "No hay citas próximas"
                    "Pasadas" -> "No hay citas pasadas"
                    "Canceladas" -> "No hay citas canceladas"
                    else -> "No hay citas"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = if (filter == "Todas" || filter == "Próximas") {
                    "Comienza agendando tu primera cita médica"
                } else {
                    "Intenta cambiar el filtro"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            if (filter == "Todas" || filter == "Próximas") {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onAddClick,
                    modifier = Modifier
                        .height(56.dp)
                        .widthIn(min = 200.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Agendar Cita",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun ModernFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.size(64.dp),
        shape = CircleShape,
        containerColor = MaterialTheme.colorScheme.secondary,
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 8.dp,
            pressedElevation = 12.dp
        )
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = "Agregar cita",
            modifier = Modifier.size(28.dp),
            tint = MaterialTheme.colorScheme.onSecondary
        )
    }
}

// Función helper para obtener nombre del mes
private fun getMonthName(date: String): String {
    val month = date.split("-").getOrNull(1) ?: return "---"
    return when(month) {
        "01" -> "ENE"
        "02" -> "FEB"
        "03" -> "MAR"
        "04" -> "ABR"
        "05" -> "MAY"
        "06" -> "JUN"
        "07" -> "JUL"
        "08" -> "AGO"
        "09" -> "SEP"
        "10" -> "OCT"
        "11" -> "NOV"
        "12" -> "DIC"
        else -> "---"
    }
}

// Los diálogos ModernAddAppointmentDialog y ModernEditAppointmentDialog
// se mantienen igual que antes, solo cambio los nombres para consistencia

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModernAddAppointmentDialog(
    userId: String,
    onDismiss: () -> Unit,
    onConfirm: (AppointmentRequest) -> Unit
) {
    var doctorName by rememberSaveable { mutableStateOf("") }
    var specialty by rememberSaveable { mutableStateOf("") }
    var selectedDateMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var selectedHour by rememberSaveable { mutableStateOf(14) }
    var selectedMinute by rememberSaveable { mutableStateOf(0) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var location by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var showLocationPicker by rememberSaveable { mutableStateOf(false) }

    val dateFormatted = remember(selectedDateMillis) {
        selectedDateMillis?.let { millis ->
            // ✅ Usar TimeZone UTC porque DatePicker devuelve millis en UTC
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            calendar.timeInMillis = millis
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            val month = calendar.get(Calendar.MONTH) + 1
            val year = calendar.get(Calendar.YEAR)
            String.format("%02d/%02d/%04d", day, month, year)
        } ?: "Seleccionar fecha"
    }

    // Crear time desde hora y minuto seleccionados
    val time = remember(selectedHour, selectedMinute) {
        "${selectedHour.toString().padStart(2, '0')}:${selectedMinute.toString().padStart(2, '0')}"
    }

    val isValid = doctorName.isNotBlank() && specialty.isNotBlank() &&
                  selectedDateMillis != null && location.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.EventNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Agendar Cita Médica",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = doctorName,
                        onValueChange = { doctorName = it },
                        label = { Text("Nombre del médico *") },
                        leadingIcon = {
                            Icon(Icons.Filled.Person, contentDescription = "Nombre del doctor")
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = specialty,
                        onValueChange = { specialty = it },
                        label = { Text("Especialidad *") },
                        leadingIcon = {
                            Icon(Icons.Filled.MedicalServices, contentDescription = "Especialidad")
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Date Picker Visual
                item {
                    Text(
                        text = "Fecha de la cita",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                item {
                    OutlinedCard(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.CalendarToday,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Fecha seleccionada",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = dateFormatted,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedDateMillis != null)
                                            MaterialTheme.colorScheme.secondary
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Filled.Event,
                                contentDescription = "Cambiar fecha",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }

                // Time Picker Visual
                item {
                    Text(
                        text = "Hora de la cita",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                item {
                    OutlinedCard(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.AccessTime,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Hora seleccionada",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = time,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Filled.Schedule,
                                contentDescription = "Cambiar hora",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }

                // Selector de ubicación
                item {
                    Text(
                        text = "Ubicación / Lugar *",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                item {
                    OutlinedCard(
                        onClick = { showLocationPicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = if (location.isBlank()) "Seleccionar ubicación" else "Ubicación seleccionada",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (location.isNotBlank()) {
                                        Text(
                                            text = location,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary,
                                            maxLines = 2
                                        )
                                    } else {
                                        Text(
                                            text = "Toca para buscar",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Buscar ubicación",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notas (opcional)") },
                        leadingIcon = {
                            Icon(Icons.Filled.Notes, contentDescription = "Notas")
                        },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Convertir milisegundos a fecha yyyy-MM-dd usando UTC (DatePicker usa UTC)
                    val formattedDate = selectedDateMillis?.let { millis ->
                        // ✅ Usar TimeZone UTC porque DatePicker devuelve millis en UTC
                        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                        calendar.timeInMillis = millis
                        val year = calendar.get(Calendar.YEAR)
                        val month = calendar.get(Calendar.MONTH) + 1
                        val day = calendar.get(Calendar.DAY_OF_MONTH)
                        String.format("%04d-%02d-%02d", year, month, day)
                    } ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                    val appointmentRequest = AppointmentRequest(
                        user_id = userId,
                        doctor_name = doctorName.trim(),
                        specialty = specialty.trim(),
                        date = formattedDate,
                        time = time.trim(),
                        location = location.trim(),
                        notes = notes.trim().ifBlank { null },
                        status = "scheduled"
                    )
                    onConfirm(appointmentRequest)
                },
                enabled = isValid,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Agendar")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancelar")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )

    // Date Picker Dialog
    if (showDatePicker) {
        AppointmentDatePickerDialog(
            initialDateMillis = selectedDateMillis,
            onDismiss = { showDatePicker = false },
            onConfirm = { dateMillis: Long ->
                selectedDateMillis = dateMillis
                showDatePicker = false
            }
        )
    }

    // Time Picker Dialog
    if (showTimePicker) {
        AppointmentTimePickerDialog(
            initialHour = selectedHour,
            initialMinute = selectedMinute,
            onDismiss = { showTimePicker = false },
            onConfirm = { hour, minute ->
                selectedHour = hour
                selectedMinute = minute
                showTimePicker = false
            }
        )
    }

    // Location Picker Dialog
    if (showLocationPicker) {
        LocationPickerDialog(
            currentLocation = location,
            onDismiss = { showLocationPicker = false },
            onLocationSelected = { selectedLocation ->
                location = selectedLocation
                showLocationPicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModernEditAppointmentDialog(
    appointment: Appointment,
    onDismiss: () -> Unit,
    onConfirm: (AppointmentRequest) -> Unit
) {
    // Convertir fecha de yyyy-MM-dd a milisegundos
    val initialDateMillis = remember {
        try {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(appointment.date)?.time
        } catch (e: Exception) {
            null
        }
    }

    var selectedDateMillis by rememberSaveable { mutableStateOf(initialDateMillis) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    // Extraer hora y minuto inicial
    val initialTimeParts = appointment.time.split(":")
    var selectedHour by rememberSaveable { mutableStateOf(initialTimeParts[0].toIntOrNull() ?: 14) }
    var selectedMinute by rememberSaveable { mutableStateOf(initialTimeParts.getOrNull(1)?.toIntOrNull() ?: 0) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var showLocationPicker by rememberSaveable { mutableStateOf(false) }

    var doctorName by rememberSaveable { mutableStateOf(appointment.doctor_name) }
    var specialty by rememberSaveable { mutableStateOf(appointment.specialty) }
    var location by rememberSaveable { mutableStateOf(appointment.location) }
    var notes by rememberSaveable { mutableStateOf(appointment.notes ?: "") }

    // Crear date desde milisegundos seleccionados (usando UTC para evitar desfase)
    val dateFormatted = remember(selectedDateMillis) {
        selectedDateMillis?.let { millis ->
            val calendar = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
            calendar.timeInMillis = millis
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            val month = calendar.get(Calendar.MONTH) + 1
            val year = calendar.get(Calendar.YEAR)
            String.format("%02d/%02d/%04d", day, month, year)
        } ?: "Seleccionar fecha"
    }

    // Crear time desde hora y minuto seleccionados
    val time = remember(selectedHour, selectedMinute) {
        "${selectedHour.toString().padStart(2, '0')}:${selectedMinute.toString().padStart(2, '0')}"
    }

    val isValid = doctorName.isNotBlank() && specialty.isNotBlank() &&
                  selectedDateMillis != null && location.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Editar Cita",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = doctorName,
                        onValueChange = { doctorName = it },
                        label = { Text("Nombre del médico *") },
                        leadingIcon = {
                            Icon(Icons.Filled.Person, contentDescription = "Nombre del doctor")
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = specialty,
                        onValueChange = { specialty = it },
                        label = { Text("Especialidad *") },
                        leadingIcon = {
                            Icon(Icons.Filled.MedicalServices, contentDescription = "Especialidad")
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Date Picker Visual
                item {
                    Text(
                        text = "Fecha de la cita",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                item {
                    OutlinedCard(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.CalendarToday,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Fecha seleccionada",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = dateFormatted,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedDateMillis != null)
                                            MaterialTheme.colorScheme.secondary
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Filled.Event,
                                contentDescription = "Cambiar fecha",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }

                // Time Picker Visual
                item {
                    Text(
                        text = "Hora de la cita",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                item {
                    OutlinedCard(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.AccessTime,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Hora seleccionada",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = time,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Filled.Schedule,
                                contentDescription = "Cambiar hora",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }

                // Selector de ubicación
                item {
                    Text(
                        text = "Ubicación / Lugar *",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                item {
                    OutlinedCard(
                        onClick = { showLocationPicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = if (location.isBlank()) "Seleccionar ubicación" else "Ubicación seleccionada",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (location.isNotBlank()) {
                                        Text(
                                            text = location,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary,
                                            maxLines = 2
                                        )
                                    } else {
                                        Text(
                                            text = "Toca para buscar",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Buscar ubicación",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notas (opcional)") },
                        leadingIcon = {
                            Icon(Icons.Filled.Notes, contentDescription = "Notas")
                        },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Convertir milisegundos a fecha yyyy-MM-dd (SOLUCIÓN DEFINITIVA)
                    val formattedDate = selectedDateMillis?.let { millis ->
                        // Usar Calendar en UTC para extraer los componentes de fecha directamente
                        val calendar = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
                        calendar.timeInMillis = millis
                        val year = calendar.get(Calendar.YEAR)
                        val month = calendar.get(Calendar.MONTH) + 1 // Calendar.MONTH es 0-based
                        val day = calendar.get(Calendar.DAY_OF_MONTH)
                        String.format("%04d-%02d-%02d", year, month, day)
                    } ?: appointment.date

                    val appointmentRequest = AppointmentRequest(
                        user_id = appointment.user_id,
                        doctor_name = doctorName.trim(),
                        specialty = specialty.trim(),
                        date = formattedDate,
                        time = time.trim(),
                        location = location.trim(),
                        notes = notes.trim().ifBlank { null },
                        status = appointment.status
                    )
                    onConfirm(appointmentRequest)
                },
                enabled = isValid,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Save,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Guardar Cambios")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancelar")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )

    // Date Picker Dialog
    if (showDatePicker) {
        AppointmentDatePickerDialog(
            initialDateMillis = selectedDateMillis,
            onDismiss = { showDatePicker = false },
            onConfirm = { dateMillis: Long ->
                selectedDateMillis = dateMillis
                showDatePicker = false
            }
        )
    }

    // Time Picker Dialog
    if (showTimePicker) {
        AppointmentTimePickerDialog(
            initialHour = selectedHour,
            initialMinute = selectedMinute,
            onDismiss = { showTimePicker = false },
            onConfirm = { hour: Int, minute: Int ->
                selectedHour = hour
                selectedMinute = minute
                showTimePicker = false
            }
        )
    }

    // Location Picker Dialog
    if (showLocationPicker) {
        LocationPickerDialog(
            currentLocation = location,
            onDismiss = { showLocationPicker = false },
            onLocationSelected = { selectedLocation ->
                location = selectedLocation
                showLocationPicker = false
            }
        )
    }
}

// Componente DatePickerDialog para Citas
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentDatePickerDialog(
    initialDateMillis: Long? = null,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDateMillis ?: System.currentTimeMillis()
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    datePickerState.selectedDateMillis?.let { onConfirm(it) }
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    ) {
        DatePicker(
            state = datePickerState,
            colors = DatePickerDefaults.colors(
                selectedDayContainerColor = MaterialTheme.colorScheme.secondary,
                todayDateBorderColor = MaterialTheme.colorScheme.secondary,
                todayContentColor = MaterialTheme.colorScheme.secondary
            )
        )
    }
}

// Componente TimePickerDialog para Citas
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Selecciona la hora",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TimePicker(
                    state = timePickerState,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectorColor = MaterialTheme.colorScheme.secondary,
                        timeSelectorSelectedContainerColor = MaterialTheme.colorScheme.secondary,
                        timeSelectorUnselectedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(timePickerState.hour, timePickerState.minute)
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Confirmar")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancelar")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

