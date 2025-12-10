package com.example.medicai.data.repository

import android.util.Log
import com.example.medicai.MedicAIApplication
import com.example.medicai.data.models.Medicine
import com.example.medicai.data.models.MedicineRequest
import com.example.medicai.data.models.Result
import com.example.medicai.data.remote.SupabaseClient
import com.example.medicai.utils.NetworkMonitor
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import java.io.IOException

/**
 * Repositorio para operaciones CRUD de Medicamentos
 * ✅ Incluye detección de conexión a internet
 */
class MedicineRepository {

    private val client = SupabaseClient.client

    /**
     * Obtener todos los medicamentos del usuario (activos e inactivos)
     */
    suspend fun getMedicines(userId: String): Result<List<Medicine>> {
        return try {
            // Verificar conexión a internet
            val context = MedicAIApplication.getInstance()
            if (!NetworkMonitor.isNetworkAvailable(context)) {
                Log.w("MedicineRepository", "⚠️ Sin conexión a internet")
                return Result.Error(
                    message = "Sin conexión a internet. Por favor verifica tu conexión.",
                    exception = IOException("No hay conexión a internet")
                )
            }

            Log.d("MedicineRepository", "Obteniendo medicamentos para user: $userId")

            val medicines = client.from("medicines")
                .select()
                .decodeList<Medicine>()
                .filter { it.user_id == userId }
                .sortedByDescending { it.created_at }

            Log.d("MedicineRepository", "✅ ${medicines.size} medicamentos obtenidos")
            Result.Success(medicines)
        } catch (e: IOException) {
            Log.e("MedicineRepository", "❌ Error de conexión: ${e.message}", e)
            Result.Error(
                message = "Error de conexión. Por favor verifica tu internet.",
                exception = e
            )
        } catch (e: Exception) {
            Log.e("MedicineRepository", "❌ Error obteniendo medicamentos: ${e.message}", e)
            Result.Error(
                message = "Error al cargar medicamentos: ${e.message}",
                exception = e
            )
        }
    }

    /**
     * Agregar nuevo medicamento
     */
    suspend fun addMedicine(medicine: MedicineRequest): Result<Medicine> {
        return try {
            // Verificar conexión a internet
            val context = MedicAIApplication.getInstance()
            if (!NetworkMonitor.isNetworkAvailable(context)) {
                return Result.Error(
                    message = "Sin conexión a internet. Por favor verifica tu conexión.",
                    exception = IOException("No hay conexión a internet")
                )
            }

            Log.d("MedicineRepository", "Agregando medicamento: ${medicine.name}")

            val newMedicine = client.from("medicines")
                .insert(medicine) {
                    select()
                }
                .decodeSingle<Medicine>()

            Log.d("MedicineRepository", "✅ Medicamento agregado: ${newMedicine.id}")
            Result.Success(newMedicine)
        } catch (e: IOException) {
            Log.e("MedicineRepository", "❌ Error de conexión: ${e.message}", e)
            Result.Error(
                message = "Error de conexión. Por favor verifica tu internet.",
                exception = e
            )
        } catch (e: Exception) {
            Log.e("MedicineRepository", "❌ Error agregando medicamento: ${e.message}", e)
            Result.Error(
                message = "Error al agregar medicamento: ${e.message}",
                exception = e
            )
        }
    }

    /**
     * Actualizar medicamento existente
     */
    suspend fun updateMedicine(id: String, medicine: MedicineRequest): Result<Medicine> {
        return try {
            Log.d("MedicineRepository", "Actualizando medicamento: $id")
            Log.d("MedicineRepository", "  - Nombre: ${medicine.name}")
            Log.d("MedicineRepository", "  - Active: ${medicine.active}")

            val updated = client.from("medicines")
                .update(medicine) {
                    select()
                    filter {
                        eq("id", id)
                    }
                }
                .decodeSingle<Medicine>()

            Log.d("MedicineRepository", "✅ Medicamento actualizado: $id")
            Log.d("MedicineRepository", "  - Active después de actualizar: ${updated.active}")
            Result.Success(updated)
        } catch (e: Exception) {
            Log.e("MedicineRepository", "❌ Error actualizando medicamento: ${e.message}", e)
            Result.Error(
                message = "Error al actualizar medicamento: ${e.message}",
                exception = e
            )
        }
    }

    /**
     * Eliminar medicamento (soft delete - marca como inactivo)
     */
    suspend fun deleteMedicine(id: String): Result<Unit> {
        return try {
            Log.d("MedicineRepository", "Desactivando medicamento: $id")

            client.from("medicines")
                .update(mapOf("active" to false)) {
                    filter {
                        eq("id", id)
                    }
                }

            Log.d("MedicineRepository", "✅ Medicamento desactivado: $id")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e("MedicineRepository", "❌ Error desactivando medicamento: ${e.message}", e)
            Result.Error(
                message = "Error al desactivar medicamento: ${e.message}",
                exception = e
            )
        }
    }

    /**
     * Eliminar medicamento permanentemente (hard delete - borra de la BD)
     */
    suspend fun deletePermanently(id: String): Result<Unit> {
        return try {
            Log.d("MedicineRepository", "Eliminando permanentemente medicamento: $id")

            client.from("medicines")
                .delete {
                    filter {
                        eq("id", id)
                    }
                }

            Log.d("MedicineRepository", "✅ Medicamento eliminado permanentemente: $id")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e("MedicineRepository", "❌ Error eliminando permanentemente: ${e.message}", e)
            Result.Error(
                message = "Error al eliminar permanentemente: ${e.message}",
                exception = e
            )
        }
    }

    /**
     * Reactivar medicamento (marcar como activo nuevamente)
     */
    suspend fun reactivateMedicine(id: String): Result<Unit> {
        return try {
            Log.d("MedicineRepository", "Reactivando medicamento: $id")

            client.from("medicines")
                .update(mapOf("active" to true)) {
                    filter {
                        eq("id", id)
                    }
                }

            Log.d("MedicineRepository", "✅ Medicamento reactivado: $id")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e("MedicineRepository", "❌ Error reactivando medicamento: ${e.message}", e)
            Result.Error(
                message = "Error al reactivar medicamento: ${e.message}",
                exception = e
            )
        }
    }

    /**
     * Obtener medicamentos que deben tomarse hoy
     */
    suspend fun getTodayMedicines(userId: String): Result<List<Medicine>> {
        return try {
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())

            val medicines = client.from("medicines")
                .select()
                .decodeList<Medicine>()
                .filter {
                    it.user_id == userId &&
                    it.active &&
                    it.start_date <= today &&
                    (it.end_date == null || it.end_date >= today)
                }
                .sortedBy { it.times.firstOrNull() }

            Result.Success(medicines)
        } catch (e: Exception) {
            Log.e("MedicineRepository", "Error obteniendo medicamentos de hoy: ${e.message}", e)
            Result.Error(
                message = "Error al cargar medicamentos de hoy",
                exception = e
            )
        }
    }
}

