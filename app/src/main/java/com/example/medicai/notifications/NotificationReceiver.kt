package com.example.medicai.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.medicai.data.local.UserPreferencesManager

/**
 * Receiver para manejar notificaciones programadas
 * Verifica preferencias del usuario antes de mostrar notificaciones
 */
class NotificationReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_MEDICINE_REMINDER = "com.example.medicai.MEDICINE_REMINDER"
        const val ACTION_APPOINTMENT_REMINDER = "com.example.medicai.APPOINTMENT_REMINDER"

        // Extras para medicamentos
        const val EXTRA_MEDICINE_ID = "medicine_id"
        const val EXTRA_MEDICINE_NAME = "medicine_name"
        const val EXTRA_MEDICINE_DOSAGE = "medicine_dosage"
        const val EXTRA_MEDICINE_TIME = "medicine_time"

        // Extras para citas
        const val EXTRA_APPOINTMENT_ID = "appointment_id"
        const val EXTRA_DOCTOR_NAME = "doctor_name"
        const val EXTRA_SPECIALTY = "specialty"
        const val EXTRA_DATE_TIME = "date_time"
        const val EXTRA_LOCATION = "location"
        const val EXTRA_MINUTES_BEFORE = "minutes_before"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("NotificationReceiver", "📬 Notificación recibida: ${intent.action}")

        // ✅ VERIFICAR SI LAS NOTIFICACIONES ESTÁN HABILITADAS
        val notificationsEnabled = UserPreferencesManager.areNotificationsEnabled(context)
        
        if (!notificationsEnabled) {
            Log.d("NotificationReceiver", "🔕 Notificaciones deshabilitadas por el usuario - no se muestra")
            return
        }

        when (intent.action) {
            ACTION_MEDICINE_REMINDER -> {
                handleMedicineReminder(context, intent)
            }
            ACTION_APPOINTMENT_REMINDER -> {
                handleAppointmentReminder(context, intent)
            }
        }
    }

    private fun handleMedicineReminder(context: Context, intent: Intent) {
        val medicineId = intent.getStringExtra(EXTRA_MEDICINE_ID) ?: return
        val medicineName = intent.getStringExtra(EXTRA_MEDICINE_NAME) ?: "Medicamento"
        val dosage = intent.getStringExtra(EXTRA_MEDICINE_DOSAGE) ?: ""
        val time = intent.getStringExtra(EXTRA_MEDICINE_TIME) ?: ""

        Log.d("NotificationReceiver", "💊 Mostrando notificación de medicamento: $medicineName")

        MedicAINotificationManager.showMedicineNotification(
            context = context,
            medicineId = medicineId,
            medicineName = medicineName,
            dosage = dosage,
            time = time
        )

        // ✅ Reprogramar la alarma para mañana a la misma hora
        reprogramMedicineAlarm(context, medicineId, medicineName, dosage, time)
    }

    /**
     * Reprogramar la alarma de medicamento para el día siguiente
     */
    private fun reprogramMedicineAlarm(
        context: Context,
        medicineId: String,
        medicineName: String,
        dosage: String,
        time: String
    ) {
        try {
            val calendar = java.util.Calendar.getInstance().apply {
                val timeParts = time.split(":")
                set(java.util.Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                set(java.util.Calendar.MINUTE, timeParts[1].toInt())
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
                add(java.util.Calendar.DAY_OF_YEAR, 1) // Mañana
            }

            val newIntent = Intent(context, NotificationReceiver::class.java).apply {
                action = ACTION_MEDICINE_REMINDER
                putExtra(EXTRA_MEDICINE_ID, medicineId)
                putExtra(EXTRA_MEDICINE_NAME, medicineName)
                putExtra(EXTRA_MEDICINE_DOSAGE, dosage)
                putExtra(EXTRA_MEDICINE_TIME, time)
            }

            val requestCode = medicineId.hashCode()
            val pendingIntent = android.app.PendingIntent.getBroadcast(
                context,
                requestCode,
                newIntent,
                android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
            )

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    android.app.AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    android.app.AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }

            Log.d("NotificationReceiver", "♻️ Alarma reprogramada para $medicineName a las $time mañana")
        } catch (e: Exception) {
            Log.e("NotificationReceiver", "❌ Error reprogramando alarma: ${e.message}")
        }
    }

    private fun handleAppointmentReminder(context: Context, intent: Intent) {
        val appointmentId = intent.getStringExtra(EXTRA_APPOINTMENT_ID) ?: return
        val doctorName = intent.getStringExtra(EXTRA_DOCTOR_NAME) ?: "Doctor"
        val specialty = intent.getStringExtra(EXTRA_SPECIALTY) ?: ""
        val dateTime = intent.getStringExtra(EXTRA_DATE_TIME) ?: ""
        val location = intent.getStringExtra(EXTRA_LOCATION) ?: ""
        val minutesBefore = intent.getIntExtra(EXTRA_MINUTES_BEFORE, 15)

        Log.d("NotificationReceiver", "🩺 Mostrando notificación de cita: $doctorName")

        MedicAINotificationManager.showAppointmentNotification(
            context = context,
            appointmentId = appointmentId,
            doctorName = doctorName,
            specialty = specialty,
            dateTime = dateTime,
            location = location,
            minutesBefore = minutesBefore
        )
    }
}

