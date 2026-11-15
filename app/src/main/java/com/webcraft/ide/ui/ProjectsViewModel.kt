package com.webcraft.ide.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.webcraft.ide.data.models.Project
import com.webcraft.ide.data.models.ProjectFile
import com.webcraft.ide.data.templates.ProjectTemplates
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProjectsUiState(
    val projects: List<Project> = emptyList(),
    val favorites: List<Project> = emptyList(),
    val recent: List<Project> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val searchQuery: String = ""
)

class ProjectsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = ServiceLocator.repository

    private val _ui = MutableStateFlow(ProjectsUiState())
    val ui = _ui.asStateFlow()

    private val allProjects = repo.observeProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val allFavorites = repo.observeFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            allProjects.collect { projects ->
                val q = _ui.value.searchQuery
                _ui.value = _ui.value.copy(
                    projects = if (q.isBlank()) projects else projects.filter {
                        it.name.contains(q, true) || it.description.contains(q, true)
                    },
                    favorites = allFavorites.value,
                    recent = projects.take(10),
                    loading = false
                )
            }
        }
        viewModelScope.launch {
            allFavorites.collect { favorites ->
                _ui.value = _ui.value.copy(favorites = favorites)
            }
        }
    }

    fun search(query: String) {
        _ui.value = _ui.value.copy(searchQuery = query)
        viewModelScope.launch {
            val all = allProjects.value
            _ui.value = _ui.value.copy(
                projects = if (query.isBlank()) all
                else all.filter {
                    it.name.contains(query, true) || it.description.contains(query, true)
                }
            )
        }
    }

    fun createProject(name: String, templateId: String?, description: String = "", onDone: (Result<Project>) -> Unit = {}) {
        viewModelScope.launch {
            val res = repo.createProject(name, description, templateId)
            if (res.isSuccess) {
                _ui.value = _ui.value.copy(error = null)
            } else {
                _ui.value = _ui.value.copy(error = res.exceptionOrNull()?.message ?: "Unknown error")
            }
            onDone(res)
        }
    }

    fun renameProject(id: Long, newName: String) {
        viewModelScope.launch { repo.renameProject(id, newName) }
    }

    fun deleteProject(id: Long) {
        viewModelScope.launch { repo.deleteProject(id) }
    }

    fun duplicateProject(id: Long) {
        viewModelScope.launch { repo.duplicateProject(id) }
    }

    fun toggleFavorite(id: Long, current: Boolean) {
        viewModelScope.launch { repo.setFavorite(id, !current) }
    }

    val templates = ProjectTemplates.all
}
