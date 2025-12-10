package com.example.thematiclibraryclient.ui.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider

@Composable
fun QuoteActionMenu(
    expanded: Boolean,
    popupPositionProvider: PopupPositionProvider,
    onDismissRequest: () -> Unit,
    onQuoteClick: () -> Unit
) {
    if (expanded) {
        Popup(
            popupPositionProvider = popupPositionProvider,
            onDismissRequest = onDismissRequest
        ) {
            Card(
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(modifier = Modifier.padding(horizontal = 8.dp)) {
                    TextButton(onClick = onQuoteClick) {
                        Text("Цитировать")
                    }
                }
            }
        }
    }
}