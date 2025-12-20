package com.example.thematiclibraryclient.ui.model

data class BookFilterState(
    val selectedAuthors: Set<String> = emptySet(),
    val selectedTags: Set<String> = emptySet(),
    val selectedShelves: Set<Int> = emptySet()
) {
    fun isActive(): Boolean = selectedAuthors.isNotEmpty() || selectedTags.isNotEmpty() || selectedShelves.isNotEmpty()
}