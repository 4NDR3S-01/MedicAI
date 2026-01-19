package com.example.medicai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.medicai.data.models.UserProfile

/**
 * Entidad Room para Perfil de Usuario
 * Mapea la tabla 'profiles' en la base de datos local
 */
@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey
    val id: String,
    val email: String,
    val full_name: String,
    val phone: String? = null,
    val avatar_url: String? = null,
    val notifications_enabled: Boolean = true,
    val reminder_minutes: Int = 15,
    val created_at: String? = null,
    val updated_at: String? = null,
    val is_synced: Boolean = false // Indica si está sincronizado con Supabase
)

/**
 * Convertir de Entity a modelo de dominio
 */
fun UserProfileEntity.toUserProfile(): UserProfile {
    return UserProfile(
        id = id,
        email = email,
        full_name = full_name,
        phone = phone,
        avatar_url = avatar_url,
        notifications_enabled = notifications_enabled,
        reminder_minutes = reminder_minutes,
        created_at = created_at,
        updated_at = updated_at
    )
}

/**
 * Convertir de modelo de dominio a Entity
 */
fun UserProfile.toEntity(isSynced: Boolean = true): UserProfileEntity {
    return UserProfileEntity(
        id = id,
        email = email,
        full_name = full_name,
        phone = phone,
        avatar_url = avatar_url,
        notifications_enabled = notifications_enabled,
        reminder_minutes = reminder_minutes,
        created_at = created_at,
        updated_at = updated_at,
        is_synced = isSynced
    )
}
