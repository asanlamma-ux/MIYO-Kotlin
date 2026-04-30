package com.miyu.reader.data.repository

import com.miyu.reader.data.local.dao.TermDao
import com.miyu.reader.data.local.entity.TermEntity
import com.miyu.reader.data.local.entity.TermGroupEntity
import com.miyu.reader.data.toDomain
import com.miyu.reader.data.toEntity
import com.miyu.reader.domain.model.Term
import com.miyu.reader.domain.model.TermGroup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TermRepository @Inject constructor(
    private val termDao: TermDao,
) {
    fun getAllGroups(): Flow<List<TermGroup>> = termDao.getAllGroups().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun getGroup(groupId: String): TermGroup? {
        val group = termDao.getGroupById(groupId) ?: return null
        val terms = termDao.getTermsForGroup(groupId).firstOrNull() ?: emptyList()
        return group.toDomain(terms.map { it.toDomain() })
    }

    fun getTermsForGroup(groupId: String): Flow<List<Term>> =
        termDao.getTermsForGroup(groupId).map { entities ->
            entities.map { it.toDomain() }
        }

    suspend fun findTermsForBook(text: String, bookId: String): List<Term> =
        termDao.findTermsForBook(text, bookId).map { it.toDomain() }

    suspend fun saveGroup(group: TermGroup) {
        termDao.upsertGroup(TermGroupEntity(
            id = group.id,
            name = group.name,
            description = group.description,
            appliedToBooks = group.appliedToBooks,
            createdAt = group.createdAt,
            updatedAt = java.time.Instant.now().toString(),
        ))
        termDao.deleteTermsForGroup(group.id)
        termDao.upsertTerms(group.terms.map { it.toEntity(group.id) })
    }

    suspend fun deleteGroup(groupId: String) {
        termDao.deleteTermsForGroup(groupId)
        termDao.deleteGroup(TermGroupEntity(
            id = groupId,
            name = "",
            description = null,
            appliedToBooks = emptyList(),
            createdAt = "",
            updatedAt = "",
        ))
    }

    suspend fun addTermToGroup(groupId: String, term: Term) {
        termDao.upsertTerm(term.toEntity(groupId))
    }

    suspend fun removeTerm(term: Term) {
        termDao.deleteTerm(TermEntity(
            id = term.id,
            originalText = term.originalText,
            translationText = term.translationText,
            correctedText = term.correctedText,
            context = term.context,
            imageUri = term.imageUri,
            createdAt = term.createdAt,
            updatedAt = term.updatedAt,
            groupId = "",
        ))
    }
}