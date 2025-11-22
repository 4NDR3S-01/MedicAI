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
        times: List<String>
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Verificar permisos en Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.e("AlarmScheduler", "No hay permiso para programar alarmas exactas")
                android.widget.Toast.makeText(
                    context,
                    "⚠️ Activa los permisos de alarmas en Ajustes para recibir notificaciones",
                    android.widget.Toast.LENGTH_LONG
                ).show()
                return
            }
        }

        times.forEachIndexed { index, time ->
            try {
                val calendar = Calendar.getInstance().apply {
                    val timeParts = time.split(":")
                    set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                    set(Calendar.MINUTE, timeParts[1].toInt())
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)

                    if (timeInMillis < System.currentTimeMillis()) {
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
                
                val currentTime = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                val alarmTime = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault()).format(calendar.time)
                
                Log.d("AlarmScheduler", "⏰ Alarma #$index programada:")
                Log.d("AlarmScheduler", "   ├─ Horario: $time")
                Log.d("AlarmScheduler", "   ├─ Fecha/Hora actual: $currentTime")
                Log.d("AlarmScheduler", "   ├─ Programada para: $alarmTime")

            } catch (e: Exception) {
                Log.e("AlarmScheduler", "Error programando alarma para horario $time", e)
            }
        }
    }

    /**
     * Programar recordatorios para un medicamento con recordatorio anticipado
     */
    fun scheduleMedicineRemindersWithAdvance(
        context: Context,
        medicineId: String,
        medicineName: String,
        dosage: String,
        times: List<String>,
        minutesBefore: Int = 5
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Verificar permisos
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.e("AlarmScheduler", "No hay permiso para programar alarmas exactas")
                android.widget.Toast.makeText(
                    context,
                    "⚠️ Activa los permisos de alarmas en Ajustes para recibir notificaciones",
                    android.widget.Toast.LENGTH_LONG
                ).show()
                return
            }
        }

        times.forEachIndexed { index, time ->
            try {
                val currentTimeMillis = System.currentTimeMillis()
                
                // 1. Programar recordatorio anticipado
                val advanceCalendar = Calendar.getInstance().apply {
                    val timeParts = time.split(":")
                    set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                    set(Calendar.MINUTE, timeParts[1].toInt())
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    add(Calendar.MINUTE, -minutesBefore)

                    if (this.timeInMillis < currentTimeMillis) {
                        add(Calendar.DAY_OF_YEAR, 1)
                    }
                }

                val advanceIntent = Intent(context, NotificationReceiver::class.java).apply {
                    action = NotificationReceiver.ACTION_MEDICINE_REMINDER
                    putExtra(NotificationReceiver.EXTRA_MEDICINE_ID, medicineId)
                    putExtra(NotificationReceiver.EXTRA_MEDICINE_NAME, medicineName)
                    putExtra(NotificationReceiver.EXTRA_MEDICINE_DOSAGE, dosage)
                    putExtra(NotificationReceiver.EXTRA_MEDICINE_TIME, "$time (🔔 Recordatorio en $minutesBefore min)")
                    putExtra("is_advance", true)
                }

                val advanceRequestCode = (medicineId + "_advance_" + index).hashCode()
                val advancePendingIntent = PendingIntent.getBroadcast(
                    context,
                    advanceRequestCode,
                    advanceIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        advanceCalendar.timeInMillis,
                        advancePendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        advanceCalendar.timeInMillis,
                        advancePendingIntent
                    )
                }

                // 2. Programar recordatorio a la hora exacta
                val exactCalendar = Calendar.getInstance().apply {
                    val timeParts = time.split(":")
                    set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                    set(Calendar.MINUTE, timeParts[1].toInt())
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)

                    if (this.timeInMillis < currentTimeMillis) {
                        add(Calendar.DAY_OF_YEAR, 1)
                    }
                }

                val exactIntent = Intent(context, NotificationReceiver::class.java).apply {
                    action = NotificationReceiver.ACTION_MEDICINE_REMINDER
                    putExtra(NotificationReceiver.EXTRA_MEDICINE_ID, medicineId)
                    putExtra(NotificationReceiver.EXTRA_MEDICINE_NAME, medicineName)
                    putExtra(NotificationReceiver.EXTRA_MEDICINE_DOSAGE, dosage)
                    putExtra(NotificationReceiver.EXTRA_MEDICINE_TIME, time)
                    putExtra("is_advance", false)
                }

                val exactRequestCode = (medicineId + "_exact_" + index).hashCode()
                val exactPendingIntent = PendingIntent.getBroadcast(
                    context,
                    exactRequestCode,
                    exactIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        exactCalendar.timeInMillis,
                        exactPendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        exactCalendar.timeInMillis,
                        exactPendingIntent
                    )
                }

            } catch (e: Exception) {
                Log.e("AlarmScheduler", "Error programando alarmas para horario #$index", e)
            }
        }
    }

    /**
     * Programar la siguiente alarma para el día siguiente
     * Esto permite tener alarmas diarias sin usar setRepeating
     */
    private fun scheduleNextDayAlarm(
        context: Context,
        alarmManager: AlarmManager,
        currentCalendar: Calendar,
        pendingIntent: PendingIntent,
        medicineId: String,
        index: Int
    ) {
        val nextDayCalendar = currentCalendar.clone() as Calendar
        nextDayCalendar.add(Calendar.DAY_OF_YEAR, 1)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextDayCalendar.timeInMillis,
                pendingIntent
            )
        }
    }

    /**
     * Cancelar todos los recordatorios de un medicamento (anticipados y exactos)
     */
    fun cancelMedicineReminders(
        context: Context,
        medicineId: String,
        timesCount: Int
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        Log.d("AlarmScheduler", "🗑️ Cancelando alarmas para medicamento $medicineId")

        repeat(timesCount) { index ->
            // Cancelar recordatorio anticipado
            val advanceIntent = Intent(context, NotificationReceiver::class.java).apply {
                action = NotificationReceiver.ACTION_MEDICINE_REMINDER
            }
            val advanceRequestCode = (medicineId + "_advance_" + index).hashCode()
            val advancePendingIntent = PendingIntent.getBroadcast(
                context,
                advanceRequestCode,
                advanceIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
            )
            advancePendingIntent?.let {
                alarmManager.cancel(it)
                it.cancel()
                Log.d("AlarmScheduler", "🗑️ Alarma anticipada #$index cancelada")
            }

            // Cancelar recordatorio exacto
            val exactIntent = Intent(context, NotificationReceiver::class.java).apply {
                action = NotificationReceiver.ACTION_MEDICINE_REMINDER
            }
            val exactRequestCode = (medicineId + "_exact_" + index).hashCode()
            val exactPendingIntent = PendingIntent.getBroadcast(
                context,
                exactRequestCode,
                exactIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
            )
            exactPendingIntent?.let {
                alarmManager.cancel(it)
                it.cancel()
                Log.d("AlarmScheduler", "🗑️ Alarma exacta #$index cancelada")
            }
        }
        
        Log.d("AlarmScheduler", "✅ Total de ${timesCount * 2} alarmas canceladas")
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
        time: String, // formato "HH:mm" o "HH:mm:ss"
        location: String,
        minutesBefore: Int = 15
    ) {
        try {
            val currentTimeMillis = System.currentTimeMillis()
            
            // Normalizar hora a formato HH:mm (remover segundos si vienen de Supabase)
            val normalizedTime = if (time.length > 5) time.substring(0, 5) else time
            
            val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val appointmentDateTime = dateTimeFormat.parse("$date $normalizedTime")
            
            if (appointmentDateTime == null) {
                Log.e("AlarmScheduler", "Error parseando fecha/hora de la cita: $date $time")
                return
            }

            // Verificar que la cita sea futura
            val appointmentTimeMillis = appointmentDateTime.time
            if (appointmentTimeMillis < currentTimeMillis) {
                return
            }

            // Calcular hora del recordatorio (X minutos antes)
            val calendar = Calendar.getInstance().apply {
                this.time = appointmentDateTime
                add(Calendar.MINUTE, -minutesBefore)
            }

            // Si el recordatorio anticipado ya pasó, programar para la hora exacta de la cita
            val alarmTimeMillis = if (calendar.timeInMillis < currentTimeMillis) {
                appointmentTimeMillis
            } else {
                calendar.timeInMillis
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

            val requestCode = appointmentId.hashCode()
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        alarmTimeMillis,
                        pendingIntent
                    )
                } else {
                    Log.w("AlarmScheduler", "No hay permiso para alarmas exactas")
                }
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    alarmTimeMillis,
                    pendingIntent
                )
            }

        } catch (e: Exception) {
            Log.e("AlarmScheduler", "Error programando recordatorio de cita", e)
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

