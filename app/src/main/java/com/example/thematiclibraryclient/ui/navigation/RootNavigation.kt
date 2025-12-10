package com.example.thematiclibraryclient.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import com.example.thematiclibraryclient.ui.screens.LoginScreen
import com.example.thematiclibraryclient.ui.screens.MainScreen
import com.example.thematiclibraryclient.ui.screens.RegisterScreen

@Composable
fun RootNavigation(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(navController = navController, startDestination = startDestination) {
        navigation(
            startDestination = ScreenRoute.Login.route,
            route = ScreenRoute.AUTH_GRAPH_ROUTE
        ) {
            composable(ScreenRoute.Login.route) {
                LoginScreen(
                    onRegisterClick = {
                        navController.navigate(ScreenRoute.Register.route)
                    }
                )
            }

            composable(ScreenRoute.Register.route) {
                RegisterScreen(
                    onRegistrationSuccess = {
                        navController.popBackStack()
                    }
                )
            }
        }

        composable(ScreenRoute.MAIN_GRAPH_ROUTE) {
            MainScreen(
                onLogout = {
                    navController.navigate(ScreenRoute.AUTH_GRAPH_ROUTE) {
                        popUpTo(ScreenRoute.MAIN_GRAPH_ROUTE) {
                            inclusive = true
                        }
                    }
                }
            )
        }
    }
}