package com.example.medicai.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleMapsLocationPickerDialog(
    currentLocation: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by rememberSaveable { mutableStateOf(currentLocation) }
    var selectedLocation by rememberSaveable { mutableStateOf(currentLocation) }
    var showMapButton by remember { mutableStateOf(true) }
    
    // Categorías de ubicaciones
    val locationCategories = mapOf(
        "Hospitales" to listOf(
            "Hospital General",
            "Hospital Central",
            "Hospital Regional",
            "Hospital Universitario",
            "Hospital Materno Infantil"
        ),
        "Clínicas" to listOf(
            "Clínica Santa María",
            "Clínica San José",
            "Clínica del Country",
            "Clínica Universitaria",
            "Clínica Especializada"
        ),
        "Centros Médicos" to listOf(
            "Centro Médico ABC",
            "Centro de Salud",
            "Centro de Especialidades",
            "Centro de Diagnóstico"
        ),
        "Otros" to listOf(
            "Consultorio Médico Privado",
            "Cruz Roja",
            "IMSS",
            "ISSSTE",
            "Teleconsulta / Virtual",
            "Domicilio del paciente"
        )
    )
    
    // Filtrar ubicaciones según búsqueda
    val filteredCategories = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            locationCategories
        } else {
            locationCategories.mapValues { (_, locations) ->
                locations.filter { it.contains(searchQuery, ignoreCase = true) }
            }.filterValues { it.isNotEmpty() }
        }
    }

    // Función para abrir Google Maps
    fun openGoogleMaps() {
        try {
            // Intenta abrir Google Maps en modo de búsqueda
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("geo:0,0?q=${Uri.encode(searchQuery.ifBlank { "hospital" })}")
                setPackage("com.google.android.apps.maps")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Si Google Maps no está instalado, abre en el navegador
            try {
                val webIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://www.google.com/maps/search/${Uri.encode(searchQuery.ifBlank { "hospital" })}")
                }
                context.startActivity(webIntent)
            } catch (ex: Exception) {
                // No hacer nada si tampoco se puede abrir el navegador
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "Seleccionar Ubicación",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Elige o escribe la ubicación de tu cita",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Barra de búsqueda
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Buscar o escribir ubicación personalizada") },
                    placeholder = { Text("Ej: Hospital General, Consultorio Dr. López...") },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { 
                                searchQuery = ""
                                selectedLocation = ""
                            }) {
                                Icon(Icons.Filled.Clear, "Limpiar")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Botón para abrir Google Maps
                ElevatedButton(
                    onClick = { openGoogleMaps() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Map,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Buscar en Google Maps",
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.OpenInNew,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Ubicación seleccionada
                if (selectedLocation.isNotBlank()) {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Ubicación seleccionada:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = selectedLocation,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                Divider()

                Text(
                    text = "O elige una ubicación común:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Lista de ubicaciones por categorías
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (filteredCategories.isNotEmpty()) {
                        filteredCategories.forEach { (category, locations) ->
                            item {
                                Text(
                                    text = category,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            items(locations) { location ->
                                ElevatedCard(
                                    onClick = {
                                        selectedLocation = location
                                        searchQuery = location
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.elevatedCardColors(
                                        containerColor = if (selectedLocation == location)
                                            MaterialTheme.colorScheme.secondaryContainer
                                        else
                                            MaterialTheme.colorScheme.surface
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
                                            Icon(
                                                imageVector = when {
                                                    location.contains("Hospital", ignoreCase = true) -> Icons.Filled.LocalHospital
                                                    location.contains("Clínica", ignoreCase = true) -> Icons.Filled.MedicalServices
                                                    location.contains("Virtual", ignoreCase = true) || 
                                                    location.contains("Teleconsulta", ignoreCase = true) -> Icons.Filled.Videocam
                                                    location.contains("Domicilio", ignoreCase = true) -> Icons.Filled.Home
                                                    else -> Icons.Filled.LocationOn
                                                },
                                                contentDescription = null,
                                                tint = if (selectedLocation == location)
                                                    MaterialTheme.colorScheme.secondary
                                                else
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = location,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = if (selectedLocation == location) 
                                                    FontWeight.Bold 
                                                else 
                                                    FontWeight.Normal,
                                                color = if (selectedLocation == location)
                                                    MaterialTheme.colorScheme.secondary
                                                else
                                                    MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        if (selectedLocation == location) {
                                            Icon(
                                                imageVector = Icons.Filled.CheckCircle,
                                                contentDescription = "Seleccionado",
                                                tint = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else if (searchQuery.isNotBlank()) {
                        // Ubicación personalizada
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.EditLocation,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Text(
                                        text = "Ubicación personalizada",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    Text(
                                        text = "\"$searchQuery\"",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                    Text(
                                        text = "Presiona Confirmar para usar esta ubicación",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalLocation = if (searchQuery.isNotBlank()) searchQuery else selectedLocation
                    if (finalLocation.isNotBlank()) {
                        onConfirm(finalLocation)
                    }
                },
                enabled = searchQuery.isNotBlank() || selectedLocation.isNotBlank(),
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
