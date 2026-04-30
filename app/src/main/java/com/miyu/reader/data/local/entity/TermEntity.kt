package com.miyu.reader.data.local.entity

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey

@Keep
@Entity(tableName = "terms")
data class TermEntity(
    @PrimaryKey val id: String,
    val originalText: String,
    val translationText: String? = null,
    val correctedText: String,
    val context: String? = null,
    val imageUri: String? = null,
    val createdAt: String,
    val updatedAt: String? = null,
    val groupId: String,
)

@Keep
@Entity(tableName = "term_groups")
data class TermGroupEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String? = null,
    val appliedToBooks: List<String> = emptyList(),
    val createdAt: String,
    val updatedAt: String,
)