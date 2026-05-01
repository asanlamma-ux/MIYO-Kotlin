package com.miyu.reader.data.repository

import com.miyu.reader.data.local.dao.TermDao
import com.miyu.reader.data.local.entity.TermEntity
import com.miyu.reader.data.local.entity.TermGroupEntity
import com.miyu.reader.data.toDomain
import com.miyu.reader.data.toEntity
import com.miyu.reader.domain.model.Term
import com.miyu.reader.domain.model.TermGroup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TermRepository @Inject constructor(
    private val termDao: TermDao,
) {
    fun getAllGroups(): Flow<List<TermGroup>> =
        combine(termDao.getAllGroups(), termDao.getAllTerms()) { groups, terms ->
            groups.map { group ->
                group.toDomain(terms.filter { it.groupId == group.id }.map { it.toDomain() })
            }
        }

    suspend fun getGroup(groupId: String): TermGroup? {
        val group = termDao.getGroupById(groupId) ?: return null
        val terms = termDao.getTermsForGroup(groupId).firstOrNull() ?: emptyList()
        return group.toDomain(terms.map { it.toDomain() })
    }

    suspend fun getAllGroupsOnce(): List<TermGroup> {
        val groups = termDao.getAllGroups().firstOrNull() ?: emptyList()
        val terms = termDao.getAllTermsOnce()
        return groups.map { group ->
            group.toDomain(terms.filter { it.groupId == group.id }.map { it.toDomain() })
        }
    }

    fun getTermsForGroup(groupId: String): Flow<List<Term>> =
        termDao.getTermsForGroup(groupId).map { entities ->
            entities.map { it.toDomain() }
        }

    suspend fun getGroupsForBook(bookId: String): List<TermGroup> {
        val groups = termDao.getAllGroups().firstOrNull() ?: emptyList()
        val terms = termDao.getAllTermsOnce()
        return groups
            .filter { bookId in it.appliedToBooks }
            .map { group ->
                group.toDomain(terms.filter { it.groupId == group.id }.map { it.toDomain() })
            }
    }

    suspend fun getReplacementMapForBook(bookId: String): Map<String, String> =
        getGroupsForBook(bookId)
            .flatMap { it.terms }
            .filter { it.originalText.isNotBlank() && it.correctedText.isNotBlank() }
            .associate { it.originalText to it.correctedText }

    suspend fun getReplacementMarkupForBook(bookId: String): Map<String, String> =
        getGroupsForBook(bookId)
            .flatMap { group -> group.terms.map { term -> group to term } }
            .filter { (_, term) -> term.originalText.isNotBlank() && term.correctedText.isNotBlank() }
            .sortedByDescending { (_, term) -> term.originalText.length }
            .associate { (group, term) ->
                term.originalText to buildTermMarkup(group.name, term)
            }

    suspend fun saveGroup(group: TermGroup) {
        termDao.upsertGroup(TermGroupEntity(
            id = group.id,
            name = group.name.sanitizePlainText(MAX_GROUP_NAME_CHARS),
            description = group.description?.sanitizePlainText(MAX_CONTEXT_CHARS)?.takeIf { it.isNotBlank() },
            appliedToBooks = group.appliedToBooks,
            createdAt = group.createdAt,
            updatedAt = java.time.Instant.now().toString(),
        ))
        termDao.deleteTermsForGroup(group.id)
        termDao.upsertTerms(group.terms.map { it.sanitizedForStorage().toEntity(group.id) })
    }

    suspend fun deleteGroup(groupId: String) {
        termDao.deleteTermsForGroup(groupId)
        termDao.deleteGroupById(groupId)
    }

    suspend fun addTermToGroup(groupId: String, term: Term) {
        termDao.upsertTerm(term.sanitizedForStorage().toEntity(groupId))
    }

    suspend fun addTermToGroupAndApplyToBook(groupId: String, term: Term, bookId: String?) {
        val group = termDao.getGroupById(groupId) ?: return
        val appliedBooks = if (bookId != null && bookId !in group.appliedToBooks) {
            group.appliedToBooks + bookId
        } else {
            group.appliedToBooks
        }
        termDao.upsertGroup(group.copy(appliedToBooks = appliedBooks, updatedAt = java.time.Instant.now().toString()))
        termDao.upsertTerm(term.sanitizedForStorage().toEntity(groupId))
    }

    suspend fun applyGroupToBook(groupId: String, bookId: String) {
        val group = termDao.getGroupById(groupId) ?: return
        if (bookId in group.appliedToBooks) return
        termDao.upsertGroup(group.copy(appliedToBooks = group.appliedToBooks + bookId, updatedAt = java.time.Instant.now().toString()))
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

    private fun buildTermMarkup(groupName: String, term: Term): String {
        val safeTerm = term.sanitizedForStorage()
        val original = safeTerm.originalText.escapeHtml()
        val corrected = safeTerm.correctedText.escapeHtml()
        val translation = safeTerm.translationText.orEmpty().escapeHtml()
        val context = safeTerm.context.orEmpty().escapeHtml()
        val imageUri = safeTerm.imageUri.orEmpty().escapeHtml()
        val group = groupName.sanitizePlainText(MAX_GROUP_NAME_CHARS).escapeHtml()
        return "<span class=\"miyu-term\" data-original=\"$original\" data-corrected=\"$corrected\" data-translation=\"$translation\" data-context=\"$context\" data-image-uri=\"$imageUri\" data-group=\"$group\">$corrected</span>"
    }

    private fun Term.sanitizedForStorage(): Term =
        copy(
            originalText = originalText.sanitizePlainText(MAX_TERM_TEXT_CHARS),
            correctedText = correctedText.sanitizePlainText(MAX_TERM_TEXT_CHARS),
            translationText = translationText?.sanitizePlainText(MAX_TERM_TEXT_CHARS)?.takeIf { it.isNotBlank() },
            context = context?.sanitizePlainText(MAX_CONTEXT_CHARS)?.takeIf { it.isNotBlank() },
            imageUri = imageUri?.sanitizePlainText(MAX_URI_CHARS)?.takeIf { uri ->
                uri.isNotBlank() &&
                    (uri.startsWith("content://", ignoreCase = true) || uri.startsWith("file://", ignoreCase = true))
            },
        )

    private fun String.sanitizePlainText(maxChars: Int): String =
        replace(Regex("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F\\u007F]"), "")
            .trim()
            .take(maxChars)

    private fun String.escapeHtml(): String = buildString {
        this@escapeHtml.forEach { char ->
            when (char) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&#39;")
                else -> append(char)
            }
        }
    }

    private companion object {
        const val MAX_GROUP_NAME_CHARS = 120
        const val MAX_TERM_TEXT_CHARS = 8_000
        const val MAX_CONTEXT_CHARS = 12_000
        const val MAX_URI_CHARS = 2_000
    }
}
