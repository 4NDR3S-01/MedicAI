package com.example.medicai.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.medicai.MainActivity
import com.example.medicai.R

/**
 * Manager para gestionar notificaciones de la aplicación
 */
object MedicAINotificationManager {

    private const val CHANNEL_ID_MEDICINES = "medicine_reminders"
    private const val CHANNEL_ID_APPOINTMENTS = "appointment_reminders"
    private const val CHANNEL_NAME_MEDICINES = "Recordatorios de Medicamentos"
    private const val CHANNEL_NAME_APPOINTMENTS = "Recordatorios de Citas"

    /**
     * Crear canales de notificación (necesario para Android 8.0+)
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Canal para medicamentos
            val medicineChannel = NotificationChannel(
                CHANNEL_ID_MEDICINES,
                CHANNEL_NAME_MEDICINES,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Recordatorios para tomar medicamentos"
                enableVibration(true)
                enableLights(true)
            }

            // Canal para citas
            val appointmentChannel = NotificationChannel(
                CHANNEL_ID_APPOINTMENTS,
                CHANNEL_NAME_APPOINTMENTS,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Recordatorios de citas médicas"
                enableVibration(true)
                enableLights(true)
            }

            notificationManager.createNotificationChannel(medicineChannel)
            notificationManager.createNotificationChannel(appointmentChannel)
        }
    }

    /**
     * Mostrar notificación de recordatorio de medicamento
     * Respeta las preferencias de sonido y vibración del usuario
     */
    fun showMedicineNotification(
        context: Context,
        medicineId: String,
        medicineName: String,
        dosage: String,
        time: String
    ) {
        // ✅ Obtener preferencias del usuario
        val prefs = com.example.medicai.data.local.UserPreferencesManager.getNotificationPreferences(context)
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            medicineId.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID_MEDICINES)
            .setSmallIcon(R.drawable.logo_app)
            .setContentTitle("💊 Hora de tomar tu medicamento")
            .setContentText("$medicineName - $dosage a las $time")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Es hora de tomar:\n\n📋 $medicineName\n💊 Dosis: $dosage\n⏰ Hora: $time\n\nNo olvides tomar tu medicamento.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
        
        // ✅ Aplicar sonido solo si está habilitado
        if (prefs.soundEnabled) {
            notificationBuilder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            android.util.Log.d("MedicAINotificationManager", "🔊 Sonido habilitado")
        } else {
            android.util.Log.d("MedicAINotificationManager", "🔇 Sonido deshabilitado")
        }
        
        // ✅ Aplicar vibración solo si está habilitada
        if (prefs.vibrationEnabled) {
            notificationBuilder.setVibrate(longArrayOf(0, 500, 200, 500))
            android.util.Log.d("MedicAINotificationManager", "📳 Vibración habilitada")
        } else {
            android.util.Log.d("MedicAINotificationManager", "📵 Vibración deshabilitada")
        }
        
        val notification = notificationBuilder.build()

        try {
            NotificationManagerCompat.from(context).notify(medicineId.hashCode(), notification)
            android.util.Log.d("MedicAINotificationManager", "✅ Notificación de medicamento mostrada: $medicineName")
        } catch (e: SecurityException) {
            android.util.Log.e("Notification", "No hay permiso para mostrar notificaciones", e)
        }
    }

    /**
     * Mostrar notificación de recordatorio de cita
     * Respeta las preferencias de sonido y vibración del usuario
     */
    fun showAppointmentNotification(
        context: Context,
        appointmentId: String,
        doctorName: String,
        specialty: String,
        dateTime: String,
        location: String,
        minutesBefore: Int
    ) {
        // ✅ Obtener preferencias del usuario
        val prefs = com.example.medicai.data.local.UserPreferencesManager.getNotificationPreferences(context)
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            appointmentId.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID_APPOINTMENTS)
            .setSmallIcon(R.drawable.logo_app)
            .setContentTitle("🩺 Recordatorio de Cita Médica")
            .setContentText("$doctorName - En $minutesBefore minutos")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Tienes una cita médica en $minutesBefore minutos:\n\n" +
                            "👨‍⚕️ Doctor: $doctorName\n" +
                            "🏥 Especialidad: $specialty\n" +
                            "📅 Fecha y hora: $dateTime\n" +
                            "📍 Lugar: $location\n\n" +
                            "Recuerda llegar con anticipación.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
        
        // ✅ Aplicar sonido solo si está habilitado
        if (prefs.soundEnabled) {
            notificationBuilder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            android.util.Log.d("MedicAINotificationManager", "🔊 Sonido habilitado")
        } else {
            android.util.Log.d("MedicAINotificationManager", "🔇 Sonido deshabilitado")
        }
        
        // ✅ Aplicar vibración solo si está habilitada
        if (prefs.vibrationEnabled) {
            notificationBuilder.setVibrate(longArrayOf(0, 500, 200, 500, 200, 500))
            android.util.Log.d("MedicAINotificationManager", "📳 Vibración habilitada")
        } else {
            android.util.Log.d("MedicAINotificationManager", "📵 Vibración deshabilitada")
        }
        
        val notification = notificationBuilder.build()

        try {
            NotificationManagerCompat.from(context).notify(appointmentId.hashCode(), notification)
            android.util.Log.d("MedicAINotificationManager", "✅ Notificación de cita mostrada: $doctorName")
        } catch (e: SecurityException) {
            android.util.Log.e("Notification", "No hay permiso para mostrar notificaciones", e)
        }
    }

    /**
     * Cancelar una notificación específica
     */
    fun cancelNotification(context: Context, notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }

    /**
     * Cancelar todas las notificaciones
     */
    fun cancelAllNotifications(context: Context) {
        NotificationManagerCompat.from(context).cancelAll()
    }
}

