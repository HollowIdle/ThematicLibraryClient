package com.example.thematiclibraryclient.domain.usecase.shelves

import com.example.thematiclibraryclient.domain.model.shelves.ShelfDomainModel
import com.example.thematiclibraryclient.domain.repository.IShelvesRemoteRepository
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow

class GetShelvesUseCase @Inject constructor(
    private val repository: IShelvesRemoteRepository
) {
    operator fun invoke(): Flow<List<ShelfDomainModel>> =
        repository.getShelves()
}