package com.webcraft.ide.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.webcraft.ide.WebCraftApp
import com.webcraft.ide.ai.LocalAIRegistry
import com.webcraft.ide.data.db.WebCraftDatabase
import com.webcraft.ide.data.fs.VirtualFileSystem
import com.webcraft.ide.data.repo.ProjectRepository
import com.webcraft.ide.git.GitManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppViewModelFactory(val app: Application) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // Defensive: guarantee singletons exist even if this factory is used
        // from a context where Application.onCreate has not run yet.
        // init() is idempotent and cheap when already initialized.
        ServiceLocator.init(app)
        return when (modelClass) {
            ProjectsViewModel::class.java -> ProjectsViewModel(app) as T
            EditorViewModel::class.java -> EditorViewModel(app) as T
            CraftAiViewModel::class.java -> CraftAiViewModel(app) as T
            else -> error("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

/** Shared container for singletons that live as long as the app. */
object ServiceLocator {
    @Volatile lateinit var repository: ProjectRepository
    @Volatile lateinit var gitManager: GitManager

    @Synchronized
    fun init(app: Application) {
        if (::repository.isInitialized) return
        val db = WebCraftDatabase.get(app)
        val vfs = VirtualFileSystem(app)
        repository = ProjectRepository(db, vfs)
        gitManager = GitManager(app)
    }
}
