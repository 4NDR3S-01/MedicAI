# MedicAI 💊

Aplicación móvil de Android para gestión de medicamentos y asistente médico con IA.

## 🚀 Características

- ✅ **Gestión de Medicamentos**: Agrega, edita y organiza tus medicamentos
- ⏰ **Recordatorios Inteligentes**: Alarmas precisas para tomar medicamentos
- 📅 **Citas Médicas**: Organiza tus citas con recordatorios
- 🤖 **Asistente IA**: Chat con inteligencia artificial médica (Groq Llama 3.3)
- 👤 **Perfiles Personalizados**: Avatares con emojis o fotos personalizadas
- 🔐 **Autenticación Segura**: Sistema de login con Supabase
- 🌙 **Tema Moderno**: Interfaz Material Design 3

## 📋 Requisitos Previos

- Android Studio Hedgehog (2023.1.1) o superior
- JDK 11 o superior
- Android SDK 36 (compileSdk)
- Dispositivo Android con API 24+ (Android 7.0+)

## 🔧 Configuración

### 1. Clonar el Repositorio

```bash
git clone https://github.com/4NDR3S-01/MedicAI.git
cd MedicAI
```

### 2. Configurar API Keys

Crea o edita el archivo `local.properties` en la raíz del proyecto y agrega:

```properties
# SDK Location
sdk.dir=/ruta/a/tu/Android/Sdk

# API Keys (NO SUBIR A GIT)
GROQ_API_KEY=tu_groq_api_key_aqui
```

#### Obtener Groq API Key:

1. Visita [Groq Console](https://console.groq.com/keys)
2. Crea una cuenta gratis
3. Genera una nueva API key
4. Copia la key y pégala en `local.properties`

### 3. Configurar Supabase

El proyecto ya incluye la configuración de Supabase. Si deseas usar tu propia instancia:

1. Crea un proyecto en [Supabase](https://supabase.com)
2. Ejecuta las migraciones SQL (disponibles en `/database/migrations/`)
3. Actualiza las credenciales en `SupabaseClient.kt`:

```kotlin
private const val SUPABASE_URL = "tu_supabase_url"
private const val SUPABASE_KEY = "tu_supabase_anon_key"
```

### 4. Crear Bucket de Avatares en Supabase

1. Ve a Storage en tu proyecto de Supabase
2. Crea un nuevo bucket llamado `avatars`
3. Marca como "Public bucket"
4. Configura las políticas RLS (ya están incluidas en el código)

### 5. Compilar y Ejecutar

```bash
# Desde Android Studio: Run > Run 'app'
# O desde terminal:
./gradlew assembleDebug
```

## 📱 Permisos Requeridos

La app solicitará los siguientes permisos:

- ✅ **Notificaciones** (Android 13+): Para recordatorios de medicamentos
- ✅ **Alarmas Exactas** (Android 12+): Para programar recordatorios precisos
- ✅ **Cámara**: Para tomar fotos de avatar
- ✅ **Almacenamiento**: Para seleccionar fotos de galería
- ✅ **Ubicación**: Para usar Google Maps en selector de ubicación de citas

## 🏗️ Arquitectura

```
app/
├── data/
│   ├── local/          # DataStore, preferencias locales
│   ├── models/         # Modelos de datos (Medicine, Appointment, etc.)
│   ├── remote/         # Clientes API (Supabase, Groq)
│   └── repository/     # Repositorios (patrón Repository)
├── notifications/      # Sistema de notificaciones y alarmas
├── screens/            # Pantallas Compose (Home, Profile, AI, etc.)
├── ui/                 # Theme, componentes UI compartidos
├── utils/              # Utilidades (AvatarUploadHelper, etc.)
└── viewmodel/          # ViewModels (MVVM)
```

## 🛠️ Tecnologías Utilizadas

- **Jetpack Compose**: UI declarativa moderna
- **Material 3**: Sistema de diseño de Google
- **Supabase**: Backend (Auth, Database, Storage)
- **Groq API**: IA conversacional (Llama 3.3 70B)
- **Ktor Client**: HTTP client para APIs
- **Coil**: Carga de imágenes
- **DataStore**: Almacenamiento de preferencias
- **WorkManager**: Tareas en background
- **AlarmManager**: Alarmas exactas para recordatorios

## 📝 Base de Datos

### Tablas Principales:

- `profiles`: Perfiles de usuario con avatar
- `medicines`: Gestión de medicamentos
- `appointments`: Citas médicas
- Storage: Bucket `avatars` para fotos de perfil

## 🔐 Seguridad

- ✅ API Keys en `local.properties` (no versionadas)
- ✅ Row Level Security (RLS) en Supabase
- ✅ Autenticación JWT con Supabase Auth
- ✅ Políticas de acceso a Storage configuradas

## 🐛 Debugging

Para ver logs en Logcat:

```bash
# Filtrar por tags importantes:
adb logcat -s MedicAI AlarmScheduler NotificationReceiver GroqClient
```

## 📄 Licencia

Este proyecto es de código abierto y está disponible bajo la licencia MIT.

## 👨‍💻 Autor

**Andrés** - [4NDR3S-01](https://github.com/4NDR3S-01)

## 🙏 Agradecimientos

- [Groq](https://groq.com) por su API rápida de IA
- [Supabase](https://supabase.com) por el backend completo
- [Material Design](https://m3.material.io/) por las guías de diseño

---

⚠️ **Nota Importante**: Esta aplicación proporciona información médica general y recordatorios. NO reemplaza la consulta con un profesional de la salud. Siempre consulta con tu médico antes de tomar decisiones sobre tu salud.
