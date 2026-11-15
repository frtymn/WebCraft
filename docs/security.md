# WebCraft Security

## Threat model

WebCraft is a local-first mobile IDE. The primary assets to protect are:
1. User code (projects)
2. GitHub Personal Access Tokens (optional feature)

## WebView security

### Editor WebView
- Loads only from `file:///android_asset/editor/editor.html` (bundled, immutable)
- No user-controlled content is loaded into this WebView
- JavaScript is enabled (required for CodeMirror)
- No storage APIs enabled

### Preview WebView
- Uses a custom origin `https://webcraft.local/`
- All requests intercepted by `shouldInterceptRequest` and resolved from an in-memory file map
- **Disabled**:
  - `allowFileAccess`
  - `allowContentAccess`
  - `allowUniversalAccessFromFileURLs`
  - `allowFileAccessFromFileURLs`
  - `databaseEnabled`
- **Enabled**:
  - `javaScriptEnabled` (required to render user projects)
  - `domStorageEnabled` (required for localStorage/sessionStorage in previewed apps)
  - `mediaPlaybackRequiresUserGesture = true`

### JavaScript bridge exposure
The preview WebView exposes one JS interface: `WebCraftConsole`. It accepts only log/error messages (string level + string message). It cannot read files, write files, or execute arbitrary code on the Android side.

### URL scheme handling
- `https://webcraft.local/*` — intercepted, resolved locally
- Other `http://` / `https://` URLs — allowed (for CDN dependencies in user projects)
- `file://`, `content://`, `intent://`, `mailto:`, `tel:`, etc. — blocked via `shouldOverrideUrlLoading`

## Token storage

GitHub Personal Access Tokens are stored encrypted at rest:

- **Cipher**: AES-256-GCM
- **Key derivation**: PBKDF2 with HMAC-SHA256, 65536 iterations, 32-byte random salt
- **IV**: 96-bit random, prepended to ciphertext

### Limitation
The key material is derived from a fixed app secret compiled into the binary, plus a per-install random salt stored in `SharedPreferences`. This is **obfuscation-grade**, not hardware-backed (no Android Keystore, no StrongBox).

**Threat**: A determined attacker with physical access to a rooted device and the APK can extract the fixed secret and decrypt the token.

**Mitigation roadmap (v0.2.0)**:
- Migrate to Android Keystore (`AndroidKeyStore`) for the master key
- Consider `BiometricPrompt` for token unlock
- Consider `EncryptedSharedPreferences` from androidx.security

### What is NOT stored
- No tokens in plaintext
- No tokens in the source code
- No tokens in logs
- No tokens in the Room database

## ZIP import security

`ZipIO.importZip` validates every entry name:
- Rejects entries containing `..` (path traversal)
- Rejects entries starting with `/` (absolute paths)

This prevents the classic Zip Slip attack where a malicious ZIP extracts files outside the destination directory.

## Data backup

`android:allowBackup="true"` is set. Android Auto Backup will include `filesDir/projects/` (user code). This is intentional — losing code is worse than the marginal privacy tradeoff. Users who prefer can disable backup per-app in Android settings.

## Permissions

WebCraft requests only two permissions:
- `INTERNET` — required for the preview WebView to load external CDN dependencies
- `ACCESS_NETWORK_STATE` — required to detect network availability for the HTTP Request Tester (not yet implemented)

No location, contacts, storage, camera, microphone, or other sensitive permissions are requested.

## Responsible disclosure

If you discover a security vulnerability, please:
1. Do NOT open a public GitHub issue
2. Open a private security advisory on GitHub
3. Allow reasonable time for a fix before public disclosure
