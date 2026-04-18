package com.luminapdf.di

import android.content.Context
import androidx.room.Room
import com.luminapdf.data.local.LuminaDatabase
import com.luminapdf.data.local.dao.PdfDocumentDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LuminaDatabase =
        Room.databaseBuilder(
            context,
            LuminaDatabase::class.java,
            "lumina_db"
        )
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun providePdfDocumentDao(db: LuminaDatabase): PdfDocumentDao = db.pdfDocumentDao()
}
