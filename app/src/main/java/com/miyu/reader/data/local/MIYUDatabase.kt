package com.miyu.reader.data.local

import androidx.annotation.Keep
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.miyu.reader.data.local.converter.Converters
import com.miyu.reader.data.local.dao.BookDao
import com.miyu.reader.data.local.dao.DictionaryDao
import com.miyu.reader.data.local.dao.TermDao
import com.miyu.reader.data.local.entity.*

@Keep
@Database(
    entities = [
        BookEntity::class,
        BookmarkEntity::class,
        HighlightEntity::class,
        ReadingPositionEntity::class,
        TermEntity::class,
        TermGroupEntity::class,
        DictionaryEntity::class,
        DictionaryEntryEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class MIYUDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun termDao(): TermDao
    abstract fun dictionaryDao(): DictionaryDao
}
