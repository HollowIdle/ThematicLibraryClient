package com.example.thematiclibraryclient.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.thematiclibraryclient.ui.navigation.AppNavigation
import com.example.thematiclibraryclient.ui.navigation.BottomNavItem
import com.example.thematiclibraryclient.ui.viewmodel.SessionViewModel

@Composable
fun MainScreen(
    onLogout: () -> Unit,
    onSyncRequest: () -> Unit,
    sessionViewModel: SessionViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val sessionState by sessionViewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                sessionViewModel.validateSession()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(sessionState.shouldLogout) {
        if (sessionState.shouldLogout) {
            onLogout()
            sessionViewModel.onLogoutNavigated()
        }
    }

    if (sessionState.isBlocked) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Доступ заблокирован") },
            text = { Text("Вы были заблокированы. Свяжитесь с администратором.") },
            confirmButton = {
                Button(onClick = {
                    sessionViewModel.onLogoutConfirmed()
                }) {
                    Text("Выйти")
                }
            }
        )
    }

    if (sessionState.isSessionReset) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Сессия сброшена") },
            text = { Text("Ваша сессия была сброшена. Пожалуйста, войдите снова.") },
            confirmButton = {
                Button(onClick = {
                    sessionViewModel.onLogoutConfirmed()
                }) {
                    Text("Выйти")
                }
            }
        )
    }

    Scaffold(
        bottomBar = { BottomBar(navController = navController) }
    ) { innerPadding ->
        AppNavigation(
            modifier = Modifier.padding(innerPadding),
            navController = navController,
            onLogout = onLogout,
            onSyncRequest = onSyncRequest
        )
    }
}

@Composable
private fun BottomBar(navController: NavHostController) {
    val items = listOf(
        BottomNavItem.Library,
        BottomNavItem.Quotes,
        BottomNavItem.Profile
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val bottomBarDestination = items.any { it.route == currentDestination?.route }
    if (bottomBarDestination) {
        NavigationBar {
            items.forEach { item ->
                NavigationBarItem(
                    selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                    onClick = {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(imageVector = item.icon, contentDescription = item.title) },
                    label = { Text(text = item.title) }
                )
            }
        }
    }
}