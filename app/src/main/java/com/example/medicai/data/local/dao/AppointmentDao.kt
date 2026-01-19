package com.example.medicai.data.local.dao

import androidx.room.*
import com.example.medicai.data.local.entity.AppointmentEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operaciones de Citas en Room
 */
@Dao
interface AppointmentDao {
    
    /**
     * Obtener todas las citas del usuario (como Flow para actualizaciones reactivas)
     */
    @Query("SELECT * FROM appointments WHERE user_id = :userId ORDER BY date DESC, time DESC")
    fun getAppointmentsFlow(userId: String): Flow<List<AppointmentEntity>>
    
    /**
     * Obtener todas las citas del usuario (snapshot único)
     */
    @Query("SELECT * FROM appointments WHERE user_id = :userId ORDER BY date DESC, time DESC")
    suspend fun getAppointments(userId: String): List<AppointmentEntity>
    
    /**
     * Obtener citas pendientes del usuario
     */
    @Query("SELECT * FROM appointments WHERE user_id = :userId AND status = 'scheduled' ORDER BY date ASC, time ASC")
    suspend fun getPendingAppointments(userId: String): List<AppointmentEntity>
    
    /**
     * Obtener una cita por ID
     */
    @Query("SELECT * FROM appointments WHERE id = :appointmentId")
    suspend fun getAppointmentById(appointmentId: String): AppointmentEntity?
    
    /**
     * Insertar o actualizar cita
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppointment(appointment: AppointmentEntity)
    
    /**
     * Insertar o actualizar múltiples citas
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppointments(appointments: List<AppointmentEntity>)
    
    /**
     * Actualizar cita
     */
    @Update
    suspend fun updateAppointment(appointment: AppointmentEntity)
    
    /**
     * Eliminar cita
     */
    @Delete
    suspend fun deleteAppointment(appointment: AppointmentEntity)
    
    /**
     * Eliminar cita por ID
     */
    @Query("DELETE FROM appointments WHERE id = :appointmentId")
    suspend fun deleteAppointmentById(appointmentId: String)
    
    /**
     * Obtener citas no sincronizadas
     */
    @Query("SELECT * FROM appointments WHERE is_synced = 0")
    suspend fun getUnsyncedAppointments(): List<AppointmentEntity>
    
    /**
     * Marcar cita como sincronizada
     */
    @Query("UPDATE appointments SET is_synced = 1 WHERE id = :appointmentId")
    suspend fun markAsSynced(appointmentId: String)
    
    /**
     * Eliminar todas las citas del usuario
     */
    @Query("DELETE FROM appointments WHERE user_id = :userId")
    suspend fun deleteAllAppointments(userId: String)
}
