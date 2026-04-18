package com.luminapdf.ui.screens.reader

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luminapdf.data.local.entity.BookmarkEntry
import com.luminapdf.ui.components.formatReadingTime
import com.luminapdf.ui.theme.LuminaPDFTheme
import com.luminapdf.viewmodel.ReaderViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    uriString: String,
    onNavigateBack: () -> Unit,
    viewModel: ReaderViewModel = hiltViewModel()
) {
    val state    by viewModel.uiState.collectAsStateWithLifecycle()
    val context  = LocalContext.current
    val scope    = rememberCoroutineScope()

    // Bookmark dialog state
    var showBookmarkDialog by remember { mutableStateOf(false) }
    var bookmarkLabel      by remember { mutableStateOf("") }

    // Blue light overlay colour derived from preference
    val blueLightColor = remember(state.blueLightFilter) {
        Color(0xFFFF8C00).copy(alpha = state.blueLightFilter * 0.4f)
    }

    // Focus mode auto-hide after 3 s of inactivity
    var lastInteraction by remember { mutableStateOf(System.currentTimeMillis()) }
    val uiVisible by remember {
        derivedStateOf { !state.showFocusMode }
    }

    LaunchedEffect(uriString) { viewModel.loadDocument(uriString) }

    // Lifecycle: pause/resume timer
    DisposableEffect(Unit) {
        onDispose { viewModel.pauseTimer() }
    }

    LuminaPDFTheme(darkTheme = state.isDarkMode) {
        val drawerState = rememberDrawerState(
            if (state.isDrawerOpen) DrawerValue.Open else DrawerValue.Closed
        )
        LaunchedEffect(state.isDrawerOpen) {
            if (state.isDrawerOpen) drawerState.open() else drawerState.close()
        }
        LaunchedEffect(drawerState.currentValue) {
            if (drawerState.currentValue == DrawerValue.Closed) viewModel.closeDrawer()
        }

        ModalNavigationDrawer(
            drawerState   = drawerState,
            drawerContent = {
                BookmarkDrawer(
                    bookmarks      = state.bookmarks,
                    currentPage    = state.currentPage,
                    onJumpToPage   = { viewModel.jumpToPage(it); viewModel.closeDrawer() },
                    onAddBookmark  = { showBookmarkDialog = true },
                    onRemove       = { viewModel.removeBookmark(it) }
                )
            }
        ) {
            Scaffold(
                topBar = {
                    AnimatedVisibility(
                        visible = uiVisible,
                        enter   = slideInVertically() + fadeIn(),
                        exit    = slideOutVertically() + fadeOut()
                    ) {
                        ReaderTopBar(
                            fileName       = state.document?.fileName ?: "",
                            currentPage    = state.currentPage,
                            totalPages     = state.totalPages,
                            isDarkMode     = state.isDarkMode,
                            sessionTime    = state.sessionReadingTimeMs,
                            isTtsPlaying   = state.isTtsPlaying,
                            ttsReady       = state.ttsReady,
                            blueLightFilter = state.blueLightFilter,
                            onBack         = onNavigateBack,
                            onToggleDark   = viewModel::toggleDarkMode,
                            onToggleTts    = { /* triggered from FAB area */ },
                            onOpenDrawer   = viewModel::toggleDrawer,
                            onBlueLightChange = viewModel::setBlueLightFilter
                        )
                    }
                },
                floatingActionButton = {
                    AnimatedVisibility(
                        visible = uiVisible,
                        enter   = scaleIn(),
                        exit    = scaleOut()
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            // TTS button
                            if (state.ttsReady) {
                                SmallFloatingActionButton(
                                    onClick          = {
                                        // In real usage, extract text via PDFBox or pass rendered text
                                        viewModel.speakCurrentPage("Page ${state.currentPage + 1}")
                                    },
                                    containerColor   = if (state.isTtsPlaying)
                                        MaterialTheme.colorScheme.tertiary
                                        else MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Icon(
                                        if (state.isTtsPlaying) Icons.Default.VolumeOff
                                        else Icons.Default.VolumeUp,
                                        contentDescription = "TTS"
                                    )
                                }
                            }
                            // Bookmark FAB
                            SmallFloatingActionButton(
                                onClick        = { showBookmarkDialog = true },
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Icon(Icons.Outlined.Bookmark, contentDescription = "Bookmark")
                            }
                        }
                    }
                }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(MaterialTheme.colorScheme.background)
                        .pointerInput(Unit) {
                            detectTapGestures { lastInteraction = System.currentTimeMillis() }
                        }
                ) {
                    when {
                        state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                        state.error != null -> ErrorMessage(
                            message = state.error!!,
                            onBack  = onNavigateBack
                        )
                        else -> PdfPageViewer(
                            uriString   = uriString,
                            startPage   = state.currentPage,
                            isDarkMode  = state.isDarkMode,
                            onPageChange = { page, total ->
                                viewModel.onPageChanged(page, total)
                                lastInteraction = System.currentTimeMillis()
                            }
                        )
                    }

                    // Blue light filter overlay
                    if (state.blueLightFilter > 0.01f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(blueLightColor)
                        )
                    }
                }
            }
        }

        // Bookmark dialog
        if (showBookmarkDialog) {
            AlertDialog(
                onDismissRequest = { showBookmarkDialog = false; bookmarkLabel = "" },
                title   = { Text("Add Bookmark") },
                text    = {
                    Column {
                        Text("Page ${state.currentPage + 1}", style = MaterialTheme.typography.bodySmall,
                             color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value         = bookmarkLabel,
                            onValueChange = { bookmarkLabel = it },
                            placeholder   = { Text("Label (optional)") },
                            singleLine    = true
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.addBookmark(
                            if (bookmarkLabel.isBlank()) "Page ${state.currentPage + 1}"
                            else bookmarkLabel
                        )
                        showBookmarkDialog = false
                        bookmarkLabel = ""
                    }) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = { showBookmarkDialog = false; bookmarkLabel = "" }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

// ── Reader Top Bar ────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderTopBar(
    fileName: String,
    currentPage: Int,
    totalPages: Int,
    isDarkMode: Boolean,
    sessionTime: Long,
    isTtsPlaying: Boolean,
    ttsReady: Boolean,
    blueLightFilter: Float,
    onBack: () -> Unit,
    onToggleDark: () -> Unit,
    onToggleTts: () -> Unit,
    onOpenDrawer: () -> Unit,
    onBlueLightChange: (Float) -> Unit
) {
    var showBlueLightPanel by remember { mutableStateOf(false) }

    Column {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text      = fileName.removeSuffix(".pdf"),
                        maxLines  = 1,
                        overflow  = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold,
                        fontSize  = 16.sp
                    )
                    if (totalPages > 0) {
                        Text(
                            text  = "Page ${currentPage + 1} of $totalPages  •  ${formatReadingTime(sessionTime)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = { showBlueLightPanel = !showBlueLightPanel }) {
                    Icon(
                        Icons.Outlined.WbIncandescent,
                        contentDescription = "Blue light filter",
                        tint = if (blueLightFilter > 0f) MaterialTheme.colorScheme.tertiary
                               else MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = onToggleDark) {
                    Icon(
                        if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Toggle theme"
                    )
                }
                IconButton(onClick = onOpenDrawer) {
                    Icon(Icons.Outlined.BookmarkBorder, contentDescription = "Bookmarks")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        // Inline blue light filter slider
        AnimatedVisibility(visible = showBlueLightPanel) {
            Surface(
                color    = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier            = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment   = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.WbSunny,
                        contentDescription = null,
                        modifier           = Modifier.size(16.dp),
                        tint               = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value         = blueLightFilter,
                        onValueChange = onBlueLightChange,
                        modifier      = Modifier.weight(1f).padding(horizontal = 8.dp),
                        colors        = SliderDefaults.colors(
                            thumbColor       = Color(0xFFFF8C00),
                            activeTrackColor = Color(0xFFFF8C00)
                        )
                    )
                    Icon(
                        Icons.Outlined.WbIncandescent,
                        contentDescription = null,
                        modifier           = Modifier.size(16.dp),
                        tint               = Color(0xFFFF8C00)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${(blueLightFilter * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Reading progress bar at the very bottom of the top bar
        if (totalPages > 0) {
            LinearProgressIndicator(
                progress   = { (currentPage + 1).toFloat() / totalPages },
                modifier   = Modifier.fillMaxWidth().height(2.dp),
                color      = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}

// ── PDF Page Viewer (Coroutine-based PdfRenderer) ─────────────────────────────

@Composable
fun PdfPageViewer(
    uriString: String,
    startPage: Int,
    isDarkMode: Boolean,
    onPageChange: (Int, Int) -> Unit
) {
    val context     = LocalContext.current
    val scope       = rememberCoroutineScope()
    val lazyState   = rememberLazyListState()
    var totalPages  by remember { mutableStateOf(0) }
    var renderer    by remember { mutableStateOf<PdfRenderer?>(null) }
    var pfd         by remember { mutableStateOf<android.os.ParcelFileDescriptor?>(null) }
    var renderedPages by remember { mutableStateOf<Map<Int, Bitmap>>(emptyMap()) }

    // Open renderer
    LaunchedEffect(uriString) {
        withContext(Dispatchers.IO) {
            runCatching {
                val uri  = Uri.parse(uriString)
                val desc = context.contentResolver.openFileDescriptor(uri, "r") ?: return@runCatching
                val r    = PdfRenderer(desc)
                pfd      = desc
                renderer = r
                totalPages = r.pageCount
                withContext(Dispatchers.Main) {
                    onPageChange(startPage, r.pageCount)
                    if (startPage > 0) lazyState.scrollToItem(startPage)
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            renderer?.close()
            pfd?.close()
        }
    }

    // Track visible page
    LaunchedEffect(lazyState.firstVisibleItemIndex) {
        val page = lazyState.firstVisibleItemIndex
        if (totalPages > 0) onPageChange(page, totalPages)
    }

    if (totalPages == 0) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        state   = lazyState,
        modifier = Modifier.fillMaxSize()
    ) {
        items(totalPages) { pageIndex ->
            PdfPageItem(
                pageIndex  = pageIndex,
                renderer   = renderer,
                isDarkMode = isDarkMode
            )
        }
    }
}

@Composable
private fun PdfPageItem(
    pageIndex: Int,
    renderer: PdfRenderer?,
    isDarkMode: Boolean
) {
    var bitmap by remember(pageIndex) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(pageIndex, isDarkMode) {
        if (renderer == null) return@LaunchedEffect
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                synchronized(renderer) {
                    val page   = renderer.openPage(pageIndex)
                    val width  = page.width  * 2   // 2x density for crisp rendering
                    val height = page.height * 2
                    val bmp    = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()

                    // Dark mode: invert colours with a matrix colour filter
                    if (isDarkMode) applyNightFilter(bmp) else bmp
                }
            }.getOrNull()
        }
    }

    Box(
        modifier           = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .shadow(2.dp, RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp)),
        contentAlignment   = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap             = bitmap!!.asImageBitmap(),
                contentDescription = "Page ${pageIndex + 1}",
                modifier           = Modifier.fillMaxWidth(),
                contentScale       = androidx.compose.ui.layout.ContentScale.FillWidth
            )
        } else {
            Box(
                modifier         = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.77f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            }
        }
    }
}

/**
 * Applies a midnight-friendly colour inversion to the page bitmap.
 * Uses a Canvas + Paint with a [ColorMatrix] that inverts luma
 * but preserves colour hue — easier on the eyes than full inversion.
 */
private fun applyNightFilter(source: Bitmap): Bitmap {
    val output = source.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = android.graphics.Canvas(output)
    val paint  = Paint()

    // Invert: new_val = 255 − old_val for R, G, B; keep A
    val cm = android.graphics.ColorMatrix(floatArrayOf(
        -1f,  0f,  0f, 0f, 255f,
         0f, -1f,  0f, 0f, 255f,
         0f,  0f, -1f, 0f, 255f,
         0f,  0f,  0f, 1f,   0f
    ))
    // Tint slightly warm to match midnight palette
    val warmCm = android.graphics.ColorMatrix(floatArrayOf(
        1.0f, 0f,   0f,   0f, 8f,
        0f,   1.0f, 0f,   0f, 4f,
        0f,   0f,   0.9f, 0f, 0f,
        0f,   0f,   0f,   1f, 0f
    ))
    cm.postConcat(warmCm)
    paint.colorFilter = android.graphics.ColorMatrixColorFilter(cm)
    canvas.drawBitmap(source, 0f, 0f, paint)
    source.recycle()
    return output
}

// ── Bookmark Drawer ───────────────────────────────────────────────────────────

@Composable
private fun BookmarkDrawer(
    bookmarks: List<BookmarkEntry>,
    currentPage: Int,
    onJumpToPage: (Int) -> Unit,
    onAddBookmark: () -> Unit,
    onRemove: (String) -> Unit
) {
    ModalDrawerSheet(
        modifier     = Modifier.width(280.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface
    ) {
        Spacer(Modifier.height(16.dp))
        Row(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment   = Alignment.CenterVertically
        ) {
            Text(
                "Bookmarks",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            FilledTonalIconButton(onClick = onAddBookmark) {
                Icon(Icons.Default.Add, contentDescription = "Add bookmark")
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        if (bookmarks.isEmpty()) {
            Box(
                modifier         = Modifier.fillMaxWidth().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No bookmarks yet.\nTap + to bookmark this page.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            bookmarks.sortedBy { it.pageIndex }.forEach { bm ->
                BookmarkItem(
                    bookmark       = bm,
                    isCurrentPage  = bm.pageIndex == currentPage,
                    onJump         = { onJumpToPage(bm.pageIndex) },
                    onRemove       = { onRemove(bm.id) }
                )
            }
        }
    }
}

@Composable
private fun BookmarkItem(
    bookmark: BookmarkEntry,
    isCurrentPage: Boolean,
    onJump: () -> Unit,
    onRemove: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                bookmark.label,
                fontWeight = if (isCurrentPage) FontWeight.Bold else FontWeight.Normal,
                color      = if (isCurrentPage) MaterialTheme.colorScheme.primary
                             else MaterialTheme.colorScheme.onSurface
            )
        },
        supportingContent = { Text("Page ${bookmark.pageIndex + 1}") },
        leadingContent = {
            Icon(
                Icons.Filled.Bookmark,
                contentDescription = null,
                tint  = if (isCurrentPage) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = "Remove",
                     tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
            }
        },
        modifier = Modifier.clickable(onClick = onJump)
    )
}

// ── Error Message ─────────────────────────────────────────────────────────────

@Composable
private fun ErrorMessage(message: String, onBack: () -> Unit) {
    Column(
        modifier            = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.ErrorOutline,
            contentDescription = null,
            modifier           = Modifier.size(56.dp),
            tint               = MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(16.dp))
        Text("Could not open PDF", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodySmall,
             color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        FilledTonalButton(onClick = onBack) { Text("Go Back") }
    }
}
