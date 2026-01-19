package com.example.medicai.data.local.dao

import androidx.room.*
import com.example.medicai.data.local.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operaciones de Perfil de Usuario en Room
 */
@Dao
interface UserProfileDao {
    
    /**
     * Obtener perfil de usuario (como Flow para actualizaciones reactivas)
     */
    @Query("SELECT * FROM user_profiles WHERE id = :userId LIMIT 1")
    fun getUserProfileFlow(userId: String): Flow<UserProfileEntity?>
    
    /**
     * Obtener perfil de usuario (snapshot único)
     */
    @Query("SELECT * FROM user_profiles WHERE id = :userId LIMIT 1")
    suspend fun getUserProfile(userId: String): UserProfileEntity?
    
    /**
     * Insertar o actualizar perfil
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: UserProfileEntity)
    
    /**
     * Actualizar perfil
     */
    @Update
    suspend fun updateUserProfile(profile: UserProfileEntity)
    
    /**
     * Eliminar perfil
     */
    @Delete
    suspend fun deleteUserProfile(profile: UserProfileEntity)
    
    /**
     * Eliminar perfil por ID
     */
    @Query("DELETE FROM user_profiles WHERE id = :userId")
    suspend fun deleteUserProfileById(userId: String)
    
    /**
     * Verificar si existe perfil
     */
    @Query("SELECT EXISTS(SELECT 1 FROM user_profiles WHERE id = :userId)")
    suspend fun profileExists(userId: String): Boolean
    
    /**
     * Marcar perfil como sincronizado
     */
    @Query("UPDATE user_profiles SET is_synced = 1 WHERE id = :userId")
    suspend fun markAsSynced(userId: String)
    
    /**
     * Obtener perfil no sincronizado
     */
    @Query("SELECT * FROM user_profiles WHERE is_synced = 0 LIMIT 1")
    suspend fun getUnsyncedProfile(): UserProfileEntity?
}
