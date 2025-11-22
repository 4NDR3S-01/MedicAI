package com.example.medicai.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.medicai.data.local.UserPreferencesManager
import com.example.medicai.notifications.NotificationReceiver
import java.text.SimpleDateFormat
import java.util.*

/**
 * Herramienta para debuggear alarmas
 */
object AlarmDebugger {
    
    /**
     * Programar una alarma de prueba en 1 minuto
     */
    fun scheduleTestAlarmInOneMinute(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        Log.d("AlarmDebugger", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d("AlarmDebugger", "🧪 INICIANDO PRUEBA DE ALARMA")
        
        // Verificar permisos primero
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.e("AlarmDebugger", "❌ No hay permiso para alarmas exactas")
                android.widget.Toast.makeText(
                    context,
                    "❌ No tienes permiso para alarmas exactas. Ve a Ajustes → Aplicaciones → MedicAI → Alarmas y recordatorios",
                    android.widget.Toast.LENGTH_LONG
                ).show()
                return
            } else {
                Log.d("AlarmDebugger", "✅ Permiso de alarmas exactas: CONCEDIDO")
            }
        }
        
        val calendar = Calendar.getInstance().apply {
            add(Calendar.MINUTE, 1) // En 1 minuto
        }
        
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = NotificationReceiver.ACTION_MEDICINE_REMINDER
            putExtra(NotificationReceiver.EXTRA_MEDICINE_ID, "test_alarm_${System.currentTimeMillis()}")
            putExtra(NotificationReceiver.EXTRA_MEDICINE_NAME, "🧪 PRUEBA DE ALARMA")
            putExtra(NotificationReceiver.EXTRA_MEDICINE_DOSAGE, "1 tableta de prueba")
            putExtra(NotificationReceiver.EXTRA_MEDICINE_TIME, "EN 1 MINUTO")
        }
        
        val requestCode = 999999
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
            
            val alarmTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(calendar.time)
            val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            
            Log.d("AlarmDebugger", "✅ ALARMA DE PRUEBA PROGRAMADA")
            Log.d("AlarmDebugger", "⏰ Hora actual: $currentTime")
            Log.d("AlarmDebugger", "⏰ Programada para: $alarmTime")
            Log.d("AlarmDebugger", "📱 Request Code: $requestCode")
            Log.d("AlarmDebugger", "🔔 Espera 1 minuto para ver la notificación")
            Log.d("AlarmDebugger", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            
            android.widget.Toast.makeText(
                context,
                "✅ Alarma de prueba programada para $alarmTime\n\nEspera 1 minuto para recibir la notificación.",
                android.widget.Toast.LENGTH_LONG
            ).show()
            
        } catch (e: Exception) {
            Log.e("AlarmDebugger", "❌ Error programando alarma de prueba", e)
            android.widget.Toast.makeText(
                context,
                "❌ Error: ${e.message}",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }
    
    /**
     * Verificar estado de permisos y preferencias
     */
    fun checkPermissions(context: Context): String {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val sb = StringBuilder()
        
        Log.d("AlarmDebugger", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d("AlarmDebugger", "🔍 DIAGNÓSTICO COMPLETO")
        
        sb.appendLine("📱 ESTADO DE PERMISOS:")
        sb.appendLine("━━━━━━━━━━━━━━━━━━━━")
        
        // Permiso de alarmas exactas (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val canSchedule = alarmManager.canScheduleExactAlarms()
            sb.appendLine("⏰ Alarmas exactas: ${if (canSchedule) "✅ CONCEDIDO" else "❌ DENEGADO"}")
            Log.d("AlarmDebugger", "⏰ Alarmas exactas: ${if (canSchedule) "✅" else "❌"}")
            if (!canSchedule) {
                sb.appendLine("   ⚠️ Ve a: Ajustes → Apps → MedicAI → Alarmas y recordatorios")
            }
        } else {
            sb.appendLine("⏰ Alarmas exactas: ✅ AUTOMÁTICO (Android < 12)")
            Log.d("AlarmDebugger", "⏰ Alarmas exactas: ✅ AUTOMÁTICO")
        }
        
        // Permiso de notificaciones (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasNotificationPermission = context.checkSelfPermission(
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            
            sb.appendLine("🔔 Notificaciones: ${if (hasNotificationPermission) "✅ CONCEDIDO" else "❌ DENEGADO"}")
            Log.d("AlarmDebugger", "🔔 Notificaciones: ${if (hasNotificationPermission) "✅" else "❌"}")
            if (!hasNotificationPermission) {
                sb.appendLine("   ⚠️ Ve a: Ajustes → Apps → MedicAI → Notificaciones")
            }
        } else {
            sb.appendLine("🔔 Notificaciones: ✅ AUTOMÁTICO (Android < 13)")
            Log.d("AlarmDebugger", "🔔 Notificaciones: ✅ AUTOMÁTICO")
        }
        
        sb.appendLine("")
        sb.appendLine("⚙️ PREFERENCIAS DE LA APP:")
        sb.appendLine("━━━━━━━━━━━━━━━━━━━━")
        
        // Verificar preferencias
        val notificationsEnabled = UserPreferencesManager.areNotificationsEnabled(context)
        val prefs = UserPreferencesManager.getNotificationPreferences(context)
        
        sb.appendLine("🔔 Notificaciones app: ${if (notificationsEnabled) "✅ ACTIVADAS" else "❌ DESACTIVADAS"}")
        sb.appendLine("🔊 Sonido: ${if (prefs.soundEnabled) "✅ ON" else "❌ OFF"}")
        sb.appendLine("📳 Vibración: ${if (prefs.vibrationEnabled) "✅ ON" else "❌ OFF"}")
        sb.appendLine("⏱️ Recordatorio: ${prefs.reminderMinutes} min antes")
        
        Log.d("AlarmDebugger", "🔔 Notificaciones app: ${if (notificationsEnabled) "✅" else "❌"}")
        Log.d("AlarmDebugger", "🔊 Sonido: ${if (prefs.soundEnabled) "✅" else "❌"}")
        Log.d("AlarmDebugger", "📳 Vibración: ${if (prefs.vibrationEnabled) "✅" else "❌"}")
        
        sb.appendLine("━━━━━━━━━━━━━━━━━━━━")
        
        Log.d("AlarmDebugger", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        
        val result = sb.toString()
        return result
    }
    
    /**
     * Diagnóstico completo de por qué no funcionan las alarmas
     */
    fun fullDiagnostic(context: Context): String {
        val sb = StringBuilder()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        sb.appendLine("🔬 DIAGNÓSTICO COMPLETO DE ALARMAS")
        sb.appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        sb.appendLine("")
        
        // 1. Versión de Android
        sb.appendLine("📱 Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        sb.appendLine("")
        
        // 2. Permisos del sistema
        sb.appendLine("🔐 PERMISOS DEL SISTEMA:")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val canSchedule = alarmManager.canScheduleExactAlarms()
            sb.appendLine("   Alarmas exactas: ${if (canSchedule) "✅ CONCEDIDO" else "❌ DENEGADO"}")
            if (!canSchedule) {
                sb.appendLine("   ⚠️ PROBLEMA CRÍTICO: Sin este permiso las alarmas NO funcionarán")
                sb.appendLine("   📋 Solución: Ajustes → Apps → MedicAI → Alarmas y recordatorios → ACTIVAR")
            }
        } else {
            sb.appendLine("   Alarmas exactas: ✅ No requerido (Android < 12)")
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasNotif = context.checkSelfPermission(
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            sb.appendLine("   Notificaciones: ${if (hasNotif) "✅ CONCEDIDO" else "❌ DENEGADO"}")
            if (!hasNotif) {
                sb.appendLine("   ⚠️ PROBLEMA CRÍTICO: Sin este permiso NO verás notificaciones")
                sb.appendLine("   📋 Solución: Ajustes → Apps → MedicAI → Notificaciones → ACTIVAR")
            }
        } else {
            sb.appendLine("   Notificaciones: ✅ No requerido (Android < 13)")
        }
        
        sb.appendLine("")
        
        // 3. Configuración de la app
        sb.appendLine("⚙️ CONFIGURACIÓN DE LA APP:")
        val notifEnabled = UserPreferencesManager.areNotificationsEnabled(context)
        val prefs = UserPreferencesManager.getNotificationPreferences(context)
        
        sb.appendLine("   Notificaciones activadas: ${if (notifEnabled) "✅ SÍ" else "❌ NO"}")
        if (!notifEnabled) {
            sb.appendLine("   ⚠️ PROBLEMA: Las notificaciones están desactivadas en la app")
            sb.appendLine("   📋 Solución: Ve a Perfil → Configurar Notificaciones → ACTIVAR")
        }
        
        sb.appendLine("   Sonido: ${if (prefs.soundEnabled) "✅" else "❌"}")
        sb.appendLine("   Vibración: ${if (prefs.vibrationEnabled) "✅" else "❌"}")
        sb.appendLine("")
        
        // 4. Resumen
        sb.appendLine("📊 RESUMEN:")
        val allOk = (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) &&
                    (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) &&
                    notifEnabled
        
        if (allOk) {
            sb.appendLine("   ✅ Todo configurado correctamente")
            sb.appendLine("   ✅ Las alarmas deberían funcionar")
        } else {
            sb.appendLine("   ❌ HAY PROBLEMAS que impiden que funcionen las alarmas")
            sb.appendLine("   📋 Revisa los puntos marcados con ⚠️ arriba")
        }
        
        sb.appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        
        val result = sb.toString()
        Log.d("AlarmDebugger", result)
        return result
    }
}
