package com.example.medicai.data.repository

import android.util.Log
import com.example.medicai.data.models.Appointment
import com.example.medicai.data.models.AppointmentRequest
import com.example.medicai.data.models.Result
import com.example.medicai.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.from

/**
 * Repositorio para operaciones CRUD de Citas Médicas
 */
class AppointmentRepository {

    private val client = SupabaseClient.client

    /**
     * Obtener todas las citas del usuario
     */
    suspend fun getAppointments(userId: String): Result<List<Appointment>> {
        return try {
            Log.d("AppointmentRepository", "Obteniendo citas para user: $userId")

            val appointments = client.from("appointments")
                .select()
                .decodeList<Appointment>()
                .filter { it.user_id == userId }
                .sortedBy { it.date }

            Log.d("AppointmentRepository", "✅ ${appointments.size} citas obtenidas")
            Result.Success(appointments)
        } catch (e: Exception) {
            Log.e("AppointmentRepository", "❌ Error obteniendo citas: ${e.message}", e)
            Result.Error(
                message = "Error al cargar citas: ${e.message}",
                exception = e
            )
        }
    }

    /**
     * Obtener citas próximas (scheduled)
     */
    suspend fun getUpcomingAppointments(userId: String): Result<List<Appointment>> {
        return try {
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())

            val appointments = client.from("appointments")
                .select()
                .decodeList<Appointment>()
                .filter {
                    it.user_id == userId &&
                    it.status == "scheduled" &&
                    it.date >= today
                }
                .sortedBy { it.date }

            Log.d("AppointmentRepository", "✅ ${appointments.size} citas próximas")
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
     */
    suspend fun addAppointment(appointment: AppointmentRequest): Result<Appointment> {
        return try {
            Log.d("AppointmentRepository", "Agregando cita con: ${appointment.doctor_name}")

            val newAppointment = client.from("appointments")
                .insert(appointment) {
                    select()
                }
                .decodeSingle<Appointment>()

            Log.d("AppointmentRepository", "✅ Cita agregada: ${newAppointment.id}")
            Result.Success(newAppointment)
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
     */
    suspend fun updateAppointment(id: String, appointment: AppointmentRequest): Result<Appointment> {
        return try {
            Log.d("AppointmentRepository", "Actualizando cita: $id")

            val updated = client.from("appointments")
                .update(appointment) {
                    select()
                    filter {
                        eq("id", id)
                    }
                }
                .decodeSingle<Appointment>()

            Log.d("AppointmentRepository", "✅ Cita actualizada: $id")
            Result.Success(updated)
        } catch (e: Exception) {
            Log.e("AppointmentRepository", "❌ Error actualizando cita: ${e.message}", e)
            Result.Error(
                message = "Error al actualizar cita: ${e.message}",
                exception = e
            )
        }
    }

    /**
     * Cancelar cita (cambiar status a cancelled)
     */
    suspend fun cancelAppointment(id: String): Result<Unit> {
        return try {
            Log.d("AppointmentRepository", "Cancelando cita: $id")

            client.from("appointments")
                .update(mapOf("status" to "cancelled")) {
                    filter {
                        eq("id", id)
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

            client.from("appointments")
                .update(mapOf("status" to "completed")) {
                    filter {
                        eq("id", id)
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

            client.from("appointments")
                .delete {
                    filter {
                        eq("id", id)
                    }
                }

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

