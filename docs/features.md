# WebCraft Features

## Code Editor

The editor is powered by CodeMirror 6, compiled to a single self-contained JavaScript bundle (~576 KB) and loaded into a WebView. All assets are bundled locally — no CDN.

Supported languages:
- HTML / HTM / XHTML
- CSS
- JavaScript (JS, MJS)
- TypeScript (TS, TSX)
- JSON

Features:
- Syntax highlighting (One Dark theme by default)
- Autocomplete (language-aware, icons)
- Code folding (with fold gutter)
- Line numbers
- Bracket matching
- Multiple tabs (pinned tabs supported)
- Find and replace (search panel)
- Multi-cursor editing
- Undo and redo
- Auto indentation
- Manual format button (uses Craft AI formatters)
- Word wrap toggle
- Font size controls (10–24sp)
- Error markers (diagnostics from Craft AI pushed via linter compartment)

Touch optimizations:
- `adjustResize` for the on-screen keyboard
- Extra bottom padding in the editor content (40vh) so text doesn't hide behind the keyboard
- Disabled pull-to-refresh and pinch-zoom in the editor WebView

## Live Preview

The preview screen renders the current project in a real Android WebView.

- **Automatic refresh** on file change
- **Manual refresh** button
- **Viewport presets**: Mobile (360dp), Tablet (768dp), Desktop (1280dp)
- **Fullscreen mode** (hides the app bar)
- **In-app JavaScript console** with log/info/warn/error levels
- **Runtime error reporting** — window.onerror and unhandled promise rejections captured
- **Local asset resolution** — `index.html`, `style.css`, `script.js`, and nested paths resolve against the project file map

### Security architecture
The preview uses a custom origin `https://webcraft.local/`. All requests are intercepted by `shouldInterceptRequest` and resolved from an in-memory file map. The WebView has:
- `allowFileAccess = false`
- `allowContentAccess = false`
- `allowUniversalAccessFromFileURLs = false`
- `allowFileAccessFromFileURLs = false`
- `databaseEnabled = false`

External URLs (http/https outside webcraft.local) fall through to the network — this is intentional so CDN-loaded libraries still work in previewed projects.

## Project Management

- Create project (with or without template)
- Rename project
- Duplicate project (deep copy of files)
- Delete project (with confirmation)
- Favorite projects (star toggle)
- Recent projects list
- Search projects by name or description
- Create files and folders
- Delete files
- Rename files
- Project-wide content search (returns file + matching line numbers)
- Autosave (debounced 1.5s + 30s safety net + onCleared final save)

## Project Templates

8 templates, each with real, editable source files:

| Template | Files |
|---|---|
| Blank HTML | index.html, style.css, script.js |
| Portfolio | index.html, style.css, script.js |
| Landing Page | index.html, style.css |
| Personal Blog | index.html, post1.html, post2.html, style.css |
| Dashboard | index.html, style.css, script.js |
| Product Showcase | index.html, style.css, script.js |
| Documentation Website | index.html, style.css, script.js |
| CSS Animation Playground | index.html, style.css, script.js |

## Craft AI

Craft AI is a **rules-based** local coding assistant. It runs entirely on-device with no network access, no model weights, and no cloud API.

**It is not an LLM and is never advertised as one.**

Capabilities:
- **HTML structural analysis** — unclosed tags, missing alt/href, missing doctype/viewport
- **CSS linting** — undefined variables, !important overuse, duplicate selectors, oversized font-size
- **JavaScript linting** — var usage, == vs ===, console statements, eval, document.write, debugger, unbalanced braces/parens
- **TypeScript detection** — TS-style annotations flagged in JS files
- **JSON validation** — trailing commas, single quotes, comments, structural parse
- **Code explanation** — natural-language summary of structure (tags, selectors, functions, DOM refs)
- **Suggestions** — actionable refactoring hints
- **Formatting** — pretty-print JSON, CSS, HTML, JavaScript
- **Project-wide diagnostics** — aggregated error/warning/info counts across all files
- **Code templates** — fetch helper, debounce, dark mode toggle, form, CSS grid

### Future LLM integration
The `LocalAIProvider` interface is designed so an on-device LLM (e.g. llama.cpp bindings, MLC LLM, Gemini Nano) can be registered via `LocalAIRegistry.register()` without changing UI code.

## Developer Toolkit

All tools run locally and offline.

- **JSON Formatter** — pretty-print with 2-space indentation, or minify
- **Base64** — standard and URL-safe encode/decode
- **URL** — percent-encode/decode
- **UUID Generator** — v4 (random) and v7 (timestamp-ordered)
- **Timestamp Converter** — epoch seconds/milliseconds ↔ ISO 8601, both directions
- **Regex Tester** — live match highlighting, flags (i/m/s), match position display
- **Color Picker** — HEX ↔ RGB ↔ HSL ↔ CMYK, contrast ratio (WCAG)
- **HTTP Request Tester** — UI placeholder in v0.1.0 (see limitations)
- **Project Search** — search all files in a project

## Git

- Initialize local repository
- Create and switch branches
- Local snapshot commits (stored on disk)
- Commit history log
- Diff between two file maps (added/modified/deleted)
- .gitignore-aware file filtering
- GitHub Personal Access Token storage (encrypted)

## Export and Import

- **ZIP export** — writes the entire project root to a ZIP via Storage Access Framework
- **ZIP import** — reads a ZIP into a new project, with path traversal protection
- **Backup** — export all projects to a single ZIP
- **Restore** — import the backup ZIP

## Settings

- Autosave toggle
- Word wrap toggle
- Font size slider
- Auto-refresh preview toggle
- Dark theme toggle
- About (version, license)

## Navigation

Bottom-level navigation between five destinations:
- Home
- Projects
- Editor
- Craft AI
- Settings

The editor screen maximizes usable code area by hiding the tab strip when no tabs are open and keeping the app bar compact.
