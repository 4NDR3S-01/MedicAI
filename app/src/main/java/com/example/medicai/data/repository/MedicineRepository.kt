package com.example.medicai.data.repository

import android.util.Log
import com.example.medicai.MedicAIApplication
import com.example.medicai.data.local.AppDatabase
import com.example.medicai.data.local.entity.toEntity
import com.example.medicai.data.local.entity.toMedicine
import com.example.medicai.data.models.Medicine
import com.example.medicai.data.models.MedicineRequest
import com.example.medicai.data.models.Result
import com.example.medicai.data.remote.SupabaseClient
import com.example.medicai.utils.NetworkMonitor
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * Repositorio para operaciones CRUD de Medicamentos
 * ✅ Usa Room como caché local y Supabase como backend remoto
 * ✅ Estrategia offline-first: lee de Room, sincroniza con Supabase cuando hay conexión
 */
class MedicineRepository {

    private val client = SupabaseClient.client
    private val database = AppDatabase.getInstance(MedicAIApplication.getInstance())
    private val medicineDao = database.medicineDao()

    /**
     * Obtener todos los medicamentos del usuario como Flow (reactivo)
     * Lee de la caché local y sincroniza con el servidor en background
     */
    fun getMedicinesFlow(userId: String): Flow<List<Medicine>> {
        // Sincronizar en background
        syncMedicinesFromServer(userId)
        
        // Retornar Flow desde Room
        return medicineDao.getMedicinesFlow(userId).map { entities ->
            entities.map { it.toMedicine() }
        }
    }
    
    /**
     * Obtener todos los medicamentos del usuario (activos e inactivos)
     * Lee primero de caché local, luego sincroniza con servidor si hay conexión
     */
    suspend fun getMedicines(userId: String): Result<List<Medicine>> {
        return try {
            val context = MedicAIApplication.getInstance()
            
            // 1. Leer de caché local primero (offline-first)
            val cachedMedicines = medicineDao.getMedicines(userId).map { it.toMedicine() }
            
            // 2. Si hay conexión, sincronizar con servidor
            if (NetworkMonitor.isNetworkAvailable(context)) {
                try {
                    Log.d("MedicineRepository", "Sincronizando medicamentos desde servidor...")
                    
                    val remoteMedicines = client.from("medicines")
                        .select()
                        .decodeList<Medicine>()
                        .filter { it.user_id == userId }
                    
                    // Actualizar caché local
                    medicineDao.insertMedicines(remoteMedicines.map { it.toEntity(isSynced = true) })
                    
                    Log.d("MedicineRepository", "✅ ${remoteMedicines.size} medicamentos sincronizados")
                    Result.Success(remoteMedicines.sortedByDescending { it.created_at })
                } catch (e: Exception) {
                    Log.w("MedicineRepository", "⚠️ Error sincronizando, usando caché local", e)
                    Result.Success(cachedMedicines)
                }
            } else {
                Log.d("MedicineRepository", "📴 Sin conexión, usando ${cachedMedicines.size} medicamentos en caché")
                Result.Success(cachedMedicines)
            }
        } catch (e: Exception) {
            Log.e("MedicineRepository", "❌ Error obteniendo medicamentos: ${e.message}", e)
            Result.Error(
                message = "Error al cargar medicamentos: ${e.message}",
                exception = e
            )
        }
    }
    
    /**
     * Sincronizar medicamentos desde el servidor (función auxiliar)
     */
    private fun syncMedicinesFromServer(userId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val context = MedicAIApplication.getInstance()
                if (NetworkMonitor.isNetworkAvailable(context)) {
                    val remoteMedicines = client.from("medicines")
                        .select()
                        .decodeList<Medicine>()
                        .filter { it.user_id == userId }
                    
                    medicineDao.insertMedicines(remoteMedicines.map { it.toEntity(isSynced = true) })
                    Log.d("MedicineRepository", "✅ Sincronización en background completada")
                }
            } catch (e: Exception) {
                Log.w("MedicineRepository", "⚠️ Error en sincronización background", e)
            }
        }
    }

    /**
     * Agregar nuevo medicamento
     * Guarda primero en caché local, luego sincroniza con servidor
     */
    suspend fun addMedicine(medicine: MedicineRequest): Result<Medicine> {
        return try {
            val context = MedicAIApplication.getInstance()
            
            if (NetworkMonitor.isNetworkAvailable(context)) {
                // Con conexión: guardar en servidor y en caché
                Log.d("MedicineRepository", "Agregando medicamento: ${medicine.name}")
                
                val newMedicine = client.from("medicines")
                    .insert(medicine) {
                        select()
                    }
                    .decodeSingle<Medicine>()
                
                // Guardar en caché local
                medicineDao.insertMedicine(newMedicine.toEntity(isSynced = true))
                
                Log.d("MedicineRepository", "✅ Medicamento agregado y sincronizado: ${newMedicine.id}")
                Result.Success(newMedicine)
            } else {
                // Sin conexión: guardar solo en caché local con flag de no sincronizado
                Log.d("MedicineRepository", "📴 Sin conexión, guardando en caché local")
                
                val tempId = java.util.UUID.randomUUID().toString()
                val localMedicine = Medicine(
                    id = tempId,
                    user_id = medicine.user_id,
                    name = medicine.name,
                    dosage = medicine.dosage,
                    frequency = medicine.frequency,
                    times = medicine.times,
                    start_date = medicine.start_date,
                    end_date = medicine.end_date,
                    notes = medicine.notes,
                    active = medicine.active,
                    created_at = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                )
                
                medicineDao.insertMedicine(localMedicine.toEntity(isSynced = false))
                
                Result.Success(localMedicine)
            }
        } catch (e: IOException) {
            Log.e("MedicineRepository", "❌ Error de conexión: ${e.message}", e)
            Result.Error(
                message = "Error de conexión. Los cambios se guardarán localmente.",
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
     * Actualiza en caché local y servidor
     */
    suspend fun updateMedicine(id: String, medicine: MedicineRequest): Result<Medicine> {
        return try {
            Log.d("MedicineRepository", "Actualizando medicamento: $id")
            
            val context = MedicAIApplication.getInstance()
            
            if (NetworkMonitor.isNetworkAvailable(context)) {
                // Con conexión: actualizar en servidor
                val updated = client.from("medicines")
                    .update(medicine) {
                        select()
                        filter {
                            eq("id", id)
                        }
                    }
                    .decodeSingle<Medicine>()
                
                // Actualizar en caché local
                medicineDao.insertMedicine(updated.toEntity(isSynced = true))
                
                Log.d("MedicineRepository", "✅ Medicamento actualizado y sincronizado: $id")
                Result.Success(updated)
            } else {
                // Sin conexión: actualizar solo en caché local
                val cachedMedicine = medicineDao.getMedicineById(id)
                if (cachedMedicine != null) {
                    val updatedEntity = cachedMedicine.copy(
                        name = medicine.name,
                        dosage = medicine.dosage,
                        frequency = medicine.frequency,
                        times = medicine.times,
                        start_date = medicine.start_date,
                        end_date = medicine.end_date,
                        notes = medicine.notes,
                        active = medicine.active,
                        is_synced = false
                    )
                    medicineDao.insertMedicine(updatedEntity)
                    
                    Log.d("MedicineRepository", "📴 Medicamento actualizado localmente: $id")
                    Result.Success(updatedEntity.toMedicine())
                } else {
                    Result.Error(message = "Medicamento no encontrado en caché")
                }
            }
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
            
            val context = MedicAIApplication.getInstance()
            
            if (NetworkMonitor.isNetworkAvailable(context)) {
                // Con conexión: desactivar en servidor
                client.from("medicines")
                    .update(mapOf("active" to false)) {
                        filter {
                            eq("id", id)
                        }
                    }
                
                // Actualizar en caché local
                val cachedMedicine = medicineDao.getMedicineById(id)
                if (cachedMedicine != null) {
                    medicineDao.insertMedicine(cachedMedicine.copy(active = false, is_synced = true))
                }
            } else {
                // Sin conexión: desactivar solo en caché local
                val cachedMedicine = medicineDao.getMedicineById(id)
                if (cachedMedicine != null) {
                    medicineDao.insertMedicine(cachedMedicine.copy(active = false, is_synced = false))
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

            val context = MedicAIApplication.getInstance()
            
            if (NetworkMonitor.isNetworkAvailable(context)) {
                // Con conexión: eliminar del servidor
                client.from("medicines")
                    .delete {
                        filter {
                            eq("id", id)
                        }
                    }
            }
            
            // Eliminar de caché local siempre
            medicineDao.deleteMedicineById(id)

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

            val context = MedicAIApplication.getInstance()
            
            if (NetworkMonitor.isNetworkAvailable(context)) {
                // Con conexión: reactivar en servidor
                client.from("medicines")
                    .update(mapOf("active" to true)) {
                        filter {
                            eq("id", id)
                        }
                    }
                
                // Actualizar en caché local
                val cachedMedicine = medicineDao.getMedicineById(id)
                if (cachedMedicine != null) {
                    medicineDao.insertMedicine(cachedMedicine.copy(active = true, is_synced = true))
                }
            } else {
                // Sin conexión: reactivar solo en caché local
                val cachedMedicine = medicineDao.getMedicineById(id)
                if (cachedMedicine != null) {
                    medicineDao.insertMedicine(cachedMedicine.copy(active = true, is_synced = false))
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
     * Lee primero de caché local
     */
    suspend fun getTodayMedicines(userId: String): Result<List<Medicine>> {
        return try {
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())

            // Leer de caché local
            val medicines = medicineDao.getActiveMedicines(userId)
                .map { it.toMedicine() }
                .filter {
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

