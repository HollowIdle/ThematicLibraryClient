package com.example.thematiclibraryclient.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ManageItemsDialog(
    title: String,
    initialItems: List<String>,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit
) {
    // Локальное состояние списка
    val items = remember { mutableStateListOf(*initialItems.toTypedArray()) }

    // Состояния диалогов
    var showAddDialog by remember { mutableStateOf(false) }
    var editIndex by remember { mutableStateOf(-1) }

    // <-- НОВОЕ СОСТОЯНИЕ: Индекс элемента, который хотим удалить
    var itemToDeleteIndex by remember { mutableStateOf(-1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Добавить")
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(300.dp)
                ) {
                    itemsIndexed(items) { index, item ->
                        ItemCard(
                            text = item,
                            onEdit = { editIndex = index },
                            // <-- ИЗМЕНЕНИЕ: Не удаляем сразу, а запоминаем индекс
                            onDelete = { itemToDeleteIndex = index }
                        )
                    }
                    if (items.isEmpty()) {
                        item {
                            Text(
                                text = "Список пуст",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(items.toList()) }) {
                Text("Сохранить изменения")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )

    // Диалог добавления
    if (showAddDialog) {
        InputItemDialog(
            title = "Добавить элемент",
            initialValue = "",
            onDismiss = { showAddDialog = false },
            onConfirm = { newItem ->
                if (newItem.isNotBlank()) items.add(newItem)
                showAddDialog = false
            }
        )
    }

    // Диалог редактирования
    if (editIndex != -1) {
        InputItemDialog(
            title = "Редактировать элемент",
            initialValue = items[editIndex],
            onDismiss = { editIndex = -1 },
            onConfirm = { updatedItem ->
                if (updatedItem.isNotBlank()) {
                    items[editIndex] = updatedItem
                }
                editIndex = -1
            }
        )
    }

    // <-- НОВЫЙ ДИАЛОГ: Подтверждение удаления
    if (itemToDeleteIndex != -1) {
        val itemText = items.getOrNull(itemToDeleteIndex) ?: ""
        ConfirmationDialog(
            title = "Удаление",
            text = "Вы уверены, что хотите удалить \"$itemText\"?",
            onConfirm = {
                if (itemToDeleteIndex in items.indices) {
                    items.removeAt(itemToDeleteIndex)
                }
                itemToDeleteIndex = -1
            },
            onDismiss = { itemToDeleteIndex = -1 }
        )
    }
}

@Composable
private fun ItemCard(
    text: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Редактировать")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun InputItemDialog(
    title: String,
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(text) }) { Text("ОК") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}