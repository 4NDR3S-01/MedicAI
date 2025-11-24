package com.example.medicai.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Pantalla para mostrar todos los sensores disponibles en el dispositivo
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorListScreen() {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val sensors = remember { sensorManager.getSensorList(Sensor.TYPE_ALL) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sensores del Dispositivo") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Total de sensores: ${sensors.size}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Estos son todos los sensores disponibles en tu dispositivo",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            items(sensors) { sensor ->
                SensorCard(sensor = sensor)
            }
        }
    }
}

@Composable
private fun SensorCard(sensor: Sensor) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = getSensorIcon(sensor.type),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = sensor.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = getSensorTypeName(sensor.type),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            SensorDetailRow("Vendor", sensor.vendor)
            SensorDetailRow("Versión", sensor.version.toString())
            SensorDetailRow("Potencia", "${sensor.power} mA")
            SensorDetailRow("Resolución", sensor.resolution.toString())
            SensorDetailRow("Rango máximo", sensor.maximumRange.toString())
        }
    }
}

@Composable
private fun SensorDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun getSensorIcon(type: Int): ImageVector {
    return when (type) {
        Sensor.TYPE_ACCELEROMETER -> Icons.Filled.PhoneAndroid
        Sensor.TYPE_MAGNETIC_FIELD -> Icons.Filled.Explore
        Sensor.TYPE_GYROSCOPE -> Icons.Filled.ScreenRotation
        Sensor.TYPE_LIGHT -> Icons.Filled.LightMode
        Sensor.TYPE_PRESSURE -> Icons.Filled.Speed
        Sensor.TYPE_PROXIMITY -> Icons.Filled.Sensors
        Sensor.TYPE_GRAVITY -> Icons.Filled.Height
        Sensor.TYPE_LINEAR_ACCELERATION -> Icons.Filled.TrendingUp
        Sensor.TYPE_ROTATION_VECTOR -> Icons.Filled.ThreeDRotation
        Sensor.TYPE_AMBIENT_TEMPERATURE -> Icons.Filled.Thermostat
        Sensor.TYPE_STEP_COUNTER -> Icons.Filled.DirectionsWalk
        Sensor.TYPE_STEP_DETECTOR -> Icons.Filled.DirectionsRun
        Sensor.TYPE_HEART_RATE -> Icons.Filled.Favorite
        else -> Icons.Filled.Sensors
    }
}

private fun getSensorTypeName(type: Int): String {
    return when (type) {
        Sensor.TYPE_ACCELEROMETER -> "Acelerómetro"
        Sensor.TYPE_MAGNETIC_FIELD -> "Campo Magnético"
        Sensor.TYPE_ORIENTATION -> "Orientación (deprecated)"
        Sensor.TYPE_GYROSCOPE -> "Giroscopio"
        Sensor.TYPE_LIGHT -> "Sensor de Luz"
        Sensor.TYPE_PRESSURE -> "Barómetro (Presión)"
        Sensor.TYPE_TEMPERATURE -> "Temperatura (deprecated)"
        Sensor.TYPE_PROXIMITY -> "Proximidad"
        Sensor.TYPE_GRAVITY -> "Gravedad"
        Sensor.TYPE_LINEAR_ACCELERATION -> "Aceleración Lineal"
        Sensor.TYPE_ROTATION_VECTOR -> "Vector de Rotación"
        Sensor.TYPE_RELATIVE_HUMIDITY -> "Humedad Relativa"
        Sensor.TYPE_AMBIENT_TEMPERATURE -> "Temperatura Ambiental"
        Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED -> "Campo Magnético (sin calibrar)"
        Sensor.TYPE_GAME_ROTATION_VECTOR -> "Vector de Rotación (Juegos)"
        Sensor.TYPE_GYROSCOPE_UNCALIBRATED -> "Giroscopio (sin calibrar)"
        Sensor.TYPE_SIGNIFICANT_MOTION -> "Movimiento Significativo"
        Sensor.TYPE_STEP_DETECTOR -> "Detector de Pasos"
        Sensor.TYPE_STEP_COUNTER -> "Contador de Pasos"
        Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR -> "Vector de Rotación Geomagnético"
        Sensor.TYPE_HEART_RATE -> "Frecuencia Cardíaca"
        Sensor.TYPE_POSE_6DOF -> "Pose 6DOF"
        Sensor.TYPE_STATIONARY_DETECT -> "Detección Estacionaria"
        Sensor.TYPE_MOTION_DETECT -> "Detección de Movimiento"
        Sensor.TYPE_HEART_BEAT -> "Latido Cardíaco"
        Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT -> "Detección Fuera del Cuerpo"
        Sensor.TYPE_ACCELEROMETER_UNCALIBRATED -> "Acelerómetro (sin calibrar)"
        else -> "Tipo desconocido ($type)"
    }
}

