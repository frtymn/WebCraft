# Contributing to WebCraft

Thank you for your interest in contributing!

## Ways to contribute

- Report bugs or request features via GitHub Issues
- Fix bugs or implement features (see "good first issue" label)
- Improve documentation
- Add or improve project templates
- Add or improve unit tests

## Getting started

1. Fork the repository
2. Clone your fork locally:
   ```bash
   git clone https://github.com/YOUR_USERNAME/WebCraft.git
   cd WebCraft
   ```
3. Create a branch for your work:
   ```bash
   git checkout -b feature/my-feature
   ```
4. Make your changes
5. Run the tests:
   ```bash
   ./gradlew testDebugUnitTest
   ```
6. Build to verify:
   ```bash
   ./gradlew assembleDebug
   ```
7. Commit and push to your fork
8. Open a Pull Request against `main`

## Code style

- Kotlin, with `kotlin.code.style=official` (set in `gradle.properties`)
- Follow existing naming conventions in the codebase
- Prefer `val` over `var` where possible
- Use Coroutines and Flow for async work
- Compose UI functions should be small and composable

## Testing

- All new features should have unit tests
- Tests live in `app/src/test/java/com/webcraft/ide/`
- Run with `./gradlew testDebugUnitTest`

## Security

- Never hardcode credentials, API keys, or tokens
- Never commit personal information
- If you find a security vulnerability, please open a private security advisory rather than a public issue

## Commit message convention

Use Conventional Commits:
- `feat: add X`
- `fix: resolve Y`
- `docs: update README`
- `refactor: simplify Z`
- `test: add test for W`

## License

By contributing, you agree that your contributions will be licensed under the MIT License.
