package com.example.medicai.data.local.dao

import androidx.room.*
import com.example.medicai.data.local.entity.MedicineEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operaciones de Medicamentos en Room
 */
@Dao
interface MedicineDao {
    
    /**
     * Obtener todos los medicamentos del usuario (como Flow para actualizaciones reactivas)
     */
    @Query("SELECT * FROM medicines WHERE user_id = :userId ORDER BY created_at DESC")
    fun getMedicinesFlow(userId: String): Flow<List<MedicineEntity>>
    
    /**
     * Obtener todos los medicamentos del usuario (snapshot único)
     */
    @Query("SELECT * FROM medicines WHERE user_id = :userId ORDER BY created_at DESC")
    suspend fun getMedicines(userId: String): List<MedicineEntity>
    
    /**
     * Obtener medicamentos activos del usuario
     */
    @Query("SELECT * FROM medicines WHERE user_id = :userId AND active = 1 ORDER BY created_at DESC")
    suspend fun getActiveMedicines(userId: String): List<MedicineEntity>
    
    /**
     * Obtener un medicamento por ID
     */
    @Query("SELECT * FROM medicines WHERE id = :medicineId")
    suspend fun getMedicineById(medicineId: String): MedicineEntity?
    
    /**
     * Insertar o actualizar medicamento
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicine(medicine: MedicineEntity)
    
    /**
     * Insertar o actualizar múltiples medicamentos
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicines(medicines: List<MedicineEntity>)
    
    /**
     * Actualizar medicamento
     */
    @Update
    suspend fun updateMedicine(medicine: MedicineEntity)
    
    /**
     * Eliminar medicamento
     */
    @Delete
    suspend fun deleteMedicine(medicine: MedicineEntity)
    
    /**
     * Eliminar medicamento por ID
     */
    @Query("DELETE FROM medicines WHERE id = :medicineId")
    suspend fun deleteMedicineById(medicineId: String)
    
    /**
     * Obtener medicamentos no sincronizados
     */
    @Query("SELECT * FROM medicines WHERE is_synced = 0")
    suspend fun getUnsyncedMedicines(): List<MedicineEntity>
    
    /**
     * Marcar medicamento como sincronizado
     */
    @Query("UPDATE medicines SET is_synced = 1 WHERE id = :medicineId")
    suspend fun markAsSynced(medicineId: String)
    
    /**
     * Eliminar todos los medicamentos del usuario
     */
    @Query("DELETE FROM medicines WHERE user_id = :userId")
    suspend fun deleteAllMedicines(userId: String)
}
