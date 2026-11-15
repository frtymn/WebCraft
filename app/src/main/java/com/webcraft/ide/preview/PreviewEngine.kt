package com.webcraft.ide.preview

import android.content.Context
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.ByteArrayInputStream

/**
 * Loads project files into a WebView via a custom scheme handler.
 *
 * The WebView is loaded with `https://webcraft.local/<path>` URLs.
 * shouldInterceptRequest resolves them against the in-memory project
 * file map. This keeps the preview in a local origin (no file://,
 * no arbitrary network) and is fully deterministic.
 *
 * Security properties:
 *  - No file system access from the WebView.
 *  - No JavaScript bridge is exposed to previewed pages.
 *  - Console and errors are piped to an in-app console only.
 */
class PreviewEngine(private val context: Context) {

    private val _consoleLines = MutableStateFlow<List<ConsoleLine>>(emptyList())
    val consoleLines: StateFlow<List<ConsoleLine>> = _consoleLines

    private val _runtimeErrors = MutableStateFlow<List<RuntimeError>>(emptyList())
    val runtimeErrors: StateFlow<List<RuntimeError>> = _runtimeErrors

    /** Path -> content for all files in the project (populated before load). */
    @Volatile private var files: Map<String, String> = emptyMap()
    @Volatile private var rootEntry: String = "index.html"

    data class ConsoleLine(val level: String, val message: String, val timestamp: Long = System.currentTimeMillis())
    data class RuntimeError(val message: String, val source: String = "", val line: Int = 0)

    fun setFiles(map: Map<String, String>, entry: String = "index.html") {
        files = map
        rootEntry = entry
        _consoleLines.value = emptyList()
        _runtimeErrors.value = emptyList()
    }

    fun createWebView(): WebView {
        val wv = WebView(context)
        wv.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            allowUniversalAccessFromFileURLs = false
            allowFileAccessFromFileURLs = false
            databaseEnabled = false
            mediaPlaybackRequiresUserGesture = true
            cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
            userAgentString = "WebCraftPreview/0.1 Android"
        }
        wv.webViewClient = PreviewClient()
        attachConsoleBridge(wv)
        return wv
    }

    fun loadEntry() {
        val wv = current ?: return
        val path = resolveEntry()
        wv.loadUrl("https://webcraft.local/$path")
    }

    fun reload() { current?.reload() }

    fun setViewport(widthDp: Int, heightDp: Int) {
        val wv = current ?: return
        val density = context.resources.displayMetrics.density
        wv.layoutParams = wv.layoutParams.apply {
            width = (widthDp * density).toInt()
            height = (heightDp * density).toInt()
        }
    }

    private var current: WebView? = null

    fun attach(wv: WebView) { current = wv }

    fun detach() { current = null }

    // ── Entry resolution ───────────────────────────────────────────

    private fun resolveEntry(): String {
        if (files[rootEntry] != null) return rootEntry
        // Fall back to any index.html
        val candidate = files.keys.firstOrNull { it.endsWith("index.html") }
        return candidate ?: rootEntry
    }

    // ── Console bridge ─────────────────────────────────────────────

    private fun attachConsoleBridge(wv: WebView) {
        wv.addJavascriptInterface(ConsoleBridge(), "WebCraftConsole")
    }

    inner class ConsoleBridge {
        @android.webkit.JavascriptInterface
        fun log(level: String, message: String) {
            val line = ConsoleLine(level, message)
            _consoleLines.value = _consoleLines.value + line
        }
        @android.webkit.JavascriptInterface
        fun error(message: String, source: String, line: Int) {
            _runtimeErrors.value = _runtimeErrors.value + RuntimeError(message, source, line)
            _consoleLines.value = _consoleLines.value + ConsoleLine("error", "$message ($source:$line)")
        }
    }

    // ── WebViewClient ──────────────────────────────────────────────

    inner class PreviewClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val url = request.url
            return when (url.scheme) {
                "https", "http" -> url.host == "webcraft.local" // let internal loads through
                else -> true // block anything else (mailto:, intent:, tel:, file://)
            }
        }

        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
            val url = request.url
            if (url.host != "webcraft.local") return null // fall through to network
            val path = url.path?.removePrefix("/") ?: return notFound()
            val normalized = path.removePrefix("/")
            val content = files[normalized] ?: files["/$normalized"] ?: return notFound()
            return WebResourceResponse(
                mimeTypeFor(normalized),
                null,
                ByteArrayInputStream(content.toByteArray())
            )
        }

        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)
            injectConsoleHook(view)
        }

        private fun injectConsoleHook(view: WebView) {
            val js = """
                (function() {
                  if (window.__wcConsoleHooked) return;
                  window.__wcConsoleHooked = true;
                  ['log','info','warn','error','debug'].forEach(function(level) {
                    var orig = console[level];
                    console[level] = function() {
                      var args = Array.prototype.slice.call(arguments)
                          .map(function(a) {
                            try { return typeof a === 'object' ? JSON.stringify(a) : String(a); }
                            catch (e) { return String(a); }
                          }).join(' ');
                      try { WebCraftConsole.log(level, args); } catch(e) {}
                      orig.apply(console, arguments);
                    };
                  });
                  window.addEventListener('error', function(e) {
                    try { WebCraftConsole.error(e.message, e.filename || '', e.lineno || 0); } catch(err) {}
                  });
                  window.addEventListener('unhandledrejection', function(e) {
                    try { WebCraftConsole.error('Unhandled promise rejection: ' + (e.reason && e.reason.message || e.reason), '', 0); } catch(err) {}
                  });
                })();
            """.trimIndent()
            view.evaluateJavascript(js, null)
        }

        private fun notFound(): WebResourceResponse =
            WebResourceResponse("text/plain", "utf-8",
                ByteArrayInputStream("404: file not found in project".toByteArray()))
    }

    companion object {
        fun mimeTypeFor(path: String): String = when (path.substringAfterLast('.', "").lowercase()) {
            "html", "htm" -> "text/html"
            "css" -> "text/css"
            "js", "mjs" -> "text/javascript"
            "json" -> "application/json"
            "svg" -> "image/svg+xml"
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "ico" -> "image/x-icon"
            "txt" -> "text/plain"
            "md", "markdown" -> "text/markdown"
            "xml" -> "application/xml"
            "woff" -> "font/woff"
            "woff2" -> "font/woff2"
            else -> "application/octet-stream"
        }
    }
}
