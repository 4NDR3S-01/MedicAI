package com.example.medicai.data

/**
 * Modelos de datos para la integración con el backend
 */

// ============================================================================
// REQUEST MODELS (Lo que se envía al servidor)
// ============================================================================

/**
 * Request para iniciar sesión
 */
data class LoginRequest(
    val email: String,
    val password: String,
    val rememberMe: Boolean = false
)

/**
 * Request para registro de usuario
 */
data class RegisterRequest(
    val fullName: String,
    val email: String,
    val password: String,
    val phone: String? = null,
    val preferences: UserPreferences
)

data class UserPreferences(
    val notificationsEnabled: Boolean,
    val reminderMinutes: Int
)

/**
 * Request para recuperación de contraseña
 */
data class ForgotPasswordRequest(
    val email: String
)

// ============================================================================
// RESPONSE MODELS (Lo que se recibe del servidor)
// ============================================================================

/**
 * Response exitoso de autenticación
 */
data class AuthResponse(
    val success: Boolean,
    val token: String,
    val user: UserData,
    val message: String? = null
)

/**
 * Datos del usuario autenticado
 */
data class UserData(
    val id: String,
    val fullName: String,
    val email: String,
    val phone: String?,
    val profileImage: String?,
    val preferences: UserPreferences,
    val createdAt: String
)

/**
 * Response genérico para operaciones simples
 */
data class ApiResponse(
    val success: Boolean,
    val message: String,
    val error: String? = null
)

// ============================================================================
// ERROR HANDLING
// ============================================================================

/**
 * Tipos de errores de autenticación
 */
sealed class AuthError {
    data class NetworkError(val message: String) : AuthError()
    data class InvalidCredentials(val message: String) : AuthError()
    data class EmailAlreadyExists(val message: String) : AuthError()
    data class ValidationError(val field: String, val message: String) : AuthError()
    data class ServerError(val code: Int, val message: String) : AuthError()
    object UnknownError : AuthError()
}

/**
 * Estado de resultado de operación
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val error: AuthError) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

// ============================================================================
// EJEMPLO DE USO CON RETROFIT (Comentado)
// ============================================================================

/*
// 1. Agregar dependencias en build.gradle.kts:
dependencies {
    // Retrofit para llamadas HTTP
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // OkHttp para logging
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

    // Coroutines para async
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}

// 2. Definir el API Service:
interface AuthApiService {

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<ApiResponse>
}

// 3. Crear el Repository:
class AuthRepository(private val api: AuthApiService) {

    suspend fun login(email: String, password: String, rememberMe: Boolean): Result<AuthResponse> {
        return try {
            val response = api.login(LoginRequest(email, password, rememberMe))
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(AuthError.InvalidCredentials("Credenciales inválidas"))
            }
        } catch (e: Exception) {
            Result.Error(AuthError.NetworkError(e.message ?: "Error de red"))
        }
    }

    suspend fun register(data: RegistrationData): Result<AuthResponse> {
        return try {
            val request = RegisterRequest(
                fullName = data.fullName,
                email = data.email,
                password = data.password,
                phone = data.phone,
                preferences = UserPreferences(
                    notificationsEnabled = data.notificationsEnabled,
                    reminderMinutes = data.reminderMinutes
                )
            )
            val response = api.register(request)
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(AuthError.EmailAlreadyExists("El email ya está registrado"))
            }
        } catch (e: Exception) {
            Result.Error(AuthError.NetworkError(e.message ?: "Error de red"))
        }
    }

    suspend fun resetPassword(email: String): Result<ApiResponse> {
        return try {
            val response = api.forgotPassword(ForgotPasswordRequest(email))
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(AuthError.ServerError(response.code(), "Error al enviar email"))
            }
        } catch (e: Exception) {
            Result.Error(AuthError.NetworkError(e.message ?: "Error de red"))
        }
    }
}

// 4. Crear el ViewModel:
class AuthViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _loginState = MutableStateFlow<Result<AuthResponse>?>(null)
    val loginState: StateFlow<Result<AuthResponse>?> = _loginState

    private val _registerState = MutableStateFlow<Result<AuthResponse>?>(null)
    val registerState: StateFlow<Result<AuthResponse>?> = _registerState

    private val _resetPasswordState = MutableStateFlow<Result<ApiResponse>?>(null)
    val resetPasswordState: StateFlow<Result<ApiResponse>?> = _resetPasswordState

    fun login(email: String, password: String, rememberMe: Boolean) {
        viewModelScope.launch {
            _loginState.value = Result.Loading
            _loginState.value = repository.login(email, password, rememberMe)
        }
    }

    fun register(data: RegistrationData) {
        viewModelScope.launch {
            _registerState.value = Result.Loading
            _registerState.value = repository.register(data)
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            _resetPasswordState.value = Result.Loading
            _resetPasswordState.value = repository.resetPassword(email)
        }
    }

    fun clearStates() {
        _loginState.value = null
        _registerState.value = null
        _resetPasswordState.value = null
    }
}

// 5. Usar en Composable:
@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: (UserData) -> Unit
) {
    val loginState by viewModel.loginState.collectAsState()

    // Observar cambios de estado
    LaunchedEffect(loginState) {
        when (val state = loginState) {
            is Result.Success -> {
                onLoginSuccess(state.data.user)
            }
            is Result.Error -> {
                // Mostrar error
                when (state.error) {
                    is AuthError.InvalidCredentials -> {
                        // Mostrar mensaje de credenciales inválidas
                    }
                    is AuthError.NetworkError -> {
                        // Mostrar error de red
                    }
                    // ... otros errores
                }
            }
            is Result.Loading -> {
                // Mostrar loading
            }
            null -> {
                // Estado inicial
            }
        }
    }

    // ... resto del composable
}

// 6. Configurar Retrofit en Application o Module:
object RetrofitClient {
    private const val BASE_URL = "https://tu-api.com/api/"

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    val authApi: AuthApiService = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(AuthApiService::class.java)
}
*/

// ============================================================================
// EJEMPLO DE USO CON KTOR (Alternativa)
// ============================================================================

/*
// Dependencias para Ktor:
dependencies {
    implementation("io.ktor:ktor-client-android:2.3.5")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.5")
    implementation("io.ktor:ktor-serialization-gson:2.3.5")
}

// Cliente Ktor:
class KtorAuthClient {
    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            gson()
        }
    }

    suspend fun login(email: String, password: String): AuthResponse {
        return client.post("https://tu-api.com/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(email, password))
        }.body()
    }
}
*/

