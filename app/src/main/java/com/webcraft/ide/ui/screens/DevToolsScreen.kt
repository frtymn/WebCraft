package com.webcraft.ide.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.webcraft.ide.tools.*
import java.util.regex.Pattern

/**
 * Developer Toolkit.
 * All tools run locally and offline.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperToolsScreen(nav: NavController, projectId: Long) {
    var activeTool by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Developer Tools", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Filled.ArrowBack, "Back") }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (activeTool == null) {
                ToolCard("JSON Formatter", "Pretty-print or minify JSON") { activeTool = "json" }
                ToolCard("Base64 Encoder/Decoder", "Encode and decode Base64") { activeTool = "base64" }
                ToolCard("URL Encoder/Decoder", "Percent-encode/decode URLs") { activeTool = "url" }
                ToolCard("UUID Generator", "Generate v4 and v7 UUIDs") { activeTool = "uuid" }
                ToolCard("Timestamp Converter", "Epoch ↔ ISO 8601") { activeTool = "timestamp" }
                ToolCard("Regex Tester", "Test regex patterns live") { activeTool = "regex" }
                ToolCard("Color Picker", "Convert HEX, RGB, HSL") { activeTool = "color" }
                ToolCard("HTTP Request Tester", "Send GET/POST requests") { activeTool = "http" }
            } else {
                when (activeTool) {
                    "json" -> JsonFormatterTool(onBack = { activeTool = null })
                    "base64" -> Base64Tool(onBack = { activeTool = null })
                    "url" -> UrlTool(onBack = { activeTool = null })
                    "uuid" -> UuidTool(onBack = { activeTool = null })
                    "timestamp" -> TimestampTool(onBack = { activeTool = null })
                    "regex" -> RegexTool(onBack = { activeTool = null })
                    "color" -> ColorPickerTool(onBack = { activeTool = null })
                    "http" -> HttpTesterTool(onBack = { activeTool = null })
                }
            }
        }
    }
}

// ── Components ───────────────────────────────────────────────────

@Composable
private fun ToolCard(title: String, description: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(description, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ToolScaffold(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") }
            Text(title, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun CodeField(
    label: String,
    value: String,
    readOnly: Boolean = false,
    minLines: Int = 4,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        readOnly = readOnly,
        minLines = minLines,
        textStyle = androidx.compose.ui.text.TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

// ── JSON Formatter ──────────────────────────────────────────────

@Composable
private fun JsonFormatterTool(onBack: () -> Unit) {
    var input by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    ToolScaffold("JSON Formatter", onBack) {
        CodeField("Input", input) { input = it; error = null }
        Spacer(Modifier.height(8.dp))
        Row {
            TextButton(onClick = {
                try { output = JsonTools.prettyPrint(input); error = null }
                catch (e: Exception) { error = e.message }
            }) { Text("Pretty") }
            TextButton(onClick = {
                try { output = JsonTools.minify(input); error = null }
                catch (e: Exception) { error = e.message }
            }) { Text("Minify") }
        }
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
        }
        CodeField("Output", output, readOnly = true, minLines = 6, onValueChange = { _ -> })
    }
}

// ── Base64 ──────────────────────────────────────────────────────

@Composable
private fun Base64Tool(onBack: () -> Unit) {
    var input by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf("encode") }

    ToolScaffold("Base64", onBack) {
        CodeField("Input", input) { input = it }
        Spacer(Modifier.height(8.dp))
        Row {
            FilterChip(selected = mode == "encode", onClick = { mode = "encode" },
                label = { Text("Encode") })
            Spacer(Modifier.width(8.dp))
            FilterChip(selected = mode == "decode", onClick = { mode = "decode" },
                label = { Text("Decode") })
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = {
            output = try {
                if (mode == "encode") Base64Tools.encode(input)
                else Base64Tools.decode(input)
            } catch (e: Exception) { "Error: ${e.message}" }
        }) { Text("Run") }
        CodeField("Output", output, readOnly = true, onValueChange = { _ -> })
    }
}

// ── URL Encoder ─────────────────────────────────────────────────

@Composable
private fun UrlTool(onBack: () -> Unit) {
    var input by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf("encode") }

    ToolScaffold("URL Encoder/Decoder", onBack) {
        CodeField("Input", input) { input = it }
        Spacer(Modifier.height(8.dp))
        Row {
            FilterChip(selected = mode == "encode", onClick = { mode = "encode" },
                label = { Text("Encode") })
            Spacer(Modifier.width(8.dp))
            FilterChip(selected = mode == "decode", onClick = { mode = "decode" },
                label = { Text("Decode") })
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = {
            output = try {
                if (mode == "encode") UrlTools.encode(input) else UrlTools.decode(input)
            } catch (e: Exception) { "Error: ${e.message}" }
        }) { Text("Run") }
        CodeField("Output", output, readOnly = true, onValueChange = { _ -> })
    }
}

// ── UUID ────────────────────────────────────────────────────────

@Composable
private fun UuidTool(onBack: () -> Unit) {
    var uuids by remember { mutableStateOf(listOf<String>()) }

    ToolScaffold("UUID Generator", onBack) {
        Row {
            TextButton(onClick = { uuids = uuids + UuidTools.v4() }) { Text("Generate v4") }
            TextButton(onClick = { uuids = uuids + UuidTools.v7() }) { Text("Generate v7") }
        }
        Spacer(Modifier.height(8.dp))
        Column(Modifier.verticalScroll(rememberScrollState())) {
            uuids.reversed().forEach { uuid ->
                Text(uuid, fontFamily = FontFamily.Monospace, fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 2.dp))
            }
            if (uuids.isEmpty()) {
                Text("Tap generate to create UUIDs.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
        }
    }
}

// ── Timestamp ────────────────────────────────────────────────────

@Composable
private fun TimestampTool(onBack: () -> Unit) {
    var epochInput by remember { mutableStateOf(TimestampTools.nowEpoch().toString()) }
    var isoOutput by remember { mutableStateOf("") }
    var isoInput by remember { mutableStateOf("") }
    var epochOutput by remember { mutableStateOf("") }

    ToolScaffold("Timestamp Converter", onBack) {
        Text("Epoch → ISO", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = epochInput, onValueChange = { epochInput = it },
                label = { Text("Epoch (s or ms)") },
                modifier = Modifier.weight(1f), singleLine = true,
                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp)
            )
            TextButton(onClick = {
                isoOutput = try {
                    val n = epochInput.trim().toLong()
                    if (n > 10_000_000_000) TimestampTools.epochMsToIso(n)
                    else TimestampTools.epochToIso(n)
                } catch (e: Exception) { "Error: ${e.message}" }
            }) { Text("Convert") }
        }
        CodeField("ISO 8601", isoOutput, readOnly = true, minLines = 1, onValueChange = { _ -> })
        Spacer(Modifier.height(16.dp))
        Text("ISO → Epoch", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = isoInput, onValueChange = { isoInput = it },
            label = { Text("ISO 8601") },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp)
        )
        TextButton(onClick = {
            epochOutput = try { TimestampTools.isoToEpochMs(isoInput).toString() }
            catch (e: Exception) { "Error: ${e.message}" }
        }) { Text("Convert") }
        Text(epochOutput, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
    }
}

// ── Regex Tester ─────────────────────────────────────────────────

@Composable
private fun RegexTool(onBack: () -> Unit) {
    var pattern by remember { mutableStateOf("") }
    var input by remember { mutableStateOf("") }
    var flags by remember { mutableStateOf(0) }
    var matches by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }

    fun run() {
        if (pattern.isBlank() || !RegexTools.isValid(pattern)) {
            matches = emptyList(); return
        }
        matches = try { RegexTools.findMatches(pattern, input, flags) }
        catch (_: Exception) { emptyList() }
    }

    ToolScaffold("Regex Tester", onBack) {
        OutlinedTextField(
            value = pattern, onValueChange = { pattern = it; run() },
            label = { Text("Pattern") },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp)
        )
        Spacer(Modifier.height(8.dp))
        Row {
            FilterChip(selected = flags and Pattern.CASE_INSENSITIVE != 0,
                onClick = { flags = flags xor Pattern.CASE_INSENSITIVE; run() },
                label = { Text("i") })
            Spacer(Modifier.width(8.dp))
            FilterChip(selected = flags and Pattern.MULTILINE != 0,
                onClick = { flags = flags xor Pattern.MULTILINE; run() },
                label = { Text("m") })
            Spacer(Modifier.width(8.dp))
            FilterChip(selected = flags and Pattern.DOTALL != 0,
                onClick = { flags = flags xor Pattern.DOTALL; run() },
                label = { Text("s") })
        }
        Spacer(Modifier.height(8.dp))
        CodeField("Input", input) { input = it; run() }
        Spacer(Modifier.height(8.dp))
        if (pattern.isBlank()) {
            Text("Enter a pattern.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else if (!RegexTools.isValid(pattern)) {
            Text("Invalid pattern", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        } else {
            Text("${matches.size} match${if (matches.size == 1) "" else "es"}",
                style = MaterialTheme.typography.labelMedium)
            matches.take(20).forEach { (text, start) ->
                Text("[$start] \"$text\"", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }
        }
    }
}

// ── Color Picker ─────────────────────────────────────────────────

@Composable
private fun ColorPickerTool(onBack: () -> Unit) {
    var hex by remember { mutableStateOf("#7AA2FF") }
    val rgb = ColorTools.hexToRgb(hex) ?: ColorTools.Rgb(122, 162, 255)
    val hexOut = ColorTools.rgbToHex(rgb.r, rgb.g, rgb.b)
    val hsl = ColorTools.rgbToHsl(rgb.r, rgb.g, rgb.b)
    val cmyk = ColorTools.rgbToCmyk(rgb.r, rgb.g, rgb.b)
    val contrast = ColorTools.contrastRatio(hexOut, "#0F1114") ?: 1.0

    ToolScaffold("Color Picker", onBack) {
        OutlinedTextField(
            value = hex, onValueChange = { hex = it },
            label = { Text("HEX") },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp)
        )
        Spacer(Modifier.height(8.dp))
        Surface(
            color = androidx.compose.ui.graphics.Color(
                android.graphics.Color.rgb(rgb.r, rgb.g, rgb.b)
            ),
            modifier = Modifier.fillMaxWidth().height(80.dp)
        ) {}
        Spacer(Modifier.height(8.dp))
        Text("HEX: $hexOut", fontFamily = FontFamily.Monospace, fontSize = 13.sp)
        Text("RGB: rgb(${rgb.r}, ${rgb.g}, ${rgb.b})", fontFamily = FontFamily.Monospace, fontSize = 13.sp)
        Text("HSL: hsl(${hsl.first.toInt()}, ${(hsl.second * 100).toInt()}%, ${(hsl.third * 100).toInt()}%)",
            fontFamily = FontFamily.Monospace, fontSize = 13.sp)
        Text("CMYK: cmyk(${(cmyk.first * 100).toInt()}%, ${(cmyk.second * 100).toInt()}%, ${(cmyk.third * 100).toInt()}%, ${(0 * 100).toInt()}%)",
            fontFamily = FontFamily.Monospace, fontSize = 13.sp)
        Text("Contrast vs #0F1114: ${"%.2f".format(contrast)}:1 ${if (contrast >= 4.5) "(AA)" else "(low)"}",
            fontFamily = FontFamily.Monospace, fontSize = 13.sp)
    }
}

// ── HTTP Tester ──────────────────────────────────────────────────

/**
 * HTTP Request Tester. NOTE: this performs a real network request when
 * executed. Since we're building offline-first, this tool is available
 * but disabled by default in this release to avoid surprising the user
 * with network access. It is documented as a known limitation.
 */
@Composable
private fun HttpTesterTool(onBack: () -> Unit) {
    var url by remember { mutableStateOf("https://") }
    var method by remember { mutableStateOf("GET") }
    var body by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }

    ToolScaffold("HTTP Request Tester", onBack) {
        OutlinedTextField(
            value = url, onValueChange = { url = it },
            label = { Text("URL") },
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        Row {
            listOf("GET", "POST", "PUT", "DELETE").forEach { m ->
                FilterChip(
                    selected = method == m,
                    onClick = { method = m },
                    label = { Text(m) },
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
            }
        }
        if (method != "GET") {
            Spacer(Modifier.height(8.dp))
            CodeField("Body", body) { body = it }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Network requests are not implemented in v0.1.0 (offline-first release). " +
            "This tool is a placeholder UI only and is documented as a limitation.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextButton(onClick = { result = "Not implemented in v0.1.0" }) { Text("Send") }
        if (result.isNotBlank()) {
            Text(result, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        }
    }
}
