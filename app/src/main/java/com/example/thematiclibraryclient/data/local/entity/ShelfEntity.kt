package com.example.thematiclibraryclient.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.thematiclibraryclient.domain.model.shelves.ShelfDomainModel

@Entity(tableName = "shelves")
data class ShelfEntity(
    @PrimaryKey(autoGenerate = false)
    val id: Int,
    val name: String
)

fun ShelfEntity.toDomainModel() = ShelfDomainModel(
    id = this.id,
    name = this.name
)