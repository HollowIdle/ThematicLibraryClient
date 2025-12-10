package com.example.thematiclibraryclient.domain.usecase.shelves

import com.example.thematiclibraryclient.domain.repository.IShelvesRemoteRepository
import javax.inject.Inject

class CreateShelfUseCase @Inject constructor(
    private val repository: IShelvesRemoteRepository
) {
    suspend operator fun invoke(name: String) = repository.createShelf(name)
}