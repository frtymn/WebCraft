# WebCraft Architecture

## Overview

WebCraft is a single-module Android application written in Kotlin with Jetpack Compose (Material 3). It follows MVVM + Clean Architecture, separating concerns into:

1. **Data layer** — Room database, filesystem, repositories, templates
2. **Domain models** — plain Kotlin data classes
3. **UI layer** — Compose screens + ViewModels
4. **Feature modules** — Craft AI, Preview, Git, DevTools, ZipIO

## Module map

```
app/src/main/java/com/webcraft/ide/
├── MainActivity.kt          # Single-activity Compose host
├── WebCraftApp.kt           # Application (singleton instance)
│
├── data/
│   ├── db/                  # Room
│   │   ├── WebCraftDatabase.kt
│   │   ├── entities.kt      # ProjectEntity, FileEntity, TabEntity
│   │   ├── daos.kt          # ProjectDao, FileDao, TabDao
│   │   └── WebCraftConverters.kt
│   ├── fs/
│   │   └── VirtualFileSystem.kt   # Wraps Context.filesDir
│   ├── models/
│   │   └── DomainModels.kt        # Project, ProjectFile, Tab + mappers
│   ├── repo/
│   │   └── ProjectRepository.kt   # All project/file operations
│   └── templates/
│       ├── TemplateFile.kt
│       └── ProjectTemplates.kt    # 8 built-in templates
│
├── ai/                      # Craft AI
│   ├── LocalAIProvider.kt   # Interface (pluggable)
│   ├── RulesBasedProvider.kt# Rules-based implementation
│   └── LocalAIRegistry.kt   # Provider registry
│
├── preview/
│   └── PreviewEngine.kt     # Secure WebView preview
│
├── git/
│   ├── GitManager.kt        # Local snapshots, token storage
│   └── GitManagerStatics.kt # Pure functions for testing
│
├── tools/
│   └── DevTools.kt          # JSON, Base64, URL, UUID, Regex, Color, Timestamp
│
├── io/
│   └── ZipIO.kt             # Export/import with traversal guards
│
└── ui/
    ├── theme/
    │   ├── Theme.kt         # Material 3 dark-first palette
    │   └── Typography.kt
    ├── screens/
    │   ├── HomeScreen.kt
    │   ├── ProjectsScreen.kt
    │   ├── EditorScreen.kt
    │   ├── PreviewScreen.kt
    │   ├── CraftAiScreen.kt
    │   ├── DevToolsScreen.kt
    │   └── SettingsScreen.kt
    ├── AppViewModelFactory.kt
    ├── ProjectsViewModel.kt
    ├── EditorViewModel.kt
    ├── CraftAiViewModel.kt
    └── Nav.kt                # Navigation routes
```

## Key decisions

### 1. CodeMirror 6 bundled offline
The editor runs in a WebView with CodeMirror 6 compiled into a single self-contained IIFE bundle (`app/src/main/assets/editor/editor.js`, ~576 KB minified) using esbuild. No CDN dependency at runtime.

Communication:
- Android → JS: `evaluateJavascript("window.__webcraft.setX(...)")`
- JS → Android: `window.AndroidEditor.onContentChanged(...)` via `@JavascriptInterface`

Large documents (>200KB) are not sent on every keystroke; Android pulls them on demand via `getText()`.

### 2. Secure preview architecture
The preview WebView loads URLs from `https://webcraft.local/<path>`. `shouldInterceptRequest` resolves these against an in-memory project file map.

Security properties:
- `allowFileAccess = false`
- `allowContentAccess = false`
- `allowUniversalAccessFromFileURLs = false`
- `allowFileAccessFromFileURLs = false`
- `databaseEnabled = false`
- No `file://` or `content://` scheme allowed
- A JS interface (`WebCraftConsole`) is attached for console/error piping — this is the *only* bridge, and it only accepts log/error messages from the previewed page

### 3. Pluggable Craft AI
`LocalAIProvider` is an interface modeled after what an on-device LLM would expose (explain, lint, suggest, format, generateTemplate, analyzeProject). Today only `RulesBasedProvider` is registered — a deterministic static-analysis engine with no model weights and no network access.

To add an on-device LLM later: implement `LocalAIProvider`, call `LocalAIRegistry.register(provider)`, and the UI picks it up.

### 4. Encrypted token storage
GitHub Personal Access Tokens are stored with AES-256-GCM. The key is derived via PBKDF2 (65536 iterations, HMAC-SHA256) from a fixed app secret plus a random 32-byte salt stored alongside the ciphertext.

**Note:** This is obfuscation-grade, not hardware-backed. See `docs/security.md`.

### 5. File storage model
File content is stored twice:
- On disk at `filesDir/projects/<sanitized-name>/...` (authoritative for the VirtualFileSystem)
- In Room (`FileEntity.content`) for fast in-memory lookup and search

Both are updated together in `ProjectRepository` within `Dispatchers.IO` coroutines.

### 6. Autosave strategy
- `EditorViewModel.onContentChanged` debounces 1500ms before calling `saveActive()`
- A periodic safety-net (every 30 seconds) saves if the doc is dirty
- `onCleared()` does a final blocking save if dirty

### 7. Room schema
Three entities with a unique index on `(projectId, path)`:
- `projects` — id, name, description, timestamps, isFavorite, templateId, rootPath, git fields
- `files` — id, projectId, path, content, isDirectory, size, timestamps
- `tabs` — id, projectId, filePath, order, isPinned, openedAt

## Dependencies

See `gradle/libs.versions.toml` for the full version catalog. Highlights:

| Library | Version | Purpose |
|---|---|---|
| Kotlin | 2.0.20 | Language |
| AGP | 8.5.2 | Gradle Android plugin |
| Compose BOM | 2024.09.02 | UI framework |
| Material 3 | (BOM) | Design system |
| Room | 2.6.1 | Local database |
| DataStore Preferences | 1.1.1 | User preferences |
| Navigation Compose | 2.8.1 | In-app navigation |
| Coroutines | 1.8.1 | Async primitives |
| Kotlinx Serialization JSON | 1.7.3 | JSON utilities |

## Testing

- `RulesBasedProviderTest` — 19 tests covering lint rules, formatting, suggestions, templates
- `DevToolsTest` — 26 tests covering JSON, Base64, URL, UUID, Timestamp, Regex, Color, ZipIO roundtrip, Git diff/ignore
- `ProjectTemplatesTest` — 10 tests validating all 8 templates
- `ZipIOTest` — 4 tests covering export/import roundtrip, folder preservation, traversal guards

Run: `./gradlew testDebugUnitTest`

54 tests total, all passing.
