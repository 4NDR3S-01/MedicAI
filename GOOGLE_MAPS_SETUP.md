# Configuración de Google Maps para MedicAI

## 📋 Requisitos Previos

Para usar Google Maps en tu aplicación, necesitas obtener una API Key de Google Cloud.

## 🔑 Paso 1: Obtener tu API Key

1. Ve a [Google Cloud Console](https://console.cloud.google.com/)
2. Crea un nuevo proyecto o selecciona uno existente
3. Habilita las siguientes APIs:
   - **Maps SDK for Android**
   - **Places API** (opcional, para búsqueda de lugares)
   - **Geocoding API** (opcional, para convertir coordenadas en direcciones)

4. Ve a **Credenciales** en el menú lateral
5. Haz clic en **Crear credenciales** → **Clave de API**
6. Copia la clave generada

### Restricciones de seguridad (Recomendado)

Para mayor seguridad, restringe tu API Key:

1. En la configuración de tu API Key, ve a **Restricciones de la aplicación**
2. Selecciona **Aplicaciones de Android**
3. Agrega el nombre del paquete: `com.example.medicai`
4. Agrega la huella digital SHA-1 de tu certificado:

```bash
# Para certificado de debug
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android

# Para certificado de release
keytool -list -v -keystore /path/to/your/keystore -alias your-alias-name
```

## ⚙️ Paso 2: Configurar la API Key en tu Proyecto

### Opción A: Directamente en AndroidManifest.xml (Menos seguro)

Abre el archivo `app/src/main/AndroidManifest.xml` y reemplaza:

```xml
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="YOUR_GOOGLE_MAPS_API_KEY_HERE" />
```

Por:

```xml
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="TU_API_KEY_AQUI" />
```

### Opción B: Usando local.properties (Más seguro - Recomendado)

1. Abre el archivo `local.properties` en la raíz del proyecto
2. Agrega tu API Key:

```properties
MAPS_API_KEY=TU_API_KEY_AQUI
```

3. Modifica `app/build.gradle.kts` para leer la clave:

```kotlin
android {
    ...
    
    defaultConfig {
        ...
        
        // Leer API Key desde local.properties
        val localProperties = File(rootProject.projectDir, "local.properties")
        if (localProperties.exists()) {
            val properties = java.util.Properties()
            properties.load(localProperties.inputStream())
            val mapsApiKey = properties.getProperty("MAPS_API_KEY") ?: ""
            manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
        }
    }
}
```

4. Actualiza `AndroidManifest.xml`:

```xml
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="${MAPS_API_KEY}" />
```

## 📱 Paso 3: Permisos

Los permisos ya están configurados en `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

### Solicitar permisos en tiempo de ejecución

Necesitas solicitar permisos de ubicación en tiempo de ejecución (Android 6.0+). Esto se puede hacer en `MainActivity.kt`:

```kotlin
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Solicitar permisos de ubicación
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
        
        // ... resto del código
    }
    
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
}
```

## 🔄 Paso 4: Sincronizar el proyecto

1. En Android Studio, haz clic en **File** → **Sync Project with Gradle Files**
2. Espera a que se descarguen las dependencias
3. Compila y ejecuta la aplicación

## ✅ Verificación

Para verificar que Google Maps funciona correctamente:

1. Ejecuta la aplicación
2. Ve a la pantalla de **Citas**
3. Toca **Agregar nueva cita** o edita una existente
4. Toca el selector de ubicación
5. Deberías ver el mapa de Google con capacidad de:
   - Hacer zoom
   - Tocar en el mapa para seleccionar una ubicación
   - Ver las coordenadas de la ubicación seleccionada
   - Usar ubicaciones rápidas predefinidas

## 🐛 Solución de Problemas

### El mapa aparece en gris o no se carga

- Verifica que tu API Key esté correctamente configurada
- Asegúrate de haber habilitado **Maps SDK for Android**
- Revisa que las restricciones de la API Key permitan tu paquete y SHA-1
- Verifica los logs de Android Studio para ver errores específicos

### Error de permisos

- Asegúrate de aceptar los permisos de ubicación cuando la app los solicite
- Verifica que los permisos estén en AndroidManifest.xml

### La app no compila

- Sincroniza el proyecto con Gradle
- Limpia y reconstruye: **Build** → **Clean Project** → **Rebuild Project**
- Asegúrate de que todas las dependencias se hayan descargado correctamente

## 📚 Documentación Adicional

- [Google Maps Platform](https://developers.google.com/maps)
- [Maps Compose Documentation](https://github.com/googlemaps/android-maps-compose)
- [Maps SDK for Android](https://developers.google.com/maps/documentation/android-sdk)

## 💡 Características Implementadas

✅ Mapa interactivo de Google Maps
✅ Selector de ubicación mediante toque en el mapa
✅ Visualización de coordenadas (latitud/longitud)
✅ Ubicaciones rápidas predefinidas
✅ Barra de búsqueda (preparada para integrar Places API)
✅ Marcador visual en la ubicación seleccionada
✅ Controles de zoom y brújula
✅ Diseño responsive con Material 3

## 🚀 Próximas Mejoras Sugeridas

- [ ] Integrar **Places API** para autocompletar direcciones
- [ ] Geocodificación inversa (convertir coordenadas en direcciones)
- [ ] Mostrar ubicación actual del usuario
- [ ] Guardar lugares favoritos
- [ ] Historial de ubicaciones usadas
