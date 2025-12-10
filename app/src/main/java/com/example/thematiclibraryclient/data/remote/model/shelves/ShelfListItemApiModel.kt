package com.example.thematiclibraryclient.data.remote.model.shelves

import com.example.thematiclibraryclient.domain.model.shelves.ShelfDomainModel
import com.google.gson.annotations.SerializedName

data class ShelfListItemApiModel(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String
)

fun ShelfListItemApiModel.toDomainModel() = ShelfDomainModel(
    id = this.id,
    name = this.name
)