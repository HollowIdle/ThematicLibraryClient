package com.example.thematiclibraryclient.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.thematiclibraryclient.domain.model.shelves.ShelfDomainModel

@Entity(tableName = "shelves")
data class ShelfEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val serverId: Int? = null,
    val name: String,
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false
)

fun ShelfEntity.toDomainModel() = ShelfDomainModel(
    id = this.id,
    name = this.name
)