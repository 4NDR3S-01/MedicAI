package com.example.medicai.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.widget.Toast
import android.util.Log
import com.example.medicai.viewmodel.AuthViewModel
import com.example.medicai.data.models.PredefinedAvatars
import com.example.medicai.viewmodel.AIViewModel
import com.example.medicai.ui.theme.GradientStartTertiary
import com.example.medicai.ui.theme.GradientEndTertiary
import com.example.medicai.ui.theme.OnGradientLight
import com.example.medicai.ui.theme.UpdateSystemBars
import kotlinx.coroutines.launch
import dev.jeziellago.compose.markdowntext.MarkdownText
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * Pantalla Moderna de Asistente IA con Chat
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIAssistantScreen(
    authViewModel: AuthViewModel,
    aiViewModel: AIViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUser by authViewModel.currentUser.collectAsState()
    val messages by aiViewModel.messages.collectAsState()
    val isLoading by aiViewModel.isLoading.collectAsState()
    val error by aiViewModel.error.collectAsState()

    // Actualizar status bar con color del header (gradiente morado)
    UpdateSystemBars(
        statusBarColor = GradientStartTertiary,
        darkIcons = false
    )

    var userInput by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Mostrar errores
    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, "❌ $it", Toast.LENGTH_LONG).show()
            aiViewModel.clearError()
        }
    }

    // Auto-scroll al último mensaje cuando se agregan nuevos mensajes
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            coroutineScope.launch {
                kotlinx.coroutines.delay(100)
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    // Obtener insets para aplicar padding del status bar y navegación
    val insets = androidx.compose.foundation.layout.WindowInsets
    val systemBars = insets.statusBars.union(insets.navigationBars)
    val bottomBarPadding = insets.navigationBars.asPaddingValues().calculateBottomPadding()
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            // Aplicar todos los insets del sistema para respetar barras nativas en modo horizontal
            .windowInsetsPadding(systemBars)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            ModernAIHeader(
                userName = currentUser?.full_name?.split(" ")?.firstOrNull() ?: "Usuario"
            )

            // Mensajes del chat
            if (messages.isEmpty()) {
                EmptyAIState(modifier = Modifier.weight(1f))
            } else {
                // Calcular padding para que el último mensaje no quede oculto por el campo de entrada
                // Nota: bottomBarPadding ya está aplicado en el Box principal, no se suma aquí
                val navigationBarHeight = 64.dp
                val inputBarHeight = 88.dp
                val totalBottomPadding = inputBarHeight + navigationBarHeight + 24.dp
                
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    state = listState,
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = totalBottomPadding
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(messages) { message ->
                        MessageBubble(message = message, userAvatar = currentUser?.avatar_url)
                    }

                    if (isLoading) {
                        item {
                            TypingIndicator()
                        }
                    }
                }
            }
        }

        // Input bar posicionado absolutamente en la parte inferior
        val imePadding = insets.ime.asPaddingValues().calculateBottomPadding()
        val navigationBarHeight = 64.dp // Altura de la NavigationBar del Scaffold
        val extraPadding = 12.dp
        
        MessageInputBar(
            value = userInput,
            onValueChange = { userInput = it },
            onSend = {
                if (userInput.isNotBlank() && !isLoading) {
                    aiViewModel.sendMessage(userInput)
                    userInput = ""
                }
            },
            isEnabled = !isLoading,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .then(
                    if (imePadding > 0.dp) {
                        // Teclado abierto: usar imePadding que maneja automáticamente el teclado
                        Modifier.imePadding()
                    } else {
                        // Teclado cerrado: usar padding para estar sobre la NavigationBar del Scaffold
                        // No sumar bottomBarPadding aquí porque ya está aplicado en el Box principal
                        Modifier.padding(bottom = navigationBarHeight + extraPadding)
                    }
                )
        )
    }
}

@Composable
private fun ModernAIHeader(
    userName: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 4.dp,
        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            com.example.medicai.ui.theme.GradientStartTertiary,
                            com.example.medicai.ui.theme.GradientEndTertiary
                        )
                    ),
                    shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                )
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
                        .background(
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.SmartToy,
                        contentDescription = "Asistente IA Icon",
                        tint = OnGradientLight,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Asistente Médico IA",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = OnGradientLight
                    )
                    Text(
                        text = "Hola $userName, Soy tu asistente médico",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnGradientLight.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyAIState(modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 32.dp, vertical = 24.dp)
            .padding(bottom = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Puedo ayudarte con:",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SuggestionCard(
                icon = Icons.Filled.Medication,
                title = "Información sobre medicamentos",
                description = "Dosis, efectos secundarios, interacciones"
            )
            SuggestionCard(
                icon = Icons.Filled.HealthAndSafety,
                title = "Síntomas y condiciones",
                description = "Consultas generales de salud"
            )
            SuggestionCard(
                icon = Icons.Filled.Schedule,
                title = "Recordatorios y citas",
                description = "Organiza tu tratamiento médico"
            )
        }
    }
}

@Composable
private fun SuggestionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        MaterialTheme.colorScheme.tertiaryContainer,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title, // Descripción accesible para TalkBack
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(message: com.example.medicai.data.models.ChatMessage, userAvatar: String? = null) {
    val isUser = message.isUser

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        MaterialTheme.colorScheme.tertiaryContainer,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.SmartToy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Surface(
            modifier = Modifier.widthIn(max = 280.dp),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                if (isUser) {
                    // Mensaje del usuario - texto simple
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    // Mensaje de la IA - renderizar Markdown
                    MarkdownText(
                        markdown = message.text,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        linkColor = MaterialTheme.colorScheme.tertiary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message.timestamp,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isUser)
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                modifier = Modifier.size(32.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        userAvatar != null && PredefinedAvatars.isEmojiAvatar(userAvatar) -> {
                            // Mostrar emoji
                            Text(
                                text = userAvatar,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        userAvatar != null && userAvatar.startsWith("http") -> {
                            // Mostrar imagen desde URL
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(userAvatar)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Avatar del usuario",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        }
                        else -> {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TypingIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    MaterialTheme.colorScheme.tertiaryContainer,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.SmartToy,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    val infiniteTransition = rememberInfiniteTransition(label = "typing")
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, delayMillis = index * 200),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "alpha"
                    )

                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                MaterialTheme.colorScheme.tertiary.copy(alpha = alpha),
                                CircleShape
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    isEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    
    Surface(
        modifier = modifier,
        shadowElevation = 8.dp,
        tonalElevation = 3.dp,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Escribe tu pregunta...") },
                shape = RoundedCornerShape(24.dp),
                enabled = isEnabled,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (isEnabled && value.isNotBlank()) {
                            onSend()
                            keyboardController?.hide()
                        }
                    }
                ),
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.tertiary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            FloatingActionButton(
                onClick = onSend,
                modifier = Modifier.size(56.dp),
                containerColor = if (isEnabled && value.isNotBlank()) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Send,
                    contentDescription = "Enviar",
                    tint = if (isEnabled && value.isNotBlank()) {
                        MaterialTheme.colorScheme.onTertiary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

