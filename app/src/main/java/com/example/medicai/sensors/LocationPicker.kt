package com.example.medicai.sensors

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.Locale

/**
 * Diálogo para seleccionar ubicación usando GPS o búsqueda manual
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerDialog(
    currentLocation: String,
    onDismiss: () -> Unit,
    onLocationSelected: (String) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf(currentLocation) }
    var isLoadingGPS by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var detectedAddress by remember { mutableStateOf<String?>(null) }

    // Ubicaciones sugeridas comunes
    val suggestedLocations = remember {
        listOf(
            "Hospital General",
            "Clínica San José",
            "Centro Médico ABC",
            "Consultorio Médico",
            "Hospital Universitario",
            "Clínica del Norte"
        )
    }

    // Launcher para solicitar permisos de ubicación
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
                // Permiso concedido, obtener ubicación
                getCurrentLocation(context) { address, error ->
                    isLoadingGPS = false
                    if (address != null) {
                        detectedAddress = address
                        searchQuery = address
                    } else {
                        errorMessage = error ?: "No se pudo obtener la ubicación"
                    }
                }
            }
            else -> {
                isLoadingGPS = false
                errorMessage = "Se necesitan permisos de ubicación para usar el GPS"
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Seleccionar Ubicación",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Campo de búsqueda
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Buscar o escribir ubicación") },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = "Buscar ubicación")
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Limpiar")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Botón de GPS
                Button(
                    onClick = {
                        errorMessage = null
                        when {
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED -> {
                                // Ya tiene permiso, obtener ubicación
                                isLoadingGPS = true
                                getCurrentLocation(context) { address, error ->
                                    isLoadingGPS = false
                                    if (address != null) {
                                        detectedAddress = address
                                        searchQuery = address
                                    } else {
                                        errorMessage = error ?: "No se pudo obtener la ubicación"
                                    }
                                }
                            }
                            else -> {
                                // Solicitar permisos
                                isLoadingGPS = true
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoadingGPS,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    if (isLoadingGPS) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Obteniendo ubicación...")
                    } else {
                        Icon(Icons.Filled.MyLocation, contentDescription = "Mi ubicación actual")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Usar mi ubicación actual (GPS)")
                    }
                }

                // Mensaje de error
                errorMessage?.let { error ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                error,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                // Dirección detectada
                detectedAddress?.let { address ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Ubicación detectada:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    address,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                // Ubicaciones sugeridas
                Text(
                    "Ubicaciones sugeridas:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyColumn(
                    modifier = Modifier.heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(suggestedLocations.filter {
                        it.contains(searchQuery, ignoreCase = true) || searchQuery.isEmpty()
                    }) { location ->
                        OutlinedCard(
                            onClick = { searchQuery = location },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    location,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (searchQuery.isNotBlank()) {
                        onLocationSelected(searchQuery.trim())
                    }
                },
                enabled = searchQuery.isNotBlank()
            ) {
                Icon(Icons.Filled.Check, contentDescription = "Confirmar selección")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Seleccionar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

/**
 * Obtiene la ubicación actual del dispositivo y la convierte a dirección
 */
@SuppressLint("MissingPermission")
private fun getCurrentLocation(
    context: Context,
    onResult: (address: String?, error: String?) -> Unit
) {
    try {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        val cancellationTokenSource = CancellationTokenSource()

        // Verificar permisos antes de continuar
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            onResult(null, "No se tienen permisos de ubicación")
            return
        }

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cancellationTokenSource.token
        ).addOnSuccessListener { location: Location? ->
            if (location != null) {
                // Convertir coordenadas a dirección
                getAddressFromLocation(context, location) { address ->
                    if (address != null) {
                        onResult(address, null)
                    } else {
                        // Si falla el geocoding, devolver coordenadas
                        onResult(
                            "Lat: ${location.latitude}, Lon: ${location.longitude}",
                            null
                        )
                    }
                }
            } else {
                onResult(null, "No se pudo obtener la ubicación. Intenta de nuevo.")
            }
        }.addOnFailureListener { exception ->
            onResult(null, "Error al obtener ubicación: ${exception.message}")
        }
    } catch (e: Exception) {
        onResult(null, "Error: ${e.message}")
    }
}

/**
 * Convierte coordenadas GPS a dirección legible
 */
@Suppress("DEPRECATION")
private fun getAddressFromLocation(
    context: Context,
    location: Location,
    onResult: (String?) -> Unit
) {
    try {
        val geocoder = Geocoder(context, Locale.getDefault())
        // Usar el método compatible con API 24+
        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
        if (!addresses.isNullOrEmpty()) {
            val address = addresses[0]
            val addressParts = mutableListOf<String>()

            // Construir dirección completa
            address.thoroughfare?.let { addressParts.add(it) }
            address.subThoroughfare?.let { addressParts.add(it) }
            address.locality?.let { addressParts.add(it) }
            address.adminArea?.let { addressParts.add(it) }

            val fullAddress = addressParts.joinToString(", ")
            onResult(if (fullAddress.isNotEmpty()) fullAddress else null)
        } else {
            onResult(null)
        }
    } catch (_: Exception) {
        onResult(null)
    }
}

