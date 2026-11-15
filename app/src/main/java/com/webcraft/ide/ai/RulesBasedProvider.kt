package com.webcraft.ide.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

/**
 * Deterministic, rule-based analysis engine. Runs 100% on-device with
 * no network access, no model weights, and no external service.
 *
 * This is NOT an LLM. It applies curated static-analysis heuristics to
 * produce explanations, lint diagnostics, and formatting recommendations.
 */
class RulesBasedProvider : LocalAIProvider {

    override val displayName = "Craft Rules"
    override val isGenerative = false

    // ── Language detection ─────────────────────────────────────────

    private fun detectLanguage(code: String, hint: String = ""): String {
        if (hint.isNotBlank()) return hint.lowercase()
        if (code.contains("<!DOCTYPE") || code.contains("<html")) return "html"
        if (Regex("""\{[^}]*:[^}]*;""").containsMatchIn(code) &&
            !code.contains("function") && !code.contains("=>")) return "css"
        if (code.trim().startsWith("{") || code.trim().startsWith("[")) return "json"
        if (code.contains("interface ") || code.contains(": string") || code.contains(": number")) return "typescript"
        return "javascript"
    }

    // ── HTML structural analysis ───────────────────────────────────

    private fun analyzeHtml(code: String): List<Diagnostic> {
        val issues = mutableListOf<Diagnostic>()
        val lines = code.lines()

        lines.forEachIndexed { i, line ->
            val n = i + 1
            val openTags = Regex("""<([a-zA-Z][a-zA-Z0-9]*)\b[^>]*(?<!/)>""").findAll(line)
            for (m in openTags) {
                val tag = m.groupValues[1].lowercase()
                if (tag in VOID_TAGS) continue
                val closePattern = Regex("""</$tag\s*>""", RegexOption.IGNORE_CASE)
                if (!closePattern.containsMatchIn(code.substring(m.range.first))) {
                    issues.add(Diagnostic(n, 1, Severity.WARNING,
                        "Unclosed <$tag> tag. Every non-void element needs a matching closing tag.",
                        "html/unclosed-tag"))
                }
            }
            if (line.contains("<img") && !line.contains("alt=")) {
                issues.add(Diagnostic(n, 1, Severity.WARNING,
                    "<img> without alt attribute hurts accessibility.",
                    "html/img-alt"))
            }
            if (line.contains("<a ") && !line.contains("href")) {
                issues.add(Diagnostic(n, 1, Severity.WARNING,
                    "<a> without href is not navigable.",
                    "html/a-href"))
            }
        }
        if (!code.contains("<!DOCTYPE", ignoreCase = true)) {
            issues.add(Diagnostic(1, 1, Severity.INFO,
                "Missing <!DOCTYPE html>. Browsers fall back to quirks mode.",
                "html/doctype"))
        }
        if (!code.contains("viewport", ignoreCase = true) && code.contains("<head>", ignoreCase = true)) {
            issues.add(Diagnostic(1, 1, Severity.WARNING,
                "Missing viewport meta tag; mobile rendering will be scaled incorrectly.",
                "html/viewport"))
        }
        return issues
    }

    // ── CSS linting ────────────────────────────────────────────────

    private fun lintCss(code: String): List<Diagnostic> {
        val issues = mutableListOf<Diagnostic>()
        val lines = code.lines()
        val usedVars = mutableSetOf<String>()

        lines.forEachIndexed { i, line ->
            val n = i + 1
            // undefined var usage
            Regex("""var\(--([a-zA-Z0-9-]+)\)""").findAll(line).forEach { m ->
                val name = m.groupValues[1]
                if (!code.contains("--$name:") && name !in usedVars) {
                    issues.add(Diagnostic(n, 1, Severity.ERROR,
                        "CSS variable --$name is used but never defined.", "css/undefined-var"))
                }
            }
            // duplicate property in same block (simple heuristic)
            Regex("""([a-zA-Z-]+)\s*:""").findAll(line).forEach { m ->
                // handled below
            }
            if (line.contains("!important")) {
                issues.add(Diagnostic(n, 1, Severity.INFO,
                    "!important can be avoided by increasing specificity.", "css/important"))
            }
            if (line.contains("px") && line.contains("font-size") && Regex("""font-size:\s*\d{3,}px""").containsMatchIn(line)) {
                issues.add(Diagnostic(n, 1, Severity.WARNING,
                    "Very large font-size in px; prefer rem units for accessibility.",
                    "css/font-size"))
            }
        }
        // duplicate selectors (very simple)
        val selectorCounts = mutableMapOf<String, Int>()
        Regex("""^([.#]?[a-zA-Z][\w-]*(?:[\s>+][\w.#-]+)*)\s*\{""", RegexOption.MULTILINE)
            .findAll(code).forEach { m ->
                val sel = m.groupValues[1].trim()
                selectorCounts[sel] = (selectorCounts[sel] ?: 0) + 1
            }
        selectorCounts.filter { it.value > 1 }.forEach { (sel, count) ->
            issues.add(Diagnostic(1, 1, Severity.WARNING,
                "Selector \"$sel\" appears $count times. Consider combining rules.",
                "css/duplicate-selector"))
        }
        return issues
    }

    // ── JavaScript linting ─────────────────────────────────────────

    private fun lintJs(code: String, isTs: Boolean): List<Diagnostic> {
        val issues = mutableListOf<Diagnostic>()
        val lines = code.lines()

        lines.forEachIndexed { i, line ->
            val n = i + 1
            val stripped = line.trim()
            if (stripped.isEmpty() || stripped.startsWith("//") || stripped.startsWith("*")) return@forEachIndexed

            if (Regex("""\bvar\s+""").containsMatchIn(line)) {
                issues.add(Diagnostic(n, 1, Severity.WARNING,
                    "Use let/const instead of var. var is function-scoped and hoisted.",
                    "js/no-var"))
            }
            if (Regex("""\b(==|!=)\s""").containsMatchIn(line) && !line.contains("===") && !line.contains("!==")) {
                issues.add(Diagnostic(n, 1, Severity.WARNING,
                    "Prefer strict equality (=== / !==) to avoid type coercion.",
                    "js/eqeqeq"))
            }
            if (Regex("""console\.(log|debug|info)\(""").containsMatchIn(line)) {
                issues.add(Diagnostic(n, 1, Severity.INFO,
                    "console statement left in code. Consider removing before shipping.",
                    "js/no-console"))
            }
            if (Regex("""eval\(""").containsMatchIn(line)) {
                issues.add(Diagnostic(n, 1, Severity.ERROR,
                    "eval() is dangerous and blocks CSP. Avoid it.",
                    "js/no-eval"))
            }
            if (Regex("""document\.write\(""").containsMatchIn(line)) {
                issues.add(Diagnostic(n, 1, Severity.ERROR,
                    "document.write blocks parsing. Use DOM APIs instead.",
                    "js/no-document-write"))
            }
            if (Regex("""\bdebugger\b""").containsMatchIn(line)) {
                issues.add(Diagnostic(n, 1, Severity.ERROR,
                    "debugger statement will pause execution in devtools.",
                    "js/no-debugger"))
            }
            // unbalanced braces on this line (rough heuristic for readability)
            if (line.count { it == '{' } != line.count { it == '}' } &&
                stripped.endsWith("{") == false && stripped.endsWith("}") == false &&
                !stripped.contains("{") && !stripped.contains("}")) {
                // skip — balanced multiline block
            }
            if (!isTs && Regex("""\bany\b""").containsMatchIn(line) && line.contains(":")) {
                issues.add(Diagnostic(n, 1, Severity.INFO,
                    "TS-style annotation in a JS file will be a syntax error.",
                    "js/ts-in-js"))
            }
        }

        // Check balanced braces file-wide
        val open = code.count { it == '{' }
        val close = code.count { it == '}' }
        if (open != close) {
            issues.add(Diagnostic(1, 1, Severity.ERROR,
                "Unbalanced braces: $open opening vs $close closing. Check your blocks.",
                "js/unbalanced-braces"))
        }
        val parenOpen = code.count { it == '(' }
        val parenClose = code.count { it == ')' }
        if (parenOpen != parenClose) {
            issues.add(Diagnostic(1, 1, Severity.ERROR,
                "Unbalanced parentheses: $parenOpen open vs $parenClose close.",
                "js/unbalanced-parens"))
        }
        return issues
    }

    // ── Explanation ────────────────────────────────────────────────

    override suspend fun explain(language: String, code: String): AiResult =
        withContext(Dispatchers.Default) {
            val lang = detectLanguage(code, language)
            val lines = code.lines().size
            val builder = StringBuilder()
            builder.appendLine("Language: ${lang.replaceFirstChar { it.uppercase() }}")
            builder.appendLine("Length: $lines lines, ${code.length} characters")
            builder.appendLine()

            when (lang) {
                "html" -> {
                    val tags = Regex("""<([a-zA-Z][a-zA-Z0-9]*)\b""")
                        .findAll(code).map { it.groupValues[1].lowercase() }
                        .filter { it !in VOID_TAGS }.distinct().toList()
                    builder.appendLine("Structure:")
                    builder.appendLine("  This document uses these elements: ${tags.joinToString(", ")}.")
                    if ("<form" in code) builder.appendLine("  It contains a form for user input.")
                    if ("<table" in code) builder.appendLine("  It includes tabular data.")
                    if ("<script" in code) builder.appendLine("  Script behavior is embedded or linked.")
                    if ("<link" in code) builder.appendLine("  External stylesheets are linked.")
                }
                "css" -> {
                    val selectors = Regex("""^([^{}]+)\{""", RegexOption.MULTILINE)
                        .findAll(code).map { it.groupValues[1].trim() }.toList()
                    val props = Regex("""([a-zA-Z-]+)\s*:""").findAll(code)
                        .map { it.groupValues[1] }.distinct().toList()
                    builder.appendLine("Styles:")
                    builder.appendLine("  Selectors: ${selectors.take(10).joinToString(", ")}${if (selectors.size > 10) "…" else ""}")
                    builder.appendLine("  Properties used: ${props.take(15).joinToString(", ")}${if (props.size > 15) "…" else ""}")
                    if (selectors.isEmpty()) builder.appendLine("  No top-level rules found; may be inside @media or nested.")
                }
                "javascript", "typescript" -> {
                    val functions = Regex("""(function\s+(\w+)|const\s+(\w+)\s*=\s*(?:async\s*)?\(|(\w+)\s*=\s*(?:async\s*)?\([^)]*\)\s*=>)""")
                        .findAll(code).map { it.groups.drop(1).firstOrNull { it != null }?.value ?: "?" }
                        .distinct().toList()
                    val awaits = Regex("""\bawait\b""").findAll(code).count()
                    val promises = Regex("""\.then\(""").findAll(code).count()
                    builder.appendLine("Behavior:")
                    if (functions.isNotEmpty()) builder.appendLine("  Functions: ${functions.joinToString(", ")}")
                    if (awaits > 0) builder.appendLine("  Uses async/await ($awaits await expressions).")
                    if (promises > 0) builder.appendLine("  Uses promise chains (.then × $promises).")
                    val domRefs = Regex("""(?:getElementById|querySelector|querySelectorAll)\(""([^"]+)""""")
                        .findAll(code).map { it.groupValues[1] }.distinct().toList()
                    if (domRefs.isNotEmpty()) builder.appendLine("  Touches DOM elements: ${domRefs.joinToString(", ")}")
                    if (functions.isEmpty() && awaits == 0 && promises == 0) {
                        builder.appendLine("  Top-level script; runs immediately on load.")
                    }
                }
                "json" -> {
                    builder.appendLine("Data:")
                    val keys = Regex(""""([^"]+)"\s*:""").findAll(code)
                        .map { it.groupValues[1] }.distinct().take(10).toList()
                    builder.appendLine("  Top-level keys: ${keys.joinToString(", ")}")
                }
            }
            builder.appendLine()
            val issues = lint(lang, code)
            if (issues.isNotEmpty()) {
                builder.appendLine("Potential issues (${issues.size}):")
                issues.take(5).forEach {
                    builder.appendLine("  [${it.severity}] line ${it.line}: ${it.message}")
                }
            } else {
                builder.appendLine("No issues detected by static rules.")
            }
            AiResult(builder.toString().trim(), confidence = 0.85f)
        }

    // ── Lint ──────────────────────────────────────────────────────

    override suspend fun lint(language: String, code: String): List<Diagnostic> =
        withContext(Dispatchers.Default) {
            val lang = detectLanguage(code, language)
            when (lang) {
                "html" -> analyzeHtml(code)
                "css" -> lintCss(code)
                "javascript" -> lintJs(code, isTs = false)
                "typescript" -> lintJs(code, isTs = true)
                "json" -> lintJson(code)
                else -> emptyList()
            }
        }

    private fun lintJson(code: String): List<Diagnostic> {
        val issues = mutableListOf<Diagnostic>()
        // Trailing commas
        if (Regex(""",\s*[}\]]""").containsMatchIn(code)) {
            val line = code.substringBefore(Regex(""",\s*[}\]]""").find(code)!!.value).count { it == '\n' } + 1
            issues.add(Diagnostic(line, 1, Severity.ERROR,
                "Trailing comma in JSON is invalid per RFC 8259.", "json/trailing-comma"))
        }
        // Single quotes
        if (Regex("""'[^']*'\s*:""").containsMatchIn(code)) {
            issues.add(Diagnostic(1, 1, Severity.ERROR,
                "JSON requires double quotes for strings.", "json/single-quotes"))
        }
        // Comments
        if (code.contains("//") && !code.contains("http")) {
            issues.add(Diagnostic(1, 1, Severity.ERROR,
                "Comments are not allowed in JSON.", "json/comments"))
        }
        // Validate structure
        try {
            val trimmed = code.trim()
            if (trimmed.isNotEmpty()) {
                // Basic structural check with a tiny recursive descent
                validateJsonStructure(trimmed)?.let { (line, msg) ->
                    issues.add(Diagnostic(line, 1, Severity.ERROR, msg, "json/parse"))
                }
            }
        } catch (e: Exception) {
            issues.add(Diagnostic(1, 1, Severity.ERROR, "Invalid JSON: ${e.message}", "json/parse"))
        }
        return issues
    }

    private fun validateJsonStructure(s: String): Pair<Int, String>? {
        val parser = JsonParser(s)
        val err = parser.parseValue()
        if (err != null) {
            return parser.currentLine() to err
        }
        parser.skipWhitespace()
        if (parser.hasMore()) {
            return parser.currentLine() to "unexpected trailing data"
        }
        return null
    }

    /** Minimal recursive-descent JSON parser for diagnostics. */
    private class JsonParser(private val s: String) {
        private var i = 0

        fun skipWhitespace() { while (i < s.length && s[i].isWhitespace()) i++ }
        fun hasMore(): Boolean = i < s.length
        fun currentLine(): Int = s.substring(0, i.coerceAtMost(s.length)).count { it == '\n' } + 1

        fun parseValue(): String? {
            skipWhitespace()
            if (i >= s.length) return "unexpected end of input"
            return when (s[i]) {
                '{' -> parseObject()
                '[' -> parseArray()
                '"' -> parseString()
                't' -> if (s.startsWith("true", i)) { i += 4; null } else "invalid literal"
                'f' -> if (s.startsWith("false", i)) { i += 5; null } else "invalid literal"
                'n' -> if (s.startsWith("null", i)) { i += 4; null } else "invalid literal"
                else -> {
                    val m = Regex("""-?\d+(\.\d+)?([eE][+-]?\d+)?""").matchAt(s, i)
                    if (m != null) { i = m.range.last + 1; null } else "unexpected character '${s[i]}'"
                }
            }
        }

        private fun parseObject(): String? {
            i++; skipWhitespace()
            if (i < s.length && s[i] == '}') { i++; return null }
            while (true) {
                skipWhitespace()
                if (i >= s.length || s[i] != '"') return "expected string key"
                parseString()?.let { return it }
                skipWhitespace()
                if (i >= s.length || s[i] != ':') return "expected ':'"
                i++
                parseValue()?.let { return it }
                skipWhitespace()
                if (i < s.length && s[i] == ',') { i++; continue }
                if (i < s.length && s[i] == '}') { i++; return null }
                return "expected ',' or '}'"
            }
        }

        private fun parseArray(): String? {
            i++; skipWhitespace()
            if (i < s.length && s[i] == ']') { i++; return null }
            while (true) {
                parseValue()?.let { return it }
                skipWhitespace()
                if (i < s.length && s[i] == ',') { i++; continue }
                if (i < s.length && s[i] == ']') { i++; return null }
                return "expected ',' or ']'"
            }
        }

        private fun parseString(): String? {
            if (i >= s.length || s[i] != '"') return "expected string"
            i++
            while (i < s.length && s[i] != '"') {
                if (s[i] == '\\') i++
                i++
            }
            if (i >= s.length) return "unterminated string"
            i++
            return null
        }
    }

    // ── Suggestions ───────────────────────────────────────────────

    override suspend fun suggest(language: String, code: String): List<Suggestion> =
        withContext(Dispatchers.Default) {
            val lang = detectLanguage(code, language)
            val suggestions = mutableListOf<Suggestion>()
            when (lang) {
                "html" -> {
                    if (!code.contains("lang=")) suggestions.add(Suggestion(
                        "Add lang attribute", "Set <html lang=\"en\"> for screen readers and translation tools.",
                        "html"))
                    if (!code.contains("<meta name=\"description\"")) suggestions.add(Suggestion(
                        "Add meta description", "Improves SEO and link previews.",
                        "html"))
                    if (Regex("""<(div|span) class="[^"]*"\s*>\s*</\1>""").containsMatchIn(code)) suggestions.add(Suggestion(
                        "Remove empty elements", "Empty <div>/<span> with only classes can usually be removed.",
                        "html"))
                }
                "css" -> {
                    if (Regex("""#[0-9a-fA-F]{3,8}\b""").containsMatchIn(code)) suggestions.add(Suggestion(
                        "Extract colors to CSS variables",
                        "Define --brand: #7aa2ff at :root and reference it for consistency.",
                        "css"))
                    if (!code.contains("@media")) suggestions.add(Suggestion(
                        "Add responsive breakpoints",
                        "Include at least one @media query for mobile widths.",
                        "css"))
                    if (Regex("""margin:\s*0\s+auto""").containsMatchIn(code)) suggestions.add(Suggestion(
                        "Consider margin-inline: auto",
                        "Logical properties work in RTL contexts too.",
                        "css"))
                }
                "javascript", "typescript" -> {
                    if (Regex("""\bvar\s""").containsMatchIn(code)) suggestions.add(Suggestion(
                        "Replace var with const/let",
                        "const prevents accidental reassignment; let for mutable bindings.",
                        lang))
                    if (Regex("""document\.getElementById\(""").containsMatchIn(code)) suggestions.add(Suggestion(
                        "Use querySelector consistently",
                        "document.querySelector('#id') matches CSS selector syntax everywhere.",
                        lang))
                    if (Regex("""for\s*\(\s*let\s+i\s*=\s*0""").containsMatchIn(code)) suggestions.add(Suggestion(
                        "Prefer for...of or Array methods",
                        "for (const item of arr) is clearer than index loops.",
                        lang))
                    if (Regex("""\.innerHTML\s*=""").containsMatchIn(code)) suggestions.add(Suggestion(
                        "Avoid innerHTML for user data",
                        "Use textContent to prevent XSS when inserting untrusted strings.",
                        lang))
                }
                "json" -> {
                    if (Regex(""",\s*[}\]]""").containsMatchIn(code)) suggestions.add(Suggestion(
                        "Remove trailing commas", "JSON spec forbids trailing commas.", "json"))
                }
            }
            suggestions
        }

    // ── Formatting ────────────────────────────────────────────────

    override suspend fun format(language: String, code: String): String =
        withContext(Dispatchers.Default) {
            val lang = detectLanguage(code, language)
            when (lang) {
                "json" -> formatJson(code)
                "css" -> formatCss(code)
                "html" -> formatHtml(code)
                else -> formatJs(code)
            }
        }

    private fun formatJson(code: String): String {
        val sb = StringBuilder()
        var indent = 0
        var inString = false
        var escape = false
        for (c in code) {
            if (inString) {
                sb.append(c)
                if (escape) escape = false
                else if (c == '\\') escape = true
                else if (c == '"') inString = false
                continue
            }
            when (c) {
                '"' -> { inString = true; sb.append(c) }
                '{', '[' -> { sb.append(c); indent++; sb.append('\n'); repeat(indent) { sb.append("  ") } }
                '}', ']' -> {
                    indent--
                    sb.append('\n'); repeat(indent) { sb.append("  ") }; sb.append(c)
                }
                ',' -> { sb.append(c); sb.append('\n'); repeat(indent) { sb.append("  ") } }
                ':' -> { sb.append(": ") }
                else -> if (!c.isWhitespace()) sb.append(c)
            }
        }
        // Collapse empty object/array
        return sb.toString()
            .replace(Regex("""\{\s*\}"""), "{}")
            .replace(Regex("""\[\s*\]"""), "[]")
            .trim()
    }

    private fun formatCss(code: String): String {
        val cleaned = code.replace(Regex("""\s+"""), " ")
            .replace(" {", "{").replace("{ ", "{")
            .replace(" }", "}").replace("; ", ";")
        val sb = StringBuilder()
        var indent = 0
        for (c in cleaned) {
            when (c) {
                '{' -> { sb.append(" {\n"); indent++; repeat(indent) { sb.append("  ") } }
                '}' -> {
                    indent--
                    sb.setLength(sb.length - 2)
                    sb.append("}\n")
                    if (indent > 0) repeat(indent) { sb.append("  ") }
                }
                ';' -> sb.append(";\n").let { if (indent > 0) repeat(indent) { sb.append("  ") } }
                else -> sb.append(c)
            }
        }
        return sb.toString().replace(Regex("""\n\s*\n"""), "\n").trim() + "\n"
    }

    private fun formatHtml(code: String): String {
        // Basic HTML formatter: newline before tags at depth 0/1
        var out = code
        out = out.replace(Regex(""">\s*<"""), ">\n<")
        val lines = out.lines()
        val sb = StringBuilder()
        var depth = 0
        val blockTags = setOf("html","head","body","div","section","main","header","footer","nav","article","aside","ul","ol","table","form","script","style")
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            val closeMatch = Regex("""^</(\w+)""").find(trimmed)
            if (closeMatch != null && closeMatch.groupValues[1].lowercase() in blockTags) {
                depth = (depth - 1).coerceAtLeast(0)
            }
            repeat(depth) { sb.append("  ") }
            sb.append(trimmed).append('\n')
            val openMatch = Regex("""^<(\w+)""").find(trimmed)
            val isSelfClosing = trimmed.endsWith("/>") || trimmed.startsWith("<!") || trimmed.startsWith("<?")
            val tagName = openMatch?.groupValues?.get(1)?.lowercase()
            if (openMatch != null && !isSelfClosing && tagName in blockTags &&
                !trimmed.contains("</$tagName")) {
                depth++
            }
        }
        return sb.toString().trim() + "\n"
    }

    private fun formatJs(code: String): String {
        val lines = code.lines()
        val sb = StringBuilder()
        var indent = 0
        for (raw in lines) {
            val line = raw.trim()
            if (line.isEmpty()) { sb.append('\n'); continue }
            when {
                line.startsWith("}") || line.startsWith("]") || line.startsWith(")") -> {
                    indent = (indent - 1).coerceAtLeast(0)
                    repeat(indent) { sb.append("  ") }
                    sb.append(line).append('\n')
                }
                else -> {
                    repeat(indent) { sb.append("  ") }
                    sb.append(line).append('\n')
                }
            }
            indent += line.count { it == '{' } + line.count { it == '[' }
            indent -= line.count { it == '}' } + line.count { it == ']' }
            indent = indent.coerceAtLeast(0)
        }
        return sb.toString().trim() + "\n"
    }

    // ── Templates ─────────────────────────────────────────────────

    override suspend fun generateTemplate(intent: String, language: String): String =
        withContext(Dispatchers.Default) {
            when (intent.lowercase()) {
                "fetch" -> """
                    |async function fetchJson(url) {
                    |  const res = await fetch(url);
                    |  if (!res.ok) throw new Error(`HTTP ${'$'}{res.status}`);
                    |  return res.json();
                    |}
                """.trimMargin()
                "debounce" -> """
                    |function debounce(fn, ms = 300) {
                    |  let t;
                    |  return (...args) => {
                    |    clearTimeout(t);
                    |    t = setTimeout(() => fn(...args), ms);
                    |  };
                    |}
                """.trimMargin()
                "dark-mode" -> """
                    |const btn = document.querySelector('[data-theme-toggle]');
                    |const root = document.documentElement;
                    |const saved = localStorage.getItem('theme');
                    |if (saved) root.setAttribute('data-theme', saved);
                    |btn?.addEventListener('click', () => {
                    |  const next = root.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
                    |  root.setAttribute('data-theme', next);
                    |  localStorage.setItem('theme', next);
                    |});
                """.trimMargin()
                "form" -> """
                    |<form id="contact" novalidate>
                    |  <label for="name">Name</label>
                    |  <input id="name" name="name" required minlength="2">
                    |  <button type="submit">Send</button>
                    |</form>
                    |<script>
                    |  document.getElementById('contact').addEventListener('submit', e => {
                    |    e.preventDefault();
                    |    const data = Object.fromEntries(new FormData(e.target));
                    |    console.log(data);
                    |  });
                    |</script>
                """.trimMargin()
                "grid" -> """
                    |.layout {
                    |  display: grid;
                    |  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
                    |  gap: 1rem;
                    |}
                """.trimMargin()
                else -> "// No template for intent \"$intent\". Try: fetch, debounce, dark-mode, form, grid."
            }
        }

    // ── Project analysis ──────────────────────────────────────────

    override suspend fun analyzeProject(files: Map<String, String>): ProjectReport =
        withContext(Dispatchers.Default) {
            val allIssues = mutableListOf<Diagnostic>()
            for ((path, content) in files) {
                val ext = path.substringAfterLast('.', "").lowercase()
                val lang = when (ext) {
                    "html", "htm" -> "html"
                    "css" -> "css"
                    "js", "mjs" -> "javascript"
                    "ts", "tsx" -> "typescript"
                    "json" -> "json"
                    else -> null
                } ?: continue
                val issues = lint(lang, content)
                issues.forEach { allIssues.add(it.copy(message = "$path: ${it.message}")) }
            }
            val errors = allIssues.count { it.severity == Severity.ERROR }
            val warnings = allIssues.count { it.severity == Severity.WARNING }
            val infos = allIssues.count { it.severity == Severity.INFO }
            ProjectReport(
                totalFiles = files.size,
                totalErrors = errors,
                totalWarnings = warnings,
                issues = allIssues,
                summary = buildString {
                    append("Analyzed ${files.size} files. ")
                    append("$errors errors, $warnings warnings, $infos informational findings.")
                    if (errors == 0 && warnings == 0) append(" Project looks clean.")
                }
            )
        }

    companion object {
        private val VOID_TAGS = setOf(
            "area","base","br","col","embed","hr","img","input",
            "link","meta","param","source","track","wbr"
        )
    }
}
