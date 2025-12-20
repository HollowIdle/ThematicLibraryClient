package com.example.thematiclibraryclient.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.thematiclibraryclient.ui.screens.BookDetailsScreen
import com.example.thematiclibraryclient.ui.screens.LibraryScreen
import com.example.thematiclibraryclient.ui.screens.ProfileScreen
import com.example.thematiclibraryclient.ui.screens.QuotesScreen
import com.example.thematiclibraryclient.ui.screens.ReaderScreen
import com.example.thematiclibraryclient.ui.screens.ShelfBooksScreen
@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    onLogout: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = ScreenRoute.Library.route,
        modifier = modifier
    ) {
        composable(ScreenRoute.Library.route) {
            LibraryScreen(navController = navController)
        }
        composable(ScreenRoute.Quotes.route) {
            QuotesScreen(navController = navController)
        }
        composable(ScreenRoute.Profile.route){
            ProfileScreen(onLogout = onLogout)
        }
        composable(
            route = ScreenRoute.Reader.route,
            arguments = listOf(
                navArgument("bookId") { type = NavType.IntType },
                navArgument("position") {
                    type = NavType.IntType
                    defaultValue = -1
                }
            )
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getInt("bookId") ?: 0
            val position = backStackEntry.arguments?.getInt("position") ?: -1
            ReaderScreen(
                bookId = bookId,
                initialPosition = position,
                navController = navController
            )
        }

        composable(
            route = ScreenRoute.BookDetails.route,
            arguments = listOf(navArgument("bookId") { type = NavType.IntType })
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getInt("bookId") ?: 0
            BookDetailsScreen(
                bookId = bookId,
                navController = navController
            )
        }

        composable(
            route = ScreenRoute.ShelfBooks.route,
            arguments = listOf(
                navArgument("shelfId") { type = NavType.IntType },
                navArgument("shelfName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val shelfId = backStackEntry.arguments?.getInt("shelfId") ?: 0
            val shelfName = backStackEntry.arguments?.getString("shelfName") ?: ""

            ShelfBooksScreen(
                shelfId = shelfId,
                shelfName = shelfName,
                navController = navController
            )
        }

    }
}