package com.example.medicai.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

/**
 * Scheduler para programar alarmas de notificaciones
 */
object AlarmScheduler {

    /**
     * Programar recordatorios para un medicamento
     */
    fun scheduleMedicineReminders(
        context: Context,
        medicineId: String,
        medicineName: String,
        dosage: String,
        times: List<String> // Lista de horarios en formato "HH:mm"
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        times.forEachIndexed { index, time ->
            try {
                val calendar = Calendar.getInstance().apply {
                    val timeParts = time.split(":")
                    set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                    set(Calendar.MINUTE, timeParts[1].toInt())
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)

                    // Si la hora ya pasó hoy, programar para mañana
                    if (timeInMillis <= System.currentTimeMillis()) {
                        add(Calendar.DAY_OF_YEAR, 1)
                    }
                }

                val intent = Intent(context, NotificationReceiver::class.java).apply {
                    action = NotificationReceiver.ACTION_MEDICINE_REMINDER
                    putExtra(NotificationReceiver.EXTRA_MEDICINE_ID, medicineId)
                    putExtra(NotificationReceiver.EXTRA_MEDICINE_NAME, medicineName)
                    putExtra(NotificationReceiver.EXTRA_MEDICINE_DOSAGE, dosage)
                    putExtra(NotificationReceiver.EXTRA_MEDICINE_TIME, time)
                }

                val requestCode = (medicineId + index.toString()).hashCode()
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )

                // Programar alarma repetitiva diaria
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setRepeating(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            AlarmManager.INTERVAL_DAY,
                            pendingIntent
                        )
                        Log.d("AlarmScheduler", "⏰ Alarma programada para $medicineName a las $time")
                    } else {
                        Log.w("AlarmScheduler", "⚠️ No hay permiso para alarmas exactas")
                    }
                } else {
                    alarmManager.setRepeating(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        AlarmManager.INTERVAL_DAY,
                        pendingIntent
                    )
                    Log.d("AlarmScheduler", "⏰ Alarma programada para $medicineName a las $time")
                }

            } catch (e: Exception) {
                Log.e("AlarmScheduler", "❌ Error programando alarma: ${e.message}", e)
            }
        }
    }

    /**
     * Cancelar todos los recordatorios de un medicamento
     */
    fun cancelMedicineReminders(
        context: Context,
        medicineId: String,
        timesCount: Int
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        repeat(timesCount) { index ->
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                action = NotificationReceiver.ACTION_MEDICINE_REMINDER
            }

            val requestCode = (medicineId + index.toString()).hashCode()
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
            )

            pendingIntent?.let {
                alarmManager.cancel(it)
                it.cancel()
                Log.d("AlarmScheduler", "🗑️ Alarma cancelada para medicamento $medicineId (índice $index)")
            }
        }
    }

    /**
     * Programar recordatorio para una cita médica
     */
    fun scheduleAppointmentReminder(
        context: Context,
        appointmentId: String,
        doctorName: String,
        specialty: String,
        date: String, // formato "yyyy-MM-dd"
        time: String, // formato "HH:mm"
        location: String,
        minutesBefore: Int = 15
    ) {
        try {
            val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val appointmentDateTime = dateTimeFormat.parse("$date $time") ?: return

            val calendar = Calendar.getInstance().apply {
                this.time = appointmentDateTime
                add(Calendar.MINUTE, -minutesBefore) // Recordar X minutos antes
            }

            // Solo programar si la fecha es futura
            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                Log.w("AlarmScheduler", "⚠️ La cita ya pasó, no se programa alarma")
                return
            }

            val intent = Intent(context, NotificationReceiver::class.java).apply {
                action = NotificationReceiver.ACTION_APPOINTMENT_REMINDER
                putExtra(NotificationReceiver.EXTRA_APPOINTMENT_ID, appointmentId)
                putExtra(NotificationReceiver.EXTRA_DOCTOR_NAME, doctorName)
                putExtra(NotificationReceiver.EXTRA_SPECIALTY, specialty)
                putExtra(NotificationReceiver.EXTRA_DATE_TIME, "$date $time")
                putExtra(NotificationReceiver.EXTRA_LOCATION, location)
                putExtra(NotificationReceiver.EXTRA_MINUTES_BEFORE, minutesBefore)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                appointmentId.hashCode(),
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                    Log.d("AlarmScheduler", "⏰ Recordatorio de cita programado para $doctorName")
                } else {
                    Log.w("AlarmScheduler", "⚠️ No hay permiso para alarmas exactas")
                }
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
                Log.d("AlarmScheduler", "⏰ Recordatorio de cita programado para $doctorName")
            }

        } catch (e: Exception) {
            Log.e("AlarmScheduler", "❌ Error programando recordatorio de cita: ${e.message}", e)
        }
    }

    /**
     * Cancelar recordatorio de una cita
     */
    fun cancelAppointmentReminder(context: Context, appointmentId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = NotificationReceiver.ACTION_APPOINTMENT_REMINDER
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            appointmentId.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
        )

        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
            Log.d("AlarmScheduler", "🗑️ Recordatorio de cita cancelado: $appointmentId")
        }
    }
}

