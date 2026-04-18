package com.luminapdf.viewmodel

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luminapdf.data.local.AppPreferences
import com.luminapdf.data.local.entity.PdfDocument
import com.luminapdf.data.repository.PdfRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryUiState(
    val documents: List<PdfDocument> = emptyList(),
    val isLoading: Boolean = true,
    val isDarkMode: Boolean = false,
    val gridColumns: Int = 2,
    val sortOrder: String = "recent",
    val searchQuery: String = ""
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: PdfRepository,
    private val preferences: AppPreferences
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<LibraryUiState> = combine(
        repository.getAllDocuments(),
        preferences.isDarkMode,
        preferences.gridColumns,
        preferences.sortOrder,
        _searchQuery
    ) { docs, dark, cols, sort, query ->
        val filtered = if (query.isBlank()) docs
                       else docs.filter { it.fileName.contains(query, ignoreCase = true) }
        val sorted = when (sort) {
            "name" -> filtered.sortedBy { it.fileName }
            else   -> filtered.sortedByDescending { it.lastAccessedAt }
        }
        LibraryUiState(
            documents = sorted,
            isLoading = false,
            isDarkMode = dark,
            gridColumns = cols,
            sortOrder = sort,
            searchQuery = query
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LibraryUiState()
    )

    // ── Actions ──────────────────────────────────────────────────────────────

    fun onPdfPicked(uri: Uri) = viewModelScope.launch {
        val (name, size) = resolveUriMeta(uri)
        repository.addOrUpdateDocument(uri, name, size)
    }

    fun removeDocument(uri: String) = viewModelScope.launch {
        repository.removeDocument(uri)
    }

    fun toggleDarkMode() = viewModelScope.launch {
        preferences.setDarkMode(!uiState.value.isDarkMode)
    }

    fun setGridColumns(cols: Int) = viewModelScope.launch {
        preferences.setGridColumns(cols)
    }

    fun setSortOrder(order: String) = viewModelScope.launch {
        preferences.setSortOrder(order)
    }

    fun setSearchQuery(q: String) { _searchQuery.value = q }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun resolveUriMeta(uri: Uri): Pair<String, Long> {
        var name = "Unknown.pdf"
        var size = 0L
        runCatching {
            val cursor: Cursor? = context.contentResolver.query(
                uri, null, null, null, null
            )
            cursor?.use { c ->
                if (c.moveToFirst()) {
                    val nameIdx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = c.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx >= 0) name = c.getString(nameIdx) ?: name
                    if (sizeIdx >= 0) size = c.getLong(sizeIdx)
                }
            }
        }
        return Pair(name, size)
    }
}
