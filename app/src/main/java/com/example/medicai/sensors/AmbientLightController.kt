package com.example.medicai.sensors

import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.WindowManager
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlin.math.abs

/**
 * Composable que registra el sensor de luz ambiental y adapta el brillo
 * de la ventana de la actividad según la luz detectada.
 *
 * Cuando hay mucha luz -> sube el brillo
 * Cuando hay poca luz -> baja el brillo
 */
@Composable
fun AmbientLightBrightnessController(
    minBrightness: Float = 0.15f,  // Brillo mínimo (poca luz)
    maxBrightness: Float = 1.0f,    // Brillo máximo (mucha luz)
    smoothing: Float = 0.25f        // Factor de suavizado para evitar cambios bruscos
) {
    val context = LocalContext.current
    val activity = remember(context) { context as? Activity }

    // Última luminosidad aplicada (0f..1f)
    var lastBrightness by remember { mutableStateOf<Float?>(null) }

    DisposableEffect(activity) {
        val act = activity
        if (act == null) {
            onDispose { }
        } else {
            val sensorManager = act.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val lightSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

            if (lightSensor == null) {
                // Dispositivo no tiene sensor de luz
                onDispose { }
            } else {
                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        val lux = event.values.getOrNull(0) ?: return

                        // Mapear lux a brillo ventana entre minBrightness..maxBrightness
                        val target = mapLuxToBrightness(lux, minBrightness, maxBrightness)

                        // Suavizar cambios para evitar parpadeos
                        val smoothed = lastBrightness?.let { prev ->
                            prev * (1f - smoothing) + target * smoothing
                        } ?: target

                        // Aplicar solo si diferencia significativa (> 0.02) para evitar parpadeos constantes
                        if (lastBrightness == null || abs(smoothed - lastBrightness!!) > 0.02f) {
                            applyWindowBrightness(act, smoothed)
                            lastBrightness = smoothed
                        }
                    }

                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                        // No necesitamos hacer nada aquí
                    }
                }

                // Registrar listener (SENSOR_DELAY_NORMAL es suficiente para brillo)
                sensorManager.registerListener(listener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)

                onDispose {
                    sensorManager.unregisterListener(listener)
                }
            }
        }
    }
}

/**
 * Mapea el valor de lux del sensor a un nivel de brillo.
 *
 * Valores típicos de lux:
 * - 0-10 lux: Oscuridad/Poca luz
 * - 10-50 lux: Luz tenue
 * - 50-200 lux: Ambiente interior normal
 * - 200-500 lux: Oficina bien iluminada
 * - 500-1000 lux: Día nublado exterior
 * - 1000+ lux: Mucha luz (día soleado)
 */
private fun mapLuxToBrightness(lux: Float, minB: Float, maxB: Float): Float {
    // Mapeo logarítmico para mejor respuesta visual
    val low = 10f      // 10 lux o menos = brillo mínimo
    val high = 1000f   // 1000 lux o más = brillo máximo

    // Interpolación lineal entre low y high
    val t = ((lux - low) / (high - low)).coerceIn(0f, 1f)

    return minB + t * (maxB - minB)
}

/**
 * Aplica el brillo a la ventana de la actividad.
 * Nota: Esto solo afecta a la ventana de esta app, no al brillo global del sistema.
 */
private fun applyWindowBrightness(activity: Activity, brightness: Float) {
    try {
        val window = activity.window ?: return
        val attrs: WindowManager.LayoutParams = window.attributes
        // screenBrightness: -1 = auto, 0 = mínimo, 1 = máximo
        attrs.screenBrightness = brightness.coerceIn(0f, 1f)
        window.attributes = attrs
    } catch (e: Exception) {
        // Silencioso: no bloquear UI si falla
        e.printStackTrace()
    }
}

