package com.example.thematiclibraryclient.domain.usecase.shelves

import com.example.thematiclibraryclient.domain.repository.IShelvesRemoteRepository
import javax.inject.Inject

class DeleteShelfUseCase @Inject constructor(
    private val repository: IShelvesRemoteRepository
) {
    suspend operator fun invoke(shelfId: Int) = repository.deleteShelf(shelfId)
}