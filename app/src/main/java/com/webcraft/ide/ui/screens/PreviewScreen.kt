package com.webcraft.ide.ui.screens

import android.app.Application
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.webcraft.ide.ui.AppViewModelFactory
import com.webcraft.ide.ui.EditorViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Live Preview screen.
 *
 * Uses a secure WebView with a custom https://webcraft.local/ scheme
 * handler so project assets resolve locally. No file:// access, no
 * external network, no JavaScript bridge exposed to the page.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(nav: NavController, projectId: Long) {
    val vm: EditorViewModel = viewModel(factory = AppViewModelFactory(nav.context.applicationContext as Application))
    val state by vm.ui.collectAsState()
    val scope = rememberCoroutineScope()

    val engine = remember { com.webcraft.ide.preview.PreviewEngine(nav.context) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var autoRefresh by remember { mutableStateOf(true) }
    var viewport by remember { mutableStateOf<ViewportPreset>(ViewportPreset.Mobile) }
    var showConsole by remember { mutableStateOf(false) }
    var fullscreen by remember { mutableStateOf(false) }
    val consoleLines by engine.consoleLines.collectAsState()
    val runtimeErrors by engine.runtimeErrors.collectAsState()

    LaunchedEffect(projectId) { vm.load(projectId) }

    // Refresh the preview whenever the active file changes
    LaunchedEffect(state.activeFilePath, state.activeContent, state.lastSavedAt) {
        if (state.activeFilePath != null) {
            val files = state.files.associate { it.path to it.content }
            engine.setFiles(files)
            webView?.let { wv ->
                if (wv.url.isNullOrBlank() || !wv.url!!.contains("webcraft.local")) {
                    engine.loadEntry()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            if (!fullscreen) {
                TopAppBar(
                    title = {
                        Text(
                            "Preview — ${state.project?.name ?: ""}",
                            fontWeight = FontWeight.SemiBold, fontSize = 16.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Filled.ArrowBack, "Back") }
                    },
                    actions = {
                        IconButton(onClick = {
                            engine.reload()
                        }) { Icon(Icons.Filled.Refresh, "Refresh") }
                        IconButton(onClick = { viewport = ViewportPreset.cycle(viewport) }) {
                            Icon(viewport.icon, viewport.label)
                        }
                        IconButton(onClick = { showConsole = !showConsole }) {
                            Icon(
                                Icons.Filled.Terminal, "Console",
                                tint = if (runtimeErrors.isNotEmpty()) MaterialTheme.colorScheme.error
                                       else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = { fullscreen = !fullscreen }) {
                            Icon(Icons.Filled.Fullscreen, "Fullscreen")
                        }
                    }
                )
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(if (fullscreen) PaddingValues(0.dp) else padding)) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                AndroidView(
                    factory = { ctx ->
                        val wv = engine.createWebView().apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            setInitialScale(100)
                        }
                        engine.attach(wv)
                        // Load entry after the view is created
                        wv.post { engine.loadEntry() }
                        webView = wv
                        wv
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (showConsole) {
                ConsolePanel(
                    lines = consoleLines,
                    errors = runtimeErrors,
                    onClear = {
                        engine.setFiles(state.files.associate { it.path to it.content })
                    }
                )
            }
        }
    }
}

enum class ViewportPreset(val label: String, val widthDp: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Mobile("Mobile", 360, Icons.Filled.PhoneAndroid),
    Tablet("Tablet", 768, Icons.Filled.TabletMac),
    Desktop("Desktop", 1280, Icons.Filled.DesktopWindows);

    companion object {
        fun cycle(from: ViewportPreset): ViewportPreset =
            when (from) {
                Mobile -> Tablet
                Tablet -> Desktop
                Desktop -> Mobile
            }
    }
}

@Composable
private fun ConsolePanel(
    lines: List<com.webcraft.ide.preview.PreviewEngine.ConsoleLine>,
    errors: List<com.webcraft.ide.preview.PreviewEngine.RuntimeError>,
    onClear: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth().height(220.dp)
    ) {
        Column(Modifier.padding(8.dp)) {
            Row {
                Text("Console", style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                if (errors.isNotEmpty()) {
                    Text("${errors.size} error${if (errors.size > 1) "s" else ""}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.width(8.dp))
                }
                TextButton(onClick = onClear) { Text("Clear", fontSize = 12.sp) }
            }
            val scroll = rememberScrollState()
            Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
                lines.forEach { line ->
                    Text(
                        "[${line.level}] ${line.message}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = when (line.level) {
                            "error" -> MaterialTheme.colorScheme.error
                            "warn" -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }
                if (lines.isEmpty()) {
                    Text("No console output yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
