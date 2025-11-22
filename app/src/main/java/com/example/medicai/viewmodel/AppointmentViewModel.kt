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
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            Log.d("AppointmentViewModel", "➕ Agregando cita con: ${appointment.doctor_name}")

            when (val result = repository.addAppointment(appointment)) {
                is Result.Success -> {
                    _successMessage.value = "Cita con ${appointment.doctor_name} agendada"

                    // ✅ Verificar si las notificaciones están habilitadas
                    val notificationsEnabled = UserPreferencesManager.areNotificationsEnabled(getApplication())
                    
                    // 🔔 Programar recordatorio de cita solo si está habilitado
                    if (result.data.status == "scheduled" && notificationsEnabled) {
                        try {
                            // ✅ Obtener reminder_minutes del usuario
                            val reminderMinutes = UserPreferencesManager.getReminderMinutes(getApplication())
                            
                            AlarmScheduler.scheduleAppointmentReminder(
                                context = getApplication(),
                                appointmentId = result.data.id,
                                doctorName = result.data.doctor_name,
                                specialty = result.data.specialty,
                                date = result.data.date,
                                time = result.data.time,
                                location = result.data.location,
                                minutesBefore = reminderMinutes // ✅ Usar preferencia del usuario
                            )
                            Log.d("AppointmentViewModel", "⏰ Recordatorio programado $reminderMinutes min antes para ${result.data.doctor_name}")
                        } catch (e: Exception) {
                            Log.e("AppointmentViewModel", "❌ Error programando recordatorio: ${e.message}")
                        }
                    } else if (!notificationsEnabled) {
                        Log.d("AppointmentViewModel", "🔕 Notificaciones deshabilitadas, no se programa recordatorio")
                    }

                    loadAppointments(appointment.user_id)
                    onSuccess()
                    Log.d("AppointmentViewModel", "✅ Cita agregada")
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

