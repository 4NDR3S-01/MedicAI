package com.example.medicai.data.repository

import android.util.Log
import com.example.medicai.MedicAIApplication
import com.example.medicai.data.local.AppDatabase
import com.example.medicai.data.local.entity.toEntity
import com.example.medicai.data.local.entity.toUserProfile
import com.example.medicai.data.models.RegistrationData
import com.example.medicai.data.models.Result
import com.example.medicai.data.models.UpdateProfileRequest
import com.example.medicai.data.models.UserProfile
import com.example.medicai.data.remote.SupabaseClient
import com.example.medicai.data.local.UserPreferencesManager
import com.example.medicai.utils.NetworkMonitor
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.IOException

/**
 * Repository para manejar todas las operaciones de autenticación con Supabase
 * ✅ Incluye Room para caché local de perfil de usuario
 * ✅ Incluye detección de conexión a internet
 */
class AuthRepository {

    private val client = SupabaseClient.client
    private val auth = client.auth
    private val database = AppDatabase.getInstance(MedicAIApplication.getInstance())
    private val userProfileDao = database.userProfileDao()

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

            val profileMap = mapOf(
                "id" to userId,
                "email" to data.email,
                "full_name" to data.fullName,
                "phone" to data.phone,
                "notifications_enabled" to data.notificationsEnabled,
                "reminder_minutes" to data.reminderMinutes
            )

            Log.d("AuthRepository", "Creando perfil (map) con datos: name=${data.fullName}, phone=${data.phone}, notifications=${data.notificationsEnabled}, reminder=${data.reminderMinutes}")

            // 3. Insertar el perfil en la base de datos usando un Map para evitar problemas de serialización
            try {
                client.from("profiles").insert(profileMap)
                Log.d("AuthRepository", "✅ Perfil creado exitosamente con todos los datos (map)")
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
                
                // Guardar en caché local
                userProfileDao.insertUserProfile(createdProfile.toEntity(isSynced = true))
                
                return Result.Success(createdProfile)
            } else {
                Log.w("AuthRepository", "⚠️ Perfil creado pero no encontrado en verificación")

                // Reconstruir el objeto UserProfile desde profileMap
                val fallbackProfile = UserProfile(
                    id = profileMap["id"] as String,
                    email = profileMap["email"] as String,
                    full_name = profileMap["full_name"] as String,
                    phone = profileMap["phone"] as? String,
                    notifications_enabled = profileMap["notifications_enabled"] as? Boolean ?: true,
                    reminder_minutes = profileMap["reminder_minutes"] as? Int ?: 15,
                    avatar_url = null,
                    created_at = null,
                    updated_at = null
                )

                // Guardar en caché local
                userProfileDao.insertUserProfile(fallbackProfile.toEntity(isSynced = true))

                return Result.Success(fallbackProfile)
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
            // Verificar conexión a internet
            val context = MedicAIApplication.getInstance()
            if (!NetworkMonitor.isNetworkAvailable(context)) {
                return Result.Error(
                    message = "Sin conexión a internet. Por favor verifica tu conexión.",
                    exception = IOException("No hay conexión a internet")
                )
            }

            // Log sin información sensible (solo dominio del email)
            val emailDomain = email.substringAfter("@", "unknown")
            Log.d("AuthRepository", "Iniciando sesión para dominio: @$emailDomain")

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
            
            // Guardar perfil en caché local
            userProfileDao.insertUserProfile(profile.toEntity(isSynced = true))
            
            Result.Success(profile)

        } catch (e: IOException) {
            Log.e("AuthRepository", "❌ Error de conexión: ${e.message}", e)
            Result.Error(
                message = "Error de conexión. Por favor verifica tu internet.",
                exception = e
            )
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
     * Limpia completamente la sesión local y remota
     */
    suspend fun logout(): Result<Unit> {
        return try {
            Log.d("AuthRepository", "Cerrando sesión...")
            auth.signOut()
            
            // Verificar que la sesión se limpió correctamente
            val sessionAfterLogout = auth.currentSessionOrNull()
            if (sessionAfterLogout == null) {
                Log.d("AuthRepository", "✅ Sesión cerrada exitosamente - no hay sesión local")
            } else {
                Log.w("AuthRepository", "⚠️ Sesión cerrada pero aún existe sesión local (puede ser temporal)")
            }
            
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
                // Intentar usar caché local si existe
                val cachedUserId = UserPreferencesManager.getUserId(MedicAIApplication.getInstance())
                val cachedProfile = cachedUserId?.let { userProfileDao.getUserProfile(it)?.toUserProfile() }
                return Result.Success(cachedProfile)
            }

            // Si no hay internet, usar caché local
            val context = MedicAIApplication.getInstance()
            if (!NetworkMonitor.isNetworkAvailable(context)) {
                val cachedProfile = userProfileDao.getUserProfile(currentUser.id)?.toUserProfile()
                return Result.Success(cachedProfile)
            }

            val profiles = client.from("profiles")
                .select()
                .decodeList<UserProfile>()

            val profile = profiles.find { it.id == currentUser.id }

            if (profile != null) {
                Result.Success(profile)
            } else {
                // Fallback a caché local si el perfil no se encontró
                val cachedUserId = UserPreferencesManager.getUserId(MedicAIApplication.getInstance())
                val cachedProfile = cachedUserId?.let { userProfileDao.getUserProfile(it)?.toUserProfile() }
                Result.Success(cachedProfile)
            }

        } catch (e: Exception) {
            Log.e("AuthRepository", "Error al obtener usuario actual: ${e.message}", e)
            // Fallback a caché local si hay problema de red
            val cachedUserId = UserPreferencesManager.getUserId(MedicAIApplication.getInstance())
            val cachedProfile = cachedUserId?.let { userProfileDao.getUserProfile(it)?.toUserProfile() }
            if (cachedProfile != null) {
                Result.Success(cachedProfile)
            } else {
                Result.Error(
                    message = e.message ?: "Error al obtener usuario",
                    exception = e
                )
            }
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
     * Verificar si hay una sesión guardada localmente (sin intentar refrescar)
     * Útil para verificar rápidamente si hay una sesión antes de intentar operaciones costosas
     * No es suspend porque currentSessionOrNull() no es suspend
     */
    fun hasLocalSession(): Boolean {
        return try {
            val session = auth.currentSessionOrNull()
            val hasSession = session != null
            if (hasSession) {
                Log.d("AuthRepository", "🔍 Verificación local de sesión: ✅ Encontrada")
                return true
            }

            val cachedUserId = UserPreferencesManager.getUserId(MedicAIApplication.getInstance())
            val hasCachedUser = cachedUserId != null
            Log.d(
                "AuthRepository",
                "🔍 Verificación local sin sesión: ${if (hasCachedUser) "✅ UserId en caché" else "❌ Sin caché"}"
            )
            hasCachedUser
        } catch (e: Exception) {
            Log.e("AuthRepository", "❌ Error al verificar sesión local: ${e.message}", e)
            false
        }
    }

    /**
     * Verificar si hay una sesión activa
     * Intenta refrescar la sesión si está expirada antes de verificar
     * Si hay una sesión guardada localmente, retorna true incluso si el refresh falla temporalmente
     */
    suspend fun isUserLoggedIn(): Boolean {
        return try {
            // Primero verificar si hay una sesión guardada localmente
            val session = auth.currentSessionOrNull()
            
            if (session == null) {
                Log.d("AuthRepository", "🔐 No hay sesión guardada")
                return false
            }
            
            Log.d("AuthRepository", "🔍 Sesión encontrada localmente, verificando validez...")
            
            // Intentar refrescar la sesión si existe (puede estar expirada)
            // Usamos withTimeout más largo para dar más tiempo en caso de problemas de red
            try {
                kotlinx.coroutines.withTimeout(10000) {
                    // refreshCurrentSession() refresca la sesión si es necesario
                    // Si la sesión está completamente expirada, lanzará una excepción
                    auth.refreshCurrentSession()
                }
                Log.d("AuthRepository", "✅ Sesión válida o refrescada exitosamente")
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                // Si hay timeout, puede ser un problema de red temporal
                // Si hay una sesión guardada, asumimos que está activa y dejamos que getCurrentUser() lo verifique
                Log.w("AuthRepository", "⚠️ Timeout al refrescar sesión (puede ser problema de red temporal)")
                // NO retornamos false aquí - si hay sesión guardada, la consideramos válida
                // y dejamos que getCurrentUser() verifique si realmente funciona
            } catch (e: Exception) {
                // Verificar si el error es por sesión expirada o problema de red
                val errorMessage = e.message?.lowercase() ?: ""
                if (errorMessage.contains("expired") || errorMessage.contains("invalid") || 
                    errorMessage.contains("unauthorized") || errorMessage.contains("401")) {
                    // Sesión realmente expirada
                    Log.w("AuthRepository", "⚠️ Sesión expirada, no se pudo refrescar: ${e.message}")
                    // Limpiar la sesión expirada
                    try {
                        auth.signOut()
                    } catch (signOutError: Exception) {
                        Log.w("AuthRepository", "Error al limpiar sesión expirada: ${signOutError.message}")
                    }
                    return false
                } else {
                    // Puede ser un problema de red temporal
                    Log.w("AuthRepository", "⚠️ Error al refrescar sesión (puede ser problema de red): ${e.message}")
                    // Si hay sesión guardada, asumimos que está activa y dejamos que getCurrentUser() lo verifique
                }
            }
            
            // Verificar nuevamente después del refresh (o si el refresh falló temporalmente)
            val currentSession = auth.currentSessionOrNull()
            val isLoggedIn = currentSession != null
            Log.d("AuthRepository", "🔐 Verificando sesión: ${if (isLoggedIn) "✅ Activa" else "❌ Inactiva"}")
            if (currentSession != null) {
                val userId = auth.currentUserOrNull()?.id
                Log.d("AuthRepository", "📱 Sesión válida para usuario ID: ${userId?.take(8)}...")
            }
            isLoggedIn
        } catch (e: Exception) {
            Log.e("AuthRepository", "❌ Error al verificar sesión: ${e.message}", e)
            // En caso de error inesperado, verificar si hay sesión guardada
            val session = auth.currentSessionOrNull()
            if (session != null) {
                Log.d("AuthRepository", "⚠️ Error pero hay sesión guardada, asumiendo válida temporalmente")
                return true // Si hay sesión guardada, asumimos que está activa
            }
            false
        }
    }

    /**
     * Observar cambios en el estado de autenticación
     */
    fun observeAuthState(): Flow<Boolean> = flow {
        emit(isUserLoggedIn())
        // Aquí podrías implementar un listener de cambios de sesión
        // Nota: isUserLoggedIn() ahora es suspend, así que se puede usar aquí
    }
}

