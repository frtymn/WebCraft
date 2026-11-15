package com.webcraft.ide.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.webcraft.ide.data.models.Project
import com.webcraft.ide.data.models.ProjectFile
import com.webcraft.ide.data.models.Tab
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class EditorUiState(
    val project: Project? = null,
    val files: List<ProjectFile> = emptyList(),
    val tabs: List<Tab> = emptyList(),
    val activeFilePath: String? = null,
    val activeContent: String = "",
    val isDirty: Boolean = false,
    val isSaving: Boolean = false,
    val lastSavedAt: Long = 0,
    val searchQuery: String = "",
    val searchResults: List<Pair<ProjectFile, String>> = emptyList(),
    val error: String? = null
)

class EditorViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = ServiceLocator.repository

    private val _ui = MutableStateFlow(EditorUiState())
    val ui = _ui.asStateFlow()

    private var projectId: Long = -1
    private var saveJob: Job? = null
    private val saveMutex = Mutex()

    private val _project = MutableStateFlow<Project?>(null)
    private val _files = MutableStateFlow<List<ProjectFile>>(emptyList())
    private val _tabs = MutableStateFlow<List<Tab>>(emptyList())

    fun load(pid: Long) {
        if (pid == projectId && _ui.value.project != null) return
        projectId = pid
        viewModelScope.launch {
            repo.observeProject(pid).collect { p ->
                _project.value = p
                combine()
            }
        }
        viewModelScope.launch {
            repo.observeFiles(pid).collect { files ->
                _files.value = files
                combine()
            }
        }
        viewModelScope.launch {
            repo.observeTabs(pid).collect { tabs ->
                _tabs.value = tabs
                combine()
            }
        }
    }

    private fun combine() {
        val p = _project.value ?: return
        val files = _files.value
        val tabs = _tabs.value
        val active = _ui.value.activeFilePath
        val activeTab = tabs.firstOrNull { it.filePath == active } ?: tabs.firstOrNull()
        val activeFile = files.firstOrNull { it.path == activeTab?.filePath }
        _ui.value = _ui.value.copy(
            project = p,
            files = files,
            tabs = tabs,
            activeFilePath = activeTab?.filePath,
            activeContent = activeFile?.content ?: _ui.value.activeContent
        )
    }

    fun openFile(path: String) {
        viewModelScope.launch {
            repo.openTab(projectId, path)
            val file = _files.value.firstOrNull { it.path == path }
            _ui.value = _ui.value.copy(
                activeFilePath = path,
                activeContent = file?.content ?: "",
                isDirty = false
            )
        }
    }

    fun closeTab(path: String) {
        viewModelScope.launch { repo.closeTab(projectId, path) }
    }

    /** Called by the editor WebView when content changes (debounced). */
    fun onContentChanged(content: String?) {
        if (content == null) return
        _ui.value = _ui.value.copy(activeContent = content, isDirty = true)
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(1500) // debounce autosave
            saveActive()
        }
    }

    suspend fun saveActive() {
        val path = _ui.value.activeFilePath ?: return
        val content = _ui.value.activeContent
        saveMutex.withLock {
            _ui.value = _ui.value.copy(isSaving = true)
            val res = repo.writeFile(projectId, path, content)
            _ui.value = _ui.value.copy(
                isSaving = false,
                isDirty = res.isSuccess,
                lastSavedAt = if (res.isSuccess) System.currentTimeMillis() else _ui.value.lastSavedAt,
                error = res.exceptionOrNull()?.message
            )
        }
    }

    fun saveNow() {
        viewModelScope.launch { saveActive() }
    }

    fun createFile(path: String, isDirectory: Boolean = false) {
        viewModelScope.launch {
            val res = repo.createFile(projectId, path, isDirectory)
            if (res.isFailure) {
                _ui.value = _ui.value.copy(error = res.exceptionOrNull()?.message)
            }
        }
    }

    fun deleteFile(path: String) {
        viewModelScope.launch { repo.deleteFile(projectId, path) }
    }

    fun renameFile(old: String, new: String) {
        viewModelScope.launch { repo.renameFile(projectId, old, new) }
    }

    fun searchInProject(query: String) {
        _ui.value = _ui.value.copy(searchQuery = query)
        if (query.isBlank()) {
            _ui.value = _ui.value.copy(searchResults = emptyList())
            return
        }
        viewModelScope.launch {
            val results = repo.searchFiles(projectId, query)
            _ui.value = _ui.value.copy(searchResults = results)
        }
    }

    override fun onCleared() {
        saveJob?.cancel()
        // Last-ditch save on ViewModel teardown
        val st = _ui.value
        if (st.isDirty && st.activeFilePath != null && projectId > 0) {
            kotlinx.coroutines.runBlocking {
                repo.writeFile(projectId, st.activeFilePath!!, st.activeContent)
            }
        }
        super.onCleared()
    }
}
