package com.example.medicai.data.repository

import android.util.Log
import com.example.medicai.data.models.RegistrationData
import com.example.medicai.data.models.Result
import com.example.medicai.data.models.UpdateProfileRequest
import com.example.medicai.data.models.UserProfile
import com.example.medicai.data.remote.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Repository para manejar todas las operaciones de autenticación con Supabase
 */
class AuthRepository {

    private val client = SupabaseClient.client
    private val auth = client.auth

    /**
     * Registrar nuevo usuario
     */
    suspend fun register(data: RegistrationData): Result<UserProfile> {
        return try {
            Log.d("AuthRepository", "Iniciando registro para: ${data.email}")

            // 1. Registrar usuario en Supabase Auth
            val authResult = auth.signUpWith(Email) {
                email = data.email
                password = data.password
            }

            val userId = authResult?.id ?: throw Exception("No se pudo obtener el ID del usuario")

            Log.d("AuthRepository", "Usuario registrado en Auth. ID: $userId")

            // 2. Crear perfil COMPLETO manualmente con TODOS los datos del formulario
            val newProfile = UserProfile(
                id = userId,
                email = data.email,
                full_name = data.fullName, // ✅ Nombre completo del formulario
                phone = data.phone,        // ✅ Teléfono del formulario
                notifications_enabled = data.notificationsEnabled, // ✅ Preferencia de notificaciones
                reminder_minutes = data.reminderMinutes,           // ✅ Minutos de recordatorio
                avatar_url = null,
                created_at = null,
                updated_at = null
            )

            Log.d("AuthRepository", "Creando perfil con datos: name=${data.fullName}, phone=${data.phone}, notifications=${data.notificationsEnabled}, reminder=${data.reminderMinutes}")

            // 3. Insertar el perfil en la base de datos
            try {
                client.from("profiles").insert(newProfile)
                Log.d("AuthRepository", "✅ Perfil creado exitosamente con todos los datos")
            } catch (insertError: Exception) {
                Log.e("AuthRepository", "❌ Error al insertar perfil: ${insertError.message}", insertError)
                throw insertError
            }

            // 4. Esperar un momento y verificar que se guardó correctamente
            kotlinx.coroutines.delay(500)

            // 5. Obtener el perfil creado para verificar
            val profiles = client.from("profiles")
                .select()
                .decodeList<UserProfile>()

            val createdProfile = profiles.find { it.id == userId }

            if (createdProfile != null) {
                Log.d("AuthRepository", "✅ Perfil verificado: ${createdProfile.full_name}, ${createdProfile.email}, ${createdProfile.phone}")
                return Result.Success(createdProfile)
            } else {
                Log.w("AuthRepository", "⚠️ Perfil creado pero no encontrado en verificación")
                return Result.Success(newProfile)
            }

        } catch (e: Exception) {
            Log.e("AuthRepository", "Error en registro: ${e.message}", e)

            // Mejorar mensajes de error
            val errorMessage = when {
                e.message?.contains("already registered", ignoreCase = true) == true ->
                    "Este email ya está registrado"
                e.message?.contains("invalid email", ignoreCase = true) == true ->
                    "Email inválido"
                e.message?.contains("weak password", ignoreCase = true) == true ->
                    "La contraseña debe tener al menos 8 caracteres"
                e.message?.contains("network", ignoreCase = true) == true ->
                    "Error de conexión. Verifica tu internet"
                else -> e.message ?: "Error al registrar usuario"
            }

            Result.Error(
                message = errorMessage,
                exception = e
            )
        }
    }

    /**
     * Iniciar sesión
     */
    suspend fun login(email: String, password: String): Result<UserProfile> {
        return try {
            Log.d("AuthRepository", "Iniciando sesión para: $email")

            // 1. Autenticar con Supabase
            auth.signInWith(Email) {
                this.email = email
                this.password = password
            }

            // 2. Obtener perfil del usuario
            val userId = auth.currentUserOrNull()?.id
                ?: throw Exception("No se pudo obtener el usuario autenticado")

            Log.d("AuthRepository", "Usuario autenticado. ID: $userId")

            // 3. Obtener todos los perfiles y filtrar por ID
            val profiles = client.from("profiles")
                .select()
                .decodeList<UserProfile>()

            Log.d("AuthRepository", "Perfiles obtenidos: ${profiles.size}")

            val profile = profiles.find { it.id == userId }
                ?: throw Exception("Perfil no encontrado para el usuario")

            Log.d("AuthRepository", "Inicio de sesión exitoso para: $email")
            Result.Success(profile)

        } catch (e: Exception) {
            Log.e("AuthRepository", "Error en inicio de sesión: ${e.message}", e)
            Result.Error(
                message = when {
                    e.message?.contains("Invalid login credentials") == true ->
                        "Credenciales inválidas"
                    e.message?.contains("Email not confirmed") == true ->
                        "Por favor confirma tu email primero"
                    e.message?.contains("Perfil no encontrado") == true ->
                        "Error: Tu perfil no fue creado correctamente. Contacta soporte."
                    else -> e.message ?: "Error al iniciar sesión"
                },
                exception = e
            )
        }
    }

    /**
     * Cerrar sesión
     */
    suspend fun logout(): Result<Unit> {
        return try {
            Log.d("AuthRepository", "Cerrando sesión")
            auth.signOut()
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error al cerrar sesión: ${e.message}", e)
            Result.Error(
                message = e.message ?: "Error al cerrar sesión",
                exception = e
            )
        }
    }

    /**
     * Enviar email de recuperación de contraseña
     */
    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            Log.d("AuthRepository", "Enviando email de recuperación a: $email")

            auth.resetPasswordForEmail(email)

            Log.d("AuthRepository", "Email de recuperación enviado a: $email")
            Result.Success(Unit)

        } catch (e: Exception) {
            Log.e("AuthRepository", "Error al enviar email de recuperación: ${e.message}", e)
            Result.Error(
                message = e.message ?: "Error al enviar email de recuperación",
                exception = e
            )
        }
    }

    /**
     * Obtener el usuario actual
     */
    suspend fun getCurrentUser(): Result<UserProfile?> {
        return try {
            val currentUser = auth.currentUserOrNull()

            if (currentUser == null) {
                return Result.Success(null)
            }

            val profiles = client.from("profiles")
                .select()
                .decodeList<UserProfile>()

            val profile = profiles.find { it.id == currentUser.id }

            Result.Success(profile)

        } catch (e: Exception) {
            Log.e("AuthRepository", "Error al obtener usuario actual: ${e.message}", e)
            Result.Error(
                message = e.message ?: "Error al obtener usuario",
                exception = e
            )
        }
    }

    /**
     * Actualizar perfil de usuario
     */
    suspend fun updateProfile(userId: String, updates: UpdateProfileRequest): Result<UserProfile> {
        return try {
            Log.d("AuthRepository", "Actualizando perfil para usuario: $userId")

            client.from("profiles")
                .update(updates) {
                    filter {
                        eq("id", userId)
                    }
                }

            // Obtener el perfil actualizado
            val updatedProfile = client.from("profiles")
                .select {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingle<UserProfile>()

            Log.d("AuthRepository", "Perfil actualizado exitosamente")
            Result.Success(updatedProfile)

        } catch (e: Exception) {
            Log.e("AuthRepository", "Error al actualizar perfil: ${e.message}", e)
            Result.Error(
                message = e.message ?: "Error al actualizar perfil",
                exception = e
            )
        }
    }

    /**
     * Actualizar solo el avatar del usuario
     */
    suspend fun updateAvatar(userId: String, avatarUrl: String): Result<UserProfile> {
        return try {
            Log.d("AuthRepository", "Actualizando avatar para usuario: $userId")

            val updates = UpdateProfileRequest(avatar_url = avatarUrl)

            client.from("profiles")
                .update(updates) {
                    filter {
                        eq("id", userId)
                    }
                }

            // Obtener el perfil actualizado
            val updatedProfile = client.from("profiles")
                .select {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingle<UserProfile>()

            Log.d("AuthRepository", "✅ Avatar actualizado a: $avatarUrl")
            Result.Success(updatedProfile)

        } catch (e: Exception) {
            Log.e("AuthRepository", "❌ Error al actualizar avatar: ${e.message}", e)
            Result.Error(
                message = e.message ?: "Error al actualizar avatar",
                exception = e
            )
        }
    }

    /**
     * Eliminar cuenta y perfil del usuario
     */
    suspend fun deleteAccount(userId: String): Result<Unit> {
        return try {
            Log.d("AuthRepository", "Eliminando perfil para usuario: $userId")

            // Eliminar fila en la tabla profiles
            client.from("profiles")
                .delete {
                    filter {
                        eq("id", userId)
                    }
                }

            // Intentar cerrar sesión local
            try {
                auth.signOut()
            } catch (e: Exception) {
                Log.w("AuthRepository", "Warning al cerrar sesión durante eliminación: ${e.message}")
            }

            Log.d("AuthRepository", "Perfil eliminado (si existía) y sesión cerrada")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error al eliminar cuenta: ${e.message}", e)
            Result.Error(
                message = e.message ?: "Error al eliminar cuenta",
                exception = e
            )
        }
    }

    /**
     * Verificar si hay una sesión activa
     */
    fun isUserLoggedIn(): Boolean {
        val session = auth.currentSessionOrNull()
        val isLoggedIn = session != null
        Log.d("AuthRepository", "🔐 Verificando sesión: ${if (isLoggedIn) "✅ Activa" else "❌ Inactiva"}")
        if (session != null) {
            Log.d("AuthRepository", "📱 User ID: ${auth.currentUserOrNull()?.id}")
        }
        return isLoggedIn
    }

    /**
     * Observar cambios en el estado de autenticación
     */
    fun observeAuthState(): Flow<Boolean> = flow {
        emit(isUserLoggedIn())
        // Aquí podrías implementar un listener de cambios de sesión
    }
}

