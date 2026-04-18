package com.luminapdf.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.luminapdf.data.local.dao.PdfDocumentDao
import com.luminapdf.data.local.entity.PdfDocument

@Database(
    entities = [PdfDocument::class],
    version = 1,
    exportSchema = false
)
abstract class LuminaDatabase : RoomDatabase() {
    abstract fun pdfDocumentDao(): PdfDocumentDao
}
