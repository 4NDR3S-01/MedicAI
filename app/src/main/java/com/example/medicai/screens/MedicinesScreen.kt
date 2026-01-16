package com.example.medicai.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider
import android.widget.Toast
import com.example.medicai.data.models.Medicine
import com.example.medicai.data.models.MedicineRequest
import com.example.medicai.viewmodel.AuthViewModel
import com.example.medicai.viewmodel.MedicineViewModel
import com.example.medicai.ui.theme.GradientStart
import com.example.medicai.ui.theme.OnGradientLight
import com.example.medicai.ui.theme.UpdateSystemBars
import java.text.SimpleDateFormat
import java.util.*

/**
 * Pantalla Moderna de Gestión de Medicamentos
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicinesScreen(
    authViewModel: AuthViewModel,
    medicineViewModel: MedicineViewModel = viewModel(factory = MedicineViewModel.Factory),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUser by authViewModel.currentUser.collectAsState()
    val medicines by medicineViewModel.medicines.collectAsState()
    val isLoading by medicineViewModel.isLoading.collectAsState()
    val error by medicineViewModel.error.collectAsState()
    val successMessage by medicineViewModel.successMessage.collectAsState()

    // Actualizar status bar con color del header (gradiente azul)
    UpdateSystemBars(
        statusBarColor = GradientStart,
        darkIcons = false
    )

    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var showEditDialog by rememberSaveable { mutableStateOf(false) }
    var medicineToEdit by remember { mutableStateOf<Medicine?>(null) }
    var selectedFilter by rememberSaveable { mutableStateOf("Todos") }

    // Cargar medicamentos al iniciar
    LaunchedEffect(currentUser) {
        currentUser?.id?.let { userId ->
            medicineViewModel.loadMedicines(userId)
        }
    }

    // Mostrar mensajes de éxito
    LaunchedEffect(successMessage) {
        successMessage?.let {
            Toast.makeText(context, "✅ $it", Toast.LENGTH_SHORT).show()
            medicineViewModel.clearSuccessMessage()
        }
    }

    // Mostrar mensajes de error
    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, "❌ $it", Toast.LENGTH_LONG).show()
            medicineViewModel.clearError()
        }
    }

    // Filtrar medicamentos
    val filteredMedicines = when (selectedFilter) {
        "Activos" -> medicines.filter { it.active }
        "Inactivos" -> medicines.filter { !it.active }
        else -> medicines
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header moderno con gradiente
            ModernMedicineHeader(
                totalCount = medicines.size,
                activeCount = medicines.count { it.active }
            )

            // Filtros
            FilterChips(
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it }
            )

            // Contenido
            when {
                isLoading -> LoadingState()
                error != null -> {
                    com.example.medicai.ui.components.ErrorState(
                        message = error ?: "Error desconocido",
                        onRetry = {
                            currentUser?.id?.let { userId ->
                                medicineViewModel.loadMedicines(userId)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                filteredMedicines.isEmpty() -> EmptyMedicinesState(
                    filter = selectedFilter,
                    onAddClick = { showAddDialog = true }
                )
                else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 12.dp,
                        bottom = 100.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = filteredMedicines,
                        key = { it.id.ifEmpty { UUID.randomUUID().toString() } }
                    ) { medicine ->
                        AnimatedMedicineCard(
                            medicine = medicine,
                            onEdit = {
                                medicineToEdit = medicine
                                showEditDialog = true
                            },
                            onDelete = {
                                currentUser?.id?.let { userId ->
                                    if (medicine.active) {
                                        // Si está activo, solo desactivar (soft delete)
                                        medicineViewModel.deleteMedicine(medicine.id, userId, medicine.name)
                                    } else {
                                        // Si está inactivo, eliminar permanentemente (hard delete)
                                        medicineViewModel.deletePermanently(medicine.id, userId, medicine.name)
                                    }
                                }
                            },
                            onReactivate = {
                                currentUser?.id?.let { userId ->
                                    medicineViewModel.reactivateMedicine(medicine.id, userId, medicine.name)
                                }
                            }
                        )
                    }
                }
                }
            }
        }

        // FAB moderno flotante - Solo mostrar cuando hay medicamentos o no está en estado vacío
        if (!isLoading && filteredMedicines.isNotEmpty()) {
            ModernFloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
            )
        }
    }

    // Diálogo para agregar medicamento
    if (showAddDialog) {
        currentUser?.let { user ->
            ModernAddMedicineDialog(
                userId = user.id,
                onDismiss = { showAddDialog = false },
                onConfirm = { medicineRequest ->
                    medicineViewModel.addMedicine(medicineRequest) {
                        showAddDialog = false
                    }
                }
            )
        }
    }

    // Diálogo para editar medicamento
    if (showEditDialog && medicineToEdit != null) {
        currentUser?.let { user ->
            ModernEditMedicineDialog(
                medicine = medicineToEdit!!,
                onDismiss = {
                    showEditDialog = false
                    medicineToEdit = null
                },
                onConfirm = { medicineRequest ->
                    medicineViewModel.updateMedicine(
                        id = medicineToEdit!!.id,
                        medicine = medicineRequest
                    ) {
                        showEditDialog = false
                        medicineToEdit = null
                    }
                }
            )
        }
    }
}

@Composable
private fun ModernMedicineHeader(
    totalCount: Int,
    activeCount: Int
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
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
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Medication,
                            contentDescription = null,
                            tint = OnGradientLight,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Mis Medicamentos",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = OnGradientLight
                        )
                        Text(
                            text = "Gestiona tus tratamientos",
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
                    icon = Icons.Filled.MedicalServices,
                    color = MaterialTheme.colorScheme.primary
                )
                StatChip(
                    modifier = Modifier.weight(1f),
                    label = "Activos",
                    value = activeCount.toString(),
                    icon = Icons.Filled.CheckCircle,
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
private fun FilterChips(
    selectedFilter: String,
    onFilterSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf("Todos", "Activos", "Inactivos").forEach { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = {
                    Text(
                        text = filter,
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
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

@Composable
private fun AnimatedMedicineCard(
    medicine: Medicine,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onReactivate: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

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
            // Barra superior con color
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = if (medicine.active) {
                                listOf(
                                    com.example.medicai.ui.theme.SuccessLight,
                                    com.example.medicai.ui.theme.SecondaryLight
                                )
                            } else {
                                listOf(
                                    com.example.medicai.ui.theme.MedicineInactiveLight.copy(alpha = 0.5f),
                                    com.example.medicai.ui.theme.MedicineInactiveLight.copy(alpha = 0.3f)
                                )
                            }
                        ),
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    )
            )

            // Contenido principal
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icono
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            color = if (medicine.active)
                                com.example.medicai.ui.theme.SuccessContainerLight
                            else
                                MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Medication,
                        contentDescription = null,
                        tint = if (medicine.active)
                            com.example.medicai.ui.theme.SuccessLight
                        else
                            com.example.medicai.ui.theme.MedicineInactiveLight,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Información
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = medicine.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (medicine.active)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = if (medicine.active) "Activo" else "Inactivo",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (medicine.active)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = medicine.dosage,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = medicine.frequency,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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

                    // Horarios
                    InfoSection(
                        icon = Icons.Filled.AccessTime,
                        title = "Horarios",
                        content = medicine.times.joinToString(", ")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Fecha de inicio
                    InfoSection(
                        icon = Icons.Filled.CalendarToday,
                        title = "Inicio",
                        content = medicine.start_date
                    )

                    if (medicine.end_date != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        InfoSection(
                            icon = Icons.Filled.Event,
                            title = "Finaliza",
                            content = medicine.end_date
                        )
                    }

                    if (!medicine.notes.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        InfoSection(
                            icon = Icons.Filled.Notes,
                            title = "Notas",
                            content = medicine.notes
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Botones de acción - Cambia según el estado del medicamento
                    if (medicine.active) {
                        // Medicamento ACTIVO: Editar y Desactivar
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
                                onClick = onDelete,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Block,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Desactivar")
                            }
                        }
                    } else {
                        // Medicamento INACTIVO: Reactivar, Editar y Eliminar
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onReactivate,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Reactivar", fontWeight = FontWeight.Bold)
                            }

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
                                    onClick = onDelete,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DeleteForever,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
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
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
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
private fun LoadingState() {
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
                text = "Cargando medicamentos...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyMedicinesState(
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
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.MedicalServices,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
            }

            Text(
                text = when (filter) {
                    "Activos" -> "No hay medicamentos activos"
                    "Inactivos" -> "No hay medicamentos inactivos"
                    else -> "No hay medicamentos"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = if (filter == "Todos") {
                    "Comienza agregando tu primer medicamento"
                } else {
                    "Intenta cambiar el filtro"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            if (filter == "Todos") {
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
                        text = "Agregar Medicamento",
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
        containerColor = MaterialTheme.colorScheme.primary,
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 8.dp,
            pressedElevation = 12.dp
        )
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = "Agregar medicamento",
            modifier = Modifier.size(28.dp),
            tint = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModernAddMedicineDialog(
    userId: String,
    onDismiss: () -> Unit,
    onConfirm: (MedicineRequest) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var dosage by rememberSaveable { mutableStateOf("") }
    var frequencyHours by rememberSaveable { mutableStateOf("8") }
    var selectedHour by rememberSaveable { mutableStateOf(8) }
    var selectedMinute by rememberSaveable { mutableStateOf(0) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var notes by rememberSaveable { mutableStateOf("") }

    // Crear startTime desde hora y minuto seleccionados
    val startTime = remember(selectedHour, selectedMinute) {
        "${selectedHour.toString().padStart(2, '0')}:${selectedMinute.toString().padStart(2, '0')}"
    }

    // Calcular horarios automáticamente
    val calculatedTimes = remember(frequencyHours, startTime) {
        calculateSchedule(frequencyHours.toIntOrNull() ?: 8, startTime)
    }

    val isValid = name.isNotBlank() && dosage.isNotBlank() && frequencyHours.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Medication,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Agregar Medicamento",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 600.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { 
                            Text(
                                "Nombre del medicamento *",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            ) 
                        },
                        placeholder = { 
                            Text(
                                "Ej: Paracetamol",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            ) 
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.MedicalServices, contentDescription = "Nombre del medicamento")
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = dosage,
                        onValueChange = { dosage = it },
                        label = { 
                            Text(
                                "Dosis *",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            ) 
                        },
                        placeholder = { 
                            Text(
                                "Ej: 500mg, 1 comprimido",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            ) 
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.Colorize, contentDescription = "Dosis")
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                item {
                    Text(
                        text = "Frecuencia de toma",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("4", "6", "8", "12", "24").forEach { hours ->
                            FilterChip(
                                selected = frequencyHours == hours,
                                onClick = { frequencyHours = hours },
                                label = {
                                    Text(
                                        "Cada $hours hrs",
                                        fontWeight = if (frequencyHours == hours) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                leadingIcon = if (frequencyHours == hours) {
                                    { Icon(Icons.Filled.Check, "Confirmado", Modifier.size(18.dp)) }
                                } else null
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = "Hora de primera toma",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Botón para abrir Time Picker
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
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Hora seleccionada",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = startTime,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Filled.Schedule,
                                contentDescription = "Cambiar hora",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Mostrar horarios calculados
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Horarios calculados:",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = calculatedTimes.joinToString("  •  "),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${calculatedTimes.size} tomas al día",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
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
                    val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    val frequencyText = "Cada $frequencyHours horas"

                    val medicineRequest = MedicineRequest(
                        user_id = userId,
                        name = name.trim(),
                        dosage = dosage.trim(),
                        frequency = frequencyText,
                        times = calculatedTimes,
                        start_date = todayDate,
                        notes = notes.trim().ifBlank { null },
                        active = true
                    )
                    onConfirm(medicineRequest)
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
                Text("Agregar")
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

    // Time Picker Dialog
    if (showTimePicker) {
        TimePickerDialog(
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
}

// Función para calcular horarios automáticamente
private fun calculateSchedule(frequencyHours: Int, startTime: String): List<String> {
    val times = mutableListOf<String>()

    try {
        val timeParts = startTime.split(":")
        if (timeParts.size != 2) return listOf(startTime)

        var hour = timeParts[0].toIntOrNull() ?: 8
        val minute = timeParts[1].toIntOrNull() ?: 0

        val timesPerDay = 24 / frequencyHours

        repeat(timesPerDay) {
            val formattedHour = hour.toString().padStart(2, '0')
            val formattedMinute = minute.toString().padStart(2, '0')
            times.add("$formattedHour:$formattedMinute")

            hour = (hour + frequencyHours) % 24
        }
    } catch (e: Exception) {
        times.add(startTime)
    }

    return times
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModernEditMedicineDialog(
    medicine: Medicine,
    onDismiss: () -> Unit,
    onConfirm: (MedicineRequest) -> Unit
) {
    var name by rememberSaveable { mutableStateOf(medicine.name) }
    var dosage by rememberSaveable { mutableStateOf(medicine.dosage) }
    var frequencyHours by rememberSaveable {
        mutableStateOf(
            medicine.frequency.replace("Cada ", "").replace(" horas", "").trim()
        )
    }

    // Extraer hora y minuto inicial del medicamento
    val initialTime = medicine.times.firstOrNull() ?: "08:00"
    val initialTimeParts = initialTime.split(":")
    var selectedHour by rememberSaveable { mutableStateOf(initialTimeParts[0].toIntOrNull() ?: 8) }
    var selectedMinute by rememberSaveable { mutableStateOf(initialTimeParts.getOrNull(1)?.toIntOrNull() ?: 0) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }

    var notes by rememberSaveable { mutableStateOf(medicine.notes ?: "") }
    var isActive by rememberSaveable { mutableStateOf(medicine.active) }
    val notesFocusRequester = remember { FocusRequester() }
    val scrollState = rememberLazyListState()

    // Crear startTime desde hora y minuto seleccionados
    val startTime = remember(selectedHour, selectedMinute) {
        "${selectedHour.toString().padStart(2, '0')}:${selectedMinute.toString().padStart(2, '0')}"
    }

    // Calcular horarios automáticamente
    val calculatedTimes = remember(frequencyHours, startTime) {
        calculateSchedule(frequencyHours.toIntOrNull() ?: 8, startTime)
    }

    val isValid = name.isNotBlank() && dosage.isNotBlank() && frequencyHours.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Editar Medicamento",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 600.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { 
                            Text(
                                "Nombre del medicamento *",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            ) 
                        },
                        placeholder = { 
                            Text(
                                "Ej: Paracetamol",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            ) 
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.MedicalServices, contentDescription = "Nombre del medicamento")
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = dosage,
                        onValueChange = { dosage = it },
                        label = { 
                            Text(
                                "Dosis (ej: 500mg, 1 comprimido) *",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            ) 
                        },
                        placeholder = { 
                            Text(
                                "Ej: 500mg, 1 comprimido",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            ) 
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.Colorize, contentDescription = "Dosis")
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                item {
                    Text(
                        text = "Frecuencia de toma",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("4", "6", "8", "12", "24").forEach { hours ->
                            FilterChip(
                                selected = frequencyHours == hours,
                                onClick = { frequencyHours = hours },
                                label = {
                                    Text(
                                        "Cada $hours hrs",
                                        fontWeight = if (frequencyHours == hours) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                leadingIcon = if (frequencyHours == hours) {
                                    { Icon(Icons.Filled.Check, "Confirmado", Modifier.size(18.dp)) }
                                } else null
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = "Hora de primera toma",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Botón para abrir Time Picker
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
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Hora seleccionada",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = startTime,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Filled.Schedule,
                                contentDescription = "Cambiar hora",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Mostrar horarios calculados
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Horarios calculados:",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = calculatedTimes.joinToString("  •  "),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(notesFocusRequester),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        onClick = {
                            notesFocusRequester.requestFocus()
                            CoroutineScope(Dispatchers.Main).launch {
                                delay(100)
                                scrollState.animateScrollToItem(6)
                            }
                        }
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Notes,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Notas (opcional)",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            BasicTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = MaterialTheme.typography.bodyMedium,
                                maxLines = 3,
                                decorationBox = { innerTextField ->
                                    Box {
                                        if (notes.isEmpty()) {
                                            Text(
                                                text = "Ej: Tomar con alimentos",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val frequencyText = "Cada $frequencyHours horas"

                    val medicineRequest = MedicineRequest(
                        user_id = medicine.user_id,
                        name = name.trim(),
                        dosage = dosage.trim(),
                        frequency = frequencyText,
                        times = calculatedTimes,
                        start_date = medicine.start_date,
                        end_date = medicine.end_date,
                        notes = notes.trim().ifBlank { null },
                        active = isActive
                    )
                    android.util.Log.d("MedicinesScreen", "💾 Guardando medicamento con active=$isActive")
                    onConfirm(medicineRequest)
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

    // Time Picker Dialog
    if (showTimePicker) {
        TimePickerDialog(
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
}

// Componente TimePickerDialog usando Material 3
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
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
                    tint = MaterialTheme.colorScheme.primary
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
                        clockDialColor = MaterialTheme.colorScheme.primaryContainer,
                        selectorColor = MaterialTheme.colorScheme.primary,
                        timeSelectorSelectedContainerColor = MaterialTheme.colorScheme.primary,
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
