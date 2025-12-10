# Política de Privacidad - MedicAI

**Última actualización:** Enero 2025

## 1. Información que Recopilamos

MedicAI recopila la siguiente información para proporcionar sus servicios:

### Información Personal
- **Nombre completo**: Para personalizar la experiencia del usuario
- **Correo electrónico**: Requerido para autenticación y comunicación
- **Teléfono** (opcional): Para recordatorios y contacto
- **Avatar/Foto de perfil** (opcional): Almacenada en Supabase Storage

### Información de Salud
- **Medicamentos**: Nombre, dosis, horarios de administración
- **Citas médicas**: Información sobre citas con doctores
- **Preferencias de notificaciones**: Configuración de recordatorios

### Información Técnica
- **ID de usuario**: Identificador único generado por Supabase
- **Preferencias de la aplicación**: Configuración de notificaciones y recordatorios

## 2. Cómo Usamos la Información

Utilizamos la información recopilada para:
- Proporcionar recordatorios de medicamentos y citas médicas
- Personalizar la experiencia del usuario
- Mejorar nuestros servicios
- Enviar notificaciones importantes sobre la aplicación

## 3. Almacenamiento de Datos

### Servicios de Terceros
- **Supabase**: Utilizamos Supabase para almacenar datos de forma segura. Los datos están encriptados en tránsito (HTTPS) y en reposo.
- **Groq API**: Utilizamos Groq API para el asistente de IA. Los mensajes se procesan pero no se almacenan permanentemente.

### Seguridad
- Utilizamos **EncryptedSharedPreferences** para almacenar datos sensibles localmente
- Todas las comunicaciones utilizan HTTPS
- Las claves API están protegidas y no se exponen en el código

## 4. Permisos de la Aplicación

MedicAI solicita los siguientes permisos:

- **INTERNET**: Para sincronizar datos con Supabase
- **POST_NOTIFICATIONS**: Para enviar recordatorios de medicamentos y citas
- **SCHEDULE_EXACT_ALARM**: Para programar alarmas precisas de recordatorios
- **RECEIVE_BOOT_COMPLETED**: Para restaurar alarmas después de reiniciar el dispositivo
- **ACCESS_FINE_LOCATION / ACCESS_COARSE_LOCATION**: Para la funcionalidad de geolocalización (opcional)
- **CAMERA / READ_MEDIA_IMAGES**: Para seleccionar foto de perfil (opcional)

## 5. Compartir Información

**No compartimos, vendemos ni alquilamos su información personal a terceros**, excepto:
- Cuando sea requerido por ley
- Para proteger nuestros derechos legales
- Con su consentimiento explícito

## 6. Sus Derechos

Usted tiene derecho a:
- Acceder a sus datos personales
- Corregir información incorrecta
- Eliminar su cuenta y todos sus datos
- Exportar sus datos
- Retirar su consentimiento en cualquier momento

Para ejercer estos derechos, contacte a través de la aplicación o elimine su cuenta desde la sección de Perfil.

## 7. Retención de Datos

- Conservamos sus datos mientras su cuenta esté activa
- Puede eliminar su cuenta en cualquier momento desde la aplicación
- Al eliminar la cuenta, todos los datos se eliminan permanentemente

## 8. Menores de Edad

MedicAI no está dirigida a menores de 13 años. No recopilamos intencionalmente información de menores.

## 9. Cambios a esta Política

Nos reservamos el derecho de actualizar esta política de privacidad. Le notificaremos de cambios significativos a través de la aplicación.

## 10. Contacto

Para preguntas sobre esta política de privacidad, puede contactarnos a través de:
- La sección de soporte en la aplicación
- Email: [agregar email de contacto]

---

**Nota importante**: Esta aplicación proporciona información médica orientativa y educativa. No reemplaza la consulta con un profesional de la salud. Siempre consulte con su médico antes de tomar decisiones sobre su salud.
