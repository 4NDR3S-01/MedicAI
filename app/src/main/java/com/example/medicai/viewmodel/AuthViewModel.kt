package com.example.medicai.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medicai.data.models.AuthState
import com.example.medicai.data.models.RegistrationData
import com.example.medicai.data.models.UpdateProfileRequest
import com.example.medicai.data.models.Result
import com.example.medicai.data.models.UserProfile
import com.example.medicai.data.repository.AuthRepository
import com.example.medicai.data.local.UserPreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para manejar toda la lógica de autenticación
 * ✅ Sincroniza preferencias con UserPreferencesManager para acceso rápido desde BroadcastReceivers
 */
class AuthViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    // Estado de autenticación - Inicia en Loading mientras verifica la sesión
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    // Estado para login
    private val _loginState = MutableStateFlow<Result<UserProfile>?>(null)
    val loginState: StateFlow<Result<UserProfile>?> = _loginState.asStateFlow()

    // Estado para registro
    private val _registerState = MutableStateFlow<Result<UserProfile>?>(null)
    val registerState: StateFlow<Result<UserProfile>?> = _registerState.asStateFlow()

    // Estado para recuperación de contraseña
    private val _resetPasswordState = MutableStateFlow<Result<Unit>?>(null)
    val resetPasswordState: StateFlow<Result<Unit>?> = _resetPasswordState.asStateFlow()

    // Usuario actual
    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    // Mensajes de error/éxito
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        checkCurrentUser()
    }

    /**
     * Actualizar perfil (nombre y teléfono u otros campos)
     */
    fun updateProfile(fullName: String?, phone: String?, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            val userId = _currentUser.value?.id
            if (userId == null) {
                onError("No hay usuario logueado")
                return@launch
            }

            _message.value = "Actualizando perfil..."

            val updates = UpdateProfileRequest(
                full_name = fullName,
                phone = phone
            )

            when (val result = repository.updateProfile(userId, updates)) {
                is Result.Success -> {
                    _currentUser.value = result.data
                    _message.value = "Perfil actualizado"
                    onSuccess()
                }
                is Result.Error -> {
                    _message.value = result.message
                    onError(result.message)
                }
                else -> {
                    onError("Error desconocido al actualizar perfil")
                }
            }
        }
    }

    /**
     * Actualizar preferencias de notificaciones (enabled + reminder minutes)
     * ✅ Sincroniza con caché local para acceso rápido desde BroadcastReceivers
     */
    fun updatePreferences(notificationsEnabled: Boolean, reminderMinutes: Int, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            val userId = _currentUser.value?.id
            if (userId == null) {
                onError("No hay usuario logueado")
                return@launch
            }

            _message.value = "Guardando preferencias..."

            val updates = UpdateProfileRequest(
                notifications_enabled = notificationsEnabled,
                reminder_minutes = reminderMinutes
            )

            when (val result = repository.updateProfile(userId, updates)) {
                is Result.Success -> {
                    _currentUser.value = result.data
                    _message.value = "Preferencias actualizadas"
                    
                    // ✅ Guardar también en caché local para acceso rápido
                    UserPreferencesManager.saveNotificationSettings(
                        context = com.example.medicai.MedicAIApplication.getInstance(),
                        enabled = notificationsEnabled,
                        reminderMinutes = reminderMinutes
                    )
                    Log.d("AuthViewModel", "✅ Preferencias sincronizadas en caché local")
                    
                    onSuccess()
                }
                is Result.Error -> {
                    _message.value = result.message
                    onError(result.message)
                }
                else -> {
                    onError("Error desconocido al actualizar preferencias")
                }
            }
        }
    }

    /**
     * Eliminar cuenta de usuario (perfil + cierre de sesión)
     */
    fun deleteAccount(onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            val userId = _currentUser.value?.id
            if (userId == null) {
                onError("No hay usuario logueado")
                return@launch
            }

            when (val result = repository.deleteAccount(userId)) {
                is Result.Success -> {
                    // Limpiar estado local
                    _currentUser.value = null
                    _loginState.value = null
                    _registerState.value = null
                    _authState.value = AuthState.Error("Cuenta eliminada")
                    _message.value = "Cuenta eliminada"
                    onSuccess()
                }
                is Result.Error -> {
                    _message.value = result.message
                    onError(result.message)
                }
                else -> onError("Error desconocido al eliminar cuenta")
            }
        }
    }

    /**
     * Verificar si hay un usuario con sesión activa
     * ✅ Sincroniza preferencias en caché local al cargar usuario
     * ✅ Intenta refrescar la sesión si está expirada
     * ✅ Intenta obtener el usuario incluso si isUserLoggedIn() falla temporalmente
     */
    private fun checkCurrentUser(retryCount: Int = 0) {
        viewModelScope.launch {
            try {
                Log.d("AuthViewModel", "🔍 Verificando usuario actual...")
                
                // Asegurarse de que el estado esté en Loading mientras verificamos
                _authState.value = AuthState.Loading

                // Primero verificar si hay sesión guardada localmente (sin intentar refrescar aún)
                val hasLocalSession = repository.hasLocalSession()
                Log.d("AuthViewModel", "📱 Sesión local encontrada: $hasLocalSession")

                // Si no hay sesión local, no hay nada que verificar
                if (!hasLocalSession) {
                    _authState.value = AuthState.Error("No hay sesión activa")
                    Log.d("AuthViewModel", "❌ No hay sesión local guardada")
                    return@launch
                }

                // Si hay sesión local, intentar obtener el perfil del usuario directamente
                // Esto es más confiable que depender solo de isUserLoggedIn()
                when (val result = repository.getCurrentUser()) {
                    is Result.Success -> {
                        result.data?.let { user ->
                            _currentUser.value = user
                            _authState.value = AuthState.Success(user)
                            
                            // ✅ Sincronizar preferencias en caché local
                            syncUserPreferencesToCache(user)
                            
                            // Log sin información sensible en producción
                            Log.d("AuthViewModel", "✅ Sesión activa encontrada para usuario ID: ${user.id.take(8)}...")
                            Log.d("AuthViewModel", "👤 Usuario autenticado correctamente")
                        } ?: run {
                            // Si no se puede obtener el perfil pero hay sesión local,
                            // intentar verificar la sesión con isUserLoggedIn()
                            Log.w("AuthViewModel", "⚠️ No se pudo obtener perfil, verificando sesión...")
                            val isLoggedIn = repository.isUserLoggedIn()
                            if (!isLoggedIn) {
                                _authState.value = AuthState.Error("Sesión expirada")
                                Log.d("AuthViewModel", "❌ Sesión expirada")
                            } else {
                                _authState.value = AuthState.Error("Perfil no encontrado")
                                Log.d("AuthViewModel", "❌ Sesión existe pero perfil no encontrado")
                            }
                        }
                    }
                    is Result.Error -> {
                        // Si hay error al obtener el perfil, verificar si la sesión está realmente expirada
                        Log.e("AuthViewModel", "❌ Error al obtener usuario: ${result.message}")
                        
                        // Verificar si el error es por sesión expirada o problema de red
                        val errorMessage = result.message?.lowercase() ?: ""
                        if (errorMessage.contains("expired") || errorMessage.contains("unauthorized") || 
                            errorMessage.contains("401") || errorMessage.contains("invalid")) {
                            // Sesión realmente expirada
                            _authState.value = AuthState.Error("Sesión expirada")
                            Log.d("AuthViewModel", "❌ Sesión expirada")
                        } else {
                            // Puede ser un problema de red temporal, intentar verificar la sesión
                            val stillLoggedIn = repository.isUserLoggedIn()
                            if (!stillLoggedIn) {
                                _authState.value = AuthState.Error("Sesión expirada")
                                Log.d("AuthViewModel", "❌ Sesión expirada después del error")
                            } else {
                                // Problema temporal, intentar reintentar (máximo 2 reintentos)
                                if (retryCount < 2) {
                                    Log.w("AuthViewModel", "⚠️ Error temporal, puede ser problema de red. Reintentando... (intento ${retryCount + 1}/2)")
                                    _authState.value = AuthState.Loading
                                    // Reintentar después de un breve delay
                                    kotlinx.coroutines.delay(1000)
                                    checkCurrentUser(retryCount + 1) // Reintentar con contador incrementado
                                } else {
                                    // Demasiados reintentos, mostrar error de conexión
                                    Log.e("AuthViewModel", "❌ Error persistente después de ${retryCount + 1} intentos")
                                    _authState.value = AuthState.Error("Error de conexión. Por favor verifica tu internet.")
                                }
                            }
                        }
                    }
                    else -> {
                        _authState.value = AuthState.Error("Error desconocido")
                        Log.e("AuthViewModel", "❌ Error desconocido al verificar usuario")
                    }
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "❌ Excepción inesperada al verificar usuario: ${e.message}", e)
                // Verificar si hay sesión local antes de mostrar error
                val hasLocalSession = repository.hasLocalSession()
                if (hasLocalSession && retryCount < 2) {
                    // Hay sesión local, puede ser error temporal, reintentar (máximo 2 reintentos)
                    Log.w("AuthViewModel", "⚠️ Excepción pero hay sesión local, reintentando... (intento ${retryCount + 1}/2)")
                    _authState.value = AuthState.Loading
                    kotlinx.coroutines.delay(1000)
                    checkCurrentUser(retryCount + 1) // Reintentar con contador incrementado
                } else {
                    _authState.value = AuthState.Error("Error al verificar sesión: ${e.message}")
                }
            }
        }
    }

    /**
     * Iniciar sesión
     * ✅ Sincroniza preferencias en caché local después de login exitoso
     */
    fun login(email: String, password: String) {
        viewModelScope.launch {
            // Log sin información sensible
            val emailDomain = email.substringAfter("@", "unknown")
            Log.d("AuthViewModel", "Iniciando login para dominio: @$emailDomain")
            _loginState.value = Result.Loading
            _authState.value = AuthState.Loading

            when (val result = repository.login(email.trim(), password)) {
                is Result.Success -> {
                    _currentUser.value = result.data
                    _loginState.value = result
                    _authState.value = AuthState.Success(result.data)
                    _message.value = "¡Bienvenido ${result.data.full_name}!"
                    
                    // ✅ Sincronizar preferencias en caché local
                    syncUserPreferencesToCache(result.data)
                    
                    Log.d("AuthViewModel", "Login exitoso")
                }
                is Result.Error -> {
                    _loginState.value = result
                    _authState.value = AuthState.Error(result.message)
                    _message.value = result.message
                    Log.e("AuthViewModel", "Error en login: ${result.message}")
                }
                else -> {}
            }
        }
    }

    /**
     * Registrar nuevo usuario
     * ✅ Sincroniza preferencias en caché local después de registro exitoso
     */
    fun register(data: RegistrationData) {
        viewModelScope.launch {
            Log.d("AuthViewModel", "Iniciando registro para: ${data.email}")
            _registerState.value = Result.Loading
            _authState.value = AuthState.Loading

            when (val result = repository.register(data)) {
                is Result.Success -> {
                    _currentUser.value = result.data
                    _registerState.value = result
                    _authState.value = AuthState.Success(result.data)
                    _message.value = "¡Cuenta creada exitosamente! Por favor verifica tu email."
                    
                    // ✅ Sincronizar preferencias en caché local
                    syncUserPreferencesToCache(result.data)
                    
                    Log.d("AuthViewModel", "Registro exitoso")
                }
                is Result.Error -> {
                    _registerState.value = result
                    _authState.value = AuthState.Error(result.message)
                    _message.value = result.message
                    Log.e("AuthViewModel", "Error en registro: ${result.message}")
                }
                else -> {}
            }
        }
    }

    /**
     * Enviar email de recuperación de contraseña
     */
    fun resetPassword(email: String) {
        viewModelScope.launch {
            Log.d("AuthViewModel", "Enviando recuperación de contraseña para: $email")
            _resetPasswordState.value = Result.Loading

            when (val result = repository.resetPassword(email.trim())) {
                is Result.Success -> {
                    _resetPasswordState.value = result
                    _message.value = "Email de recuperación enviado. Revisa tu bandeja de entrada."
                    Log.d("AuthViewModel", "Email de recuperación enviado")
                }
                is Result.Error -> {
                    _resetPasswordState.value = result
                    _message.value = result.message
                    Log.e("AuthViewModel", "Error al enviar recuperación: ${result.message}")
                }
                else -> {}
            }
        }
    }

    /**
     * Enviar email de recuperación con callbacks para UI
     */
    fun sendPasswordResetEmail(email: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            when (val result = repository.resetPassword(email.trim())) {
                is Result.Success -> {
                    _message.value = "Email de recuperación enviado. Revisa tu bandeja de entrada."
                    onSuccess()
                }
                is Result.Error -> {
                    _message.value = result.message
                    onError(result.message)
                }
                else -> onError("Error desconocido al enviar email")
            }
        }
    }

    /**
     * Cerrar sesión
     * ✅ Cancela todas las alarmas del usuario antes de limpiar datos
     * ✅ Limpia caché local de preferencias
     */
    fun logout() {
        viewModelScope.launch {
            Log.d("AuthViewModel", "Cerrando sesión...")

            val context = com.example.medicai.MedicAIApplication.getInstance()
            val userId = _currentUser.value?.id ?: UserPreferencesManager.getUserId(context)

            // ✅ Cancelar todas las alarmas del usuario ANTES de limpiar datos
            if (userId != null) {
                try {
                    Log.d("AuthViewModel", "🔕 Cancelando todas las alarmas del usuario...")
                    com.example.medicai.notifications.AlarmScheduler.cancelAllUserAlarms(context, userId)
                    Log.d("AuthViewModel", "✅ Todas las alarmas canceladas")
                } catch (e: Exception) {
                    Log.e("AuthViewModel", "❌ Error cancelando alarmas: ${e.message}", e)
                    // Continuar con el logout incluso si falla la cancelación de alarmas
                }
            } else {
                Log.w("AuthViewModel", "⚠️ No se pudo obtener userId para cancelar alarmas")
            }

            // ✅ Limpiar caché local de preferencias
            UserPreferencesManager.clearAll(context)
            Log.d("AuthViewModel", "🗑️ Caché de preferencias limpiado")

            // Limpiar datos locales
            _currentUser.value = null
            _loginState.value = null
            _registerState.value = null

            // Intentar cerrar sesión en Supabase
            when (repository.logout()) {
                is Result.Success -> {
                    Log.d("AuthViewModel", "✅ Sesión cerrada exitosamente en Supabase")
                }
                is Result.Error -> {
                    Log.w("AuthViewModel", "⚠️ Error al cerrar sesión en Supabase, pero se limpió localmente")
                }
                else -> {}
            }

            // Siempre establecer estado Error para mostrar login
            _authState.value = AuthState.Error("Sesión cerrada")
            _message.value = "Sesión cerrada"
        }
    }
    
    /**
     * Sincronizar preferencias del usuario en caché local
     * Permite acceso rápido desde BroadcastReceivers sin llamadas a BD
     */
    private fun syncUserPreferencesToCache(user: UserProfile) {
        val context = com.example.medicai.MedicAIApplication.getInstance()
        UserPreferencesManager.saveUserId(context, user.id)
        UserPreferencesManager.saveNotificationSettings(
            context = context,
            enabled = user.notifications_enabled ?: true,
            reminderMinutes = user.reminder_minutes ?: 15
        )
        Log.d("AuthViewModel", "✅ Preferencias de usuario sincronizadas en caché local")
    }

    /**
     * Actualizar avatar del usuario
     */
    fun updateAvatar(avatarUrl: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            val userId = _currentUser.value?.id
            if (userId == null) {
                onError("No hay usuario logueado")
                return@launch
            }

            Log.d("AuthViewModel", "Actualizando avatar a: $avatarUrl")

            when (val result = repository.updateAvatar(userId, avatarUrl)) {
                is Result.Success -> {
                    _currentUser.value = result.data
                    _message.value = "Avatar actualizado"
                    Log.d("AuthViewModel", "✅ Avatar actualizado exitosamente")
                    onSuccess()
                }
                is Result.Error -> {
                    _message.value = result.message
                    Log.e("AuthViewModel", "❌ Error al actualizar avatar: ${result.message}")
                    onError(result.message)
                }
                else -> {}
            }
        }
    }

    /**
     * Subir avatar desde URI (foto/galería) a Supabase Storage
     */
    fun uploadAvatarImage(
        context: android.content.Context,
        imageUri: android.net.Uri,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val userId = _currentUser.value?.id
            if (userId == null) {
                onError("No hay usuario logueado")
                return@launch
            }

            Log.d("AuthViewModel", "Subiendo imagen de avatar...")

            val uploadResult = com.example.medicai.utils.AvatarUploadHelper.uploadAvatar(
                context = context,
                imageUri = imageUri,
                userId = userId
            )

            uploadResult.fold(
                onSuccess = { publicUrl ->
                    Log.d("AuthViewModel", "✅ Imagen subida: $publicUrl")
                    // Ahora actualizar el perfil con la URL pública
                    updateAvatar(publicUrl, onSuccess, onError)
                },
                onFailure = { error ->
                    val errorMsg = error.message ?: "Error al subir imagen"
                    Log.e("AuthViewModel", "❌ Error al subir imagen: $errorMsg")
                    onError(errorMsg)
                }
            )
        }
    }

    /**
     * Limpiar estados
     */
    fun clearStates() {
        _loginState.value = null
        _registerState.value = null
        _resetPasswordState.value = null
        _message.value = null
    }

    /**
     * Limpiar mensaje
     */
    fun clearMessage() {
        _message.value = null
    }

    /**
     * Verificar si el usuario está autenticado
     */
    suspend fun isUserLoggedIn(): Boolean {
        return repository.isUserLoggedIn()
    }
}

