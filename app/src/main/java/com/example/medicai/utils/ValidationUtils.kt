package com.example.medicai.utils

/**
 * Utilidades y constantes compartidas para validación de formularios
 */
object ValidationUtils {

    /**
     * Regex para validar formato de email
     * Acepta formatos básicos como: usuario@dominio.com
     */
    val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")

    /**
     * Longitud mínima de contraseña
     */
    const val MIN_PASSWORD_LENGTH = 8

    /**
     * Longitud mínima de teléfono (dígitos)
     */
    const val MIN_PHONE_DIGITS = 9

    /**
     * Valida si un email tiene formato correcto
     */
    fun isValidEmail(email: String): Boolean {
        return EMAIL_REGEX.matches(email.trim())
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
     * Verifica que dos contraseñas coincidan
     */
    fun passwordsMatch(password: String, confirm: String): Boolean {
        return password == confirm
    }
}

