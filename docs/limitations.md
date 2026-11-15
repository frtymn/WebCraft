# WebCraft Limitations (v0.1.0)

This document lists known limitations honestly. WebCraft does not ship fake functionality.

## Git

### Not implemented
- **Push, pull, fetch over HTTPS** — Full Git smart HTTP protocol support is nontrivial to implement from scratch. It requires packfile encoding/decoding, ref negotiation, and protocol v2.
- **Clone** — Same reason as above.
- **Remote diff / merge** — Depends on fetch.

### Implemented
- Local snapshot commits
- Branch creation and switching
- Commit history log
- Diff between working tree and last commit (per-file added/modified/deleted)
- .gitignore-aware change detection
- Encrypted token storage

### Plan for v0.2.0
- Integrate [JGit](https://www.eclipse.org/jgit/) or bundle libgit2 via JNI for real push/pull/clone

## HTTP Request Tester

The HTTP Request Tester screen is a **UI placeholder** in v0.1.0. It shows the URL/method/body inputs but the "Send" button returns "Not implemented in v0.1.0".

This is because WebCraft is offline-first by design, and the tool would need to make real network requests. The tool will be implemented in a future release with an explicit opt-in.

## Craft AI

Craft AI is a **rules-based static analyzer**. It is NOT an LLM.

- It does not generate novel code from natural language
- It does not "understand" code semantically
- It applies curated regex and heuristic rules

This is documented in the UI (a banner says "offline rules-based analyzer — not an LLM") and in the code (`RulesBasedProvider.isGenerative = false`).

## Project rename UI

The "Rename" option in the project dropdown menu is a TODO. `ProjectRepository.renameProject` is implemented and tested, but the rename dialog UI is not yet wired up.

## Other known limitations

- **No hardware-backed crypto for tokens** — See `docs/security.md`
- **No external editor integration** — Files are edited exclusively in the built-in CodeMirror editor
- **No live preview on external displays** — Preview renders in the in-app WebView only
- **CodeMirror error markers** are pushed from Craft AI but not live as-you-type; they refresh on format/analyze actions
- **Multi-select in the file tree** is not supported
- **Project search** returns line numbers but does not jump to the specific line in the editor
- **No tablets/landscape-specific layouts** — The UI is designed phone-first; it works on tablets but doesn't use the extra space optimally
- **Testing on real devices** — Unit tests pass (54 tests), but the app has not been exhaustively tested across all Android versions, screen sizes, and OEM skins. Community testing is welcome.

## Not fake

To be clear about what IS implemented and working:
- CodeMirror 6 editor with all listed features (bundled offline)
- Live preview with console and error capture
- Project management (create/rename/duplicate/delete/favorite/search)
- All 8 templates with real source files
- Craft AI rules-based analysis (lint, explain, suggest, format, templates)
- Developer toolkit (JSON, Base64, URL, UUID, Timestamp, Regex, Color)
- ZIP export/import with path traversal protection
- 54 unit tests, all passing
- Debug and release APKs build successfully
