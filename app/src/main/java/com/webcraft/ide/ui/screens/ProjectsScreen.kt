package com.webcraft.ide.ui.screens

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.webcraft.ide.data.models.Project
import com.webcraft.ide.ui.AppViewModelFactory
import com.webcraft.ide.ui.Dest
import com.webcraft.ide.ui.ProjectsViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(nav: NavController) {
    val vm: ProjectsViewModel = viewModel(factory = AppViewModelFactory(nav.context.applicationContext as Application))
    val state by vm.ui.collectAsState()
    var search by remember { mutableStateOf("") }

    LaunchedEffect(search) { vm.search(search) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Projects", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { nav.navigate(Dest.Settings.route) }) {
                        Icon(Icons.Filled.Settings, "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Search projects…") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )
            if (state.error != null) {
                Text(state.error!!, color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp))
            }
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.projects, key = { it.id }) { project ->
                    ProjectRow(project, vm, nav)
                }
                if (state.projects.isEmpty() && !state.loading) {
                    item {
                        Text("No projects match \"${search}\".",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 32.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectRow(project: Project, vm: ProjectsViewModel, nav: NavController) {
    val fmt = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().clickable {
            nav.navigate(Dest.Editor.editor(project.id))
        },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(project.name, style = MaterialTheme.typography.titleSmall)
                    Text("${fmt.format(project.updatedAt)} • ${if (project.hasGit) project.gitBranch else "no git"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, "Menu")
                }
            }
            Row {
                TextButton(onClick = { nav.navigate(Dest.Preview.preview(project.id)) }) { Text("Preview") }
                TextButton(onClick = { nav.navigate(Dest.CraftAi.craftAi(project.id)) }) { Text("Craft AI") }
                TextButton(onClick = { nav.navigate(Dest.DevTools.devTools(project.id)) }) { Text("Tools") }
            }
        }
    }

    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
        DropdownMenuItem(
            text = { Text("Rename") },
            leadingIcon = { Icon(Icons.Filled.Edit, null) },
            onClick = {
                menuOpen = false
                // TODO: rename dialog
            }
        )
        DropdownMenuItem(
            text = { Text("Duplicate") },
            leadingIcon = { Icon(Icons.Filled.FileCopy, null) },
            onClick = { vm.duplicateProject(project.id); menuOpen = false }
        )
        DropdownMenuItem(
            text = { Text("Delete") },
            leadingIcon = { Icon(Icons.Filled.Delete, null) },
            onClick = { menuOpen = false; confirmDelete = true }
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete \"${project.name}\"?") },
            text = { Text("This permanently deletes the project and all its files. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { vm.deleteProject(project.id); confirmDelete = false }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            }
        )
    }
}
