package com.example.medicai.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.medicai.ui.theme.OnGradientLight
import com.example.medicai.ui.theme.UpdateSystemBars
import com.example.medicai.ui.theme.GradientStart
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.medicai.data.models.Medicine
import com.example.medicai.data.models.PredefinedAvatars
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.example.medicai.data.models.Appointment
import com.example.medicai.viewmodel.AuthViewModel
import com.example.medicai.viewmodel.MedicineViewModel
import com.example.medicai.viewmodel.AppointmentViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Pantalla de Inicio Moderna - Dashboard con UI mejorada
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    authViewModel: AuthViewModel,
    medicineViewModel: MedicineViewModel = viewModel(factory = MedicineViewModel.Factory),
    appointmentViewModel: AppointmentViewModel = viewModel(factory = AppointmentViewModel.Factory),
    modifier: Modifier = Modifier
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val medicines by medicineViewModel.medicines.collectAsState()
    val appointments by appointmentViewModel.appointments.collectAsState()

    // Actualizar status bar con color del header (gradiente azul)
    UpdateSystemBars(
        statusBarColor = GradientStart,
        darkIcons = false // Iconos claros sobre gradiente oscuro
    )

    val currentDate = remember {
        SimpleDateFormat("EEEE, dd 'de' MMMM", Locale("es", "ES")).format(Date())
    }

    val currentTime = remember {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    }

    // Cargar datos al iniciar
    LaunchedEffect(currentUser) {
        currentUser?.id?.let { userId ->
            medicineViewModel.loadMedicines(userId)
            appointmentViewModel.loadUpcomingAppointments(userId)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Activar sensor de luz ambiental para ajustar brillo automáticamente
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 180.dp, bottom = 0.dp)
        ) {
            // Cards de estadísticas
            item {
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                    ) + fadeIn()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))
                        StatsCards(
                            medicinesCount = medicines.count { it.active },
                            appointmentsCount = appointments.size,
                            activeReminders = medicines.count { it.active } + appointments.size
                        )
                    }
                }
            }

            // Próximos medicamentos
            item {
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                    ) + fadeIn()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(24.dp))
                        SectionHeader(
                            title = "Próximas Tomas",
                            subtitle = "${medicines.count { it.active }} medicamentos activos",
                            icon = Icons.Filled.Medication
                        )
                    }
                }
            }

            if (medicines.isEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    EmptyStateCard(
                        icon = Icons.Filled.MedicalServices,
                        title = "No hay medicamentos",
                        description = "Agrega tus medicamentos en la pestaña Medicamentos",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        val activeMedicines = medicines.filter { it.active }.take(5)
                        items(activeMedicines) { medicine ->
                            ModernMedicineCard(medicine = medicine)
                        }
                    }
                }
            }

            // Próximas citas
            item {
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                    ) + fadeIn()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(24.dp))
                        SectionHeader(
                            title = "Próximas Citas",
                            subtitle = "${appointments.size} citas programadas",
                            icon = Icons.Filled.CalendarMonth
                        )
                    }
                }
            }

            if (appointments.isEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    EmptyStateCard(
                        icon = Icons.Filled.EventNote,
                        title = "No hay citas",
                        description = "Agenda tus citas en la pestaña Citas",
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            } else {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
                appointments.take(3).forEach { appointment ->
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        ModernAppointmentCard(appointment = appointment)
                    }
                }
            }


            // Información del usuario
            currentUser?.let { user ->
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    UserInfoCard(user = user)
                }
            }
        }
        
        // Header sticky como overlay
        ModernStickyHeader(
            userName = currentUser?.full_name?.split(" ")?.firstOrNull() ?: "Usuario",
            currentDate = currentDate,
            currentTime = currentTime,
            avatarUrl = currentUser?.avatar_url,
            activeMedicinesCount = medicines.count { it.active },
            nextAppointment = appointments.minByOrNull { it.date }
        )
    }
}

@Composable
private fun ModernStickyHeader(
    userName: String,
    currentDate: String,
    currentTime: String,
    avatarUrl: String?,
    activeMedicinesCount: Int,
    nextAppointment: Appointment?
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
            )
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        com.example.medicai.ui.theme.GradientStart,
                        com.example.medicai.ui.theme.GradientMid,
                        com.example.medicai.ui.theme.GradientEnd
                    )
                ),
                shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 48.dp, bottom = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Hola, $userName",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnGradientLight
                    )
                    Text(
                        text = currentDate.capitalize(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = OnGradientLight.copy(alpha = 0.9f)
                    )
                }

                // Avatar
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    border = BorderStroke(2.dp, OnGradientLight.copy(alpha = 0.5f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (avatarUrl != null && PredefinedAvatars.isEmojiAvatar(avatarUrl)) {
                            // Mostrar emoji
                            Text(
                                text = avatarUrl,
                                style = MaterialTheme.typography.headlineLarge,
                                modifier = Modifier.padding(4.dp)
                            )
                        } else if (avatarUrl != null && avatarUrl.startsWith("http")) {
                            // Mostrar imagen desde URL
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(avatarUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Avatar del usuario",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = "Profile",
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Estado dinámico
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                val (statusIcon, statusText) = when {
                    activeMedicinesCount > 0 -> Pair(Icons.Filled.Medication, "$activeMedicinesCount medicamentos activos")
                    nextAppointment != null -> Pair(Icons.Filled.CalendarMonth, "Próxima cita: ${nextAppointment.date}")
                    else -> Pair(Icons.Filled.CheckCircle, "Todo está bajo control")
                }

                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    tint = OnGradientLight,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = currentTime,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = OnGradientLight
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "• $statusText",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnGradientLight.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun StatsCards(
    medicinesCount: Int,
    appointmentsCount: Int,
    activeReminders: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Filled.Medication,
            value = medicinesCount.toString(),
            label = "Medicamentos",
            color = MaterialTheme.colorScheme.primary
        )
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Filled.CalendarMonth,
            value = appointmentsCount.toString(),
            label = "Citas",
            color = MaterialTheme.colorScheme.secondary
        )
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Filled.Notifications,
            value = activeReminders.toString(),
            label = "Activos",
            color = MaterialTheme.colorScheme.tertiary
        )
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    value: String,
    label: String,
    color: Color
) {
    ElevatedCard(
        modifier = modifier,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Visible,
                softWrap = false,
                letterSpacing = (-0.3).sp
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    MaterialTheme.colorScheme.primaryContainer,
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ModernMedicineCard(medicine: Medicine) {
    ElevatedCard(
        modifier = Modifier
            .width(280.dp)
            .height(160.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Gradiente decorativo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .padding(top = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Medication,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (medicine.active)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = if (medicine.active) "Activo" else "Inactivo",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (medicine.active)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = medicine.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )

                Text(
                    text = medicine.dosage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.weight(1f))

                HorizontalDivider()

                Spacer(modifier = Modifier.height(8.dp))

                // Calcular próxima toma
                val currentTime = remember {
                    val cal = java.util.Calendar.getInstance()
                    val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
                    val minute = cal.get(java.util.Calendar.MINUTE)
                    hour * 60 + minute // minutos desde medianoche
                }
                
                // ✅ Optimizado con derivedStateOf para evitar recomposiciones innecesarias
                val nextDose by remember {
                    derivedStateOf {
                        medicine.times
                            .map { timeStr ->
                                val parts = timeStr.split(":")
                                val hour = parts.getOrNull(0)?.toIntOrNull() ?: 0
                                val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
                                hour * 60 + minute
                            }
                            .filter { it >= currentTime }
                            .minOrNull() ?: medicine.times.firstOrNull()?.let { timeStr ->
                                val parts = timeStr.split(":")
                                val hour = parts.getOrNull(0)?.toIntOrNull() ?: 0
                                val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
                                hour * 60 + minute
                            } ?: 0
                    }
                }
                
                val nextDoseTime by remember {
                    derivedStateOf {
                        val dose = nextDose
                        val hour = dose / 60
                        val minute = dose % 60
                        String.format("%02d:%02d", hour, minute)
                    }
                }
                
                val timeUntilNext by remember {
                    derivedStateOf {
                        val dose = nextDose
                        val diff = if (dose >= currentTime) {
                            dose - currentTime
                        } else {
                            (24 * 60) - currentTime + dose
                        }
                        val hours = diff / 60
                        val minutes = diff % 60
                        when {
                            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
                            hours > 0 -> "${hours}h"
                            else -> "${minutes}m"
                        }
                    }
                }

                // Información de próxima toma
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.AccessTime,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Próxima: $nextDoseTime",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = "En $timeUntilNext",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Mostrar número de tomas al día
                    val dosesPerDay = medicine.times.size
                    if (dosesPerDay > 0) {
                        Text(
                            text = "${dosesPerDay}x día",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernAppointmentCard(appointment: Appointment) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
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
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            MaterialTheme.colorScheme.secondaryContainer,
                            RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.MedicalServices,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = appointment.doctor_name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = appointment.specialty,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = appointment.location,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = appointment.date.split("-").getOrNull(2) ?: "--",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = appointment.date.split("-").getOrNull(1)?.let {
                                when(it) {
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
                            } ?: "---",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = appointment.time,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
private fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    description: String,
    color: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = color
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun UserInfoCard(user: com.example.medicai.data.models.UserProfile) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Configuración",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Preferencias de la cuenta",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            InfoRow(
                icon = Icons.Filled.Notifications,
                label = "Notificaciones",
                value = if (user.notifications_enabled) "Activadas" else "Desactivadas",
                color = if (user.notifications_enabled)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(12.dp))

            InfoRow(
                icon = Icons.Filled.AccessTime,
                label = "Recordatorio",
                value = "${user.reminder_minutes} min antes",
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = color
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

