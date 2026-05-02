LuminaPDF — Full Build Log & Documentation

Project Overview

A modern, feature-rich PDF reader Android app built with Kotlin, Jetpack Compose, and Material Design 3. Built entirely using GitHub Codespaces (browser-based) without Android Studio.

Tech Stack

ComponentTechnologyLanguageKotlinUI FrameworkJetpack Compose + Material 3PDF EngineAndroid native PdfRendererDatabaseRoom (SQLite)PreferencesDataStoreDIHiltNavigationJetpack Navigation ComposeArchitectureMVVM + Clean ArchitectureBuild EnvironmentGitHub Codespaces

Features Built

Library Screen

2/3 column grid toggle

PDF thumbnails auto-generated via PdfRenderer

Animated progress bar per PDF card

Last accessed timestamp

Full-width rounded search bar

Category filter tabs

Sort by Recent / Name A-Z

Long-press to delete

Dark/Light mode toggle

PDF Reader Screen

Scroll view mode (continuous pages)

Single page swipe mode with pinch-to-zoom

Auto-resumes last read page on open

Midnight/Slate dark theme with page colour inversion

Blue light filter slider (amber overlay)

Font size slider

Focus mode (tap to hide/show UI)

Reading progress bar in top bar

Session reading timer (survives screen rotation)

Pro Features

Text-to-Speech (TTS) — reads current page aloud

Smart bookmarks with side drawer

Category system — assign PDFs to named categories

Reading stats tracker

Environment Setup (Codespaces)

1. Create GitHub Repository

Go to github.com → New Repository → name it LuminaPDF

Upload all project files

Open in Codespaces: Code → Codespaces → Create codespace

2. Install Java 17





bash



sdk install java 17.0.11-mssdk use java 17.0.11-msjava -version

3. Install Gradle





bash



wget -q https://services.gradle.org/distributions/gradle-8.9-bin.zip -P /tmp && unzip -q /tmp/gradle-8.9-bin.zip -d /tmp

4. Generate Gradle Wrapper





bash



cp /tmp/gradle-8.9/bin/gradle gradlewchmod +x gradlewmkdir -p gradle/wrappercat > gradle/wrapper/gradle-wrapper.properties << 'EOF'distributionBase=GRADLE_USER_HOMEdistributionPath=wrapper/distsdistributionUrl=https\://services.gradle.org/distributions/gradle-8.9-bin.zipzipStoreBase=GRADLE_USER_HOMEzipStorePath=wrapper/distsEOF

5. Create settings.gradle.kts





bash



cat > settings.gradle.kts << 'EOF'pluginManagement {    repositories {        google()        mavenCentral()        gradlePluginPortal()    }}dependencyResolutionManagement {    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)    repositories {        google()        mavenCentral()        maven { url = uri("https://jitpack.io") }    }}rootProject.name = "LuminaPDF"include(":app")EOF

6. Install Android SDK





bash



wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -P /tmp && \mkdir -p $HOME/android-sdk/cmdline-tools && \unzip -q /tmp/commandlinetools-linux-11076708_latest.zip -d $HOME/android-sdk/cmdline-tools && \mv $HOME/android-sdk/cmdline-tools/cmdline-tools $HOME/android-sdk/cmdline-tools/latest

7. Set Environment Variables





bash



export ANDROID_HOME=$HOME/android-sdkexport PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-toolsexport JAVA_TOOL_OPTIONS=""export _JAVA_OPTIONS=""export JDK_JAVA_OPTIONS=""echo "sdk.dir=$HOME/android-sdk" > local.properties

8. Accept Licenses & Install SDK Components





bash



yes | sdkmanager --licenses && sdkmanager "platforms;android-35" "build-tools;35.0.0"

9. Add Required gradle.properties Entries





bash



echo "android.useAndroidX=true" >> gradle.propertiesecho "android.suppressUnsupportedCompileSdk=35" >> gradle.propertiesecho "org.gradle.jvmargs=-Xmx2g -XX:MaxMetaspaceSize=512m" >> gradle.properties

10. Build APK





bash



/tmp/gradle-8.9/bin/gradle assembleDebug

APK output: app/build/outputs/apk/debug/app-debug.apk

Bugs Fixed During Build

Bug 1 — Java Version Too New

Problem: Kotlin compiler doesn't support Java 25 (Codespaces default)Fix: Switch to Java 17 via sdkmanager



Bug 2 — Missing gradlew

Problem: ZIP upload didn't include the Gradle wrapperFix: Manually created gradlew and gradle-wrapper.properties



Bug 3 — android.useAndroidX not set

Problem: AndroidX dependencies detected but property not enabledFix: echo "android.useAndroidX=true" >> gradle.properties



Bug 4 — PDF Viewer library not found

Problem: com.github.barteksc:android-pdf-viewer couldn't be resolvedFix: Removed the dependency entirely since the app uses native PdfRenderer directly



Bug 5 — Theme resource not found

Problem: Theme.Material.NoTitleBar not availableFix: Switched to android:Theme.DeviceDefault.NoActionBar



Bug 6 — Missing mipmap icons

Problem: ic_launcher and ic_launcher_round not foundFix: Created XML vector drawables in all mipmap density folders



Bug 7 — Kotlinx Serialization missing

Problem: @Serializable and Json unresolvedFix: Added implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")



Bug 8 — PDFs not reopening after app restart

Problem: Content URIs lose permission after app closesFix: Added takePersistableUriPermission() in PdfRepository.addOrUpdateDocument()



Bug 9 — Timer resetting on screen rotation

Problem: LaunchedEffect re-ran on recomposition restarting the timerFix: Added timerStarted boolean flag in ReaderViewModel to prevent duplicate starts



Bug 10 — Navigation URI corruption

Problem: content:// URIs were being mangled by URLEncoderFix: Switched to android.net.Uri.encode() in LuminaNavGraph



Bug 11 — 7-flow combine type error

Problem: Kotlin couldn't infer types for custom 7-flow combine helperFix: Rewrote helper using two nested combine calls with explicit Any? arrays and unchecked casts



Bug 12 — Room database migration missing

Problem: Added category column to PdfDocument without a migrationFix: Added MIGRATION_1_2 in LuminaDatabase and updated DatabaseModule to use .addMigrations() instead of .fallbackToDestructiveMigration()

File Structure





LuminaPDF/├── build.gradle.kts├── settings.gradle.kts├── gradle/│   ├── libs.versions.toml│   └── wrapper/│       └── gradle-wrapper.properties└── app/    ├── build.gradle.kts    ├── proguard-rules.pro    └── src/main/        ├── AndroidManifest.xml        ├── res/        │   ├── values/themes.xml        │   └── drawable/ic_splash_logo.xml        └── java/com/luminapdf/            ├── LuminaPDFApp.kt            ├── MainActivity.kt            ├── data/            │   ├── local/            │   │   ├── entity/            │   │   │   ├── PdfDocument.kt            │   │   │   └── BookmarkEntry.kt            │   │   ├── dao/PdfDocumentDao.kt            │   │   ├── LuminaDatabase.kt            │   │   └── AppPreferences.kt            │   └── repository/PdfRepository.kt            ├── di/DatabaseModule.kt            ├── ui/            │   ├── LuminaNavGraph.kt            │   ├── theme/            │   │   ├── Color.kt            │   │   └── Theme.kt            │   ├── components/SharedComponents.kt            │   └── screens/            │       ├── library/LibraryScreen.kt            │       └── reader/ReaderScreen.kt            └── viewmodel/                ├── LibraryViewModel.kt                └── ReaderViewModel.kt

Important Notes for Future Rebuilds

Every time Codespaces restarts you need to re-run:





bash



cd /workspaces/LuminaPDF/LuminaPDFwget -q https://services.gradle.org/distributions/gradle-8.9-bin.zip -P /tmp && unzip -q /tmp/gradle-8.9-bin.zip -d /tmpexport ANDROID_HOME=$HOME/android-sdkexport PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-toolsexport JAVA_TOOL_OPTIONS=""export _JAVA_OPTIONS=""export JDK_JAVA_OPTIONS=""echo "sdk.dir=$HOME/android-sdk" > local.propertiessdk use java 17.0.11-ms/tmp/gradle-8.9/bin/gradle assembleDebug

The Android SDK persists in $HOME/android-sdk between sessions so you don't need to reinstall it — only Gradle and environment variables need resetting.

Installing APK on Phone

Right-click app/build/outputs/apk/debug/app-debug.apk in Codespaces Explorer

Download to your computer

Transfer to phone via WhatsApp, email, or Google Drive

Open on phone → Enable "Install from unknown sources" → Install
