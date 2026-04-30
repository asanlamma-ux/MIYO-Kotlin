package com.miyu.reader.data.local.dao

import androidx.room.*
import com.miyu.reader.data.local.entity.DictionaryEntity
import com.miyu.reader.data.local.entity.DictionaryEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DictionaryDao {
    @Query("SELECT * FROM dictionaries ORDER BY downloadedAt DESC")
    fun getAllDictionaries(): Flow<List<DictionaryEntity>>

    @Query("SELECT * FROM dictionaries WHERE id = :dictId")
    suspend fun getDictionary(dictId: String): DictionaryEntity?

    @Query("SELECT * FROM dictionary_entries WHERE dictionaryId = :dictId AND term = :term")
    suspend fun lookupEntry(dictId: String, term: String): List<DictionaryEntryEntity>

    @Query("SELECT * FROM dictionary_entries WHERE term LIKE '%' || :query || '%' AND dictionaryId IN (:dictIds) LIMIT 50")
    suspend fun searchEntries(query: String, dictIds: List<String>): List<DictionaryEntryEntity>

    @Upsert
    suspend fun upsertDictionary(dict: DictionaryEntity)

    @Delete
    suspend fun deleteDictionary(dict: DictionaryEntity)

    @Upsert
    suspend fun upsertEntries(entries: List<DictionaryEntryEntity>)

    @Query("DELETE FROM dictionary_entries WHERE dictionaryId = :dictId")
    suspend fun deleteEntries(dictId: String)
}