package com.luminapdf.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luminapdf.data.local.AppPreferences
import com.luminapdf.data.local.entity.BookmarkEntry
import com.luminapdf.data.local.entity.PdfDocument
import com.luminapdf.data.repository.PdfRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.Locale
import javax.inject.Inject

data class ReaderUiState(
    val document: PdfDocument? = null,
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val isDarkMode: Boolean = false,
    val blueLightFilter: Float = 0f,
    val isDrawerOpen: Boolean = false,
    val bookmarks: List<BookmarkEntry> = emptyList(),
    val isTtsPlaying: Boolean = false,
    val ttsReady: Boolean = false,
    val sessionReadingTimeMs: Long = 0L,
    val showFocusMode: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class ReaderViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: PdfRepository,
    private val preferences: AppPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _state.asStateFlow()

    // ── TTS Engine ───────────────────────────────────────────────────────────
    private var tts: TextToSpeech? = null
    private var pdfRenderer: PdfRenderer? = null

    // ── Session Timer ────────────────────────────────────────────────────────
    private var sessionStartMs: Long = 0L
    private var timerJob: Job? = null

    // ── Combine prefs ────────────────────────────────────────────────────────
    init {
        viewModelScope.launch {
            combine(
                preferences.isDarkMode,
                preferences.blueLightFilter
            ) { dark, blue -> Pair(dark, blue) }
                .collect { (dark, blue) ->
                    _state.update { it.copy(isDarkMode = dark, blueLightFilter = blue) }
                }
        }
        initTts()
    }

    // ── Public API ───────────────────────────────────────────────────────────

    fun loadDocument(uriString: String) = viewModelScope.launch {
        _state.update { it.copy(isLoading = true, error = null) }
        try {
            val doc = repository.getDocument(uriString) ?: run {
                _state.update { it.copy(isLoading = false, error = "Document not found") }
                return@launch
            }
            val bookmarks = repository.parseBookmarks(doc.bookmarks)
            _state.update {
                it.copy(
                    document = doc,
                    currentPage = doc.lastReadPage,
                    totalPages = doc.totalPages,
                    bookmarks = bookmarks,
                    isLoading = false
                )
            }
            startTimer()
        } catch (e: Exception) {
            _state.update { it.copy(isLoading = false, error = e.message) }
        }
    }

    fun onPageChanged(page: Int, totalPages: Int) {
        _state.update { it.copy(currentPage = page, totalPages = totalPages) }
        viewModelScope.launch {
            val uri = _state.value.document?.uri ?: return@launch
            repository.saveProgress(uri, page, totalPages)
            if (_state.value.document?.totalPages != totalPages) {
                repository.saveTotalPages(uri, totalPages)
                _state.update { it.copy(document = it.document?.copy(totalPages = totalPages)) }
            }
        }
    }

    fun toggleDarkMode() = viewModelScope.launch {
        preferences.setDarkMode(!_state.value.isDarkMode)
    }

    fun setBlueLightFilter(value: Float) = viewModelScope.launch {
        preferences.setBlueLightFilter(value)
    }

    fun toggleDrawer() = _state.update { it.copy(isDrawerOpen = !it.isDrawerOpen) }

    fun closeDrawer() = _state.update { it.copy(isDrawerOpen = false) }

    fun addBookmark(label: String) = viewModelScope.launch {
        val uri = _state.value.document?.uri ?: return@launch
        repository.addBookmark(uri, _state.value.currentPage, label)
        refreshBookmarks(uri)
    }

    fun removeBookmark(id: String) = viewModelScope.launch {
        val uri = _state.value.document?.uri ?: return@launch
        repository.removeBookmark(uri, id)
        refreshBookmarks(uri)
    }

    fun jumpToPage(page: Int) = _state.update { it.copy(currentPage = page) }

    fun toggleFocusMode() = _state.update { it.copy(showFocusMode = !it.showFocusMode) }

    // ── TTS ──────────────────────────────────────────────────────────────────

    fun speakCurrentPage(pageText: String) {
        if (!_state.value.ttsReady) return
        if (_state.value.isTtsPlaying) {
            tts?.stop()
            _state.update { it.copy(isTtsPlaying = false) }
            return
        }
        val utteranceId = "page_${_state.value.currentPage}"
        tts?.speak(pageText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        _state.update { it.copy(isTtsPlaying = true) }
    }

    fun stopTts() {
        tts?.stop()
        _state.update { it.copy(isTtsPlaying = false) }
    }

    // ── Timer ────────────────────────────────────────────────────────────────

    fun pauseTimer() {
        val elapsed = System.currentTimeMillis() - sessionStartMs
        timerJob?.cancel()
        viewModelScope.launch {
            _state.value.document?.uri?.let { repository.addReadingTime(it, elapsed) }
        }
    }

    fun resumeTimer() { startTimer() }

    // ── Private ──────────────────────────────────────────────────────────────

    private fun startTimer() {
        sessionStartMs = System.currentTimeMillis()
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1_000)
                _state.update {
                    it.copy(sessionReadingTimeMs = System.currentTimeMillis() - sessionStartMs)
                }
            }
        }
    }

    private suspend fun refreshBookmarks(uri: String) {
        val doc = repository.getDocument(uri)
        val bookmarks = repository.parseBookmarks(doc?.bookmarks ?: "[]")
        _state.update { it.copy(bookmarks = bookmarks) }
    }

    private fun initTts() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        _state.update { it.copy(isTtsPlaying = false) }
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _state.update { it.copy(isTtsPlaying = false) }
                    }
                })
                _state.update { it.copy(ttsReady = true) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pauseTimer()
        tts?.shutdown()
        pdfRenderer?.close()
    }
}
