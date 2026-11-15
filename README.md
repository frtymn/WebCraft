# WebCraft

An open-source mobile web development studio for Android.

WebCraft is a professional Android IDE that lets you create, edit, preview, debug, manage, and publish web development projects directly from your Android device.

**Screenshots coming soon** — see [docs/screenshots/README.md](docs/screenshots/README.md)

## Features

### Professional Code Editor (CodeMirror 6)
- HTML, CSS, JavaScript, TypeScript and JSON support
- Syntax highlighting
- Autocomplete
- Code folding
- Line numbers
- Bracket matching
- Multiple tabs
- Find and replace
- Multi-cursor editing
- Undo and redo
- Auto indentation
- Formatting
- Editor themes (One Dark built-in)
- Word wrap toggle
- Error markers

All editor assets are bundled locally — no CDN dependency.

### Live Preview
- Real WebView-based preview engine
- Automatic refresh on file change
- Manual refresh button
- Mobile / tablet / desktop viewport presets
- Fullscreen mode
- In-app JavaScript console
- Runtime error reporting
- Local asset resolution
- Secure local-origin architecture (`https://webcraft.local/`)

### Project Management
- Create, rename, duplicate and delete projects
- File and folder management
- Recent projects and favorites
- Autosave (debounced, background-safe)
- Project-wide search
- File search

### Project Templates
- Blank HTML
- Portfolio
- Landing Page
- Personal Blog
- Dashboard
- Product Showcase
- Documentation Website
- CSS Animation Playground

Every template contains real, editable source files.

### Craft AI (Offline)
Craft AI is a **rules-based** local coding assistant. It is **not an LLM** and never connects to any cloud service.

Capabilities:
- HTML structural analysis
- CSS linting
- JavaScript linting
- Code explanation through local rules
- Local code suggestions
- Formatting recommendations
- Basic refactoring hints
- Project-wide diagnostics
- Local code templates (fetch, debounce, dark mode, forms, grid)

The `LocalAIProvider` interface is modular so an on-device LLM (e.g. via llama.cpp, MLC, or Gemini Nano) can be dropped in later without changing the UI.

### Developer Toolkit
- JavaScript Console (in Preview)
- HTML Validator (via Craft AI lint)
- CSS Inspector (via Craft AI lint)
- JSON Formatter (pretty/minify)
- Color Picker (HEX/RGB/HSL/CMYK + contrast ratio)
- Regex Tester (with flags)
- Base64 Encoder/Decoder
- URL Encoder/Decoder
- UUID Generator (v4 and v7)
- Timestamp Converter (epoch ↔ ISO 8601)
- HTTP Request Tester (placeholder — see Limitations)
- Project Search

### Git and GitHub
- Initialize local repositories
- Local snapshot commits
- Branch creation and switching
- Commit history log
- Diff between working tree and last commit
- .gitignore-aware change detection
- GitHub Personal Access Token storage (encrypted at rest with AES-256-GCM)

### Export and Import
- ZIP export (uses Android Storage Access Framework)
- ZIP import (with path traversal protection)
- Project sharing
- Backup and restore

## Installation

### Requirements
- Android 7.0 (API 24) or higher
- ~40MB free storage

### Install from release
1. Download the latest `WebCraft-vX.Y.Z.apk` from the [Releases](https://github.com/frtymn/WebCraft/releases) page
2. Enable "Install from unknown sources" in Android settings (only needed the first time)
3. Open the APK and install

## Build from source

### Prerequisites
- Android Studio (Hedgehog or newer) or standalone Android SDK
- JDK 17
- Android SDK Platform 34

### Using Gradle
```bash
# Clone
git clone https://github.com/frtymn/WebCraft.git
cd WebCraft

# Build debug APK
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk

# Build release APK (unsigned)
./gradlew assembleRelease

# Run unit tests
./gradlew testDebugUnitTest
```

### Using Android Studio
1. File → Open → select the `WebCraft` directory
2. Wait for Gradle sync
3. Run → Run 'app' (or build APK via Build → Build Bundles/APKs)

## Architecture

WebCraft follows MVVM + Clean Architecture:

```
app/
├── src/main/java/com/webcraft/ide/
│   ├── MainActivity.kt              # Single-activity entry point
│   ├── WebCraftApp.kt               # Application
│   ├── data/                        # Data layer
│   │   ├── db/                      # Room database (Project, File, Tab entities)
│   │   ├── fs/                      # VirtualFileSystem (file storage)
│   │   ├── models/                  # Domain models + mappers
│   │   ├── repo/                    # ProjectRepository
│   │   └── templates/               # Project templates
│   ├── ai/                          # Craft AI (LocalAIProvider + RulesBasedProvider)
│   ├── preview/                     # PreviewEngine (secure WebView)
│   ├── git/                         # GitManager + GitManagerStatics
│   ├── tools/                       # DevTools (JSON, Base64, URL, UUID, Regex, Color, Timestamp)
│   ├── io/                          # ZipIO (export/import)
│   └── ui/                          # UI layer
│       ├── theme/                   # Material 3 theme (dark-first)
│       ├── screens/                 # Home, Projects, Editor, Preview, CraftAI, DevTools, Settings
│       ├── AppViewModelFactory.kt
│       └── Nav.kt                   # Navigation
└── src/main/assets/editor/          # CodeMirror 6 bundle (self-contained)
```

### Key architectural decisions
- **Kotlin + Jetpack Compose + Material 3**: modern declarative UI, dark-first design
- **MVVM + Clean Architecture**: `data` layer (Room, filesystem, repo), `ui` layer (Compose screens + ViewModels), domain models
- **Room Database** for project/file/tab metadata; filesystem for file content
- **Kotlin Coroutines and Flow** for reactive data and lifecycle-safe operations
- **DataStore** reserved for user preferences
- **CodeMirror 6 bundled offline** (576 KB, esbuild IIFE bundle) — no CDN
- **Secure WebView** for preview: custom `https://webcraft.local/` scheme handler, no `file://` access, no JS bridge exposed to previewed pages
- **Encrypted token storage** using AES-256-GCM with PBKDF2 key derivation

## Documentation
- [Architecture](docs/architecture.md)
- [Features](docs/features.md)
- [Security](docs/security.md)
- [Limitations](docs/limitations.md)
- [Contributing](CONTRIBUTING.md)

## License

MIT. See [LICENSE](LICENSE).

## Limitations (v0.1.0)

See [docs/limitations.md](docs/limitations.md) for the complete list. Key items:

- **Git push/pull/clone over HTTPS is not implemented.** WebCraft v0.1.0 provides local snapshot commits, branch tracking, .gitignore detection, and encrypted token storage. Full Git protocol support is planned for v0.2.0.
- **HTTP Request Tester is a UI placeholder** in this offline-first release.
- **Craft AI is rules-based, not an LLM.** It is never advertised as generative.
- **Project rename in the UI** creates a TODO in the dropdown; the repository method exists but the rename dialog isn't wired up yet.

---

<sub>Created with [Perplexity](https://www.perplexity.ai/computer/tasks/91356128-3f9b-446c-8bda-c4dfc7570b7b)</sub>
