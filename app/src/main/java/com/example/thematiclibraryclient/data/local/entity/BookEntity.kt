package com.example.thematiclibraryclient.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.thematiclibraryclient.domain.model.books.AuthorDomainModel
import com.example.thematiclibraryclient.domain.model.books.BookDetailsDomainModel
import com.example.thematiclibraryclient.domain.model.books.BookDomainModel

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val serverId: Int? = null,
    val title: String,
    val description: String?,
    val authors: List<AuthorDomainModel>,
    val tags: List<String>,
    val shelfIds: List<Int>,
    val lastPosition: Int = 0,
    val isDetailsLoaded: Boolean = false,
    val filePath: String? = null,
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false
)

fun BookEntity.toDomainModel(): BookDomainModel {
    var extension: String? = null

    extension = if(this.filePath != null){
        this.filePath
            .substringAfterLast('.', "")
            .uppercase()
            .takeIf { it.isNotEmpty() && it.length <= 4 }


    } else {
        this.title
            .substringAfterLast('.', "")
            .uppercase()
            .takeIf { it.isNotEmpty() && it.length <= 4 }
    }


    return BookDomainModel(
        id = id,
        title = title,
        description = description,
        authors = authors,
        tags = tags,
        shelfIds = shelfIds,
        isSynced = isSynced,
        fileExtension = extension
    )
}

fun BookEntity.toDetailsDomainModel(): BookDetailsDomainModel {

    val extension = this.filePath
        ?.substringAfterLast('.', "")
        ?.uppercase()
        ?.takeIf { it.isNotEmpty() && it.length <= 4 }

    return BookDetailsDomainModel(
        id = id,
        title = title,
        description = description,
        authors = authors.map { it.name },
        tags = tags,
        shelfIds = shelfIds,
        lastPosition = lastPosition,
        fileExtension = extension
    )

}