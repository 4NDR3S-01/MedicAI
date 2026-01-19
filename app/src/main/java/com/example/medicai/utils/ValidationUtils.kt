package com.example.medicai.utils

import java.text.SimpleDateFormat
import java.util.*

/**
 * Utilidades y constantes compartidas para validación de formularios
 * Validaciones
 */
object ValidationUtils {

    /**
     * Regex mejorado para validar formato de email
     * Valida: usuario@dominio.com con dominio válido (al menos 2 caracteres)
     */
    val EMAIL_REGEX = Regex(
        "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$"
    )

    /**
     * Longitud mínima de contraseña
     */
    const val MIN_PASSWORD_LENGTH = 8

    /**
     * Longitud mínima de teléfono (dígitos)
     */
    const val MIN_PHONE_DIGITS = 9

    /**
     * Longitud máxima de teléfono internacional (dígitos)
     */
    const val MAX_PHONE_DIGITS = 15

    /**
     * Longitud mínima de nombre completo (caracteres)
     */
    const val MIN_NAME_LENGTH = 3

    /**
     * Longitud máxima de nombre completo (caracteres)
     */
    const val MAX_NAME_LENGTH = 70

    /**
     * Longitud máxima para nombre de medicamento
     */
    const val MAX_MEDICINE_NAME_LENGTH = 100

    /**
     * Longitud máxima para dosificación
     */
    const val MAX_DOSAGE_LENGTH = 50

    /**
     * Longitud máxima para notas de medicamento
     */
    const val MAX_NOTES_LENGTH = 500

    /**
     * Longitud máxima para nombre de doctor
     */
    const val MAX_DOCTOR_NAME_LENGTH = 70

    /**
     * Longitud máxima para especialidad médica
     */
    const val MAX_SPECIALTY_LENGTH = 50

    /**
     * Longitud máxima para ubicación de cita
     */
    const val MAX_LOCATION_LENGTH = 200

    /**
     * Longitud máxima para notas de cita
     */
    const val MAX_APPOINTMENT_NOTES_LENGTH = 500

    /**
     * Valida si un email tiene formato correcto
     * ✅ Regex mejorado que valida dominio real
     */
    fun isValidEmail(email: String): Boolean {
        val trimmedEmail = email.trim()
        return trimmedEmail.isNotBlank() && EMAIL_REGEX.matches(trimmedEmail)
    }

    /**
     * Valida si una contraseña cumple los requisitos mínimos
     */
    fun isValidPassword(password: String): Boolean {
        return password.length >= MIN_PASSWORD_LENGTH
    }

    /**
     * Valida si un nombre completo cumple los requisitos
     * Solo acepta letras y espacios
     */
    fun isValidFullName(name: String): Boolean {
        val trimmed = name.trim()
        // Verificar longitud
        if (trimmed.length !in MIN_NAME_LENGTH..MAX_NAME_LENGTH) {
            return false
        }
        // Solo permitir letras y espacios
        return trimmed.all { it.isLetter() || it.isWhitespace() } && trimmed.any { it.isLetter() }
    }

    /**
     * Obtiene mensaje de error descriptivo para nombre
     */
    fun getNameErrorMessage(name: String): String? {
        val trimmed = name.trim()
        return when {
            trimmed.isEmpty() -> "El nombre no puede estar vacío"
            trimmed.length < MIN_NAME_LENGTH -> "El nombre es demasiado corto (mínimo $MIN_NAME_LENGTH caracteres)"
            trimmed.length > MAX_NAME_LENGTH -> "El nombre es demasiado largo (máximo $MAX_NAME_LENGTH caracteres)"
            !trimmed.all { it.isLetter() || it.isWhitespace() } -> "El nombre solo debe contener letras"
            !trimmed.any { it.isLetter() } -> "El nombre debe contener al menos una letra"
            else -> null
        }
    }

    /**
     * Valida si un número de teléfono tiene suficientes dígitos
     */
    fun isValidPhone(phone: String): Boolean {
        val digits = phone.filter { it.isDigit() }
        return digits.length >= MIN_PHONE_DIGITS
    }

    /**
     * Valida si un teléfono tiene formato internacional válido
     * Acepta formatos como: +1234567890, +34 123 456 789, etc.
     * Debe empezar con + y tener entre 9 y 15 dígitos
     */
    fun isValidPhoneInternational(phone: String): Boolean {
        val trimmed = phone.trim()
        if (!trimmed.startsWith("+")) {
            return false
        }
        val digits = trimmed.filter { it.isDigit() }
        return digits.length in MIN_PHONE_DIGITS..MAX_PHONE_DIGITS
    }

    /**
     * Verifica que dos contraseñas coincidan
     */
    fun passwordsMatch(password: String, confirm: String): Boolean {
        return password == confirm
    }

    /**
     * Valida si una fecha es futura (después de hoy)
     * Formato esperado: "yyyy-MM-dd"
     */
    fun isFutureDate(date: String): Boolean {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            dateFormat.isLenient = false
            val inputDate = dateFormat.parse(date) ?: return false
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
            inputDate.after(today)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Valida si una fecha es válida y no es pasada (para citas)
     * Formato esperado: "yyyy-MM-dd"
     */
    fun isValidAppointmentDate(date: String): Boolean {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            dateFormat.isLenient = false
            val inputDate = dateFormat.parse(date) ?: return false
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
            // La fecha debe ser hoy o futura
            !inputDate.before(today)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Valida si un nombre de medicamento cumple los requisitos
     */
    fun isValidMedicineName(name: String): Boolean {
        val trimmed = name.trim()
        return trimmed.isNotBlank() && trimmed.length <= MAX_MEDICINE_NAME_LENGTH
    }

    /**
     * Obtiene mensaje de error para nombre de medicamento
     */
    fun getMedicineNameErrorMessage(name: String): String? {
        val trimmed = name.trim()
        return when {
            trimmed.isEmpty() -> "El nombre del medicamento es requerido"
            trimmed.length > MAX_MEDICINE_NAME_LENGTH -> "El nombre es demasiado largo (máximo $MAX_MEDICINE_NAME_LENGTH caracteres)"
            else -> null
        }
    }

    /**
     * Valida si una dosificación cumple los requisitos
     */
    fun isValidDosage(dosage: String): Boolean {
        val trimmed = dosage.trim()
        return trimmed.isNotBlank() && trimmed.length <= MAX_DOSAGE_LENGTH
    }

    /**
     * Obtiene mensaje de error para dosificación
     */
    fun getDosageErrorMessage(dosage: String): String? {
        val trimmed = dosage.trim()
        return when {
            trimmed.isEmpty() -> "La dosificación es requerida"
            trimmed.length > MAX_DOSAGE_LENGTH -> "La dosificación es demasiado larga (máximo $MAX_DOSAGE_LENGTH caracteres)"
            else -> null
        }
    }

    /**
     * Valida si las notas cumplen los requisitos
     */
    fun isValidNotes(notes: String): Boolean {
        return notes.trim().length <= MAX_NOTES_LENGTH
    }

    /**
     * Valida si un nombre de doctor cumple los requisitos
     * Solo acepta letras y espacios
     */
    fun isValidDoctorName(name: String): Boolean {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || trimmed.length > MAX_DOCTOR_NAME_LENGTH) {
            return false
        }
        return trimmed.all { it.isLetter() || it.isWhitespace() } && trimmed.any { it.isLetter() }
    }

    /**
     * Obtiene mensaje de error para nombre de doctor
     */
    fun getDoctorNameErrorMessage(name: String): String? {
        val trimmed = name.trim()
        return when {
            trimmed.isEmpty() -> "El nombre del doctor es requerido"
            trimmed.length > MAX_DOCTOR_NAME_LENGTH -> "El nombre es demasiado largo (máximo $MAX_DOCTOR_NAME_LENGTH caracteres)"
            !trimmed.all { it.isLetter() || it.isWhitespace() } -> "El nombre solo debe contener letras"
            !trimmed.any { it.isLetter() } -> "El nombre debe contener al menos una letra"
            else -> null
        }
    }

    /**
     * Valida si una especialidad cumple los requisitos
     */
    fun isValidSpecialty(specialty: String): Boolean {
        val trimmed = specialty.trim()
        return trimmed.isNotBlank() && trimmed.length <= MAX_SPECIALTY_LENGTH
    }

    /**
     * Obtiene mensaje de error para especialidad
     */
    fun getSpecialtyErrorMessage(specialty: String): String? {
        val trimmed = specialty.trim()
        return when {
            trimmed.isEmpty() -> "La especialidad es requerida"
            trimmed.length > MAX_SPECIALTY_LENGTH -> "La especialidad es demasiado larga (máximo $MAX_SPECIALTY_LENGTH caracteres)"
            else -> null
        }
    }

    /**
     * Valida si una ubicación cumple los requisitos
     */
    fun isValidLocation(location: String): Boolean {
        val trimmed = location.trim()
        return trimmed.isNotBlank() && trimmed.length <= MAX_LOCATION_LENGTH
    }

    /**
     * Obtiene mensaje de error para ubicación
     */
    fun getLocationErrorMessage(location: String): String? {
        val trimmed = location.trim()
        return when {
            trimmed.isEmpty() -> "La ubicación es requerida"
            trimmed.length > MAX_LOCATION_LENGTH -> "La ubicación es demasiado larga (máximo $MAX_LOCATION_LENGTH caracteres)"
            else -> null
        }
    }

    /**
     * Valida si las notas de cita cumplen los requisitos
     */
    fun isValidAppointmentNotes(notes: String): Boolean {
        return notes.trim().length <= MAX_APPOINTMENT_NOTES_LENGTH
    }
}

