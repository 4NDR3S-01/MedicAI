package com.example.medicai.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.medicai.data.models.Medicine
import com.example.medicai.data.models.MedicineRequest
import com.example.medicai.data.models.Result
import com.example.medicai.data.repository.MedicineRepository
import com.example.medicai.notifications.AlarmScheduler
import com.example.medicai.data.local.UserPreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para gestión de Medicamentos
 * ✅ Respeta notifications_enabled del usuario
 */
class MedicineViewModel(
    application: Application,
    private val repository: MedicineRepository = MedicineRepository()
) : AndroidViewModel(application) {

    // Estado de medicamentos
    private val _medicines = MutableStateFlow<List<Medicine>>(emptyList())
    val medicines: StateFlow<List<Medicine>> = _medicines.asStateFlow()

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
     * Cargar medicamentos del usuario
     */
    fun loadMedicines(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            Log.d("MedicineViewModel", "🔄 Cargando medicamentos...")

            when (val result = repository.getMedicines(userId)) {
                is Result.Success -> {
                    _medicines.value = result.data
                    Log.d("MedicineViewModel", "✅ ${result.data.size} medicamentos cargados")
                }
                is Result.Error -> {
                    _error.value = result.message
                    Log.e("MedicineViewModel", "❌ Error: ${result.message}")
                }
                else -> {}
            }

            _isLoading.value = false
        }
    }

    /**
     * Agregar nuevo medicamento
     * ✅ Verifica notifications_enabled antes de programar alarmas
     */
    fun addMedicine(medicine: MedicineRequest, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            Log.d("MedicineViewModel", "➕ Agregando medicamento: ${medicine.name}")

            when (val result = repository.addMedicine(medicine)) {
                is Result.Success -> {
                    _successMessage.value = "${medicine.name} agregado exitosamente"

                    // ✅ Verificar si las notificaciones están habilitadas
                    val notificationsEnabled = UserPreferencesManager.areNotificationsEnabled(getApplication())

                    // 🔔 Programar notificaciones para el medicamento solo si está activo y habilitado
                    if (medicine.active && notificationsEnabled) {
                        try {
                            AlarmScheduler.scheduleMedicineReminders(
                                context = getApplication(),
                                medicineId = (result as Result.Success<Medicine>).data.id,
                                medicineName = medicine.name,
                                dosage = medicine.dosage,
                                times = medicine.times
                            )
                            Log.d("MedicineViewModel", "⏰ Notificaciones programadas para ${medicine.name}")
                        } catch (e: Exception) {
                            Log.e("MedicineViewModel", "❌ Error programando notificaciones: ${e.message}")
                        }
                    } else if (!notificationsEnabled) {
                        Log.d("MedicineViewModel", "🔕 Notificaciones deshabilitadas, no se programan alarmas para ${medicine.name}")
                    }

                    loadMedicines(medicine.user_id) // Recargar lista
                    onSuccess()
                    Log.d("MedicineViewModel", "✅ Medicamento agregado")
                }
                is Result.Error -> {
                    _error.value = result.message
                    Log.e("MedicineViewModel", "❌ Error: ${result.message}")
                }
                else -> {}
            }

            _isLoading.value = false
        }
    }

    /**
     * Actualizar medicamento existente
     */
    fun updateMedicine(id: String, medicine: MedicineRequest, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            when (val result = repository.updateMedicine(id, medicine)) {
                is Result.Success -> {
                    _successMessage.value = "${medicine.name} actualizado"
                    loadMedicines(medicine.user_id)
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
     * Eliminar medicamento (soft delete - desactivar)
     */
    fun deleteMedicine(id: String, userId: String, medicineName: String) {
        viewModelScope.launch {
            _error.value = null

            Log.d("MedicineViewModel", "🗑️ Desactivando medicamento: $id")

            // Obtener el medicamento antes de desactivarlo para cancelar notificaciones
            val medicine = _medicines.value.find { it.id == id }

            when (val result = repository.deleteMedicine(id)) {
                is Result.Success -> {
                    _successMessage.value = "$medicineName desactivado"

                    // 🔕 Cancelar notificaciones del medicamento
                    medicine?.let {
                        try {
                            AlarmScheduler.cancelMedicineReminders(
                                context = getApplication(),
                                medicineId = it.id,
                                timesCount = it.times.size
                            )
                            Log.d("MedicineViewModel", "🔕 Notificaciones canceladas para $medicineName")
                        } catch (e: Exception) {
                            Log.e("MedicineViewModel", "❌ Error cancelando notificaciones: ${e.message}")
                        }
                    }

                    loadMedicines(userId) // Recargar lista
                    Log.d("MedicineViewModel", "✅ Medicamento desactivado")
                }
                is Result.Error -> {
                    _error.value = result.message
                    Log.e("MedicineViewModel", "❌ Error: ${result.message}")
                }
                else -> {}
            }
        }
    }

    /**
     * Eliminar medicamento permanentemente (hard delete)
     */
    fun deletePermanently(id: String, userId: String, medicineName: String) {
        viewModelScope.launch {
            _error.value = null

            Log.d("MedicineViewModel", "⚠️ Eliminando permanentemente: $id")

            // Obtener el medicamento antes de eliminarlo para cancelar notificaciones
            val medicine = _medicines.value.find { it.id == id }

            when (val result = repository.deletePermanently(id)) {
                is Result.Success -> {
                    _successMessage.value = "$medicineName eliminado permanentemente"

                    // 🔕 Cancelar notificaciones del medicamento
                    medicine?.let {
                        try {
                            AlarmScheduler.cancelMedicineReminders(
                                context = getApplication(),
                                medicineId = it.id,
                                timesCount = it.times.size
                            )
                            Log.d("MedicineViewModel", "🔕 Notificaciones canceladas para $medicineName")
                        } catch (e: Exception) {
                            Log.e("MedicineViewModel", "❌ Error cancelando notificaciones: ${e.message}")
                        }
                    }

                    loadMedicines(userId) // Recargar lista
                    Log.d("MedicineViewModel", "✅ Medicamento eliminado permanentemente")
                }
                is Result.Error -> {
                    _error.value = result.message
                    Log.e("MedicineViewModel", "❌ Error: ${result.message}")
                }
                else -> {}
            }
        }
    }

    /**
     * Reactivar medicamento
     * ✅ Verifica notifications_enabled antes de programar alarmas
     */
    fun reactivateMedicine(id: String, userId: String, medicineName: String) {
        viewModelScope.launch {
            _error.value = null

            Log.d("MedicineViewModel", "✅ Reactivando medicamento: $id")

            // Obtener el medicamento antes de reactivar para tener sus datos
            val medicine = _medicines.value.find { it.id == id }

            when (val result = repository.reactivateMedicine(id)) {
                is Result.Success -> {
                    _successMessage.value = "$medicineName reactivado"

                    // ✅ Verificar si las notificaciones están habilitadas
                    val notificationsEnabled = UserPreferencesManager.areNotificationsEnabled(getApplication())

                    // 🔔 Reprogramar notificaciones del medicamento solo si está habilitado
                    if (notificationsEnabled) {
                        medicine?.let { med ->
                            try {
                                AlarmScheduler.scheduleMedicineReminders(
                                    context = getApplication(),
                                    medicineId = med.id,
                                    medicineName = med.name,
                                    dosage = med.dosage,
                                    times = med.times
                                )
                                Log.d("MedicineViewModel", "⏰ Notificaciones reprogramadas para $medicineName")
                            } catch (e: Exception) {
                                Log.e("MedicineViewModel", "❌ Error reprogramando notificaciones: ${e.message}")
                            }
                        }
                    } else {
                        Log.d("MedicineViewModel", "🔕 Notificaciones deshabilitadas, no se reprograman alarmas")
                    }

                    loadMedicines(userId) // Recargar lista
                    Log.d("MedicineViewModel", "✅ Medicamento reactivado")
                }
                is Result.Error -> {
                    _error.value = result.message
                    Log.e("MedicineViewModel", "❌ Error: ${result.message}")
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
     * Factory para crear MedicineViewModel
     */
    companion object {
        val Factory: androidx.lifecycle.ViewModelProvider.Factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(
                modelClass: Class<T>,
                extras: androidx.lifecycle.viewmodel.CreationExtras
            ): T {
                val application = checkNotNull(extras[androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                return MedicineViewModel(application) as T
            }
        }
    }
}

