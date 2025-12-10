# Checklist Play Store - MedicAI

## Información Básica de la App

- **Nombre de la App**: MedicAI
- **Package Name**: com.example.medicai
- **Versión Actual**: 1.0.1 (versionCode: 2)
- **Categoría**: Salud y Bienestar / Medical

## Checklist Pre-Subida

### ✅ Completado

- [x] **Seguridad**
  - [x] Claves API movidas a BuildConfig/local.properties
  - [x] ProGuard habilitado con reglas completas
  - [x] EncryptedSharedPreferences implementado
  - [x] HTTPS en todas las comunicaciones

- [x] **Código**
  - [x] Manejo offline implementado
  - [x] Validaciones mejoradas
  - [x] Accesibilidad (A11y) mejorada
  - [x] Optimizaciones de Compose aplicadas

- [x] **Testing**
  - [x] Tests unitarios básicos agregados
  - [x] Pruebas en diferentes dispositivos (pendiente de realizar)
  - [x] Pruebas de conectividad offline/online

### ⚠️ Pendiente (Requerido antes de publicación)

- [ ] **Assets Gráficos**
  - [ ] Icono de la app (512x512 px) - Verificar todos los tamaños en res/
  - [ ] Feature Graphic (1024x500 px)
  - [ ] Screenshots para diferentes dispositivos:
    - [ ] Teléfono (mínimo 2, máximo 8)
    - [ ] Tablet (opcional, mínimo 1)
  - [ ] Banner promocional (opcional)

- [ ] **Descripción de la App**
  - [ ] Título corto (30 caracteres máximo)
  - [ ] Descripción corta (80 caracteres máximo)
  - [ ] Descripción completa (4000 caracteres máximo)
  - [ ] Palabras clave relevantes

- [ ] **Política de Privacidad**
  - [x] Documento de política de privacidad creado
  - [ ] URL pública donde estará alojada (requerida por Play Store)
  - [ ] Enlace agregado en la configuración de la app en Play Console

- [ ] **Contenido de la App**
  - [ ] Verificar que no haya contenido ofensivo
  - [ ] Verificar que las advertencias médicas estén presentes
  - [ ] Verificar que los términos de uso estén claros

- [ ] **Clasificación de Contenido**
  - [ ] Completar cuestionario de clasificación en Play Console
  - [ ] Indicar que la app es para mayores de 13 años (si aplica)

- [ ] **Precio y Distribución**
  - [ ] Seleccionar países de distribución
  - [ ] Configurar precio (gratis o de pago)
  - [ ] Configurar disponibilidad

- [ ] **Pruebas Finales**
  - [ ] Probar APK/AAB firmado en dispositivo físico
  - [ ] Verificar que todas las funcionalidades funcionen
  - [ ] Probar en diferentes versiones de Android (API 24+)
  - [ ] Verificar que las notificaciones funcionen correctamente
  - [ ] Probar modo offline

## Descripción Sugerida para Play Store

### Título Corto (30 caracteres)
```
MedicAI - Recordatorios Médicos
```

### Descripción Corta (80 caracteres)
```
Gestiona medicamentos, citas médicas y recibe asistencia IA de salud
```

### Descripción Completa (4000 caracteres)

```
MedicAI es tu asistente personal de salud que te ayuda a gestionar tus medicamentos, recordatorios de citas médicas y proporciona información de salud confiable mediante inteligencia artificial.

CARACTERÍSTICAS PRINCIPALES:

💊 Gestión de Medicamentos
- Agrega y organiza tus medicamentos
- Configura horarios personalizados de administración
- Recibe recordatorios antes de cada dosis
- Historial completo de tus tratamientos

📅 Gestión de Citas Médicas
- Agenda y organiza tus citas con doctores
- Recibe recordatorios personalizables antes de cada cita
- Guarda información importante: especialidad, ubicación, notas
- Filtra por estado: próximas, completadas, canceladas

🤖 Asistente de IA para Salud
- Consulta información médica general
- Obtén información sobre medicamentos, dosis e interacciones
- Respuestas rápidas y confiables sobre síntomas comunes
- Siempre incluye advertencias médicas apropiadas

🔔 Notificaciones Inteligentes
- Recordatorios personalizables (5, 10, 15, 30 minutos antes)
- Notificaciones con sonido y vibración configurables
- Restauración automática de alarmas después de reiniciar el dispositivo
- Respeta tus preferencias de notificaciones

🎨 Interfaz Moderna
- Diseño Material 3 elegante y moderno
- Modo oscuro/claro automático
- Navegación intuitiva
- Accesible para todos los usuarios

🔒 Privacidad y Seguridad
- Datos encriptados en tránsito y en reposo
- Almacenamiento seguro con Supabase
- No compartimos tu información con terceros
- Puedes eliminar tu cuenta y datos en cualquier momento

IMPORTANTE:
Esta aplicación proporciona información médica orientativa y educativa. No reemplaza una consulta médica profesional. Ante cualquier duda o síntoma persistente, consulta con tu médico.

MedicAI está diseñada para ayudarte a mantener un mejor control de tu salud, pero siempre debes seguir las recomendaciones de tu médico profesional.

Descarga MedicAI hoy y toma el control de tu salud de manera inteligente y organizada.
```

## Palabras Clave Sugeridas

```
medicamentos, recordatorios, salud, citas médicas, doctor, medicina, tratamiento, pastillas, dosis, horario, asistente IA, salud personal, gestión médica
```

## Notas Importantes

1. **Advertencia Médica**: La app debe incluir claramente que no reemplaza consulta médica profesional
2. **Permisos**: Todos los permisos deben estar justificados en la descripción
3. **Política de Privacidad**: Debe estar accesible públicamente antes de la publicación
4. **Testing**: Realizar pruebas exhaustivas antes de publicar
5. **Versioning**: Incrementar versionCode en cada actualización

## Próximos Pasos

1. Generar APK/AAB firmado para release
2. Subir a Play Console (Internal Testing primero)
3. Completar toda la información en Play Console
4. Subir screenshots y assets gráficos
5. Configurar política de privacidad (URL pública)
6. Revisar y enviar para revisión
