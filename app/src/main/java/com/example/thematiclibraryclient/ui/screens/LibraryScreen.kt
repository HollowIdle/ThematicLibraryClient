package com.example.thematiclibraryclient.ui.screens

import android.content.ActivityNotFoundException
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.thematiclibraryclient.domain.model.shelves.ShelfDomainModel
import com.example.thematiclibraryclient.ui.common.AppSearchBar
import com.example.thematiclibraryclient.ui.common.BookList
import com.example.thematiclibraryclient.ui.common.ConfirmationDialog
import com.example.thematiclibraryclient.ui.common.ErrorComponent
import com.example.thematiclibraryclient.ui.common.SearchScope
import com.example.thematiclibraryclient.ui.navigation.ScreenRoute
import com.example.thematiclibraryclient.ui.viewmodel.LibraryUiState
import com.example.thematiclibraryclient.ui.viewmodel.LibraryViewMode
import com.example.thematiclibraryclient.ui.viewmodel.LibraryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    navController: NavHostController,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showFilterSheet by remember { mutableStateOf(false) }
    var showCreateShelfDialog by remember { mutableStateOf(false) }
    var shelfToDelete by remember { mutableStateOf<ShelfDomainModel?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            viewModel.uploadBook(it)
        }
    }

    Scaffold(
        topBar = {
            LibraryTopBar(
                uiState = uiState,
                onSearchQueryChanged = viewModel::onSearchQueryChanged,
                onShelfSearchQueryChanged = viewModel::onShelfSearchQueryChanged, // <-- Передаем метод
                onFilterClick = { showFilterSheet = true },
                onScopeChange = viewModel::onSearchScopeChanged,
                navController = navController
            )
        },
        floatingActionButton = {
            if(uiState.viewMode == LibraryViewMode.ALL_BOOKS){
                ExtendedFloatingActionButton(
                    onClick = {
                        try {
                            filePickerLauncher.launch("*/*")
                        } catch (e: ActivityNotFoundException) {
                            Toast.makeText(context, "Не найдено приложение", Toast.LENGTH_SHORT).show()
                        }
                    },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Добавить книгу") }
                )
            } else{
                ExtendedFloatingActionButton(
                    onClick = { showCreateShelfDialog = true },
                    icon = { Icon(Icons.Filled.CreateNewFolder, contentDescription = null) },
                    text = { Text("Создать полку") }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LibraryTabs(
                currentMode = uiState.viewMode,
                onModeSelected = { newMode -> viewModel.setViewMode(newMode) }
            )

            Box(modifier = Modifier.fillMaxSize()) {

                val hasContent = when (uiState.viewMode) {
                    LibraryViewMode.ALL_BOOKS -> uiState.filteredBooks.isNotEmpty()
                    LibraryViewMode.SHELVES -> uiState.filteredShelves.isNotEmpty()
                }

                if (hasContent) {
                    when (uiState.viewMode) {
                        LibraryViewMode.ALL_BOOKS -> {
                            BookList(
                                books = uiState.filteredBooks,
                                onBookClick = { bookId ->
                                    navController.navigate(ScreenRoute.BookDetails.createRoute(bookId))
                                }
                            )
                        }
                        LibraryViewMode.SHELVES -> {
                            ShelvesList(
                                shelves = uiState.filteredShelves,
                                onShelfClick = { shelf ->
                                    navController.navigate(ScreenRoute.ShelfBooks.createRoute(shelf.id, shelf.name))
                                },
                                onUpdateShelf = { id, name -> viewModel.updateShelf(id, name) },
                                onDeleteShelf = { shelf -> shelfToDelete = shelf }
                            )
                        }
                    }
                } else if (uiState.error != null) {
                    ErrorComponent(
                        errorMessage = uiState.error!!,
                        onRetry = { viewModel.refreshData() }
                    )
                } else if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (uiState.viewMode == LibraryViewMode.ALL_BOOKS) "Библиотека пуста" else "Полок нет",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
    if (showFilterSheet) {
        LibraryFilterSheet(
            currentFilter = uiState.filterState,
            availableAuthors = uiState.availableAuthors,
            availableTags = uiState.availableTags,
            availableShelves = uiState.shelves,
            onApply = { newState -> viewModel.onFilterChanged(newState) },
            onClear = { viewModel.clearFilters() },
            onDismiss = { showFilterSheet = false }
        )
    }

    if (showCreateShelfDialog) {
        EditShelfDialog(
            onDismiss = { showCreateShelfDialog = false },
            onConfirm = { name ->
                viewModel.createShelf(name)
                showCreateShelfDialog = false
            }
        )
    }

    if (shelfToDelete != null) {
        ConfirmationDialog(
            title = "Удаление полки",
            text = "Вы уверены, что хотите удалить полку \"${shelfToDelete?.name}\"? Книги останутся в библиотеке.",
            onConfirm = {
                viewModel.deleteShelf(shelfToDelete!!.id)
                shelfToDelete = null
            },
            onDismiss = { shelfToDelete = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShelvesList(
    shelves: List<ShelfDomainModel>,
    onShelfClick: (ShelfDomainModel) -> Unit,
    onUpdateShelf: (Int, String) -> Unit,
    onDeleteShelf: (ShelfDomainModel) -> Unit
) {
    if (shelves.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Полок пока нет.")
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(shelves) { shelf ->
                ShelfItem(
                    shelf = shelf,
                    onClick = { onShelfClick(shelf) },
                    onUpdate = { newName -> onUpdateShelf(shelf.id, newName) },
                    onDelete = { onDeleteShelf(shelf) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryTopBar(
    uiState: LibraryUiState,
    onSearchQueryChanged: (String) -> Unit,
    onShelfSearchQueryChanged: (String) -> Unit,
    onFilterClick: () -> Unit,
    onScopeChange: (SearchScope) -> Unit,
    navController: NavHostController
) {
    if (uiState.viewMode == LibraryViewMode.ALL_BOOKS) {
        AppSearchBar(
            query = uiState.searchQuery,
            onQueryChange = onSearchQueryChanged,
            currentScope = uiState.currentSearchScope,
            onScopeChange = onScopeChange,
            availableScopes = listOf(
                SearchScope.Everywhere,
                SearchScope.Title,
                SearchScope.Author,
                SearchScope.Tag
            ),
            onFilterClick = onFilterClick,
            isFilterActive = uiState.filterState.isActive(),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            showFilterButton = true
        )
    } else {
        AppSearchBar(
            query = uiState.shelfSearchQuery,
            onQueryChange = onShelfSearchQueryChanged,
            currentScope = SearchScope.Shelf,
            availableScopes = listOf(SearchScope.Shelf),
            onScopeChange = {},
            onFilterClick = {},
            isFilterActive = false,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            showFilterButton = false
        )
    }
}

@Composable
fun LibraryTabs(
    currentMode: LibraryViewMode,
    onModeSelected: (LibraryViewMode) -> Unit
) {
    val tabs = listOf("Полки", "Все книги")
    val selectedIndex = if (currentMode == LibraryViewMode.SHELVES) 0 else 1

    TabRow(selectedTabIndex = selectedIndex) {
        tabs.forEachIndexed { index, title ->
            Tab(
                selected = selectedIndex == index,
                onClick = {
                    val mode = if (index == 0) LibraryViewMode.SHELVES else LibraryViewMode.ALL_BOOKS
                    onModeSelected(mode)
                },
                text = { Text(title) }
            )
        }
    }
}


@Composable
private fun ShelfItem(
    shelf: ShelfDomainModel,
    onClick: () -> Unit,
    onUpdate: (String) -> Unit,
    onDelete: () -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))

            Text(text = shelf.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))

            IconButton(onClick = { showEditDialog = true }) {
                Icon(Icons.Default.Edit, contentDescription = "Редактировать")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (showEditDialog) {
        EditShelfDialog(
            initialName = shelf.name,
            onDismiss = { showEditDialog = false },
            onConfirm = { newName ->
                onUpdate(newName)
                showEditDialog = false
            }
        )
    }
}

@Composable
private fun EditShelfDialog(
    initialName: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialName.isEmpty()) "Создать полку" else "Переименовать полку") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Название полки") }
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(name) }) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
