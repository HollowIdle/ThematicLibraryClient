package com.example.thematiclibraryclient.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.thematiclibraryclient.domain.model.books.AuthorDomainModel
import com.example.thematiclibraryclient.domain.model.shelves.ShelfDomainModel
import com.example.thematiclibraryclient.ui.model.BookFilterState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LibraryFilterSheet(
    currentFilter: BookFilterState,
    availableAuthors: List<AuthorDomainModel?>,
    availableTags: List<String>,
    availableShelves: List<ShelfDomainModel>,
    onApply: (BookFilterState) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedAuthors by remember { mutableStateOf(currentFilter.selectedAuthors) }
    var selectedTags by remember { mutableStateOf(currentFilter.selectedTags) }
    var selectedShelves by remember { mutableStateOf(currentFilter.selectedShelves) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = {
                    onClear()
                    onDismiss()
                }) {
                    Text("Сбросить")
                }
                Button(onClick = {
                    onApply(BookFilterState(selectedAuthors, selectedTags, selectedShelves))
                    onDismiss()
                }) {
                    Text("Применить")
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 16.dp))

            Text(
                text = "Фильтры",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (availableShelves.isNotEmpty()) {
                ExpandableFilterSection(
                    title = "Полки",
                    items = availableShelves,
                    isItemSelected = { it.id in selectedShelves },
                    getLabel = { it.name },
                    onItemClick = { shelf ->
                        selectedShelves = if (shelf.id in selectedShelves) {
                            selectedShelves - shelf.id
                        } else {
                            selectedShelves + shelf.id
                        } }
                )
                HorizontalDivider(Modifier.padding(vertical = 16.dp))
            }

            val validAuthors = availableAuthors.filterNotNull()

            if (validAuthors.isNotEmpty()) {
                ExpandableFilterSection(
                    title = "Авторы",
                    items = validAuthors,

                    isItemSelected = { it.name in selectedAuthors },

                    getLabel = { it.name },

                    onItemClick = { author ->
                        selectedAuthors = if (author.name in selectedAuthors) {
                            selectedAuthors - author.name
                        } else {
                            selectedAuthors + author.name
                        }
                    }
                )
                HorizontalDivider(Modifier.padding(vertical = 16.dp))
            }

            if (availableTags.isNotEmpty()) {
                ExpandableFilterSection(
                    title = "Теги",
                    items = availableTags,
                    isItemSelected = { tag -> tag in selectedTags },
                    getLabel = { tag -> tag },
                    onItemClick = { tag ->
                        selectedTags = if (tag in selectedTags) {
                            selectedTags - tag
                        } else {
                            selectedTags + tag
                        }
                    }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> ExpandableFilterSection(
    title: String,
    items: List<T>,
    isItemSelected: (T) -> Boolean,
    getLabel: (T) -> String,
    onItemClick: (T) -> Unit,
    initialCount: Int = 8
) {
    var isExpanded by remember { mutableStateOf(false) }
    val showExpandButton = items.size > initialCount

    val displayedItems = if (isExpanded || !showExpandButton) items else items.take(initialCount)

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth().animateContentSize(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            displayedItems.forEach { item ->
                FilterChip(
                    selected = isItemSelected(item),
                    onClick = { onItemClick(item) },
                    label = { Text(getLabel(item)) }
                )
            }

            if (showExpandButton) {
                SuggestionChip(
                    onClick = { isExpanded = !isExpanded },
                    label = { Text(if (isExpanded) "Свернуть" else "Ещё ${items.size - initialCount}") },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
        }
    }
}