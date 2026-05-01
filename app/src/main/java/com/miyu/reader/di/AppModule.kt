package com.miyu.reader.di

import android.content.Context
import androidx.room.Room
import com.miyu.reader.BuildConfig
import com.miyu.reader.data.local.MIYUDatabase
import com.miyu.reader.data.local.dao.BookDao
import com.miyu.reader.data.local.dao.DictionaryDao
import com.miyu.reader.data.local.dao.TermDao
import com.miyu.reader.engine.bridge.EpubEngineBridge
import com.tencent.mmkv.MMKV
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    // Room DB
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MIYUDatabase =
        Room.databaseBuilder(context, MIYUDatabase::class.java, "miyu.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideBookDao(db: MIYUDatabase): BookDao = db.bookDao()
    @Provides fun provideTermDao(db: MIYUDatabase): TermDao = db.termDao()
    @Provides fun provideDictionaryDao(db: MIYUDatabase): DictionaryDao = db.dictionaryDao()

    // MMKV
    @Provides
    @Singleton
    fun provideMMKV(): MMKV = MMKV.defaultMMKV()

    // Supabase
    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL.ifBlank { "https://disabled.supabase.co" },
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY.ifBlank { "disabled-anon-key" },
    ) {
        install(Postgrest)
        install(Auth)
        install(Storage)
    }

    // Native engine
    @Provides
    @Singleton
    fun provideEpubEngineBridge(): EpubEngineBridge = EpubEngineBridge()
}
