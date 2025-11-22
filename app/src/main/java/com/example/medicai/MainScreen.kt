package com.example.medicai

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.medicai.screens.*
import com.example.medicai.viewmodel.AuthViewModel

/**
 * Navegación principal de la aplicación con BottomNavigationBar
 */
sealed class AppScreen(val route: String, val title: String, val icon: ImageVector) {
    object Home : AppScreen("home", "Inicio", Icons.Filled.Home)
    object Medicines : AppScreen("medicines", "Medicamentos", Icons.Filled.Medication)
    object Appointments : AppScreen("appointments", "Citas", Icons.Filled.EventNote)
    object AIAssistant : AppScreen("ai_assistant", "IA", Icons.Filled.SmartToy)
    object Profile : AppScreen("profile", "Perfil", Icons.Filled.Person)
}

@Composable
fun MainScreen(
    authViewModel: AuthViewModel,
    onLogout: () -> Unit = {}
) {
    val navController = rememberNavController()

    // Lista de items de navegación
    val items = listOf(
        AppScreen.Home,
        AppScreen.Medicines,
        AppScreen.Appointments,
        AppScreen.AIAssistant,
        AppScreen.Profile
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(0.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                NavigationBar(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                items.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = { 
                            Text(
                                text = screen.title,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    letterSpacing = (-0.2).sp
                                ),
                                maxLines = 1
                            ) 
                        },
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        alwaysShowLabel = false // Solo mostrar etiqueta si hay espacio o seleccionado (opcional, aquí false para look más limpio)
                    )
                }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = AppScreen.Home.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            // Pantalla de Inicio (Home)
            composable(AppScreen.Home.route) {
                HomeScreen(
                    authViewModel = authViewModel
                )
            }

            // Medicamentos
            composable(AppScreen.Medicines.route) {
                MedicinesScreen(
                    authViewModel = authViewModel
                )
            }

            // Citas Médicas
            composable(AppScreen.Appointments.route) {
                AppointmentsScreen(
                    authViewModel = authViewModel
                )
            }

            // Asistente IA
            composable(AppScreen.AIAssistant.route) {
                AIAssistantScreen(
                    authViewModel = authViewModel
                )
            }

            // Perfil / Configuración
            composable(AppScreen.Profile.route) {
                ProfileScreen(
                    authViewModel = authViewModel,
                    onLogout = onLogout
                )
            }
        }
    }
}
