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

