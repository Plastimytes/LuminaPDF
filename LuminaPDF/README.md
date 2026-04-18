# LuminaPDF — Complete Project Reference

## Project Structure

```
LuminaPDF/
├── build.gradle.kts                          ← Root build file
├── settings.gradle.kts                       ← JitPack repo included
├── gradle/libs.versions.toml                 ← Version catalog
└── app/
    ├── build.gradle.kts                      ← App dependencies
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── res/
        │   ├── values/themes.xml
        │   └── drawable/ic_splash_logo.xml
        └── java/com/luminapdf/
            ├── LuminaPDFApp.kt               ← @HiltAndroidApp
            ├── MainActivity.kt
            ├── data/
            │   ├── local/
            │   │   ├── entity/
            │   │   │   ├── PdfDocument.kt    ← Room Entity
            │   │   │   └── BookmarkEntry.kt  ← Serializable bookmark
            │   │   ├── dao/
            │   │   │   └── PdfDocumentDao.kt ← Room DAO
            │   │   ├── LuminaDatabase.kt     ← Room Database
            │   │   └── AppPreferences.kt     ← DataStore
            │   └── repository/
            │       └── PdfRepository.kt      ← Single data source
            ├── di/
            │   └── DatabaseModule.kt         ← Hilt @Module
            ├── ui/
            │   ├── LuminaNavGraph.kt         ← Navigation
            │   ├── theme/
            │   │   ├── Color.kt              ← Midnight/Slate palette
            │   │   └── Theme.kt              ← MaterialTheme
            │   ├── components/
            │   │   └── SharedComponents.kt   ← Thumbnail, EmptyState
            │   └── screens/
            │       ├── library/
            │       │   └── LibraryScreen.kt  ← Grid + progress bars
            │       └── reader/
            │           └── ReaderScreen.kt   ← Full viewer + features
            └── viewmodel/
                ├── LibraryViewModel.kt
                └── ReaderViewModel.kt
```

---

## Step-by-Step Setup in Android Studio

### 1. Create the project
- File → New → New Project → **Empty Activity**
- Name: `LuminaPDF`  |  Package: `com.luminapdf`  |  Min SDK: **26**
- Language: Kotlin  |  Build config: **Kotlin DSL** (`.kts`)

### 2. Replace generated files
Paste each file from this project into the exact path shown in the structure above.

### 3. Add the JitPack repository
`settings.gradle.kts` already includes:
```kotlin
maven { url = uri("https://jitpack.io") }
```
This is required for the `android-pdf-viewer` library by barteksc.

### 4. Add Kotlinx Serialization plugin
In `app/build.gradle.kts` add to the plugins block:
```kotlin
alias(libs.plugins.kotlin.serialization)
```
And in `libs.versions.toml` under `[plugins]`:
```toml
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```
And under `[libraries]`:
```toml
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version = "1.7.3" }
```
Then in `app/build.gradle.kts` dependencies:
```kotlin
implementation(libs.kotlinx.serialization.json)
```

### 5. Sync & Build
- Click **Sync Now** after pasting all files
- Build → **Make Project** (Ctrl+F9 / ⌘F9)
- Run on device or emulator (API 26+)

---

## Key Design Decisions

### PDF Engine: Android PdfRenderer + barteksc/AndroidPdfViewer
| Aspect | Choice |
|---|---|
| **Thumbnail generation** | Native `android.graphics.pdf.PdfRenderer` — zero deps, runs on any device |
| **Scrollable viewer** | `barteksc/AndroidPdfViewer` via JitPack for smooth fling + pinch-zoom |
| **Why not PdfiumAndroid alone?** | barteksc wraps it with a smooth scrolling surface; native PdfRenderer lacks a Compose-friendly scroll view |

The `PdfPageViewer` composable in `ReaderScreen.kt` uses a `LazyColumn` rendering each page via `PdfRenderer` — this gives you full Compose control. For production you can swap this for the `barteksc` `PDFView` via an `AndroidView` wrapper.

### Storage: Scoped Storage / SAF
- On **Android 13+**: Uses `ActivityResultContracts.OpenDocument()` — no permission needed.
- On **Android ≤ 12**: Requests `READ_EXTERNAL_STORAGE` before showing the picker.
- The `content://` URI is persisted in Room and used directly — this survives app restarts because SAF grants persistent read permission when you call `contentResolver.takePersistableUriPermission()`.

**Add this call in `PdfRepository.addOrUpdateDocument()`** for long-lived access:
```kotlin
context.contentResolver.takePersistableUriPermission(
    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
)
```

---

## Feature Map

| Feature | File | Implementation |
|---|---|---|
| Grid library | `LibraryScreen.kt` | `LazyVerticalGrid` 2/3 columns |
| Progress bar per PDF | `LibraryScreen.kt → PdfCard` | `LinearProgressIndicator` animated |
| Last accessed timestamp | `LibraryScreen.kt → PdfCard` | `SimpleDateFormat` from Room |
| Resume last page | `ReaderViewModel.loadDocument()` | Reads `lastReadPage` from Room |
| Dark / Midnight theme | `Color.kt`, `ReaderTopBar` | `DarkColorScheme` + `ColorMatrix` inversion |
| Page colour inversion | `ReaderScreen.applyNightFilter()` | `ColorMatrix` paint on `Bitmap` |
| Blue light filter | `ReaderTopBar` slider + overlay | Amber `Box` overlay with alpha |
| TTS | `ReaderViewModel.speakCurrentPage()` | `android.speech.tts.TextToSpeech` |
| Smart bookmarks | `BookmarkDrawer` + `PdfRepository` | JSON array in Room column |
| Reading timer | `ReaderViewModel` timer `Job` | Coroutine + `System.currentTimeMillis()` |
| Focus mode | `ReaderScreen` + `AnimatedVisibility` | 3s inactivity hides chrome |
| Thumbnail cache | `PdfRepository.generateAndSaveThumbnail()` | `PdfRenderer` → JPEG in cache dir |
| Hilt DI | `DatabaseModule.kt` | `@InstallIn(SingletonComponent)` |
| DataStore prefs | `AppPreferences.kt` | Dark mode, blue light, grid cols |

---

## Midnight / Slate Theme Palette

```
Background:    #0D1117  (GitHub dark)
Surface:       #161B22
SurfaceVariant:#1E2837
Primary:       #82AAFF  (cornflower blue)
Secondary:     #BBC4E8  (muted slate violet)
Tertiary:      #96CEFF  (soft teal)
Outline:       #3A4556
```

---

## Extending the App

### Add `barteksc PDFView` as AndroidView (smoother zoom/scroll):
```kotlin
@Composable
fun BartekPdfView(uri: Uri, startPage: Int, isDark: Boolean, onPageChange: (Int, Int) -> Unit) {
    AndroidView(
        factory = { ctx ->
            com.github.barteksc.pdfviewer.PDFView(ctx, null).apply {
                fromUri(uri)
                    .defaultPage(startPage)
                    .nightMode(isDark)
                    .onPageChange { page, total -> onPageChange(page, total) }
                    .load()
            }
        },
        update = { view -> view.setNightMode(isDark) }
    )
}
```

### Add text extraction for TTS (requires PdfBox-Android):
```kotlin
// In app/build.gradle.kts:
implementation("com.tom-roush:pdfbox-android:2.0.27.0")

// In ReaderViewModel:
fun extractPageText(uri: Uri, pageIndex: Int): String {
    val doc = PDDocument.load(context.contentResolver.openInputStream(uri))
    val stripper = PDFTextStripper().apply {
        startPage = pageIndex + 1; endPage = pageIndex + 1
    }
    return stripper.getText(doc).also { doc.close() }
}
```

---

## APK Generation

1. Build → **Generate Signed App Bundle / APK**
2. Choose **APK** → create/select keystore
3. Select **release** build variant
4. Locate the APK at `app/release/app-release.apk`

> For Play Store distribution use **App Bundle (.aab)** instead.
