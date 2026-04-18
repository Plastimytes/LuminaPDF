package com.luminapdf.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a PDF file stored in the user's library.
 *
 * [uri] is the content URI string (from SAF / file picker) used as the stable identifier.
 * [filePath] may differ on reinstall; URI is the source of truth for Android 13+.
 */
@Entity(tableName = "pdf_documents")
data class PdfDocument(
    @PrimaryKey
    val uri: String,                        // Content URI (stable across sessions)
    val fileName: String,                   // Display name, e.g. "My Book.pdf"
    val filePath: String = "",              // Absolute path if available (legacy)
    val totalPages: Int = 0,                // Populated after first open
    val lastReadPage: Int = 0,              // 0-indexed page number
    val readingProgress: Float = 0f,        // 0.0 – 1.0
    val lastAccessedAt: Long = System.currentTimeMillis(),
    val addedAt: Long = System.currentTimeMillis(),
    val totalReadingTimeMs: Long = 0L,      // Accumulated reading time in ms
    val bookmarks: String = "[]",           // JSON array of BookmarkEntry (serialised)
    val thumbnailPath: String = "",         // Cached thumbnail file path
    val fileSize: Long = 0L                 // Bytes
)
