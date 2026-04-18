package com.luminapdf.data.local.dao

import androidx.room.*
import com.luminapdf.data.local.entity.PdfDocument
import kotlinx.coroutines.flow.Flow

@Dao
interface PdfDocumentDao {

    // ── Queries ──────────────────────────────────────────────────────────────

    @Query("SELECT * FROM pdf_documents ORDER BY lastAccessedAt DESC")
    fun getAllDocumentsFlow(): Flow<List<PdfDocument>>

    @Query("SELECT * FROM pdf_documents WHERE uri = :uri LIMIT 1")
    suspend fun getDocumentByUri(uri: String): PdfDocument?

    @Query("SELECT * FROM pdf_documents ORDER BY lastAccessedAt DESC LIMIT 1")
    suspend fun getMostRecentDocument(): PdfDocument?

    // ── Inserts / Upserts ────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDocument(document: PdfDocument)

    @Upsert
    suspend fun upsertDocument(document: PdfDocument)

    // ── Updates ──────────────────────────────────────────────────────────────

    @Query("""
        UPDATE pdf_documents
        SET lastReadPage = :page,
            readingProgress = :progress,
            lastAccessedAt = :timestamp
        WHERE uri = :uri
    """)
    suspend fun updateReadingProgress(
        uri: String,
        page: Int,
        progress: Float,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("""
        UPDATE pdf_documents
        SET totalPages = :totalPages
        WHERE uri = :uri
    """)
    suspend fun updateTotalPages(uri: String, totalPages: Int)

    @Query("""
        UPDATE pdf_documents
        SET totalReadingTimeMs = totalReadingTimeMs + :deltaMs
        WHERE uri = :uri
    """)
    suspend fun addReadingTime(uri: String, deltaMs: Long)

    @Query("""
        UPDATE pdf_documents
        SET bookmarks = :bookmarksJson
        WHERE uri = :uri
    """)
    suspend fun updateBookmarks(uri: String, bookmarksJson: String)

    @Query("""
        UPDATE pdf_documents
        SET thumbnailPath = :path
        WHERE uri = :uri
    """)
    suspend fun updateThumbnailPath(uri: String, path: String)

    // ── Deletes ──────────────────────────────────────────────────────────────

    @Query("DELETE FROM pdf_documents WHERE uri = :uri")
    suspend fun deleteDocument(uri: String)

    @Query("DELETE FROM pdf_documents")
    suspend fun deleteAll()
}
