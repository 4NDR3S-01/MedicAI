package com.example.medicai

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.medicai.data.models.AuthState
import com.example.medicai.viewmodel.AuthViewModel

sealed class AuthScreen(val route: String) {
    object Login : AuthScreen("login")
    object Register : AuthScreen("register")
    object ForgotPassword : AuthScreen("forgot_password")
    object Main : AuthScreen("main")
}

@Composable
fun AuthNavigation(
    authViewModel: AuthViewModel = viewModel()
) {
    val navController = rememberNavController()
    val authState by authViewModel.authState.collectAsState()

    // Mostrar SplashScreen mientras verifica la sesión
    when (authState) {
        is AuthState.Idle -> {
            // Verificando sesión...
            SplashScreen()
        }
        is AuthState.Loading -> {
            // Cargando...
            SplashScreen()
        }
        is AuthState.Success -> {
            // Usuario autenticado, mostrar app principal
            MainScreen(
                authViewModel = authViewModel,
                onLogout = {
                    authViewModel.logout()
                }
            )
        }
        is AuthState.Error -> {
            // Error o sin sesión, mostrar navegación de auth
            AuthNavigationContent(
                navController = navController,
                authViewModel = authViewModel
            )
        }
    }
}

@Composable
private fun AuthNavigationContent(
    navController: androidx.navigation.NavHostController,
    authViewModel: AuthViewModel
) {
    val authState by authViewModel.authState.collectAsState()

    // Observar cuando el login sea exitoso para navegar a Main
    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            // El login fue exitoso, pero ya no necesitamos navegar
            // porque AuthNavigation ya maneja esto en el nivel superior
        }
    }

    NavHost(
        navController = navController,
        startDestination = AuthScreen.Login.route
    ) {
        composable(AuthScreen.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                    // El AuthNavigation detectará el cambio de estado
                    // y mostrará automáticamente MainScreen
                },
                onNavigateToRegister = {
                    navController.navigate(AuthScreen.Register.route)
                },
                onNavigateToForgotPassword = {
                    navController.navigate(AuthScreen.ForgotPassword.route)
                }
            )
        }

        composable(AuthScreen.Register.route) {
            RegisterScreen(
                viewModel = authViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSubmit = { registrationData ->
                    // El registro se maneja en el ViewModel
                }
            )
        }

        composable(AuthScreen.ForgotPassword.route) {
            ForgotPasswordScreen(
                viewModel = authViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onResetSuccess = {
                    navController.popBackStack()
                }
            )
        }
    }
}
