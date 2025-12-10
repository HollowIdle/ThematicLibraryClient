package com.example.thematiclibraryclient.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Library : BottomNavItem(
        route = ScreenRoute.Library.route,
        title = "Библиотека",
        icon = Icons.AutoMirrored.Filled.LibraryBooks
    )

    data object Quotes : BottomNavItem(
        route = ScreenRoute.Quotes.route,
        title = "Цитаты",
        icon = Icons.Filled.FormatQuote
    )

    data object Profile : BottomNavItem(
        route = ScreenRoute.Profile.route,
        title = "Профиль",
        icon = Icons.Default.Person
    )
}