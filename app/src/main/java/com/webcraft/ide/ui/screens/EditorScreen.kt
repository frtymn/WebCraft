package com.webcraft.ide.ui.screens

import android.annotation.SuppressLint
import android.app.Application
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.webcraft.ide.ui.AppViewModelFactory
import com.webcraft.ide.ui.Dest
import com.webcraft.ide.ui.EditorViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Editor screen hosting the CodeMirror 6 WebView.
 *
 * Communication model:
 *   Android -> JS: evaluateJavascript("window.__webcraft.setX(...)")
 *   JS -> Android: window.AndroidEditor.onContentChanged(text, isLarge)
 *
 * The JS side batches changes and only sends small docs (<200KB) on every
 * keystroke; larger docs are pulled on demand via getText().
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun EditorScreen(nav: NavController, projectId: Long) {
    val vm: EditorViewModel = viewModel(factory = AppViewModelFactory(nav.context.applicationContext as Application))
    val state by vm.ui.collectAsState()
    val scope = rememberCoroutineScope()

    var webView by remember { mutableStateOf<WebView?>(null) }
    var editorReady by remember { mutableStateOf(false) }
    var showFileTree by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var showNewFile by remember { mutableStateOf(false) }
    var newFileName by remember { mutableStateOf("") }
    var newIsDir by remember { mutableStateOf(false) }
    var wordWrap by remember { mutableStateOf(true) }
    var fontSize by remember { mutableStateOf(14) }

    LaunchedEffect(projectId) { vm.load(projectId) }

    // Push the active file to the WebView when it changes
    LaunchedEffect(state.activeFilePath, editorReady) {
        if (editorReady && state.activeFilePath != null) {
            webView?.evaluateJavascript(
                "window.__webcraft.setFile(${jsStr(state.activeFilePath!!)}, ${jsStr(state.activeContent)});",
                null
            )
        }
    }

    // Autosave safety net
    LaunchedEffect(webView) {
        while (isActive) {
            delay(30_000)
            if (state.isDirty) vm.saveNow()
        }
    }

    BackHandler(enabled = showFileTree || showSearch) {
        showFileTree = false; showSearch = false
    }

    Scaffold(
        topBar = {
            EditorTopBar(
                projectName = state.project?.name ?: "",
                fileName = state.activeFilePath?.substringAfterLast('/') ?: "",
                isDirty = state.isDirty,
                isSaving = state.isSaving,
                onBack = { nav.popBackStack() },
                onSave = { vm.saveNow() },
                onPreview = { nav.navigate(Dest.Preview.preview(projectId)) },
                onToggleFiles = { showFileTree = !showFileTree },
                onSearch = { showSearch = !showSearch },
                onNewFile = { showNewFile = true },
                onFormat = {
                    webView?.evaluateJavascript("window.__webcraft.formatDocument();", null)
                },
                onWrapToggle = {
                    wordWrap = !wordWrap
                    webView?.evaluateJavascript("window.__webcraft.setWordWrap(${if (wordWrap) "true" else "false"});", null)
                },
                wordWrap = wordWrap,
                onFontSize = { delta ->
                    fontSize = (fontSize + delta).coerceIn(10, 24)
                    webView?.evaluateJavascript("window.__webcraft.setFontSize($fontSize);", null)
                },
                fontSize = fontSize
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.fillMaxSize()) {
                if (state.tabs.isNotEmpty()) {
                    EditorTabs(
                        tabs = state.tabs,
                        activePath = state.activeFilePath,
                        onOpen = { vm.openFile(it) },
                        onClose = { vm.closeTab(it) }
                    )
                }
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            settings.apply {
                                javaScriptEnabled = true
                                allowFileAccess = false
                                allowContentAccess = false
                                domStorageEnabled = false
                                databaseEnabled = false
                                cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
                            }
                            addJavascriptInterface(EditorBridge(
                                onContentChanged = { text, isLarge ->
                                    if (!isLarge) vm.onContentChanged(text)
                                },
                                onCursorChanged = { line, col, _ ->
                                    // Could surface in status bar
                                },
                                onReady = {
                                    editorReady = true
                                    // Apply initial settings
                                    Handler(Looper.getMainLooper()).post {
                                        evaluateJavascript(
                                            "window.__webcraft.setWordWrap(${if (wordWrap) "true" else "false"});" +
                                            "window.__webcraft.setFontSize($fontSize);",
                                            null
                                        )
                                    }
                                },
                                onFormatRequested = {
                                    scope.launch {
                                        val content = state.activeContent
                                        val path = state.activeFilePath ?: return@launch
                                        val lang = when (path.substringAfterLast('.', "").lowercase()) {
                                            "json" -> "json"
                                            "css" -> "css"
                                            "html", "htm" -> "html"
                                            else -> "javascript"
                                        }
                                        val formatted = com.webcraft.ide.ai.LocalAIRegistry
                                            .active().format(lang, content)
                                        vm.onContentChanged(formatted)
                                        vm.saveNow()
                                        Handler(Looper.getMainLooper()).post {
                                            evaluateJavascript(
                                                "window.__webcraft.setText(${jsStr(formatted)});",
                                                null
                                            )
                                        }
                                    }
                                }
                            ), "AndroidEditor")
                            webViewClient = WebViewClient()
                            webChromeClient = WebChromeClient()
                            // Load the bundled editor HTML from assets
                            loadUrl("file:///android_asset/editor/editor.html")
                            webView = this
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                }
            }
        }
    }

    // File tree drawer — a side panel instead of ModalDrawerSheet
    // (ModalDrawerSheet requires being the root of a ModalDrawer composition,
    //  which conflicts with the editor's Scaffold layout.)
    if (showFileTree) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            shadowElevation = 16.dp,
            modifier = Modifier.fillMaxHeight().width(260.dp)
        ) {
            Column {
                Text("Files", style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp))
                LazyColumn {
                    items(state.files, key = { it.path }) { file ->
                        val name = file.path.substringAfterLast('/')
                        ListItem(
                            headlineContent = {
                                Text(
                                    name,
                                    fontFamily = if (file.isDirectory) FontFamily.Default else FontFamily.Monospace,
                                    fontSize = 14.sp
                                )
                            },
                            supportingContent = { Text(file.path, fontSize = 11.sp) },
                            leadingContent = {
                                Icon(
                                    if (file.isDirectory) Icons.Outlined.Folder
                                    else fileIconFor(name),
                                    null,
                                    Modifier.size(20.dp)
                                )
                            },
                            modifier = Modifier.clickable {
                                if (!file.isDirectory) {
                                    vm.openFile(file.path)
                                    showFileTree = false
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Project search sheet
    if (showSearch) {
        var query by remember { mutableStateOf("") }
        ModalBottomSheet(onDismissRequest = { showSearch = false }) {
            Column(Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it; vm.searchInProject(it) },
                    label = { Text("Search in project") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                LazyColumn(Modifier.height(320.dp)) {
                    items(state.searchResults) { (file, snippet) ->
                        ListItem(
                            headlineContent = { Text(file.path.substringAfterLast('/'),
                                fontFamily = FontFamily.Monospace, fontSize = 13.sp) },
                            supportingContent = {
                                Text(snippet, fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                                    maxLines = 3)
                            },
                            modifier = Modifier.clickable {
                                vm.openFile(file.path)
                                showSearch = false
                            }
                        )
                    }
                }
            }
        }
    }

    // New file dialog
    if (showNewFile) {
        AlertDialog(
            onDismissRequest = { showNewFile = false },
            title = { Text("New File") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newFileName,
                        onValueChange = { newFileName = it },
                        label = { Text("Path (e.g. js/app.js or assets/img)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = newIsDir, onCheckedChange = { newIsDir = it })
                        Text("Create folder")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newFileName.isNotBlank()) {
                        vm.createFile(newFileName.trim(), newIsDir)
                        newFileName = ""; newIsDir = false
                    }
                    showNewFile = false
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showNewFile = false }) { Text("Cancel") } }
        )
    }
}

// ── Top bar ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorTopBar(
    projectName: String,
    fileName: String,
    isDirty: Boolean,
    isSaving: Boolean,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onPreview: () -> Unit,
    onToggleFiles: () -> Unit,
    onSearch: () -> Unit,
    onNewFile: () -> Unit,
    onFormat: () -> Unit,
    onWrapToggle: () -> Unit,
    wordWrap: Boolean,
    onFontSize: (Int) -> Unit,
    fontSize: Int
) {
    TopAppBar(
        title = {
            Column {
                Text(projectName, style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(
                    buildString {
                        append(fileName)
                        if (isDirty) append(" •")
                        if (isSaving) append("  saving…")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") }
        },
        actions = {
            IconButton(onClick = onSave) {
                Icon(
                    Icons.Filled.Save,
                    "Save",
                    tint = if (isDirty) MaterialTheme.colorScheme.primary
                          else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onPreview) { Icon(Icons.Filled.Preview, "Preview") }
            IconButton(onClick = onFormat) { Icon(Icons.Filled.FormatAlignLeft, "Format") }
            IconButton(onClick = onWrapToggle) {
                Icon(
                    if (wordWrap) Icons.Filled.WrapText else Icons.Filled.Notes,
                    "Word wrap"
                )
            }
            IconButton(onClick = { onFontSize(+1) }) { Icon(Icons.Filled.ZoomIn, "Zoom in") }
            IconButton(onClick = { onFontSize(-1) }) { Icon(Icons.Filled.ZoomOut, "Zoom out") }
            IconButton(onClick = onToggleFiles) { Icon(Icons.Filled.Menu, "Files") }
            IconButton(onClick = onSearch) { Icon(Icons.Filled.Search, "Search") }
            IconButton(onClick = onNewFile) { Icon(Icons.Filled.Add, "New file") }
        }
    )
}

// ── Tabs strip ──────────────────────────────────────────────────

@Composable
private fun EditorTabs(
    tabs: List<com.webcraft.ide.data.models.Tab>,
    activePath: String?,
    onOpen: (String) -> Unit,
    onClose: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        tabs.forEach { tab ->
            val active = tab.filePath == activePath
            InputChip(
                selected = active,
                onClick = { onOpen(tab.filePath) },
                label = {
                    Text(
                        tab.filePath.substringAfterLast('/'),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                },
                trailingIcon = {
                    Icon(
                        Icons.Filled.Close, "Close",
                        Modifier.size(14.dp).clickable { onClose(tab.filePath) }
                    )
                },
                modifier = Modifier.padding(horizontal = 2.dp)
            )
        }
    }
}

// ── JS bridge ───────────────────────────────────────────────────

private class EditorBridge(
    private val onContentChanged: (String?, Boolean) -> Unit,
    private val onCursorChanged: (Int, Int, Int) -> Unit,
    private val onReady: () -> Unit,
    private val onFormatRequested: () -> Unit
) {
    @JavascriptInterface
    fun onContentChanged(text: String?, isLarge: Boolean) {
        Handler(Looper.getMainLooper()).post { onContentChanged(text, isLarge) }
    }
    @JavascriptInterface
    fun onCursorChanged(line: Int, col: Int, pos: Int) {
        Handler(Looper.getMainLooper()).post { onCursorChanged(line, col, pos) }
    }
    @JavascriptInterface
    fun onReady() {
        Handler(Looper.getMainLooper()).post { onReady() }
    }
    @JavascriptInterface
    fun onFormatRequested() {
        Handler(Looper.getMainLooper()).post { onFormatRequested() }
    }
}

// ── Helpers ─────────────────────────────────────────────────────

/** Escape a Kotlin string into a JS string literal. */
private fun jsStr(s: String): String {
    val sb = StringBuilder("\"")
    for (c in s) {
        when (c) {
            '\\' -> sb.append("\\\\")
            '"' -> sb.append("\\\"")
            '\n' -> sb.append("\\n")
            '\r' -> sb.append("\\r")
            '\t' -> sb.append("\\t")
            else -> sb.append(c)
        }
    }
    sb.append("\"")
    return sb.toString()
}

private fun fileIconFor(name: String) = when (name.substringAfterLast('.', "").lowercase()) {
    "html", "htm" -> Icons.Filled.Code
    "css" -> Icons.Filled.Palette
    "js", "mjs" -> Icons.Filled.Javascript
    "ts", "tsx" -> Icons.Filled.DataObject
    "json" -> Icons.Filled.DataObject
    "md", "markdown" -> Icons.Filled.Description
    "svg" -> Icons.Filled.Image
    "txt" -> Icons.Filled.Description
    else -> Icons.Filled.InsertDriveFile
}
