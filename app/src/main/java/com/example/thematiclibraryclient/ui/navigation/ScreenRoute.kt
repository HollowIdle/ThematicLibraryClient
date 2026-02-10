package com.example.thematiclibraryclient.ui.navigation

sealed class ScreenRoute(val route: String) {

    data object Main : ScreenRoute("main_screen")
    data object Register : ScreenRoute("register_screen")
    data object Login : ScreenRoute("login_screen")
    data object Library : ScreenRoute("library_screen")
    data object Quotes : ScreenRoute("quotes_screen")

    data object Reader : ScreenRoute("reader_screen/{bookId}?position={position}&isPage={isPage}") {
        fun createRoute(bookId: Int): String = "reader_screen/$bookId"

        fun createRouteWithPosition(bookId: Int, position: Int, isPage: Boolean = false): String =
            "reader_screen/$bookId?position=$position&isPage=$isPage"
    }

    data object Profile : ScreenRoute("profile_screen")

    data object ShelfBooks : ScreenRoute("shelf_books_screen/{shelfId}/{shelfName}") {
        fun createRoute(shelfId: Int, shelfName: String): String =
            "shelf_books_screen/$shelfId/$shelfName"
    }

    data object BookDetails : ScreenRoute("book_details_screen/{bookId}") {
        fun createRoute(bookId: Int): String = "book_details_screen/$bookId"
    }

    companion object{
        const val AUTH_GRAPH_ROUTE = "auth_graph"
        const val MAIN_GRAPH_ROUTE = "main_graph"
    }

}