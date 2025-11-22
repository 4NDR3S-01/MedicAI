package com.example.medicai.data.models

/**
 * Avatares predefinidos divertidos
 */
object PredefinedAvatars {

    // Emojis de avatares médicos y de salud
    val avatars = listOf(
        "👨‍⚕️", // Doctor hombre
        "👩‍⚕️", // Doctora mujer
        "🧑‍⚕️", // Doctor persona
        "💊", // Píldora
        "💉", // Jeringa
        "🩺", // Estetoscopio
        "🏥", // Hospital
        "❤️", // Corazón
        "🧠", // Cerebro
        "🦷", // Diente
        "👁️", // Ojo
        "👂", // Oreja
        "🤖", // Robot (IA)
        "🐶", // Perro
        "🐱", // Gato
        "🐼", // Panda
        "🦊", // Zorro
        "🦁", // León
        "🐸", // Rana
        "🦄", // Unicornio
        "🌟", // Estrella
        "🌈", // Arcoíris
        "🎯", // Objetivo
        "💪", // Músculo fuerte
        "🌺", // Flor
        "🍀", // Trébol
        "⚡", // Rayo
        "🔥", // Fuego
        "💎", // Diamante
        "🎨" // Paleta de arte
    )

    fun getRandomAvatar(): String {
        return avatars.random()
    }

    fun isEmojiAvatar(avatar: String): Boolean {
        return avatars.contains(avatar)
    }
}

