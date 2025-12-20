package com.example.thematiclibraryclient.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@Composable
fun AppSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    currentScope: SearchScope,
    availableScopes: List<SearchScope>,
    onScopeChange: (SearchScope) -> Unit,
    onFilterClick: () -> Unit,
    modifier: Modifier = Modifier,
    isFilterActive: Boolean = false,
    showFilterButton: Boolean = true
) {
    val focusManager = LocalFocusManager.current
    var isScopeMenuExpanded by remember { mutableStateOf(false) }

    val placeholderText = "Поиск: ${currentScope.title}..."

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text(placeholderText) },

            leadingIcon = {
                Box {
                    Row(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .then(
                                if (availableScopes.size > 1) {
                                    Modifier
                                        .clickable { isScopeMenuExpanded = true }
                                        .padding(4.dp)
                                } else Modifier
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = currentScope.icon,
                            contentDescription = currentScope.title,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (availableScopes.size > 1) {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Выбрать критерий",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = isScopeMenuExpanded,
                        onDismissRequest = { isScopeMenuExpanded = false }
                    ) {
                        availableScopes.forEach { scope ->
                            DropdownMenuItem(
                                text = { Text(scope.title) },
                                leadingIcon = { Icon(scope.icon, contentDescription = null) },
                                onClick = {
                                    onScopeChange(scope)
                                    isScopeMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            },

            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Очистить")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                focusedBorderColor = MaterialTheme.colorScheme.primary
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
        )

        if (showFilterButton) {
            Spacer(modifier = Modifier.width(8.dp))
            if (isFilterActive) {
                FilledIconButton(
                    onClick = onFilterClick,
                    modifier = Modifier.size(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.FilterList, contentDescription = "Фильтры")
                }
            } else {
                OutlinedIconButton(
                    onClick = onFilterClick,
                    modifier = Modifier.size(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                ) {
                    Icon(Icons.Default.FilterList, contentDescription = "Фильтры")
                }
            }
        }
    }
}