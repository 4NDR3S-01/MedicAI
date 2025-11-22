package com.example.medicai.data.models

import kotlinx.serialization.Serializable

/**
 * Modelo de usuario en la base de datos
 * Tabla en Supabase: profiles
 */
@Serializable
data class UserProfile(
    val id: String,
    val email: String,
    val full_name: String,
    val phone: String? = null,
    val avatar_url: String? = null,
    val notifications_enabled: Boolean = true,
    val reminder_minutes: Int = 15,
    val created_at: String? = null,
    val updated_at: String? = null
)

/**
 * Request para actualizar perfil
 */
@Serializable
data class UpdateProfileRequest(
    val full_name: String? = null,
    val phone: String? = null,
    val avatar_url: String? = null,
    val notifications_enabled: Boolean? = null,
    val reminder_minutes: Int? = null
)

/**
 * Datos de registro desde el formulario
 */
data class RegistrationData(
    val fullName: String,
    val email: String,
    val password: String,
    val phone: String?,
    val notificationsEnabled: Boolean,
    val reminderMinutes: Int,
    val termsAccepted: Boolean
)

/**
 * Estados de autenticación
 */
sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val user: UserProfile) : AuthState()
    data class Error(val message: String) : AuthState()
}

/**
 * Resultado de operaciones
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val exception: Exception? = null) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

