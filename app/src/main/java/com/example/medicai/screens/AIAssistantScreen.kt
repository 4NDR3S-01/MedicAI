package com.example.medicai.screens

import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.AnimatedContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.sp
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
import androidx.compose.ui.text.style.TextAlign
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

    var showClearDialog by remember { mutableStateOf(false) }

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
                userName = currentUser?.full_name?.split(" ")?.firstOrNull() ?: "Usuario",
                hasMessages = messages.isNotEmpty(),
                onClearChat = { showClearDialog = true }
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

    // Diálogo de confirmación para limpiar chat
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("🗑️ Limpiar conversación") },
            text = { 
                Text("¿Estás seguro de que deseas eliminar toda la conversación? Esta acción no se puede deshacer.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        aiViewModel.clearChat()
                        showClearDialog = false
                    }
                ) {
                    Text("Limpiar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun ModernAIHeader(
    userName: String,
    hasMessages: Boolean = false,
    onClearChat: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 6.dp,
        shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(85.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            com.example.medicai.ui.theme.GradientStartTertiary,
                            com.example.medicai.ui.theme.GradientEndTertiary
                        )
                    ),
                    shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
                )
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
                                )
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.SmartToy,
                        contentDescription = "Asistente IA Icon",
                        tint = OnGradientLight,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Asistente Médico IA",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnGradientLight
                    )
                    androidx.compose.animation.AnimatedContent(
                        targetState = hasMessages,
                        label = "subtitle"
                    ) { hasMsg ->
                        Text(
                            text = if (hasMsg) "💬 Conversación activa" else "Hola $userName 👋",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnGradientLight.copy(alpha = 0.9f)
                        )
                    }
                }
                
                // Botón para limpiar chat con animación (solo visible si hay mensajes)
                androidx.compose.animation.AnimatedVisibility(
                    visible = hasMessages,
                    enter = fadeIn() + androidx.compose.animation.scaleIn(),
                    exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut()
                ) {
                    IconButton(
                        onClick = onClearChat,
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteSweep,
                            contentDescription = "Limpiar chat",
                            tint = OnGradientLight,
                            modifier = Modifier.size(24.dp)
                        )
                    }
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
            .padding(horizontal = 28.dp, vertical = 16.dp)
            .padding(bottom = 220.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))
        
        // Mensaje de bienvenida mejorado con animación
        androidx.compose.animation.AnimatedVisibility(
            visible = true,
            enter = fadeIn(animationSpec = tween(600)) + 
                    slideInVertically(animationSpec = tween(600)) { -50 }
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "💬 Conversación continua",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Puedo recordar toda nuestra conversación\ny mantener el contexto de tus preguntas",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        
        Divider(
            modifier = Modifier.width(100.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
            thickness = 2.dp
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "¿En qué puedo ayudarte hoy?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SuggestionCard(
                icon = Icons.Filled.Medication,
                title = "Información sobre medicamentos",
                description = "Dosis, efectos secundarios, interacciones"
            )
            SuggestionCard(
                icon = Icons.Filled.HealthAndSafety,
                title = "Consultas sobre síntomas",
                description = "Información sobre condiciones de salud"
            )
            SuggestionCard(
                icon = Icons.Filled.LocalHospital,
                title = "Primeros auxilios básicos",
                description = "Consejos para emergencias menores"
            )
            SuggestionCard(
                icon = Icons.Filled.FitnessCenter,
                title = "Vida saludable y prevención",
                description = "Tips de nutrición, ejercicio y bienestar"
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
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.tertiaryContainer,
                                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                            )
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(message: com.example.medicai.data.models.ChatMessage, userAvatar: String? = null) {
    val isUser = message.isUser
    
    // Animación de aparición
    androidx.compose.animation.AnimatedVisibility(
        visible = true,
        enter = fadeIn(animationSpec = tween(300)) + 
                slideInVertically(animationSpec = tween(300)) { it / 2 }
    ) {
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
            modifier = Modifier.widthIn(max = 300.dp),
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (isUser) 20.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 20.dp
            ),
            color = if (isUser)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 3.dp,
            tonalElevation = if (isUser) 2.dp else 1.dp
        ) {
            Column(
                modifier = Modifier.padding(14.dp)
            ) {
                if (isUser) {
                    // Mensaje del usuario - texto simple
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        lineHeight = 22.sp
                    )
                } else {
                    // Mensaje de la IA - renderizar Markdown
                    MarkdownText(
                        markdown = message.text,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 22.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        linkColor = MaterialTheme.colorScheme.tertiary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
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
}

@Composable
private fun TypingIndicator() {
    androidx.compose.animation.AnimatedVisibility(
        visible = true,
        enter = fadeIn(animationSpec = tween(300)) + 
                slideInVertically(animationSpec = tween(300)) { it / 2 }
    ) {
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
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shadowElevation = 3.dp,
                tonalElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MedicAI está escribiendo",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    repeat(3) { index ->
                        val infiniteTransition = rememberInfiniteTransition(label = "typing")
                        val offsetY by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = -8f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(500, delayMillis = index * 150, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "offsetY"
                        )

                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .offset(y = offsetY.dp)
                                .background(
                                    MaterialTheme.colorScheme.tertiary,
                                    CircleShape
                                )
                        )
                    }
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
    val isSendEnabled = isEnabled && value.isNotBlank()
    
    Surface(
        modifier = modifier,
        shadowElevation = 12.dp,
        tonalElevation = 3.dp,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = { 
                    Text(
                        "Pregúntame sobre salud, medicamentos...",
                        style = MaterialTheme.typography.bodyMedium
                    ) 
                },
                shape = RoundedCornerShape(28.dp),
                enabled = isEnabled,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (isSendEnabled) {
                            onSend()
                            keyboardController?.hide()
                        }
                    }
                ),
                maxLines = 5,
                textStyle = MaterialTheme.typography.bodyLarge,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.tertiary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            )

            val scale = androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (isSendEnabled) 1f else 0.9f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "scale"
            )
            
            FloatingActionButton(
                onClick = {
                    if (isSendEnabled) {
                        onSend()
                        keyboardController?.hide()
                    }
                },
                modifier = Modifier
                    .size(58.dp)
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                    },
                containerColor = if (isSendEnabled) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 12.dp
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Send,
                    contentDescription = "Enviar mensaje",
                    modifier = Modifier.size(24.dp),
                    tint = if (isSendEnabled) {
                        MaterialTheme.colorScheme.onTertiary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

