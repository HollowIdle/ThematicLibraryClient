package com.example.thematiclibraryclient.domain.usecase.shelves

import com.example.thematiclibraryclient.domain.repository.IShelvesRemoteRepository
import jakarta.inject.Inject

class GetShelvesUseCase @Inject constructor(
    private val repository: IShelvesRemoteRepository
) {
    suspend operator fun invoke() = repository.getShelves()
}