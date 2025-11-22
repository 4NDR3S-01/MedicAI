package com.example.medicai.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import com.example.medicai.data.local.NotificationPreferencesDataStore
import com.example.medicai.data.local.UserPreferencesManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.example.medicai.viewmodel.AuthViewModel
import com.example.medicai.data.models.PredefinedAvatars
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * Pantalla Moderna de Perfil/Configuración
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUser by authViewModel.currentUser.collectAsState()

    var showEditDialog by rememberSaveable { mutableStateOf(false) }
    var showLogoutDialog by rememberSaveable { mutableStateOf(false) }
    var showNotificationSettings by rememberSaveable { mutableStateOf(false) }
    var showAvatarPicker by rememberSaveable { mutableStateOf(false) }
    var showPrivacyDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteAccountDialog by rememberSaveable { mutableStateOf(false) }
    var showHelpDialog by rememberSaveable { mutableStateOf(false) }
    var showChangePasswordDialog by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Header con gradiente y avatar
            item {
                ProfileHeader(
                    userName = currentUser?.full_name ?: "Usuario",
                    userEmail = currentUser?.email ?: "",
                    userAvatar = currentUser?.avatar_url,
                    onEditProfile = { showEditDialog = true },
                    onEditAvatar = { showAvatarPicker = true }
                )
            }

            // Sección de Cuenta
            item {
                Spacer(modifier = Modifier.height(24.dp))
                SectionTitle(title = "Mi Cuenta")
            }

            item {
                ProfileMenuItem(
                    icon = Icons.Filled.Person,
                    title = "Información Personal",
                    subtitle = "Actualizar nombre, email, teléfono",
                    onClick = { showEditDialog = true }
                )
            }

            item {
                ProfileMenuItem(
                    icon = Icons.Filled.Lock,
                    title = "Cambiar Contraseña",
                    subtitle = "Enviar email para cambiar contraseña",
                    onClick = { showChangePasswordDialog = true }
                )
            }

            // Sección de Notificaciones
            item {
                Spacer(modifier = Modifier.height(24.dp))
                SectionTitle(title = "Notificaciones")
            }

            item {
                ProfileMenuItem(
                    icon = Icons.Filled.Notifications,
                    title = "Configurar Notificaciones",
                    subtitle = "Recordatorios de medicamentos y citas",
                    onClick = { showNotificationSettings = true }
                )
            }

            // Sección de Datos
            item {
                Spacer(modifier = Modifier.height(24.dp))
                SectionTitle(title = "Datos y Privacidad")
            }

            item {
                ProfileMenuItem(
                    icon = Icons.Filled.Security,
                    title = "Privacidad",
                    subtitle = "Gestionar tus datos personales",
                    onClick = {
                        showPrivacyDialog = true
                    }
                )
            }

            // (Exportar Datos eliminado por decisión del equipo)

            // Sección de Ayuda
            item {
                Spacer(modifier = Modifier.height(24.dp))
                SectionTitle(title = "Ayuda y Soporte")
            }

            item {
                ProfileMenuItem(
                    icon = Icons.Filled.Help,
                    title = "Centro de Ayuda",
                    subtitle = "Preguntas frecuentes y tutoriales",
                    onClick = { showHelpDialog = true }
                )
            }

            item {
                ProfileMenuItem(
                    icon = Icons.Filled.Info,
                    title = "Acerca de MedicAI",
                    subtitle = "Versión 1.0.0",
                    onClick = {
                        Toast.makeText(context, "MedicAI v1.0.0 - Tu asistente médico", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // Botón de Cerrar Sesión
            item {
                Spacer(modifier = Modifier.height(16.dp))
                LogoutButton(
                    onClick = { showLogoutDialog = true }
                )
            }
        }
    }

    // Diálogos
    if (showEditDialog) {
        EditProfileDialog(
            currentUser = currentUser,
            onDismiss = { showEditDialog = false },
            onSave = { name, phone ->
                authViewModel.updateProfile(name, phone,
                    onSuccess = {
                        Toast.makeText(context, "✅ Perfil actualizado", Toast.LENGTH_SHORT).show()
                        showEditDialog = false
                    },
                    onError = { error ->
                        Toast.makeText(context, "❌ Error: $error", Toast.LENGTH_LONG).show()
                    }
                )
            }
        )
    }

    if (showLogoutDialog) {
        LogoutConfirmDialog(
            onDismiss = { showLogoutDialog = false },
            onConfirm = {
                showLogoutDialog = false
                authViewModel.logout()
                onLogout()
            }
        )
    }

    if (showNotificationSettings) {
        NotificationSettingsDialog(
            authViewModel = authViewModel,
            onDismiss = { showNotificationSettings = false }
        )
    }

    if (showAvatarPicker) {
        AvatarPickerDialog(
            currentAvatar = currentUser?.avatar_url,
            onDismiss = { showAvatarPicker = false },
            onAvatarSelected = { avatarUrl ->
                // Guardar el avatar en Supabase
                authViewModel.updateAvatar(
                    avatarUrl = avatarUrl,
                    onSuccess = {
                        Toast.makeText(context, "✅ Avatar actualizado", Toast.LENGTH_SHORT).show()
                        showAvatarPicker = false
                    },
                    onError = { error ->
                        Toast.makeText(context, "❌ Error: $error", Toast.LENGTH_LONG).show()
                    }
                )
            },
            onImageSelected = { imageUri ->
                // Subir imagen a Supabase Storage
                authViewModel.uploadAvatarImage(
                    context = context,
                    imageUri = imageUri,
                    onSuccess = {
                        Toast.makeText(context, "✅ Foto de avatar subida", Toast.LENGTH_SHORT).show()
                        showAvatarPicker = false
                    },
                    onError = { error ->
                        Toast.makeText(context, "❌ Error: $error", Toast.LENGTH_LONG).show()
                    }
                )
            }
        )
    }

    if (showPrivacyDialog) {
        PrivacyDialog(
            onDismiss = { showPrivacyDialog = false },
            onRequestDelete = { showDeleteAccountDialog = true }
        )
    }

    if (showDeleteAccountDialog) {
        DeleteAccountConfirmDialog(
            onDismiss = { showDeleteAccountDialog = false },
            onConfirm = {
                // Ejecutar eliminación vía ViewModel
                authViewModel.deleteAccount(
                    onSuccess = {
                        Toast.makeText(context, "Cuenta eliminada", Toast.LENGTH_SHORT).show()
                        showDeleteAccountDialog = false
                        // Enviar al flujo de logout / pantalla de login
                        onLogout()
                    },
                    onError = { error ->
                        Toast.makeText(context, "Error: $error", Toast.LENGTH_LONG).show()
                    }
                )
            }
        )
    }

    if (showHelpDialog) {
        HelpCenterDialog(
            onDismiss = { showHelpDialog = false }
        )
    }

    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            email = currentUser?.email ?: "",
            onDismiss = { showChangePasswordDialog = false },
            onSendReset = { emailToSend ->
                authViewModel.sendPasswordResetEmail(emailToSend,
                    onSuccess = {
                        Toast.makeText(context, "✅ Email de recuperación enviado", Toast.LENGTH_SHORT).show()
                        showChangePasswordDialog = false
                    },
                    onError = { err ->
                        Toast.makeText(context, "❌ Error: $err", Toast.LENGTH_LONG).show()
                    }
                )
            }
        )
    }
}

@Composable
private fun ProfileHeader(
    userName: String,
    userEmail: String,
    userAvatar: String?,
    onEditProfile: () -> Unit,
    onEditAvatar: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        com.example.medicai.ui.theme.GradientStart,
                        com.example.medicai.ui.theme.GradientEnd
                    )
                ),
                shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Avatar clickable con badge de editar fuera del círculo
            Box(
                modifier = Modifier.size(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    onClick = onEditAvatar,
                    modifier = Modifier.fillMaxSize(),
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 4.dp
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        if (userAvatar != null && PredefinedAvatars.isEmojiAvatar(userAvatar)) {
                            // Mostrar emoji
                            Text(
                                text = userAvatar,
                                style = MaterialTheme.typography.displayLarge,
                                fontSize = 48.sp
                            )
                        } else if (userAvatar != null && userAvatar.startsWith("http")) {
                            // Mostrar imagen desde URL usando Coil
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(userAvatar)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Avatar del usuario",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            // Mostrar icono por defecto
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Badge de editar superpuesto al borde del círculo y clickable
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-6).dp, y = (-6).dp)
                ) {
                    Surface(
                        onClick = onEditAvatar,
                        modifier = Modifier.size(28.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.tertiary,
                        shadowElevation = 2.dp
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Editar avatar",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onTertiary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Nombre
            Text(
                text = userName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Email
            Text(
                text = userEmail,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Botón editar
            OutlinedButton(
                onClick = onEditProfile,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Editar Perfil")
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
    )
}

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LogoutButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.error
        )
    ) {
        Icon(
            imageVector = Icons.Filled.Logout,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "Cerrar Sesión",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileDialog(
    currentUser: com.example.medicai.data.models.UserProfile?,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by rememberSaveable { mutableStateOf(currentUser?.full_name ?: "") }
    var phone by rememberSaveable { mutableStateOf(currentUser?.phone ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Editar Perfil",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre completo") },
                    leadingIcon = {
                        Icon(Icons.Filled.Person, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Teléfono") },
                    leadingIcon = {
                        Icon(Icons.Filled.Phone, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, phone) },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancelar")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun LogoutConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        },
        title = {
            Text(
                "¿Cerrar Sesión?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                "¿Estás seguro de que deseas cerrar sesión?\n\nTus datos estarán seguros y podrás volver a iniciar sesión en cualquier momento.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Sí, cerrar sesión")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancelar")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun NotificationSettingsDialog(
    authViewModel: com.example.medicai.viewmodel.AuthViewModel,
    onDismiss: () -> Unit
) {
    val currentUser by authViewModel.currentUser.collectAsState()

    // Inicializar estados desde el perfil actual (o valores por defecto)
    var notificationsEnabled by rememberSaveable { mutableStateOf(currentUser?.notifications_enabled ?: true) }
    var reminderMinutes by rememberSaveable { mutableStateOf((currentUser?.reminder_minutes ?: 15).toFloat()) }

    // Preferencias locales para sonido y vibración (DataStore)
    val ctx = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val soundEnabled by NotificationPreferencesDataStore.soundEnabledFlow(ctx).collectAsState(initial = true)
    val vibrationEnabled by NotificationPreferencesDataStore.vibrationEnabledFlow(ctx).collectAsState(initial = true)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Notificaciones",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SettingsToggle(
                    title = "Activar notificaciones",
                    checked = notificationsEnabled,
                    onCheckedChange = { notificationsEnabled = it }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Text(
                    text = "Recordatorio anticipado: ${reminderMinutes.toInt()} min",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Slider(
                    value = reminderMinutes,
                    onValueChange = { reminderMinutes = it },
                    valueRange = 5f..60f,
                    steps = 11,
                    enabled = notificationsEnabled,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                Text(
                    text = "Otras opciones",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Toggles persistidos localmente en DataStore y UserPreferencesManager
                SettingsToggle(
                    title = "Sonido",
                    checked = soundEnabled,
                    onCheckedChange = { enabled ->
                        coroutineScope.launch {
                            // Guardar en DataStore
                            NotificationPreferencesDataStore.setSoundEnabled(ctx, enabled)
                            // ✅ También guardar en UserPreferencesManager para acceso rápido
                            UserPreferencesManager.saveSoundEnabled(ctx, enabled)
                        }
                    }
                )

                SettingsToggle(
                    title = "Vibración",
                    checked = vibrationEnabled,
                    onCheckedChange = { enabled ->
                        coroutineScope.launch {
                            // Guardar en DataStore
                            NotificationPreferencesDataStore.setVibrationEnabled(ctx, enabled)
                            // ✅ También guardar en UserPreferencesManager para acceso rápido
                            UserPreferencesManager.saveVibrationEnabled(ctx, enabled)
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Guardar en backend a través del ViewModel
                    authViewModel.updatePreferences(
                        notificationsEnabled = notificationsEnabled,
                        reminderMinutes = reminderMinutes.toInt(),
                        onSuccess = {
                            onDismiss()
                        },
                        onError = { err ->
                            // Mostrar error breve (puedes mejorar con Snackbar)
                        }
                    )
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
                Text("Cancelar")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun SettingsToggle(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun AvatarPickerDialog(
    currentAvatar: String?,
    onDismiss: () -> Unit,
    onAvatarSelected: (String) -> Unit,
    onImageSelected: (android.net.Uri) -> Unit
) {
    val context = LocalContext.current
    var selectedAvatar by remember { mutableStateOf(currentAvatar ?: "") }
    var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var selectedTab by remember { mutableStateOf(0) }

    // Launcher para seleccionar imagen de galería
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            selectedImageUri = it
            selectedAvatar = "" // Limpiar emoji si se selecciona foto
        }
    }

    // Launcher para solicitar permiso de almacenamiento/galería
    val galleryPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permiso concedido, abrir galería
            galleryLauncher.launch("image/*")
        } else {
            // Permiso denegado
            android.widget.Toast.makeText(
                context,
                "Se necesita permiso de almacenamiento para seleccionar fotos",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    // Launcher para tomar foto con la cámara
    var tempPhotoUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempPhotoUri != null) {
            selectedImageUri = tempPhotoUri
            selectedAvatar = "" // Limpiar emoji si se toma foto
        }
    }

    // Launcher para solicitar permiso de cámara
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permiso concedido, abrir cámara
            val photoUri = createImageFileUri(context)
            tempPhotoUri = photoUri
            photoUri?.let { cameraLauncher.launch(it) }
        } else {
            // Permiso denegado
            android.widget.Toast.makeText(
                context,
                "Se necesita permiso de cámara para tomar fotos",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.8f),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Face,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Cambiar Avatar",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Tabs para seleccionar tipo de avatar
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("😊 Emojis") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("📷 Foto") }
                    )
                }

                when (selectedTab) {
                    0 -> {
                        // Grid de emojis
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Selecciona un avatar divertido:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            LazyVerticalGrid(
                                columns = GridCells.Fixed(5),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(PredefinedAvatars.avatars) { avatar ->
                                    AvatarItem(
                                        emoji = avatar,
                                        isSelected = selectedAvatar == avatar,
                                        onClick = { selectedAvatar = avatar }
                                    )
                                }
                            }
                        }
                    }
                    1 -> {
                        // Opciones de foto
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Elige cómo quieres subir tu foto:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Botón para galería
                            ElevatedCard(
                                onClick = {
                                    // En Android 13+ (API 33+) no se necesita permiso READ_EXTERNAL_STORAGE
                                    // Se usa READ_MEDIA_IMAGES que ya está en el manifest
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                        // Android 13+: solicitar READ_MEDIA_IMAGES
                                        galleryPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                                    } else {
                                        // Android 12 y anteriores: solicitar READ_EXTERNAL_STORAGE
                                        galleryPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PhotoLibrary,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Seleccionar de Galería",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            // Botón para cámara
                            ElevatedCard(
                                onClick = { 
                                    // Verificar y solicitar permiso de cámara
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CameraAlt,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Tomar Foto",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }

                            if (selectedImageUri != null) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.tertiary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Foto seleccionada. Lista para subir.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        selectedImageUri != null -> {
                            // Subir imagen a Supabase
                            onImageSelected(selectedImageUri!!)
                        }
                        selectedAvatar.isNotEmpty() -> {
                            // Guardar emoji
                            onAvatarSelected(selectedAvatar)
                        }
                    }
                },
                enabled = selectedAvatar.isNotEmpty() || selectedImageUri != null,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Guardar")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancelar")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

// Función auxiliar para crear URI de archivo temporal para la cámara
private fun createImageFileUri(context: android.content.Context): android.net.Uri? {
    return try {
        val timeStamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
        val imageFile = java.io.File.createTempFile(imageFileName, ".jpg", storageDir)
        androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
    } catch (e: Exception) {
        null
    }
}

@Composable
private fun AvatarItem(
    emoji: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .size(60.dp)
            .clip(CircleShape),
        shape = CircleShape,
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = if (isSelected) {
            BorderStroke(
                3.dp,
                MaterialTheme.colorScheme.primary
            )
        } else null,
        shadowElevation = if (isSelected) 4.dp else 1.dp
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = emoji,
                style = MaterialTheme.typography.headlineMedium,
                fontSize = 32.sp
            )
        }
    }
}

@Composable
private fun DeleteAccountConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        },
        title = {
            Text(
                "Eliminar cuenta",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                "Esta acción eliminará permanentemente tu perfil y datos asociados. Esta operación no se puede deshacer.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Eliminar", color = MaterialTheme.colorScheme.onError)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
                Text("Cancelar")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun PrivacyDialog(
    onDismiss: () -> Unit,
    onRequestDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Privacidad y Datos",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "En MedicAI protegemos tu información personal.\n\n" +
                            "- Solo almacenamos los datos necesarios para ofrecer el servicio (nombre, email, teléfono, avatar).\n" +
                            "- Tus datos no se comparten con terceros sin tu consentimiento.\n\n" +
                            "Si quieres la política completa, contáctanos en soporte@medicai.com",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Acciones disponibles:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "• Eliminar cuenta: elimina tu perfil y credenciales.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onRequestDelete() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Eliminar cuenta", color = MaterialTheme.colorScheme.onError)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
                Text("Cerrar")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun HelpCenterDialog(
    onDismiss: () -> Unit
) {
    val faqs = listOf(
        "¿Cómo añado medicamentos?" to "Ve a la pestaña de Medicamentos y presiona el botón + para añadir un nuevo medicamento. Completa nombre, dosis y horarios.",
        "¿Cómo configuro recordatorios?" to "Dentro del detalle del medicamento puedes activar recordatorios. También puedes configurarlos desde Notificaciones en tu perfil.",
        "¿Cómo funciona el Asistente IA?" to "El Asistente IA utiliza un modelo para ofrecer sugerencias médicas generales y ayudarte a entender síntomas. No sustituye al médico.",
        "¿Qué datos se almacenan?" to "Guardamos nombre, email, teléfono, avatar y preferencias de notificación. Más detalles en Privacidad."
    )

    // estado expandido por pregunta
    val expanded = remember { mutableStateListOf<Boolean>().apply { repeat(faqs.size) { add(false) } } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Help,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Centro de Ayuda",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Encuentra respuestas rápidas a preguntas frecuentes y guías sobre cómo usar MedicAI.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // FAQs como items expandibles
                faqs.forEachIndexed { index, pair ->
                    val (q, a) = pair
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expanded[index] = !expanded[index] }
                            .animateContentSize(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = q,
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = if (expanded[index]) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            AnimatedVisibility(visible = expanded[index]) {
                                Text(
                                    text = a,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Si necesitas más ayuda, escríbenos a soporte@medicai.com",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
                Text("Cerrar")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun ChangePasswordDialog(
    email: String,
    onDismiss: () -> Unit,
    onSendReset: (String) -> Unit
) {
    var inputEmail by rememberSaveable { mutableStateOf(email) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Cambiar contraseña",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Te enviaremos un correo con instrucciones para cambiar tu contraseña.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = inputEmail,
                    onValueChange = { inputEmail = it },
                    label = { Text("Email") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSendReset(inputEmail) }, shape = RoundedCornerShape(12.dp)) {
                Text("Enviar correo")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
                Text("Cancelar")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

