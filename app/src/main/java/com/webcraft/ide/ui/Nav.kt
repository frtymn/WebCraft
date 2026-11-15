package com.webcraft.ide.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.webcraft.ide.ui.screens.CraftAiScreen
import com.webcraft.ide.ui.screens.DeveloperToolsScreen
import com.webcraft.ide.ui.screens.EditorScreen
import com.webcraft.ide.ui.screens.HomeScreen
import com.webcraft.ide.ui.screens.PreviewScreen
import com.webcraft.ide.ui.screens.ProjectsScreen
import com.webcraft.ide.ui.screens.SettingsScreen
import com.webcraft.ide.ui.theme.WebCraftTheme

sealed class Dest(val route: String) {
    object Home : Dest("home")
    object Projects : Dest("projects")
    object Editor : Dest("editor/{projectId}")
    object Preview : Dest("preview/{projectId}")
    object CraftAi : Dest("craftai/{projectId}")
    object DevTools : Dest("devtools/{projectId}")
    object Settings : Dest("settings")

    fun editor(projectId: Long) = "editor/$projectId"
    fun preview(projectId: Long) = "preview/$projectId"
    fun craftAi(projectId: Long) = "craftai/$projectId"
    fun devTools(projectId: Long) = "devtools/$projectId"
}

@Composable
fun WebCraftRoot() {
    WebCraftTheme {
        val nav = rememberNavController()
        NavHost(navController = nav, startDestination = Dest.Home.route) {
            composable(Dest.Home.route) { HomeScreen(nav) }
            composable(Dest.Projects.route) { ProjectsScreen(nav) }
            composable(Dest.Editor.route) { backStack ->
                val projectId = backStack.arguments?.getString("projectId")?.toLongOrNull() ?: 0L
                EditorScreen(nav, projectId)
            }
            composable(Dest.Preview.route) { backStack ->
                val projectId = backStack.arguments?.getString("projectId")?.toLongOrNull() ?: 0L
                PreviewScreen(nav, projectId)
            }
            composable(Dest.CraftAi.route) { backStack ->
                val projectId = backStack.arguments?.getString("projectId")?.toLongOrNull() ?: 0L
                CraftAiScreen(nav, projectId)
            }
            composable(Dest.DevTools.route) { backStack ->
                val projectId = backStack.arguments?.getString("projectId")?.toLongOrNull() ?: 0L
                DeveloperToolsScreen(nav, projectId)
            }
            composable(Dest.Settings.route) { SettingsScreen(nav) }
        }
    }
}
