package com.webcraft.ide.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.webcraft.ide.ai.Diagnostic
import com.webcraft.ide.ai.LocalAIRegistry
import com.webcraft.ide.ai.ProjectReport
import com.webcraft.ide.ai.Suggestion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CraftAiUiState(
    val activeFile: String = "",
    val activeContent: String = "",
    val explanation: String = "",
    val diagnostics: List<Diagnostic> = emptyList(),
    val suggestions: List<Suggestion> = emptyList(),
    val formatted: String = "",
    val projectReport: ProjectReport? = null,
    val busy: Boolean = false,
    val providerName: String = "Craft Rules"
)

class CraftAiViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = ServiceLocator.repository
    private val provider = LocalAIRegistry.active()

    private val _ui = MutableStateFlow(CraftAiUiState())
    val ui = _ui.asStateFlow()

    private var projectId: Long = -1

    fun load(pid: Long) {
        projectId = pid
    }

    fun setActiveFile(path: String, content: String) {
        _ui.value = _ui.value.copy(activeFile = path, activeContent = content)
        analyze()
    }

    fun analyze() {
        val st = _ui.value
        if (st.activeFile.isBlank()) return
        viewModelScope.launch {
            _ui.value = _ui.value.copy(busy = true)
            val lang = languageOf(st.activeFile)
            val explanation = provider.explain(lang, st.activeContent)
            val diags = provider.lint(lang, st.activeContent)
            val suggs = provider.suggest(lang, st.activeContent)
            val formatted = provider.format(lang, st.activeContent)
            _ui.value = _ui.value.copy(
                explanation = explanation.text,
                diagnostics = diags,
                suggestions = suggs,
                formatted = formatted,
                busy = false,
                providerName = provider.displayName
            )
        }
    }

    fun analyzeProject() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(busy = true)
            val files = repo.listFiles(projectId)
                .filter { !it.isDirectory }
                .associate { it.path to it.content }
            val report = provider.analyzeProject(files)
            _ui.value = _ui.value.copy(projectReport = report, busy = false)
        }
    }

    fun generateTemplate(intent: String, language: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val code = provider.generateTemplate(intent, language)
            onResult(code)
        }
    }

    private fun languageOf(path: String): String = when (path.substringAfterLast('.', "").lowercase()) {
        "html", "htm" -> "html"
        "css" -> "css"
        "js", "mjs" -> "javascript"
        "ts", "tsx" -> "typescript"
        "json" -> "json"
        else -> "text"
    }
}
