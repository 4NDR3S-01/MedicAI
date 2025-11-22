package com.example.medicai.notifications

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.medicai.data.repository.MedicineRepository
import com.example.medicai.data.repository.AppointmentRepository
import com.example.medicai.data.local.UserPreferencesManager
import com.example.medicai.data.models.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Worker para reprogramar alarmas después del reinicio del dispositivo
 * Consulta medicamentos activos y citas próximas desde Supabase y reprograma alarmas
 */
class AlarmReschedulerWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("AlarmRescheduler", "🔄 Iniciando reprogramación de alarmas después de reinicio...")
                
                // ✅ Obtener userId del usuario actual logueado (desde caché local)
                val userId = UserPreferencesManager.getUserId(applicationContext)
                if (userId == null) {
                    Log.w("AlarmRescheduler", "⚠️ No hay usuario logueado, no se reprograman alarmas")
                    return@withContext Result.success()
                }
                
                // ✅ Verificar si las notificaciones están habilitadas
                val notificationsEnabled = UserPreferencesManager.areNotificationsEnabled(applicationContext)
                if (!notificationsEnabled) {
                    Log.d("AlarmRescheduler", "🔕 Notificaciones deshabilitadas, no se reprograman alarmas")
                    return@withContext Result.success()
                }
                
                Log.d("AlarmRescheduler", "👤 Reprogramando alarmas para usuario: $userId")
                
                // 💊 Reprogramar medicamentos activos
                try {
                    val medicineRepository = MedicineRepository()
                    when (val medicinesResult = medicineRepository.getMedicines(userId)) {
                        is com.example.medicai.data.models.Result.Success -> {
                            val activeMedicines = medicinesResult.data.filter { it.active }
                            
                            activeMedicines.forEach { medicine ->
                                try {
                                    AlarmScheduler.scheduleMedicineReminders(
                                        context = applicationContext,
                                        medicineId = medicine.id,
                                        medicineName = medicine.name,
                                        dosage = medicine.dosage,
                                        times = medicine.times
                                    )
                                } catch (e: Exception) {
                                    Log.e("AlarmRescheduler", "❌ Error reprogramando ${medicine.name}: ${e.message}")
                                }
                            }
                            
                            Log.d("AlarmRescheduler", "✅ ${activeMedicines.size} medicamentos reprogramados")
                        }
                        is com.example.medicai.data.models.Result.Error -> {
                            Log.e("AlarmRescheduler", "❌ Error obteniendo medicamentos: ${medicinesResult.message}")
                        }
                        else -> {
                            Log.w("AlarmRescheduler", "⚠️ Estado desconocido al obtener medicamentos")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("AlarmRescheduler", "❌ Error en reprogramación de medicamentos: ${e.message}", e)
                }
                
                // 📅 Reprogramar citas próximas
                try {
                    val appointmentRepository = AppointmentRepository()
                    when (val appointmentsResult = appointmentRepository.getUpcomingAppointments(userId)) {
                        is com.example.medicai.data.models.Result.Success -> {
                            val upcomingAppointments = appointmentsResult.data
                            
                            // ✅ Obtener reminder_minutes del usuario
                            val reminderMinutes = UserPreferencesManager.getReminderMinutes(applicationContext)
                            
                            upcomingAppointments.forEach { appointment ->
                                try {
                                    AlarmScheduler.scheduleAppointmentReminder(
                                        context = applicationContext,
                                        appointmentId = appointment.id,
                                        doctorName = appointment.doctor_name,
                                        specialty = appointment.specialty,
                                        date = appointment.date,
                                        time = appointment.time,
                                        location = appointment.location,
                                        minutesBefore = reminderMinutes
                                    )
                                } catch (e: Exception) {
                                    Log.e("AlarmRescheduler", "❌ Error reprogramando cita con ${appointment.doctor_name}: ${e.message}")
                                }
                            }
                            
                            Log.d("AlarmRescheduler", "✅ ${upcomingAppointments.size} citas reprogramadas")
                        }
                        is com.example.medicai.data.models.Result.Error -> {
                            Log.e("AlarmRescheduler", "❌ Error obteniendo citas: ${appointmentsResult.message}")
                        }
                        else -> {
                            Log.w("AlarmRescheduler", "⚠️ Estado desconocido al obtener citas")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("AlarmRescheduler", "❌ Error en reprogramación de citas: ${e.message}", e)
                }
                
                Log.d("AlarmRescheduler", "✅ Reprogramación de alarmas completada")
                Result.success()
                
            } catch (e: Exception) {
                Log.e("AlarmRescheduler", "❌ Error general reprogramando alarmas: ${e.message}", e)
                Result.failure()
            }
        }
    }
}
