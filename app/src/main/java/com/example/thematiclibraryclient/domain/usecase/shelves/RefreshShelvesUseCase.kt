package com.example.thematiclibraryclient.domain.usecase.shelves
import com.example.thematiclibraryclient.domain.repository.IShelvesRemoteRepository
import javax.inject.Inject

class RefreshShelvesUseCase @Inject constructor(
    private val repository: IShelvesRemoteRepository
) {
    suspend operator fun invoke() = repository.refreshShelves()
}