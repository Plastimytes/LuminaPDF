package com.luminapdf.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.luminapdf.data.local.dao.PdfDocumentDao
import com.luminapdf.data.local.entity.BookmarkEntry
import com.luminapdf.data.local.entity.PdfDocument
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: PdfDocumentDao
) {

    // ── Library ──────────────────────────────────────────────────────────────

    fun getAllDocuments(): Flow<List<PdfDocument>> = dao.getAllDocumentsFlow()

    suspend fun getDocument(uri: String): PdfDocument? = dao.getDocumentByUri(uri)

    /**
     * Called when the user picks a PDF via SAF. Persists a minimal record
     * and kicks off thumbnail generation.
     */
    suspend fun addOrUpdateDocument(uri: Uri, displayName: String, fileSize: Long = 0L) {
        val uriStr = uri.toString()
        val existing = dao.getDocumentByUri(uriStr)
        if (existing == null) {
            dao.insertDocument(
                PdfDocument(
                    uri = uriStr,
                    fileName = displayName,
                    fileSize = fileSize
                )
            )
            // Generate thumbnail in background
            generateAndSaveThumbnail(uri, uriStr)
        } else {
            // Re-access updates the timestamp
            dao.updateReadingProgress(uriStr, existing.lastReadPage, existing.readingProgress)
        }
    }

    suspend fun removeDocument(uri: String) = dao.deleteDocument(uri)

    // ── Reading Progress ─────────────────────────────────────────────────────

    suspend fun saveProgress(uri: String, page: Int, totalPages: Int) {
        val progress = if (totalPages > 0) page.toFloat() / totalPages else 0f
        dao.updateReadingProgress(uri, page, progress)
    }

    suspend fun saveTotalPages(uri: String, totalPages: Int) =
        dao.updateTotalPages(uri, totalPages)

    suspend fun addReadingTime(uri: String, deltaMs: Long) =
        dao.addReadingTime(uri, deltaMs)

    // ── Bookmarks ────────────────────────────────────────────────────────────

    suspend fun addBookmark(uri: String, pageIndex: Int, label: String) {
        val doc = dao.getDocumentByUri(uri) ?: return
        val current = parseBookmarks(doc.bookmarks).toMutableList()
        current.add(
            BookmarkEntry(
                id = UUID.randomUUID().toString(),
                pageIndex = pageIndex,
                label = label
            )
        )
        dao.updateBookmarks(uri, Json.encodeToString(current))
    }

    suspend fun removeBookmark(uri: String, bookmarkId: String) {
        val doc = dao.getDocumentByUri(uri) ?: return
        val updated = parseBookmarks(doc.bookmarks).filter { it.id != bookmarkId }
        dao.updateBookmarks(uri, Json.encodeToString(updated))
    }

    fun parseBookmarks(json: String): List<BookmarkEntry> = runCatching {
        Json.decodeFromString<List<BookmarkEntry>>(json)
    }.getOrDefault(emptyList())

    // ── Thumbnail Generation ─────────────────────────────────────────────────

    /**
     * Renders the first page of a PDF at thumbnail resolution (240×320) and
     * saves it as a JPEG in the app's private cache directory.
     *
     * Uses Android's built-in [PdfRenderer] — no native library needed here.
     */
    suspend fun generateAndSaveThumbnail(pdfUri: Uri, uriStr: String): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val pfd: ParcelFileDescriptor =
                    context.contentResolver.openFileDescriptor(pdfUri, "r") ?: return@runCatching null

                pfd.use { descriptor ->
                    val renderer = PdfRenderer(descriptor)
                    renderer.use { r ->
                        if (r.pageCount == 0) return@runCatching null
                        val page = r.openPage(0)
                        page.use { p ->
                            val thumbWidth = 240
                            val thumbHeight = (thumbWidth * p.height.toFloat() / p.width).toInt()
                            val bitmap = Bitmap.createBitmap(
                                thumbWidth, thumbHeight, Bitmap.Config.ARGB_8888
                            )
                            p.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                            // Save to cache
                            val cacheDir = File(context.cacheDir, "thumbnails").apply { mkdirs() }
                            val outFile = File(cacheDir, "${uriStr.hashCode()}.jpg")
                            FileOutputStream(outFile).use { out ->
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
                            }
                            bitmap.recycle()

                            dao.updateThumbnailPath(uriStr, outFile.absolutePath)
                            outFile.absolutePath
                        }
                    }
                }
            }.getOrNull()
        }

    /**
     * Opens a PdfRenderer for the given URI.
     * The caller is responsible for closing the returned renderer and its PFD.
     */
    fun openPdfRenderer(uri: Uri): Pair<PdfRenderer, ParcelFileDescriptor>? = runCatching {
        val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
        Pair(PdfRenderer(pfd), pfd)
    }.getOrNull()
}
