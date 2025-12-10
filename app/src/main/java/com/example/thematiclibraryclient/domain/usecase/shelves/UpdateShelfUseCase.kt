package com.example.thematiclibraryclient.domain.usecase.shelves

import com.example.thematiclibraryclient.domain.repository.IShelvesRemoteRepository
import javax.inject.Inject

class UpdateShelfUseCase @Inject constructor(
    private val repository: IShelvesRemoteRepository
) {
    suspend operator fun invoke(shelfId: Int, newName: String) = repository.updateShelf(shelfId, newName)
}