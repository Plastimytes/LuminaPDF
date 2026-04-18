package com.luminapdf.ui.screens.library

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.luminapdf.data.local.entity.PdfDocument
import com.luminapdf.ui.components.EmptyLibraryPlaceholder
import com.luminapdf.ui.components.PdfThumbnailImage
import com.luminapdf.ui.theme.LuminaPDFTheme
import com.luminapdf.viewmodel.LibraryViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun LibraryScreen(
    onOpenPdf: (String) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showSearch by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var documentToDelete by remember { mutableStateOf<PdfDocument?>(null) }

    // Storage permission (Android ≤ 12)
    val storagePermission = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
        rememberPermissionState(Manifest.permission.READ_EXTERNAL_STORAGE)
    } else null

    // SAF file picker — no permission needed on Android 13+
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.onPdfPicked(it) }
    }

    LuminaPDFTheme(darkTheme = state.isDarkMode) {
        Scaffold(
            topBar = {
                LibraryTopBar(
                    isDarkMode     = state.isDarkMode,
                    showSearch     = showSearch,
                    searchQuery    = state.searchQuery,
                    gridColumns    = state.gridColumns,
                    sortOrder      = state.sortOrder,
                    showSortMenu   = showSortMenu,
                    onToggleDark   = viewModel::toggleDarkMode,
                    onToggleSearch = { showSearch = !showSearch; if (!showSearch) viewModel.setSearchQuery("") },
                    onQueryChange  = viewModel::setSearchQuery,
                    onToggleGrid   = { viewModel.setGridColumns(if (state.gridColumns == 2) 3 else 2) },
                    onSortMenuToggle = { showSortMenu = !showSortMenu },
                    onSortSelected = { viewModel.setSortOrder(it); showSortMenu = false }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2 &&
                            storagePermission?.status?.isGranted == false) {
                            storagePermission.launchPermissionRequest()
                        } else {
                            pdfPickerLauncher.launch(arrayOf("application/pdf"))
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor   = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add PDF")
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(padding)
            ) {
                when {
                    state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    state.documents.isEmpty() -> EmptyLibraryPlaceholder(
                        onAddClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) }
                    )
                    else -> PdfGrid(
                        documents    = state.documents,
                        gridColumns  = state.gridColumns,
                        onOpenPdf    = onOpenPdf,
                        onDeletePdf  = { documentToDelete = it }
                    )
                }
            }
        }

        // Delete confirmation dialog
        documentToDelete?.let { doc ->
            AlertDialog(
                onDismissRequest = { documentToDelete = null },
                title = { Text("Remove from library?") },
                text  = { Text("\"${doc.fileName}\" will be removed from LuminaPDF. The original file is not deleted.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.removeDocument(doc.uri)
                        documentToDelete = null
                    }) { Text("Remove", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { documentToDelete = null }) { Text("Cancel") }
                }
            )
        }
    }
}

// ── Top App Bar ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryTopBar(
    isDarkMode: Boolean,
    showSearch: Boolean,
    searchQuery: String,
    gridColumns: Int,
    sortOrder: String,
    showSortMenu: Boolean,
    onToggleDark: () -> Unit,
    onToggleSearch: () -> Unit,
    onQueryChange: (String) -> Unit,
    onToggleGrid: () -> Unit,
    onSortMenuToggle: () -> Unit,
    onSortSelected: (String) -> Unit
) {
    Column {
        TopAppBar(
            title = {
                if (!showSearch) {
                    Text(
                        text       = "LuminaPDF",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 22.sp
                    )
                }
            },
            actions = {
                if (showSearch) {
                    OutlinedTextField(
                        value         = searchQuery,
                        onValueChange = onQueryChange,
                        placeholder   = { Text("Search PDFs…") },
                        singleLine    = true,
                        modifier      = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
                IconButton(onClick = onToggleSearch) {
                    Icon(
                        if (showSearch) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = "Search"
                    )
                }
                IconButton(onClick = onToggleGrid) {
                    Icon(
                        if (gridColumns == 2) Icons.Default.GridView else Icons.Default.ViewModule,
                        contentDescription = "Toggle grid"
                    )
                }
                Box {
                    IconButton(onClick = onSortMenuToggle) {
                        Icon(Icons.Default.Sort, contentDescription = "Sort")
                    }
                    DropdownMenu(
                        expanded        = showSortMenu,
                        onDismissRequest = { onSortSelected(sortOrder) }
                    ) {
                        DropdownMenuItem(
                            text          = { Text("Recent first") },
                            onClick       = { onSortSelected("recent") },
                            leadingIcon   = { Icon(Icons.Default.AccessTime, null) }
                        )
                        DropdownMenuItem(
                            text          = { Text("Name A–Z") },
                            onClick       = { onSortSelected("name") },
                            leadingIcon   = { Icon(Icons.Default.SortByAlpha, null) }
                        )
                    }
                }
                IconButton(onClick = onToggleDark) {
                    Icon(
                        if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Toggle theme"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor    = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface
            )
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

// ── PDF Grid ──────────────────────────────────────────────────────────────────

@Composable
private fun PdfGrid(
    documents: List<PdfDocument>,
    gridColumns: Int,
    onOpenPdf: (String) -> Unit,
    onDeletePdf: (PdfDocument) -> Unit
) {
    LazyVerticalGrid(
        columns            = GridCells.Fixed(gridColumns),
        contentPadding     = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement   = Arrangement.spacedBy(12.dp),
        modifier           = Modifier.fillMaxSize()
    ) {
        items(documents, key = { it.uri }) { doc ->
            PdfCard(
                document    = doc,
                onClick     = { onOpenPdf(doc.uri) },
                onLongClick = { onDeletePdf(doc) }
            )
        }
    }
}

// ── PDF Card ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PdfCard(
    document: PdfDocument,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val progressAnim by animateFloatAsState(
        targetValue    = document.readingProgress,
        animationSpec  = tween(600, easing = FastOutSlowInEasing),
        label          = "progress"
    )
    val dateFormatter = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }
    val lastAccessed  = remember(document.lastAccessedAt) {
        dateFormatter.format(Date(document.lastAccessedAt))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.72f)
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape  = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Thumbnail ────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                PdfThumbnailImage(
                    thumbnailPath = document.thumbnailPath,
                    fileName      = document.fileName,
                    modifier      = Modifier.fillMaxSize()
                )

                // Reading progress badge
                if (document.readingProgress > 0f) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                    ) {
                        Text(
                            text     = "${(document.readingProgress * 100).toInt()}%",
                            style    = MaterialTheme.typography.labelSmall,
                            color    = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }

                // Gradient overlay at the bottom
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                                )
                            )
                        )
                )
            }

            // ── Info Row ─────────────────────────────────────────────────────
            Column(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text     = document.fileName.removeSuffix(".pdf"),
                    style    = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color    = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text  = if (document.totalPages > 0)
                                    "p. ${document.lastReadPage + 1} / ${document.totalPages}"
                                else "Not opened",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text  = lastAccessed,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // ── Progress Bar ─────────────────────────────────────────────
                Spacer(Modifier.height(5.dp))
                LinearProgressIndicator(
                    progress       = { progressAnim },
                    modifier       = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color          = MaterialTheme.colorScheme.primary,
                    trackColor     = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            }
        }
    }
}
