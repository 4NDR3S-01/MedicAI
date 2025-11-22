package com.example.medicai.utils

import android.content.Context
import android.util.Log
import com.example.medicai.notifications.MedicAINotificationManager

/**
 * Utilidad para probar notificaciones
 */
object NotificationTester {
    
    /**
     * Disparar una notificación de prueba inmediatamente
     */
    fun testMedicineNotification(context: Context) {
        Log.d("NotificationTester", "🧪 Disparando notificación de prueba...")
        
        MedicAINotificationManager.showMedicineNotification(
            context = context,
            medicineId = "test_${System.currentTimeMillis()}",
            medicineName = "Medicamento de Prueba",
            dosage = "1 tableta",
            time = "Ahora"
        )
        
        Log.d("NotificationTester", "✅ Notificación de prueba enviada")
    }
    
    /**
     * Disparar una notificación de cita de prueba inmediatamente
     */
    fun testAppointmentNotification(context: Context) {
        Log.d("NotificationTester", "🧪 Disparando notificación de cita de prueba...")
        
        MedicAINotificationManager.showAppointmentNotification(
            context = context,
            appointmentId = "test_${System.currentTimeMillis()}",
            doctorName = "Dr. Prueba",
            specialty = "Medicina General",
            dateTime = "Hoy a las 15:00",
            location = "Hospital Central",
            minutesBefore = 15
        )
        
        Log.d("NotificationTester", "✅ Notificación de cita de prueba enviada")
    }
}
