package com.webcraft.ide.ui.screens

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.webcraft.ide.ai.Diagnostic
import com.webcraft.ide.ai.Severity
import com.webcraft.ide.ui.AppViewModelFactory
import com.webcraft.ide.ui.CraftAiViewModel
import com.webcraft.ide.ui.EditorViewModel

/**
 * Craft AI screen.
 *
 * NOTE: The current provider is a RULES-BASED engine, not an LLM.
 * The UI says so explicitly.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CraftAiScreen(nav: NavController, projectId: Long) {
    val editorVm: EditorViewModel = viewModel(factory = AppViewModelFactory(nav.context.applicationContext as Application))
    val aiVm: CraftAiViewModel = viewModel(factory = AppViewModelFactory(nav.context.applicationContext as Application))
    val editorState by editorVm.ui.collectAsState()
    val aiState by aiVm.ui.collectAsState()

    LaunchedEffect(projectId) {
        editorVm.load(projectId)
        aiVm.load(projectId)
    }

    // When the active file changes, run analysis
    LaunchedEffect(editorState.activeFilePath, editorState.lastSavedAt) {
        val path = editorState.activeFilePath ?: return@LaunchedEffect
        val file = editorState.files.firstOrNull { it.path == path }
        if (file != null) aiVm.setActiveFile(path, file.content)
    }

    var showTemplateGen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Craft AI", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Filled.ArrowBack, "Back") }
                },
                actions = {
                    TextButton(onClick = { aiVm.analyzeProject() }) { Text("Analyze Project") }
                    TextButton(onClick = { showTemplateGen = true }) { Text("Templates") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Banner
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    Text(
                        "${aiState.providerName} — offline rules-based analyzer",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Runs entirely on-device. Not an LLM.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (aiState.busy) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Explanation
                if (aiState.explanation.isNotBlank()) {
                    item {
                        SectionCard("Explanation") {
                            Text(aiState.explanation,
                                fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 18.sp)
                        }
                    }
                }

                // Diagnostics
                if (aiState.diagnostics.isNotEmpty()) {
                    item {
                        SectionCard("Diagnostics (${aiState.diagnostics.size})") {
                            aiState.diagnostics.take(20).forEach { diag ->
                                DiagnosticRow(diag)
                                Spacer(Modifier.height(6.dp))
                            }
                        }
                    }
                }

                // Suggestions
                if (aiState.suggestions.isNotEmpty()) {
                    item {
                        SectionCard("Suggestions (${aiState.suggestions.size})") {
                            aiState.suggestions.forEach { sug ->
                                Text(sug.title, style = MaterialTheme.typography.titleSmall)
                                Text(sug.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }

                // Formatted preview
                if (aiState.formatted.isNotBlank() && aiState.formatted != aiState.activeContent) {
                    item {
                        SectionCard("Formatted") {
                            Text(aiState.formatted,
                                fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 18.sp)
                        }
                    }
                }

                // Project report
                aiState.projectReport?.let { report ->
                    item {
                        SectionCard("Project Report") {
                            Text(report.summary, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(8.dp))
                            Text("${report.totalFiles} files analyzed",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (report.issues.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Text("Issues:", style = MaterialTheme.typography.labelMedium)
                                report.issues.take(15).forEach { diag ->
                                    DiagnosticRow(diag)
                                    Spacer(Modifier.height(4.dp))
                                }
                            }
                        }
                    }
                }

                if (aiState.activeFile.isBlank()) {
                    item {
                        Text(
                            "Open a file in the editor, then come back here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 32.dp)
                        )
                    }
                }
            }
        }
    }

    if (showTemplateGen) {
        var selected by remember { mutableStateOf("fetch") }
        var result by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showTemplateGen = false },
            title = { Text("Generate Template") },
            text = {
                Column {
                    val options = listOf("fetch", "debounce", "dark-mode", "form", "grid")
                    options.forEach { opt ->
                        FilterChip(
                            selected = selected == opt,
                            onClick = { selected = opt },
                            label = { Text(opt) },
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    if (result.isNotBlank()) {
                        Text(result, fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                            modifier = Modifier.verticalScroll(rememberScrollState()).height(200.dp))
                    } else {
                        Text("Pick an intent and tap Generate.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    aiVm.generateTemplate(selected, "javascript") { result = it }
                }) { Text("Generate") }
            },
            dismissButton = {
                TextButton(onClick = { showTemplateGen = false }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun DiagnosticRow(diag: Diagnostic) {
    val color = when (diag.severity) {
        Severity.ERROR -> MaterialTheme.colorScheme.error
        Severity.WARNING -> MaterialTheme.colorScheme.tertiary
        Severity.INFO -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row {
        Text(
            when (diag.severity) {
                Severity.ERROR -> "ERR"
                Severity.WARNING -> "WRN"
                Severity.INFO -> "INF"
            },
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(34.dp)
        )
        Text(
            "L${diag.line}  ${diag.message}",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp
        )
    }
}
