package com.example.medicai.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.medicai.data.models.Appointment
import com.example.medicai.data.models.AppointmentRequest
import com.example.medicai.data.models.Result
import com.example.medicai.data.repository.AppointmentRepository
import com.example.medicai.notifications.AlarmScheduler
import com.example.medicai.data.local.UserPreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para gestión de Citas Médicas
 * ✅ Usa reminder_minutes del usuario y respeta notifications_enabled
 */
class AppointmentViewModel(
    application: Application,
    private val repository: AppointmentRepository = AppointmentRepository()
) : AndroidViewModel(application) {

    // Estado de citas
    private val _appointments = MutableStateFlow<List<Appointment>>(emptyList())
    val appointments: StateFlow<List<Appointment>> = _appointments.asStateFlow()

    // Estado de carga
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Estado de error
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Estado de operación exitosa
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    /**
     * Cargar citas del usuario
     */
    fun loadAppointments(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            Log.d("AppointmentViewModel", "🔄 Cargando citas...")

            when (val result = repository.getAppointments(userId)) {
                is Result.Success -> {
                    _appointments.value = result.data
                    Log.d("AppointmentViewModel", "✅ ${result.data.size} citas cargadas")
                }
                is Result.Error -> {
                    _error.value = result.message
                    Log.e("AppointmentViewModel", "❌ Error: ${result.message}")
                }
                else -> {}
            }

            _isLoading.value = false
        }
    }

    /**
     * Cargar solo citas próximas
     */
    fun loadUpcomingAppointments(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            when (val result = repository.getUpcomingAppointments(userId)) {
                is Result.Success -> {
                    _appointments.value = result.data
                }
                is Result.Error -> {
                    _error.value = result.message
                }
                else -> {}
            }

            _isLoading.value = false
        }
    }

    /**
     * Agregar nueva cita
     * ✅ Usa reminder_minutes del usuario y verifica notifications_enabled
     */
    fun addAppointment(appointment: AppointmentRequest, onSuccess: () -> Unit = {}) {
        Log.wtf("AppointmentViewModel", "🚨🚨🚨 addAppointment() LLAMADO 🚨🚨🚨")
        Log.wtf("AppointmentViewModel", "Doctor: ${appointment.doctor_name}")
        
        // Toast inmediato para confirmar que se ejecuta
        android.widget.Toast.makeText(
            getApplication(),
            "🔥 addAppointment() ejecutándose para Dr. ${appointment.doctor_name}",
            android.widget.Toast.LENGTH_LONG
        ).show()
        
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            Log.d("AppointmentViewModel", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d("AppointmentViewModel", "➕ Agregando cita")
            Log.d("AppointmentViewModel", "📋 Datos de la cita:")
            Log.d("AppointmentViewModel", "   - Doctor: ${appointment.doctor_name}")
            Log.d("AppointmentViewModel", "   - Especialidad: ${appointment.specialty}")
            Log.d("AppointmentViewModel", "   - Fecha: ${appointment.date}")
            Log.d("AppointmentViewModel", "   - Hora: ${appointment.time}")
            Log.d("AppointmentViewModel", "   - Ubicación: ${appointment.location}")
            Log.d("AppointmentViewModel", "   - Usuario: ${appointment.user_id}")

            when (val result = repository.addAppointment(appointment)) {
                is Result.Success -> {
                    val appointmentId = result.data.id
                    val status = result.data.status
                    Log.d("AppointmentViewModel", "✅ Cita guardada en DB con ID: $appointmentId")
                    Log.d("AppointmentViewModel", "   - Status: $status")
                    
                    _successMessage.value = "Cita con ${appointment.doctor_name} agendada"

                    // ✅ Verificar si las notificaciones están habilitadas
                    val notificationsEnabled = UserPreferencesManager.areNotificationsEnabled(getApplication())
                    Log.d("AppointmentViewModel", "🔔 Verificando preferencias de notificaciones...")
                    Log.d("AppointmentViewModel", "   - areNotificationsEnabled: $notificationsEnabled")
                    Log.d("AppointmentViewModel", "   - status: $status")
                    
                    // 🔔 Programar recordatorio de cita solo si está habilitado
                    if (status == "scheduled" && notificationsEnabled) {
                        Log.d("AppointmentViewModel", "⏰ PROGRAMANDO ALARMA DE CITA...")
                        
                        try {
                            // ✅ Obtener reminder_minutes del usuario
                            val reminderMinutes = UserPreferencesManager.getReminderMinutes(getApplication())
                            
                            Log.d("AppointmentViewModel", "   - Appointment ID: $appointmentId")
                            Log.d("AppointmentViewModel", "   - Doctor: ${result.data.doctor_name}")
                            Log.d("AppointmentViewModel", "   - Especialidad: ${result.data.specialty}")
                            Log.d("AppointmentViewModel", "   - Fecha: ${result.data.date}")
                            Log.d("AppointmentViewModel", "   - Hora: ${result.data.time}")
                            Log.d("AppointmentViewModel", "   - Recordatorio: $reminderMinutes min antes")
                            
                            AlarmScheduler.scheduleAppointmentReminder(
                                context = getApplication(),
                                appointmentId = appointmentId,
                                doctorName = result.data.doctor_name,
                                specialty = result.data.specialty,
                                date = result.data.date,
                                time = result.data.time,
                                location = result.data.location,
                                minutesBefore = reminderMinutes // ✅ Usar preferencia del usuario
                            )
                            Log.d("AppointmentViewModel", "✅ AlarmScheduler.scheduleAppointmentReminder() completado")
                        } catch (e: Exception) {
                            Log.e("AppointmentViewModel", "❌ EXCEPCIÓN al programar recordatorio de cita", e)
                            Log.e("AppointmentViewModel", "   - Tipo: ${e.javaClass.name}")
                            Log.e("AppointmentViewModel", "   - Mensaje: ${e.message}")
                            Log.e("AppointmentViewModel", "   - Stack trace: ${e.stackTraceToString()}")
                        }
                    } else {
                        if (!notificationsEnabled) {
                            Log.w("AppointmentViewModel", "⚠️ NOTIFICACIONES DESHABILITADAS - No se programa recordatorio")
                            Log.w("AppointmentViewModel", "   📋 Solución: Ve a Perfil → Configurar Notificaciones → ACTIVAR")
                        }
                        if (status != "scheduled") {
                            Log.w("AppointmentViewModel", "⚠️ CITA NO ESTÁ EN STATUS 'scheduled' - No se programa recordatorio")
                            Log.w("AppointmentViewModel", "   📋 Status actual: $status")
                        }
                    }

                    loadAppointments(appointment.user_id)
                    onSuccess()
                    Log.d("AppointmentViewModel", "✅ Proceso completado")
                    Log.d("AppointmentViewModel", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                }
                is Result.Error -> {
                    _error.value = result.message
                    Log.e("AppointmentViewModel", "❌ Error guardando cita: ${result.message}")
                    Log.d("AppointmentViewModel", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                }
                else -> {
                    Log.e("AppointmentViewModel", "❓ Resultado desconocido del repositorio")
                    Log.d("AppointmentViewModel", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                }
            }

            _isLoading.value = false
        }
    }

    /**
     * Actualizar cita existente
     */
    fun updateAppointment(id: String, appointment: AppointmentRequest, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            when (val result = repository.updateAppointment(id, appointment)) {
                is Result.Success -> {
                    _successMessage.value = "Cita actualizada"
                    loadAppointments(appointment.user_id)
                    onSuccess()
                }
                is Result.Error -> {
                    _error.value = result.message
                }
                else -> {}
            }

            _isLoading.value = false
        }
    }

    /**
     * Cancelar cita
     */
    fun cancelAppointment(id: String, userId: String) {
        viewModelScope.launch {
            _error.value = null

            Log.d("AppointmentViewModel", "❌ Cancelando cita: $id")

            when (val result = repository.cancelAppointment(id)) {
                is Result.Success -> {
                    _successMessage.value = "Cita cancelada"

                    // 🔕 Cancelar recordatorio de la cita
                    try {
                        AlarmScheduler.cancelAppointmentReminder(
                            context = getApplication(),
                            appointmentId = id
                        )
                        Log.d("AppointmentViewModel", "🔕 Recordatorio cancelado")
                    } catch (e: Exception) {
                        Log.e("AppointmentViewModel", "❌ Error cancelando recordatorio: ${e.message}")
                    }

                    loadAppointments(userId)
                    Log.d("AppointmentViewModel", "✅ Cita cancelada")
                }
                is Result.Error -> {
                    _error.value = result.message
                    Log.e("AppointmentViewModel", "❌ Error: ${result.message}")
                }
                else -> {}
            }
        }
    }

    /**
     * Completar cita
     */
    fun completeAppointment(id: String, userId: String) {
        viewModelScope.launch {
            _error.value = null

            when (val result = repository.completeAppointment(id)) {
                is Result.Success -> {
                    _successMessage.value = "Cita marcada como completada"

                    // 🔕 Cancelar recordatorio de la cita completada
                    try {
                        AlarmScheduler.cancelAppointmentReminder(
                            context = getApplication(),
                            appointmentId = id
                        )
                        Log.d("AppointmentViewModel", "🔕 Recordatorio cancelado (cita completada)")
                    } catch (e: Exception) {
                        Log.e("AppointmentViewModel", "❌ Error cancelando recordatorio: ${e.message}")
                    }

                    loadAppointments(userId)
                }
                is Result.Error -> {
                    _error.value = result.message
                }
                else -> {}
            }
        }
    }

    /**
     * Eliminar cita permanentemente
     */
    fun deleteAppointment(id: String, userId: String) {
        viewModelScope.launch {
            _error.value = null

            when (val result = repository.deleteAppointment(id)) {
                is Result.Success -> {
                    _successMessage.value = "Cita eliminada"

                    // 🔕 Cancelar recordatorio de la cita eliminada
                    try {
                        AlarmScheduler.cancelAppointmentReminder(
                            context = getApplication(),
                            appointmentId = id
                        )
                        Log.d("AppointmentViewModel", "🔕 Recordatorio cancelado (cita eliminada)")
                    } catch (e: Exception) {
                        Log.e("AppointmentViewModel", "❌ Error cancelando recordatorio: ${e.message}")
                    }

                    loadAppointments(userId)
                }
                is Result.Error -> {
                    _error.value = result.message
                }
                else -> {}
            }
        }
    }

    /**
     * Limpiar mensaje de éxito
     */
    fun clearSuccessMessage() {
        _successMessage.value = null
    }

    /**
     * Limpiar mensaje de error
     */
    fun clearError() {
        _error.value = null
    }
    /**
     * Factory para crear AppointmentViewModel
     */
    companion object {
        val Factory: androidx.lifecycle.ViewModelProvider.Factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(
                modelClass: Class<T>,
                extras: androidx.lifecycle.viewmodel.CreationExtras
            ): T {
                val application = checkNotNull(extras[androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                return AppointmentViewModel(application) as T
            }
        }
    }
}

