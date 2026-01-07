package com.example.medicai.utils

import org.junit.Assert.*
import org.junit.Test

/**
 * Pruebas unitarias para ValidationUtils
 * 
 * Estas pruebas validan la lógica de negocio crítica de validación
 * de formularios sin depender de la interfaz gráfica.
 * 
 * ✅ Casos cubiertos:
 * - Validación de emails correctos e incorrectos
 * - Validación de contraseñas según longitud mínima
 * - Casos extremos (vacíos, espacios, formatos inválidos)
 */
class ValidationUtilsTest {

    // ========== PRUEBAS DE VALIDACIÓN DE EMAIL ==========
    
    @Test
    fun `email válido con formato correcto debe retornar true`() {
        // Arrange - Preparar datos
        val emailValido = "usuario@example.com"
        
        // Act - Ejecutar la función
        val resultado = ValidationUtils.isValidEmail(emailValido)
        
        // Assert - Verificar resultado
        assertTrue("El email válido debería ser aceptado", resultado)
    }
    
    @Test
    fun `email sin arroba debe retornar false`() {
        val emailInvalido = "usuarioexample.com"
        val resultado = ValidationUtils.isValidEmail(emailInvalido)
        assertFalse("El email sin @ debería ser rechazado", resultado)
    }
    
    @Test
    fun `email sin dominio debe retornar false`() {
        val emailInvalido = "usuario@"
        val resultado = ValidationUtils.isValidEmail(emailInvalido)
        assertFalse("El email sin dominio debería ser rechazado", resultado)
    }
    
    @Test
    fun `email sin extensión de dominio debe retornar false`() {
        val emailInvalido = "usuario@example"
        val resultado = ValidationUtils.isValidEmail(emailInvalido)
        assertFalse("El email sin extensión (.com, .net) debería ser rechazado", resultado)
    }
    
    @Test
    fun `email vacío debe retornar false`() {
        val emailVacio = ""
        val resultado = ValidationUtils.isValidEmail(emailVacio)
        assertFalse("El email vacío debería ser rechazado", resultado)
    }
    
    @Test
    fun `email solo con espacios debe retornar false`() {
        val emailEspacios = "   "
        val resultado = ValidationUtils.isValidEmail(emailEspacios)
        assertFalse("El email con solo espacios debería ser rechazado", resultado)
    }
    
    @Test
    fun `email con espacios alrededor debe ser aceptado si es válido`() {
        // La función debe hacer trim() internamente
        val emailConEspacios = "  usuario@example.com  "
        val resultado = ValidationUtils.isValidEmail(emailConEspacios)
        assertTrue("El email válido con espacios alrededor debería ser aceptado", resultado)
    }
    
    @Test
    fun `múltiples emails válidos con diferentes dominios`() {
        val emailsValidos = listOf(
            "test@gmail.com",
            "user.name@company.co",
            "admin@test.org",
            "contact+filter@domain.net"
        )
        
        emailsValidos.forEach { email ->
            assertTrue("El email $email debería ser válido", ValidationUtils.isValidEmail(email))
        }
    }

    // ========== PRUEBAS DE VALIDACIÓN DE CONTRASEÑA ==========
    
    @Test
    fun `contraseña con longitud mínima debe retornar true`() {
        // Longitud mínima es 8 caracteres
        val passwordValida = "12345678"
        val resultado = ValidationUtils.isValidPassword(passwordValida)
        assertTrue("La contraseña con longitud mínima debería ser aceptada", resultado)
    }
    
    @Test
    fun `contraseña mayor a longitud mínima debe retornar true`() {
        val passwordLarga = "contraseñaSegura123"
        val resultado = ValidationUtils.isValidPassword(passwordLarga)
        assertTrue("La contraseña larga debería ser aceptada", resultado)
    }
    
    @Test
    fun `contraseña menor a longitud mínima debe retornar false`() {
        val passwordCorta = "1234567" // 7 caracteres (menos de 8)
        val resultado = ValidationUtils.isValidPassword(passwordCorta)
        assertFalse("La contraseña corta debería ser rechazada", resultado)
    }
    
    @Test
    fun `contraseña vacía debe retornar false`() {
        val passwordVacia = ""
        val resultado = ValidationUtils.isValidPassword(passwordVacia)
        assertFalse("La contraseña vacía debería ser rechazada", resultado)
    }
    
    @Test
    fun `contraseña con un solo carácter debe retornar false`() {
        val passwordMuyCorta = "a"
        val resultado = ValidationUtils.isValidPassword(passwordMuyCorta)
        assertFalse("La contraseña de 1 carácter debería ser rechazada", resultado)
    }

    @Test
    fun `contraseña de exactamente 8 caracteres debe retornar true`() {
        val password = "abcd1234"
        val resultado = ValidationUtils.isValidPassword(password)
        assertTrue("La contraseña de exactamente 8 caracteres debería ser aceptada", resultado)
    }

    // ========== PRUEBAS DE CONSTANTES ==========
    
    @Test
    fun `constante MIN_PASSWORD_LENGTH debe ser 8`() {
        assertEquals("La longitud mínima de contraseña debe ser 8", 
            8, ValidationUtils.MIN_PASSWORD_LENGTH)
    }
    
    @Test
    fun `regex EMAIL_REGEX debe estar definido`() {
        assertNotNull("El regex de email debe estar definido", 
            ValidationUtils.EMAIL_REGEX)
    }

    // ========== PRUEBAS DE CASOS EXTREMOS ==========
    
    @Test
    fun `email con caracteres especiales válidos debe ser aceptado`() {
        val emailEspecial = "user+tag@example.com"
        val resultado = ValidationUtils.isValidEmail(emailEspecial)
        assertTrue("El email con + debería ser aceptado", resultado)
    }
    
    @Test
    fun `email con puntos en nombre de usuario debe ser aceptado`() {
        val emailConPuntos = "first.last@example.com"
        val resultado = ValidationUtils.isValidEmail(emailConPuntos)
        assertTrue("El email con puntos debería ser aceptado", resultado)
    }
    
    @Test
    fun `contraseña con caracteres especiales debe ser aceptada si cumple longitud`() {
        val passwordEspecial = "P@ssw0rd!"
        val resultado = ValidationUtils.isValidPassword(passwordEspecial)
        assertTrue("La contraseña con caracteres especiales debería ser aceptada", resultado)
    }
    
    @Test
    fun `contraseña solo con espacios de longitud válida debe ser aceptada`() {
        // Aunque no sea una buena práctica, si cumple con la longitud, es válida según la regla actual
        val passwordEspacios = "        " // 8 espacios
        val resultado = ValidationUtils.isValidPassword(passwordEspacios)
        assertTrue("La contraseña de 8 espacios cumple con la regla de longitud", resultado)
    }
}
