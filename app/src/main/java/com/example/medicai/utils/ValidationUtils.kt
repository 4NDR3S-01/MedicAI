package com.example.medicai.utils

import java.text.SimpleDateFormat
import java.util.*

/**
 * Utilidades y constantes compartidas para validación de formularios
 * ✅ Validaciones mejoradas para producción
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
}

