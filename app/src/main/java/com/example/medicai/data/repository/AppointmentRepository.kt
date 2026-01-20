package com.example.medicai.data.repository

import android.util.Log
import com.example.medicai.MedicAIApplication
import com.example.medicai.data.local.AppDatabase
import com.example.medicai.data.local.entity.toAppointment
import com.example.medicai.data.local.entity.toEntity
import com.example.medicai.data.models.Appointment
import com.example.medicai.data.models.AppointmentRequest
import com.example.medicai.data.models.Doctor
import com.example.medicai.data.models.DoctorUpsert
import com.example.medicai.data.models.Result
import com.example.medicai.data.remote.SupabaseClient
import com.example.medicai.utils.NetworkMonitor
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * Repositorio para operaciones CRUD de Citas Médicas
 * ✅ Usa Room como caché local y Supabase como backend remoto
 * ✅ Estrategia offline-first: lee de Room, sincroniza con Supabase cuando hay conexión
 */
class AppointmentRepository {

    private val client = SupabaseClient.client
    private val database = AppDatabase.getInstance(MedicAIApplication.getInstance())
    private val appointmentDao = database.appointmentDao()
    
    /**
     * Obtener todas las citas del usuario como Flow (reactivo)
     */
    fun getAppointmentsFlow(userId: String): Flow<List<Appointment>> {
        // Sincronizar en background
        syncAppointmentsFromServer(userId)
        
        return appointmentDao.getAppointmentsFlow(userId).map { entities ->
            entities.map { it.toAppointment() }
        }
    }

    /**
     * Sincronizar citas desde el servidor (función auxiliar)
     */
    private fun syncAppointmentsFromServer(userId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val context = MedicAIApplication.getInstance()
                if (NetworkMonitor.isNetworkAvailable(context)) {
                    // Subir pendientes locales antes de bajar del servidor
                    syncPendingAppointments(userId)

                    val remoteAppointments = client.from("appointments")
                        .select()
                        .decodeList<Appointment>()
                        .filter { it.user_id == userId }
                    
                    appointmentDao.insertAppointments(remoteAppointments.map { it.toEntity(isSynced = true) })
                    Log.d("AppointmentRepository", "✅ Sincronización de citas en background completada")
                }
            } catch (e: Exception) {
                Log.w("AppointmentRepository", "⚠️ Error en sincronización background de citas", e)
            }
        }
    }
    
    /**
     * Obtener todas las citas del usuario
     * Lee primero de caché local, luego sincroniza con servidor si hay conexión
     */
    suspend fun getAppointments(userId: String): Result<List<Appointment>> {
        return try {
            val context = MedicAIApplication.getInstance()
            
            // 1. Leer de caché local primero (offline-first)
            val cachedAppointments = appointmentDao.getAppointments(userId).map { it.toAppointment() }
            
            // 2. Si hay conexión, sincronizar con servidor
            if (NetworkMonitor.isNetworkAvailable(context)) {
                try {
                    // Subir pendientes locales antes de sincronizar
                    syncPendingAppointments(userId)

                    Log.d("AppointmentRepository", "Sincronizando citas desde servidor...")
                    
                    val remoteAppointments = client.from("appointments")
                        .select()
                        .decodeList<Appointment>()
                        .filter { it.user_id == userId }
                    
                    // Actualizar caché local
                    appointmentDao.insertAppointments(remoteAppointments.map { it.toEntity(isSynced = true) })
                    
                    Log.d("AppointmentRepository", "✅ ${remoteAppointments.size} citas sincronizadas")
                    Result.Success(remoteAppointments.sortedBy { it.date })
                } catch (e: Exception) {
                    Log.w("AppointmentRepository", "⚠️ Error sincronizando, usando caché local", e)
                    Result.Success(cachedAppointments)
                }
            } else {
                Log.d("AppointmentRepository", "📴 Sin conexión, usando ${cachedAppointments.size} citas en caché")
                Result.Success(cachedAppointments)
            }
        } catch (e: Exception) {
            Log.e("AppointmentRepository", "❌ Error obteniendo citas: ${e.message}", e)
            Result.Error(
                message = "Error al cargar citas: ${e.message}",
                exception = e
            )
        }
    }

    /**
     * Sincronizar citas locales no sincronizadas hacia el servidor
     */
    private suspend fun syncPendingAppointments(userId: String) {
        val pending = appointmentDao.getUnsyncedAppointments()
            .filter { it.user_id == userId }

        if (pending.isEmpty()) return

        pending.forEach { entity ->
            try {
                val payload = com.example.medicai.data.models.AppointmentUpsert(
                    id = entity.id,
                    user_id = entity.user_id,
                    doctor_name = entity.doctor_name,
                    specialty = entity.specialty,
                    date = entity.date,
                    time = entity.time,
                    location = entity.location,
                    notes = entity.notes,
                    status = entity.status
                )

                client.from("appointments")
                    .upsert(payload, onConflict = "id")

                // Sincronizar también el doctor asociado (en try-catch separado)
                try {
                    upsertDoctor(
                        userId = entity.user_id,
                        doctorName = entity.doctor_name,
                        specialty = entity.specialty
                    )
                } catch (doctorError: Exception) {
                    Log.w("AppointmentRepository", "⚠️ Error sincronizando doctor: ${doctorError.message}")
                }

                appointmentDao.markAsSynced(entity.id)
                Log.d("AppointmentRepository", "✅ Cita ${entity.id} sincronizada (con doctor)")
            } catch (e: Exception) {
                Log.w("AppointmentRepository", "⚠️ Error sincronizando cita ${entity.id}: ${e.message}")
            }
        }
    }

    /**
     * Obtener citas próximas (scheduled)
     * Lee desde caché local
     */
    suspend fun getUpcomingAppointments(userId: String): Result<List<Appointment>> {
        return try {
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())

            val appointments = appointmentDao.getPendingAppointments(userId)
                .map { it.toAppointment() }
                .filter { it.date >= today }
                .sortedBy { it.date }

            Log.d("AppointmentRepository", "✅ ${appointments.size} citas próximas desde caché")
            Result.Success(appointments)
        } catch (e: Exception) {
            Log.e("AppointmentRepository", "Error obteniendo citas próximas: ${e.message}", e)
            Result.Error(
                message = "Error al cargar citas próximas",
                exception = e
            )
        }
    }

    /**
     * Agregar nueva cita
     * Guarda primero en caché local, luego sincroniza con servidor
     */
    suspend fun addAppointment(appointment: AppointmentRequest): Result<Appointment> {
        return try {
            val context = MedicAIApplication.getInstance()
            
            if (NetworkMonitor.isNetworkAvailable(context)) {
                // Con conexión: guardar en servidor y en caché
                Log.d("AppointmentRepository", "Agregando cita con: ${appointment.doctor_name}")
                
                val newAppointment = client.from("appointments")
                    .insert(appointment) {
                        select()
                    }
                    .decodeSingle<Appointment>()

                // Guardar/actualizar médico en tabla dedicada
                upsertDoctor(
                    userId = appointment.user_id,
                    doctorName = appointment.doctor_name,
                    specialty = appointment.specialty
                )
                
                // Guardar en caché local
                appointmentDao.insertAppointment(newAppointment.toEntity(isSynced = true))
                
                Log.d("AppointmentRepository", "✅ Cita agregada y sincronizada: ${newAppointment.id}")
                Result.Success(newAppointment)
            } else {
                // Sin conexión: guardar solo en caché local
                Log.d("AppointmentRepository", "📴 Sin conexión, guardando en caché local")
                
                val tempId = java.util.UUID.randomUUID().toString()
                val localAppointment = Appointment(
                    id = tempId,
                    user_id = appointment.user_id,
                    doctor_name = appointment.doctor_name,
                    specialty = appointment.specialty,
                    date = appointment.date,
                    time = appointment.time,
                    location = appointment.location,
                    notes = appointment.notes,
                    status = appointment.status,
                    created_at = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                )
                
                appointmentDao.insertAppointment(localAppointment.toEntity(isSynced = false))
                
                Result.Success(localAppointment)
            }
        } catch (e: Exception) {
            Log.e("AppointmentRepository", "❌ Error agregando cita: ${e.message}", e)
            Result.Error(
                message = "Error al agendar cita: ${e.message}",
                exception = e
            )
        }
    }

    /**
     * Actualizar cita existente
     * Actualiza en caché local y servidor
     */
    suspend fun updateAppointment(id: String, appointment: AppointmentRequest): Result<Appointment> {
        return try {
            Log.d("AppointmentRepository", "Actualizando cita: $id")
            
            val context = MedicAIApplication.getInstance()
            
            if (NetworkMonitor.isNetworkAvailable(context)) {
                // Con conexión: actualizar en servidor
                val updated = client.from("appointments")
                    .update(appointment) {
                        select()
                        filter {
                            eq("id", id)
                        }
                    }
                    .decodeSingle<Appointment>()

                // Guardar/actualizar médico en tabla dedicada
                upsertDoctor(
                    userId = appointment.user_id,
                    doctorName = appointment.doctor_name,
                    specialty = appointment.specialty
                )
                
                // Actualizar en caché local
                appointmentDao.insertAppointment(updated.toEntity(isSynced = true))
                
                Log.d("AppointmentRepository", "✅ Cita actualizada y sincronizada: $id")
                Result.Success(updated)
            } else {
                // Sin conexión: actualizar solo en caché local
                val cachedAppointment = appointmentDao.getAppointmentById(id)
                if (cachedAppointment != null) {
                    val updatedEntity = cachedAppointment.copy(
                        doctor_name = appointment.doctor_name,
                        specialty = appointment.specialty,
                        date = appointment.date,
                        time = appointment.time,
                        location = appointment.location,
                        notes = appointment.notes,
                        status = appointment.status,
                        is_synced = false
                    )
                    appointmentDao.insertAppointment(updatedEntity)
                    
                    Log.d("AppointmentRepository", "📴 Cita actualizada localmente: $id")
                    Result.Success(updatedEntity.toAppointment())
                } else {
                    Result.Error(message = "Cita no encontrada en caché")
                }
            }
        } catch (e: Exception) {
            Log.e("AppointmentRepository", "❌ Error actualizando cita: ${e.message}", e)
            Result.Error(
                message = "Error al actualizar cita: ${e.message}",
                exception = e
            )
        }
    }

    /**
     * Obtener médicos del usuario
     */
    suspend fun getDoctors(userId: String): Result<List<Doctor>> {
        return try {
            val context = MedicAIApplication.getInstance()

            if (NetworkMonitor.isNetworkAvailable(context)) {
                try {
                    val doctors = client.from("doctors")
                        .select {
                            filter {
                                eq("user_id", userId)
                            }
                        }
                        .decodeList<Doctor>()

                    Result.Success(doctors.sortedBy { it.doctor_name })
                } catch (e: Exception) {
                    Log.w("AppointmentRepository", "⚠️ Error cargando médicos remotos: ${e.message}")
                    Result.Success(getDoctorsFromCache(userId))
                }
            } else {
                Result.Success(getDoctorsFromCache(userId))
            }
        } catch (e: Exception) {
            Log.e("AppointmentRepository", "❌ Error obteniendo médicos: ${e.message}", e)
            Result.Success(getDoctorsFromCache(userId))
        }
    }

    private suspend fun getDoctorsFromCache(userId: String): List<Doctor> {
        val cachedAppointments = appointmentDao.getAppointments(userId)
        return cachedAppointments
            .groupBy { it.doctor_name }
            .map { (name, items) ->
                Doctor(
                    user_id = userId,
                    doctor_name = name,
                    specialty = items.first().specialty
                )
            }
            .sortedBy { it.doctor_name }
    }

    /**
     * Insertar/actualizar médico (upsert) en tabla dedicada
     */
    suspend fun upsertDoctor(userId: String, doctorName: String, specialty: String) {
        try {
            val context = MedicAIApplication.getInstance()
            if (!NetworkMonitor.isNetworkAvailable(context)) return

            val doctor = DoctorUpsert(
                user_id = userId,
                doctor_name = doctorName.trim(),
                specialty = specialty.trim()
            )

            client.from("doctors")
                .upsert(doctor, onConflict = "user_id,doctor_name")
                
            Log.d("AppointmentRepository", "✅ Doctor ${doctorName} guardado/actualizado")
        } catch (e: Exception) {
            Log.w("AppointmentRepository", "⚠️ No se pudo guardar médico: ${e.message}")
        }
    }

    /**
     * Cancelar cita (cambiar status a cancelled)
     */
    suspend fun cancelAppointment(id: String): Result<Unit> {
        return try {
            Log.d("AppointmentRepository", "Cancelando cita: $id")
            
            val context = MedicAIApplication.getInstance()
            
            if (NetworkMonitor.isNetworkAvailable(context)) {
                // Con conexión: cancelar en servidor
                client.from("appointments")
                    .update(mapOf("status" to "cancelled")) {
                        filter {
                            eq("id", id)
                        }
                    }
                
                // Actualizar en caché local
                val cachedAppointment = appointmentDao.getAppointmentById(id)
                if (cachedAppointment != null) {
                    appointmentDao.insertAppointment(cachedAppointment.copy(status = "cancelled", is_synced = true))
                }
            } else {
                // Sin conexión: cancelar solo en caché local
                val cachedAppointment = appointmentDao.getAppointmentById(id)
                if (cachedAppointment != null) {
                    appointmentDao.insertAppointment(cachedAppointment.copy(status = "cancelled", is_synced = false))
                }
            }

            Log.d("AppointmentRepository", "✅ Cita cancelada: $id")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e("AppointmentRepository", "❌ Error cancelando cita: ${e.message}", e)
            Result.Error(
                message = "Error al cancelar cita: ${e.message}",
                exception = e
            )
        }
    }

    /**
     * Completar cita (cambiar status a completed)
     */
    suspend fun completeAppointment(id: String): Result<Unit> {
        return try {
            Log.d("AppointmentRepository", "Completando cita: $id")
            
            val context = MedicAIApplication.getInstance()
            
            if (NetworkMonitor.isNetworkAvailable(context)) {
                // Con conexión: completar en servidor
                client.from("appointments")
                    .update(mapOf("status" to "completed")) {
                        filter {
                            eq("id", id)
                        }
                    }
                
                // Actualizar en caché local
                val cachedAppointment = appointmentDao.getAppointmentById(id)
                if (cachedAppointment != null) {
                    appointmentDao.insertAppointment(cachedAppointment.copy(status = "completed", is_synced = true))
                }
            } else {
                // Sin conexión: completar solo en caché local
                val cachedAppointment = appointmentDao.getAppointmentById(id)
                if (cachedAppointment != null) {
                    appointmentDao.insertAppointment(cachedAppointment.copy(status = "completed", is_synced = false))
                }
            }

            Log.d("AppointmentRepository", "✅ Cita completada: $id")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e("AppointmentRepository", "Error completando cita: ${e.message}", e)
            Result.Error(
                message = "Error al completar cita",
                exception = e
            )
        }
    }

    /**
     * Eliminar cita permanentemente
     */
    suspend fun deleteAppointment(id: String): Result<Unit> {
        return try {
            Log.d("AppointmentRepository", "Eliminando cita: $id")
            
            val context = MedicAIApplication.getInstance()
            
            if (NetworkMonitor.isNetworkAvailable(context)) {
                // Con conexión: eliminar del servidor
                client.from("appointments")
                    .delete {
                        filter {
                            eq("id", id)
                        }
                    }
            }
            
            // Eliminar de caché local siempre
            appointmentDao.deleteAppointmentById(id)

            Log.d("AppointmentRepository", "✅ Cita eliminada: $id")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e("AppointmentRepository", "❌ Error eliminando cita: ${e.message}", e)
            Result.Error(
                message = "Error al eliminar cita: ${e.message}",
                exception = e
            )
        }
    }
}

