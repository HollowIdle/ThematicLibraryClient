package com.example.thematiclibraryclient.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.thematiclibraryclient.ui.common.AppSearchBar
import com.example.thematiclibraryclient.ui.common.BookList
import com.example.thematiclibraryclient.ui.common.ErrorComponent
import com.example.thematiclibraryclient.ui.common.SearchScope
import com.example.thematiclibraryclient.ui.navigation.ScreenRoute
import com.example.thematiclibraryclient.ui.viewmodel.ShelfBooksViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShelfBooksScreen(
    shelfId: Int,
    shelfName: String,
    navController: NavHostController,
    viewModel: ShelfBooksViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFilterSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 8.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                }

                AppSearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = viewModel::onSearchQueryChanged,
                    currentScope = uiState.currentSearchScope,
                    availableScopes = listOf(
                        SearchScope.Everywhere,
                        SearchScope.Title,
                        SearchScope.Author,
                        SearchScope.Tag
                    ),
                    onScopeChange = viewModel::onSearchScopeChanged,
                    onFilterClick = { showFilterSheet = true },
                    isFilterActive = uiState.filterState.isActive(),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val hasContent = uiState.allBooks.isNotEmpty()

            if (hasContent) {
                BookList(
                    books = uiState.filteredBooks,
                    onBookClick = { bookId ->
                        navController.navigate(ScreenRoute.BookDetails.createRoute(bookId))
                    },
                    emptyMessage = if (uiState.searchQuery.isNotEmpty() || uiState.filterState.isActive())
                        "Ничего не найдено по вашему запросу."
                    else
                        "На этой полке пока нет книг."
                )
            } else if (uiState.error != null) {
                ErrorComponent(
                    errorMessage = uiState.error!!,
                    onRetry = { viewModel.refreshBooks() } // Или refreshBooks()
                )
            } else if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                BookList(
                    books = emptyList(),
                    onBookClick = {},
                    emptyMessage = "На этой полке пока нет книг."
                )
            }
        }
    }

    if (showFilterSheet) {
        LibraryFilterSheet(
            currentFilter = uiState.filterState,
            availableAuthors = uiState.availableAuthors,
            availableTags = uiState.availableTags,
            availableShelves = emptyList(), // Полки здесь не нужны
            onApply = { newState -> viewModel.onFilterChanged(newState) },
            onClear = { viewModel.clearFilters() },
            onDismiss = { showFilterSheet = false }
        )
    }
}