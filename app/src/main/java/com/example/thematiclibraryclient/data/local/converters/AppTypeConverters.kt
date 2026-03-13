package com.example.thematiclibraryclient.data.local.converters

import androidx.room.TypeConverter
import com.example.thematiclibraryclient.domain.model.books.AuthorDomainModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

class AppTypeConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return gson.toJson(value ?: emptyList<String>())
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }

    @TypeConverter
    fun fromIntList(value: List<Int>?): String {
        return gson.toJson(value ?: emptyList<Int>())
    }

    @TypeConverter
    fun toIntList(value: String): List<Int> {
        val listType = object : TypeToken<List<Int>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }

    @TypeConverter
    fun fromAuthorList(value: List<AuthorDomainModel>?): String {
        return gson.toJson(value ?: emptyList<AuthorDomainModel>())
    }

    @TypeConverter
    fun toAuthorList(value: String): List<AuthorDomainModel> {
        val listType = object : TypeToken<List<AuthorDomainModel>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }

    @TypeConverter
    fun fromDate(value: Date?): Long? {
        return value?.time
    }

    @TypeConverter
    fun toDate(value: Long?): Date? {
        return value?.let { Date(it) }
    }
}