package com.miyu.reader.data.local.dao

import androidx.room.*
import com.miyu.reader.data.local.entity.TermEntity
import com.miyu.reader.data.local.entity.TermGroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TermDao {
    @Query("SELECT * FROM term_groups ORDER BY updatedAt DESC")
    fun getAllGroups(): Flow<List<TermGroupEntity>>

    @Query("SELECT * FROM term_groups WHERE id = :groupId")
    suspend fun getGroupById(groupId: String): TermGroupEntity?

    @Query("SELECT * FROM terms WHERE groupId = :groupId ORDER BY originalText")
    fun getTermsForGroup(groupId: String): Flow<List<TermEntity>>

    @Query("SELECT * FROM terms WHERE originalText = :originalText AND groupId IN (SELECT id FROM term_groups WHERE :bookId IN (SELECT value FROM json_each(appliedToBooks)))")
    suspend fun findTermsForBook(originalText: String, bookId: String): List<TermEntity>

    @Upsert
    suspend fun upsertGroup(group: TermGroupEntity)

    @Delete
    suspend fun deleteGroup(group: TermGroupEntity)

    @Upsert
    suspend fun upsertTerm(term: TermEntity)

    @Upsert
    suspend fun upsertTerms(terms: List<TermEntity>)

    @Delete
    suspend fun deleteTerm(term: TermEntity)

    @Query("DELETE FROM terms WHERE groupId = :groupId")
    suspend fun deleteTermsForGroup(groupId: String)
}