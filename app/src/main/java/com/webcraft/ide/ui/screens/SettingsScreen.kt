package com.webcraft.ide.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(nav: NavController) {
    var autoSave by remember { mutableStateOf(true) }
    var autoRefresh by remember { mutableStateOf(true) }
    var wrapWords by remember { mutableStateOf(true) }
    var fontSize by remember { mutableStateOf(14f) }
    var tabSize by remember { mutableStateOf(2) }
    var useDarkTheme by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            TextSection("Editor")
            SettingToggle("Autosave", "Save files automatically after edits", autoSave) { autoSave = it }
            SettingToggle("Word wrap", "Wrap long lines in the editor", wrapWords) { wrapWords = it }

            SettingSlider("Font size", fontSize, 10f, 24f, "${fontSize.toInt()}sp") { fontSize = it }

            TextSection("Preview")
            SettingToggle("Auto refresh", "Refresh preview when files change", autoRefresh) { autoRefresh = it }

            TextSection("Theme")
            SettingToggle("Dark theme", "Use the dark IDE theme", useDarkTheme) { useDarkTheme = it }

            TextSection("About")
            Text(
                "WebCraft v0.1.0\nMIT License",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Craft AI runs fully offline using rules-based analysis. It is not an LLM.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TextSection(label: String) {
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingToggle(title: String, subtitle: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onToggle)
    }
}

@Composable
private fun SettingSlider(
    label: String,
    value: Float,
    min: Float,
    max: Float,
    display: String,
    onValue: (Float) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(display, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = value,
            onValueChange = onValue,
            valueRange = min..max
        )
    }
}
