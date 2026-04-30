package com.miyu.reader.domain.model

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class Term(
    val id: String,
    val originalText: String,
    val translationText: String? = null,
    val correctedText: String,
    val context: String? = null,
    val imageUri: String? = null,
    val createdAt: String,
    val updatedAt: String? = null,
)

@Keep
@Serializable
data class TermGroup(
    val id: String,
    val name: String,
    val description: String? = null,
    val terms: List<Term> = emptyList(),
    val appliedToBooks: List<String> = emptyList(),
    val createdAt: String,
    val updatedAt: String,
)

@Keep
@Serializable
data class CommunityTermGroup(
    val id: String,
    val name: String,
    val description: String? = null,
    val terms: List<Term> = emptyList(),
    val appliedToBooks: List<String> = emptyList(),
    val createdAt: String,
    val updatedAt: String,
    val createdBy: String,
    val downloads: Int = 0,
    val tags: List<String> = emptyList(),
    val isOfficial: Boolean = false,
)