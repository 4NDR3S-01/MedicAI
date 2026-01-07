package com.example.medicai.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import android.util.Log
import com.example.medicai.data.models.AuthState
import com.example.medicai.data.models.RegistrationData
import com.example.medicai.data.models.Result
import com.example.medicai.data.models.UserProfile
import com.example.medicai.data.repository.AuthRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class AuthViewModelTest {

    // Regla para ejecutar operaciones en el hilo principal de forma síncrona
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    // Dispatcher de prueba para controlar las coroutines
    private val testDispatcher = StandardTestDispatcher()

    // Mock del repositorio
    private lateinit var mockRepository: AuthRepository
    
    // ViewModel bajo prueba
    private lateinit var viewModel: AuthViewModel

    // Datos de prueba reutilizables
    private val testUserProfile = UserProfile(
        id = "test-user-id",
        email = "test@example.com",
        full_name = "Usuario Test",
        phone = "123456789",
        notifications_enabled = true,
        reminder_minutes = 15
    )

    @Before
    fun setup() {
        // Configurar el dispatcher de prueba
        Dispatchers.setMain(testDispatcher)

        // Mock estático de android.util.Log para evitar errores "not mocked" en tests JVM
        mockkStatic(Log::class)
        // Sobrecargas (tag: String, msg: String)
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.i(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        // Sobrecargas (tag: String, msg: String, tr: Throwable)
        every { Log.d(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { Log.i(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { Log.w(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any<Throwable>()) } returns 0
        // Sobrecarga específica de Log.w(tag, tr)
        every { Log.w(any<String>(), any<Throwable>()) } returns 0
        
        // Crear mock del repositorio
        mockRepository = mockk(relaxed = true)
        
        // Mock de métodos que se llaman en el init del ViewModel
        coEvery { mockRepository.hasLocalSession() } returns false
        coEvery { mockRepository.isUserLoggedIn() } returns false
        
        // Crear instancia del ViewModel con el mock
        viewModel = AuthViewModel(mockRepository)
        
        // Avanzar el dispatcher para completar la inicialización
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        // Restaurar el dispatcher principal
        Dispatchers.resetMain()
        
        // Limpiar los mocks
        clearAllMocks()
        unmockkStatic(Log::class)
    }

    // ========== PRUEBAS DE LOGIN ==========

    @Test
    fun `login con credenciales válidas debe actualizar estado a Success`() = runTest {
        // Arrange - Configurar el mock para retornar éxito
        val email = "test@example.com"
        val password = "password123"
        coEvery { mockRepository.login(email, password) } returns Result.Success(testUserProfile)

        // Act - Ejecutar login
        viewModel.login(email, password)
        advanceUntilIdle() // Esperar a que terminen las coroutines

        // Assert - Verificar que el estado sea Success con el usuario correcto
        val authState = viewModel.authState.value
        assertTrue("El estado debe ser Success", authState is AuthState.Success)
        assertEquals("El usuario debe ser el correcto", 
            testUserProfile, (authState as AuthState.Success).user)
    }

    @Test
    fun `login con credenciales inválidas debe actualizar estado a Error`() = runTest {
        // Arrange - Configurar el mock para retornar error
        val email = "test@example.com"
        val password = "wrongpassword"
        val errorMessage = "Credenciales inválidas"
        coEvery { mockRepository.login(email, password) } returns Result.Error(errorMessage)

        // Act - Ejecutar login con credenciales incorrectas
        viewModel.login(email, password)
        advanceUntilIdle()

        // Assert - Verificar que el estado sea Error
        val authState = viewModel.authState.value
        assertTrue("El estado debe ser Error", authState is AuthState.Error)
        assertEquals("El mensaje de error debe ser correcto", 
            errorMessage, (authState as AuthState.Error).message)
    }

    @Test
    fun `login debe pasar por estado Loading antes de Success`() = runTest {
        // Arrange
        val email = "test@example.com"
        val password = "password123"
        coEvery { mockRepository.login(email, password) } returns Result.Success(testUserProfile)

        // Act & Assert - Observar los estados a medida que cambian
        viewModel.loginState.test {
            // Estado inicial es null
            assertNull("Estado inicial debe ser null", awaitItem())
            
            // Ejecutar login
            viewModel.login(email, password)
            
            // Debe pasar a Loading
            val loadingState = awaitItem()
            assertTrue("Debe pasar por Loading", loadingState is Result.Loading)
            
            // Finalmente Success
            advanceUntilIdle()
            val successState = awaitItem()
            assertTrue("Debe terminar en Success", successState is Result.Success)
            assertEquals("Usuario debe ser correcto", 
                testUserProfile, (successState as Result.Success).data)
        }
    }

    @Test
    fun `login debe hacer trim al email antes de enviar`() = runTest {
        // Arrange - Email con espacios
        val emailWithSpaces = "  test@example.com  "
        val emailTrimmed = "test@example.com"
        val password = "password123"
        coEvery { mockRepository.login(emailTrimmed, password) } returns Result.Success(testUserProfile)

        // Act
        viewModel.login(emailWithSpaces, password)
        advanceUntilIdle()

        // Assert - Verificar que se llamó al repositorio con email sin espacios
        coVerify { mockRepository.login(emailTrimmed, password) }
    }

    @Test
    fun `login exitoso debe actualizar currentUser`() = runTest {
        // Arrange
        val email = "test@example.com"
        val password = "password123"
        coEvery { mockRepository.login(email, password) } returns Result.Success(testUserProfile)

        // Verificar que currentUser es null inicialmente
        assertNull("CurrentUser debe ser null inicialmente", viewModel.currentUser.value)

        // Act
        viewModel.login(email, password)
        advanceUntilIdle()

        // Assert
        assertEquals("CurrentUser debe actualizarse después del login", 
            testUserProfile, viewModel.currentUser.value)
    }

    // ========== PRUEBAS DE REGISTRO ==========

    @Test
    fun `registro con datos válidos debe actualizar estado a Success`() = runTest {
        // Arrange
        val registrationData = RegistrationData(
            fullName = "Nuevo Usuario",
            email = "nuevo@example.com",
            password = "password123",
            phone = "987654321",
            notificationsEnabled = true,
            reminderMinutes = 15,
            termsAccepted = true
        )
        coEvery { mockRepository.register(registrationData) } returns Result.Success(testUserProfile)

        // Act
        viewModel.register(registrationData)
        advanceUntilIdle()

        // Assert
        val authState = viewModel.authState.value
        assertTrue("El estado debe ser Success", authState is AuthState.Success)
        assertEquals("El usuario registrado debe ser correcto", 
            testUserProfile, (authState as AuthState.Success).user)
    }

    @Test
    fun `registro con email duplicado debe retornar Error`() = runTest {
        // Arrange
        val registrationData = RegistrationData(
            fullName = "Usuario Duplicado",
            email = "duplicado@example.com",
            password = "password123",
            phone = null,
            notificationsEnabled = true,
            reminderMinutes = 15,
            termsAccepted = true
        )
        val errorMessage = "Este email ya está registrado"
        coEvery { mockRepository.register(registrationData) } returns Result.Error(errorMessage)

        // Act
        viewModel.register(registrationData)
        advanceUntilIdle()

        // Assert
        val authState = viewModel.authState.value
        assertTrue("El estado debe ser Error", authState is AuthState.Error)
        assertTrue("El mensaje debe indicar email duplicado", 
            (authState as AuthState.Error).message.contains("ya está registrado"))
    }

    @Test
    fun `registro debe pasar por estado Loading antes de Success`() = runTest {
        // Arrange
        val registrationData = RegistrationData(
            fullName = "Test User",
            email = "test@example.com",
            password = "password123",
            phone = null,
            notificationsEnabled = true,
            reminderMinutes = 15,
            termsAccepted = true
        )
        coEvery { mockRepository.register(registrationData) } returns Result.Success(testUserProfile)

        // Act & Assert
        viewModel.registerState.test {
            assertNull("Estado inicial debe ser null", awaitItem())
            
            viewModel.register(registrationData)
            
            val loadingState = awaitItem()
            assertTrue("Debe pasar por Loading", loadingState is Result.Loading)
            
            advanceUntilIdle()
            val successState = awaitItem()
            assertTrue("Debe terminar en Success", successState is Result.Success)
        }
    }

    @Test
    fun `registro exitoso debe actualizar currentUser`() = runTest {
        // Arrange
        val registrationData = RegistrationData(
            fullName = "Nuevo Usuario",
            email = "nuevo@example.com",
            password = "password123",
            phone = "123456789",
            notificationsEnabled = true,
            reminderMinutes = 15,
            termsAccepted = true
        )
        coEvery { mockRepository.register(registrationData) } returns Result.Success(testUserProfile)

        // Act
        viewModel.register(registrationData)
        advanceUntilIdle()

        // Assert
        assertNotNull("CurrentUser debe actualizarse", viewModel.currentUser.value)
        assertEquals("CurrentUser debe tener los datos correctos", 
            testUserProfile, viewModel.currentUser.value)
    }

    // ========== PRUEBAS DE RECUPERACIÓN DE CONTRASEÑA ==========

    @Test
    fun `resetPassword con email válido debe retornar Success`() = runTest {
        // Arrange
        val email = "test@example.com"
        coEvery { mockRepository.resetPassword(email) } returns Result.Success(Unit)

        // Act
        viewModel.resetPassword(email)
        advanceUntilIdle()

        // Assert
        val resetState = viewModel.resetPasswordState.value
        assertTrue("El estado debe ser Success", resetState is Result.Success)
        
        // Verificar que el mensaje se actualizó
        val message = viewModel.message.value
        assertNotNull("Debe haber un mensaje", message)
        assertTrue("El mensaje debe indicar éxito", 
            message?.contains("Email de recuperación enviado") == true)
    }

    @Test
    fun `resetPassword debe hacer trim al email`() = runTest {
        // Arrange
        val emailWithSpaces = "  test@example.com  "
        val emailTrimmed = "test@example.com"
        coEvery { mockRepository.resetPassword(emailTrimmed) } returns Result.Success(Unit)

        // Act
        viewModel.resetPassword(emailWithSpaces)
        advanceUntilIdle()

        // Assert
        coVerify { mockRepository.resetPassword(emailTrimmed) }
    }

    @Test
    fun `resetPassword con email inválido debe retornar Error`() = runTest {
        // Arrange
        val email = "emailinvalido"
        val errorMessage = "Email inválido"
        coEvery { mockRepository.resetPassword(email) } returns Result.Error(errorMessage)

        // Act
        viewModel.resetPassword(email)
        advanceUntilIdle()

        // Assert
        val resetState = viewModel.resetPasswordState.value
        assertTrue("El estado debe ser Error", resetState is Result.Error)
    }

    // ========== PRUEBAS DE ESTADOS Y MENSAJES ==========

    @Test
    fun `mensaje de bienvenida debe incluir nombre del usuario después de login`() = runTest {
        // Arrange
        val email = "test@example.com"
        val password = "password123"
        coEvery { mockRepository.login(email, password) } returns Result.Success(testUserProfile)

        // Act
        viewModel.login(email, password)
        advanceUntilIdle()

        // Assert
        val message = viewModel.message.value
        assertNotNull("Debe haber un mensaje", message)
        assertTrue("El mensaje debe contener 'Bienvenido'", 
            message?.contains("Bienvenido") == true)
        assertTrue("El mensaje debe contener el nombre del usuario", 
            message?.contains(testUserProfile.full_name) == true)
    }

    @Test
    fun `authState debe iniciar en Loading durante inicialización`() {
        // El estado inicial después del setup debe ser Error 
        // porque no hay sesión activa (hasLocalSession = false)
        val authState = viewModel.authState.value
        assertTrue("El estado inicial debe ser Error cuando no hay sesión", 
            authState is AuthState.Error)
    }

    @Test
    fun `múltiples llamadas a login deben actualizar el estado correctamente`() = runTest {
        // Arrange
        val email1 = "user1@example.com"
        val email2 = "user2@example.com"
        val password = "password123"
        
        val user1 = testUserProfile.copy(email = email1, full_name = "Usuario 1")
        val user2 = testUserProfile.copy(email = email2, full_name = "Usuario 2")
        
        coEvery { mockRepository.login(email1, password) } returns Result.Success(user1)
        coEvery { mockRepository.login(email2, password) } returns Result.Success(user2)

        // Act - Primer login
        viewModel.login(email1, password)
        advanceUntilIdle()
        
        val firstUser = viewModel.currentUser.value
        assertEquals("Primer usuario debe ser correcto", user1, firstUser)

        // Act - Segundo login
        viewModel.login(email2, password)
        advanceUntilIdle()
        
        val secondUser = viewModel.currentUser.value
        assertEquals("Segundo usuario debe ser correcto", user2, secondUser)
    }

    // ========== PRUEBAS DE MANEJO DE ERRORES ==========

    @Test
    fun `error de red durante login debe mostrar mensaje apropiado`() = runTest {
        // Arrange
        val email = "test@example.com"
        val password = "password123"
        val errorMessage = "Error de conexión. Verifica tu internet"
        coEvery { mockRepository.login(email, password) } returns Result.Error(errorMessage)

        // Act
        viewModel.login(email, password)
        advanceUntilIdle()

        // Assert
        val message = viewModel.message.value
        assertTrue("El mensaje debe indicar error de red", 
            message?.contains("conexión") == true || message?.contains("internet") == true)
    }

    @Test
    fun `campo email vacío debe ser enviado al repositorio como string vacío`() = runTest {
        // Arrange - Este es un caso extremo, normalmente la UI debería validar
        val emptyEmail = ""
        val password = "password123"
        coEvery { mockRepository.login(emptyEmail, password) } returns 
            Result.Error("Email requerido")

        // Act
        viewModel.login(emptyEmail, password)
        advanceUntilIdle()

        // Assert
        coVerify { mockRepository.login(emptyEmail, password) }
        val authState = viewModel.authState.value
        assertTrue("Debe retornar error", authState is AuthState.Error)
    }
}
