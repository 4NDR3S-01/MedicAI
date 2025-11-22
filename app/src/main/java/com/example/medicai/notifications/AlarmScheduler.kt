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
        Log.d("AlarmScheduler", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d("AlarmScheduler", "📋 Programando recordatorios para: $medicineName")
        Log.d("AlarmScheduler", "🆔 ID: $medicineId")
        Log.d("AlarmScheduler", "💊 Dosis: $dosage")
        Log.d("AlarmScheduler", "⏰ Horarios: ${times.joinToString(", ")}")
        
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Verificar si se pueden programar alarmas exactas en Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.e("AlarmScheduler", "❌ No hay permiso para programar alarmas exactas")
                android.widget.Toast.makeText(
                    context,
                    "⚠️ Activa los permisos de alarmas en Ajustes para recibir notificaciones",
                    android.widget.Toast.LENGTH_LONG
                ).show()
                return
            } else {
                Log.d("AlarmScheduler", "✅ Permiso de alarmas exactas concedido")
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

                    // Si la hora ya pasó hoy, programar para mañana
                    if (timeInMillis <= System.currentTimeMillis()) {
                        add(Calendar.DAY_OF_YEAR, 1)
                        Log.d("AlarmScheduler", "⏭️ Hora $time ya pasó hoy, programando para mañana")
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

                // Programar alarma exacta
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
                Log.d("AlarmScheduler", "   ├─ Request Code: $requestCode")
                Log.d("AlarmScheduler", "   └─ Millis: ${calendar.timeInMillis}")

            } catch (e: Exception) {
                Log.e("AlarmScheduler", "❌ Error programando alarma #$index: ${e.message}", e)
            }
        }
        
        Log.d("AlarmScheduler", "✅ Proceso completado: ${times.size} alarmas programadas")
        Log.d("AlarmScheduler", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
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
        Log.d("AlarmScheduler", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d("AlarmScheduler", "📋 Programando recordatorios CON ANTICIPACIÓN para: $medicineName")
        Log.d("AlarmScheduler", "🆔 ID: $medicineId")
        Log.d("AlarmScheduler", "💊 Dosis: $dosage")
        Log.d("AlarmScheduler", "⏰ Horarios: ${times.joinToString(", ")}")
        Log.d("AlarmScheduler", "⏱️ Recordatorio anticipado: $minutesBefore minutos antes")
        
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Verificar permisos
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.e("AlarmScheduler", "❌ No hay permiso para programar alarmas exactas")
                android.widget.Toast.makeText(
                    context,
                    "⚠️ Activa los permisos de alarmas en Ajustes para recibir notificaciones",
                    android.widget.Toast.LENGTH_LONG
                ).show()
                return
            } else {
                Log.d("AlarmScheduler", "✅ Permiso de alarmas exactas concedido")
            }
        }

        times.forEachIndexed { index, time ->
            Log.d("AlarmScheduler", "")
            Log.d("AlarmScheduler", "━━━ Procesando horario #$index: $time ━━━")
            
            try {
                val currentTimeMillis = System.currentTimeMillis()
                val currentTimeStr = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault()).format(Date(currentTimeMillis))
                Log.d("AlarmScheduler", "🕐 Hora actual del sistema: $currentTimeStr ($currentTimeMillis ms)")
                
                // 1. Programar recordatorio anticipado
                val advanceCalendar = Calendar.getInstance().apply {
                    val timeParts = time.split(":")
                    Log.d("AlarmScheduler", "📝 Parseando hora: ${timeParts[0]}:${timeParts[1]}")
                    
                    set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                    set(Calendar.MINUTE, timeParts[1].toInt())
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    
                    val beforeAdvance = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(this.time)
                    Log.d("AlarmScheduler", "📅 Hora base configurada: $beforeAdvance (${this.timeInMillis} ms)")
                    
                    add(Calendar.MINUTE, -minutesBefore)
                    
                    val afterAdvance = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(this.time)
                    Log.d("AlarmScheduler", "⏰ Después de restar $minutesBefore min: $afterAdvance (${this.timeInMillis} ms)")
                    
                    val diff = this.timeInMillis - currentTimeMillis
                    Log.d("AlarmScheduler", "⏱️ Diferencia con hora actual: ${diff}ms (${diff/1000} segundos)")

                    if (this.timeInMillis < currentTimeMillis) {
                        Log.d("AlarmScheduler", "⚠️ La hora anticipada YA PASÓ (${this.timeInMillis} < $currentTimeMillis)")
                        add(Calendar.DAY_OF_YEAR, 1)
                        val tomorrowTime = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(this.time)
                        Log.d("AlarmScheduler", "⏭️ Programando para MAÑANA: $tomorrowTime")
                    } else {
                        Log.d("AlarmScheduler", "✅ La hora anticipada está en el FUTURO, programando para HOY")
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

                val advanceTime = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault()).format(advanceCalendar.time)
                Log.d("AlarmScheduler", "🔔 Recordatorio anticipado #$index:")
                Log.d("AlarmScheduler", "   ├─ Horario base: $time")
                Log.d("AlarmScheduler", "   ├─ Programado para: $advanceTime")
                Log.d("AlarmScheduler", "   └─ Request Code: $advanceRequestCode")

                // 2. Programar recordatorio a la hora exacta
                Log.d("AlarmScheduler", "")
                Log.d("AlarmScheduler", "━━━ Configurando recordatorio EXACTO ━━━")
                
                val exactCalendar = Calendar.getInstance().apply {
                    val timeParts = time.split(":")
                    set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                    set(Calendar.MINUTE, timeParts[1].toInt())
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)

                    val exactTime = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(this.time)
                    Log.d("AlarmScheduler", "📅 Hora exacta configurada: $exactTime (${this.timeInMillis} ms)")
                    
                    val diffExact = this.timeInMillis - currentTimeMillis
                    Log.d("AlarmScheduler", "⏱️ Diferencia con hora actual: ${diffExact}ms (${diffExact/1000} segundos)")

                    if (this.timeInMillis < currentTimeMillis) {
                        Log.d("AlarmScheduler", "⚠️ La hora exacta YA PASÓ (${this.timeInMillis} < $currentTimeMillis)")
                        add(Calendar.DAY_OF_YEAR, 1)
                        val tomorrowExact = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(this.time)
                        Log.d("AlarmScheduler", "⏭️ Programando para MAÑANA: $tomorrowExact")
                    } else {
                        Log.d("AlarmScheduler", "✅ La hora exacta está en el FUTURO, programando para HOY")
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
                    Log.d("AlarmScheduler", "📲 Llamada a setExactAndAllowWhileIdle() EXITOSA")
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        exactCalendar.timeInMillis,
                        exactPendingIntent
                    )
                    Log.d("AlarmScheduler", "📲 Llamada a setExact() EXITOSA")
                }

                val exactTime = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault()).format(exactCalendar.time)
                Log.d("AlarmScheduler", "⏰ Recordatorio exacto #$index:")
                Log.d("AlarmScheduler", "   ├─ Horario: $time")
                Log.d("AlarmScheduler", "   ├─ Programado para: $exactTime")
                Log.d("AlarmScheduler", "   └─ Request Code: $exactRequestCode")
                Log.d("AlarmScheduler", "✅ Horario #$index COMPLETADO EXITOSAMENTE")

            } catch (e: Exception) {
                Log.e("AlarmScheduler", "❌❌❌ EXCEPCIÓN CRÍTICA programando alarmas #$index ❌❌❌", e)
                Log.e("AlarmScheduler", "   - Tipo: ${e.javaClass.name}")
                Log.e("AlarmScheduler", "   - Mensaje: ${e.message}")
                Log.e("AlarmScheduler", "   - Stack: ${e.stackTraceToString()}")
            }
        }
        
        Log.d("AlarmScheduler", "✅ Proceso completado: ${times.size * 2} alarmas programadas (anticipadas + exactas)")
        Log.d("AlarmScheduler", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
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
        time: String, // formato "HH:mm"
        location: String,
        minutesBefore: Int = 15
    ) {
        Log.d("AlarmScheduler", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d("AlarmScheduler", "📅 Programando recordatorio de CITA")
        Log.d("AlarmScheduler", "🆔 ID: $appointmentId")
        Log.d("AlarmScheduler", "👨‍⚕️ Doctor: $doctorName")
        Log.d("AlarmScheduler", "🏥 Especialidad: $specialty")
        Log.d("AlarmScheduler", "📆 Fecha: $date")
        Log.d("AlarmScheduler", "⏰ Hora: $time")
        Log.d("AlarmScheduler", "📍 Ubicación: $location")
        Log.d("AlarmScheduler", "⏱️ Recordatorio: $minutesBefore min antes")
        
        try {
            val currentTimeMillis = System.currentTimeMillis()
            val currentTimeStr = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(currentTimeMillis))
            Log.d("AlarmScheduler", "🕐 Hora actual: $currentTimeStr")
            
            val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            Log.d("AlarmScheduler", "📝 Parseando fecha/hora: $date $time")
            
            val appointmentDateTime = dateTimeFormat.parse("$date $time")
            if (appointmentDateTime == null) {
                Log.e("AlarmScheduler", "❌ ERROR: No se pudo parsear la fecha/hora '$date $time'")
                return
            }
            
            val appointmentStr = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(appointmentDateTime)
            Log.d("AlarmScheduler", "📅 Cita parseada: $appointmentStr")

            val calendar = Calendar.getInstance().apply {
                this.time = appointmentDateTime
                Log.d("AlarmScheduler", "📅 Antes de restar: ${SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(this.time)}")
                
                add(Calendar.MINUTE, -minutesBefore) // Recordar X minutos antes
                
                val reminderTime = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(this.time)
                Log.d("AlarmScheduler", "⏰ Después de restar $minutesBefore min: $reminderTime")
                Log.d("AlarmScheduler", "⏰ Timestamp: ${this.timeInMillis} ms")
                
                val diff = this.timeInMillis - currentTimeMillis
                Log.d("AlarmScheduler", "⏱️ Diferencia con ahora: ${diff}ms (${diff/1000} segundos, ${diff/60000} minutos)")
            }

            // Solo programar si la fecha es futura
            if (calendar.timeInMillis < currentTimeMillis) {
                Log.w("AlarmScheduler", "⚠️⚠️⚠️ La cita YA PASÓ ⚠️⚠️⚠️")
                Log.w("AlarmScheduler", "   - Recordatorio: ${calendar.timeInMillis} ms")
                Log.w("AlarmScheduler", "   - Ahora: $currentTimeMillis ms")
                Log.w("AlarmScheduler", "   ❌ NO SE PROGRAMA ALARMA")
                Log.d("AlarmScheduler", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                return
            } else {
                Log.d("AlarmScheduler", "✅ La cita está en el FUTURO, programando alarma...")
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
            Log.d("AlarmScheduler", "📱 Request Code: $requestCode")
            
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    Log.d("AlarmScheduler", "✅ Permiso de alarmas exactas CONCEDIDO")
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                    Log.d("AlarmScheduler", "📲 Llamada a setExactAndAllowWhileIdle() EXITOSA")
                    Log.d("AlarmScheduler", "✅ Recordatorio de cita programado para $doctorName")
                } else {
                    Log.e("AlarmScheduler", "❌ NO HAY PERMISO para alarmas exactas")
                }
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
                Log.d("AlarmScheduler", "📲 Llamada a setExact() EXITOSA")
                Log.d("AlarmScheduler", "✅ Recordatorio de cita programado para $doctorName")
            }
            
            Log.d("AlarmScheduler", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        } catch (e: Exception) {
            Log.e("AlarmScheduler", "❌❌❌ EXCEPCIÓN CRÍTICA programando recordatorio de cita ❌❌❌", e)
            Log.e("AlarmScheduler", "   - Tipo: ${e.javaClass.name}")
            Log.e("AlarmScheduler", "   - Mensaje: ${e.message}")
            Log.e("AlarmScheduler", "   - Stack: ${e.stackTraceToString()}")
            Log.d("AlarmScheduler", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
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

