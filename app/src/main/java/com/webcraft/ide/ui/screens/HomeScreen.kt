package com.webcraft.ide.ui.screens

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.webcraft.ide.ui.AppViewModelFactory
import com.webcraft.ide.ui.Dest
import com.webcraft.ide.ui.ProjectsViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(nav: NavController) {
    val vm: ProjectsViewModel = viewModel(factory = AppViewModelFactory(nav.context.applicationContext as Application))
    val state by vm.ui.collectAsState()

    var showCreate by remember { mutableStateOf(false) }
    var projectName by remember { mutableStateOf("") }
    var projectDesc by remember { mutableStateOf("") }
    var selectedTemplate by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WebCraft", fontWeight = FontWeight.Bold, fontSize = 22.sp) },
                actions = {
                    IconButton(onClick = { nav.navigate(Dest.Projects.route) }) {
                        Icon(Icons.Filled.Folder, "Projects")
                    }
                    IconButton(onClick = { nav.navigate(Dest.Settings.route) }) {
                        Icon(Icons.Filled.Settings, "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreate = true },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("New Project") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Quick actions
            item {
                Text("Quick Actions", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = { nav.navigate(Dest.Projects.route) },
                        label = { Text("Projects") }, leadingIcon = { Icon(Icons.Filled.Folder, null, Modifier.size(16.dp)) })
                    AssistChip(onClick = { nav.navigate(Dest.Settings.route) },
                        label = { Text("Settings") }, leadingIcon = { Icon(Icons.Filled.Settings, null, Modifier.size(16.dp)) })
                }
            }

            // Recent projects
            item {
                if (state.recent.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text("Recent Projects", style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            items(state.recent) { project ->
                ProjectCard(
                    project = project,
                    onClick = { nav.navigate(Dest.Editor.editor(project.id)) },
                    onPreview = { nav.navigate(Dest.Preview.preview(project.id)) },
                    onFavorite = { vm.toggleFavorite(project.id, project.isFavorite) }
                )
            }

            if (state.recent.isEmpty() && !state.loading) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No projects yet", style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Tap \"New Project\" to get started",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    if (showCreate) {
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("New Project") },
            text = {
                Column {
                    OutlinedTextField(
                        value = projectName,
                        onValueChange = { projectName = it },
                        label = { Text("Project name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = projectDesc,
                        onValueChange = { projectDesc = it },
                        label = { Text("Description (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Template", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.height(240.dp)) {
                        items(vm.templates) { t ->
                            Surface(
                                color = if (selectedTemplate == t.id)
                                    MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surface,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .clickable { selectedTemplate = if (selectedTemplate == t.id) null else t.id }
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(t.name, style = MaterialTheme.typography.titleSmall)
                                    Text(t.description, style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (projectName.isNotBlank()) {
                            vm.createProject(projectName, selectedTemplate, projectDesc) { res ->
                                showCreate = false
                                projectName = ""; projectDesc = ""; selectedTemplate = null
                                res.onSuccess { nav.navigate(Dest.Editor.editor(it.id)) }
                            }
                        }
                    }
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreate = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ProjectCard(
    project: com.webcraft.ide.data.models.Project,
    onClick: () -> Unit,
    onPreview: () -> Unit,
    onFavorite: () -> Unit
) {
    val fmt = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    project.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onFavorite, modifier = Modifier.size(24.dp)) {
                    Icon(
                        if (project.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        "Favorite",
                        tint = if (project.isFavorite) MaterialTheme.colorScheme.tertiary
                               else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            if (project.description.isNotBlank()) {
                Text(project.description, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Updated ${fmt.format(project.updatedAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onPreview) { Text("Preview") }
            }
        }
    }
}
