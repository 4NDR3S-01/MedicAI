package com.example.medicai

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.example.medicai.ui.theme.MedicAITheme

class MainActivity : ComponentActivity() {

    // Launcher para solicitar permiso de notificaciones
    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            android.util.Log.d("MainActivity", "✅ Permiso de notificaciones CONCEDIDO")
            android.widget.Toast.makeText(
                this,
                "✅ Notificaciones activadas",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        } else {
            android.util.Log.w("MainActivity", "⚠️ Permiso de notificaciones DENEGADO")
            android.widget.Toast.makeText(
                this,
                "⚠️ Las notificaciones están desactivadas. Actívalas en Ajustes para recibir recordatorios.",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    // Launcher para abrir ajustes de alarmas exactas
    private val requestExactAlarmPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        // Verificar si se concedió el permiso
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (alarmManager.canScheduleExactAlarms()) {
                android.util.Log.d("MainActivity", "✅ Permiso de alarmas exactas CONCEDIDO")
            } else {
                android.util.Log.w("MainActivity", "⚠️ Permiso de alarmas exactas DENEGADO")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Solicitar permisos de notificaciones para Android 13+
        requestNotificationPermissionIfNeeded()

        // Verificar y solicitar permiso de alarmas exactas para Android 12+
        checkExactAlarmPermission()

        // Habilitar edge-to-edge para que la app use toda la pantalla
        enableEdgeToEdge()

        setContent {
            val isDarkTheme = isSystemInDarkTheme()

            SideEffect {
                // Configurar colores de las barras del sistema
                window.statusBarColor = Color.Transparent.toArgb()
                window.navigationBarColor = Color.Transparent.toArgb()

                // Configurar iconos claros u oscuros según el tema
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !isDarkTheme
                    isAppearanceLightNavigationBars = !isDarkTheme
                }
            }

            MedicAITheme {
                AuthNavigation()
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Ya tiene permiso
                    android.util.Log.d("MainActivity", "✅ Permiso de notificaciones ya concedido")
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Mostrar explicación antes de solicitar
                    android.util.Log.d("MainActivity", "ℹ️ Mostrando explicación de permisos")
                    android.widget.Toast.makeText(
                        this,
                        "MedicAI necesita permisos de notificación para recordarte tomar tus medicamentos y asistir a tus citas.",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                    // Solicitar permiso después de mostrar explicación
                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    // Solicitar permiso directamente
                    android.util.Log.d("MainActivity", "📱 Solicitando permiso de notificaciones...")
                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // Android 12 o anterior - permisos concedidos automáticamente
            android.util.Log.d("MainActivity", "✅ Android < 13: Permisos de notificación automáticos")
        }
    }

    private fun checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                android.util.Log.w("MainActivity", "⚠️ No hay permiso para alarmas exactas")
                // Opcionalmente, puedes abrir ajustes para que el usuario conceda el permiso
                // val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                // requestExactAlarmPermissionLauncher.launch(intent)
            } else {
                android.util.Log.d("MainActivity", "✅ Permiso de alarmas exactas concedido")
            }
        }
    }
}
