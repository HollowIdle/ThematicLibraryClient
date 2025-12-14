package com.example.thematiclibraryclient.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.thematiclibraryclient.domain.model.quotes.QuoteDomainModel

@Composable
fun QuoteActionsDialog(
    quote: QuoteDomainModel,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onSaveNote: (String) -> Unit,
    onGoToSource: (QuoteDomainModel) -> Unit
) {
    var noteText by remember { mutableStateOf(quote.noteContent ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Управление цитатой") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "“${quote.selectedText}”",
                    style = MaterialTheme.typography.bodyLarge
                )
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Заметка к цитате") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Отмена")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onSaveNote(noteText) }) {
                        Text("Сохранить")
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Удалить")
                    }
                    TextButton(onClick = { onGoToSource(quote) }) {
                        Text("Перейти к источнику")
                    }
                }
            }
        }
    )
}