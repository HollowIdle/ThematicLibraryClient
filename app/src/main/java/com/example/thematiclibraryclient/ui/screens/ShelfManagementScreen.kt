package com.example.thematiclibraryclient.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.thematiclibraryclient.domain.model.shelves.ShelfDomainModel
import com.example.thematiclibraryclient.ui.viewmodel.ShelfManagementViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShelfManagementScreen(
    viewModel: ShelfManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Управление полками") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Создать полку")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.shelves) { shelf ->
                        ShelfItem(
                            shelf = shelf,
                            onUpdate = { newName -> viewModel.updateShelf(shelf.id, newName) },
                            onDelete = { viewModel.deleteShelf(shelf.id) }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        EditShelfDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name ->
                viewModel.createShelf(name)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun ShelfItem(
    shelf: ShelfDomainModel,
    onUpdate: (String) -> Unit,
    onDelete: () -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Folder, contentDescription = null)
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = shelf.name, modifier = Modifier.weight(1f))
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