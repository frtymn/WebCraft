package com.webcraft.ide.ai

/**
 * Contract for pluggable local AI inference providers.
 *
 * WebCraft ships with a rules-based [RulesBasedProvider] that runs entirely
 * on-device with no network access. The interface is intentionally modeled
 * after what an on-device LLM (e.g. MLC, llama.cpp bindings, Gemini Nano)
 * would expose, so implementations can be swapped without touching UI code.
 *
 * IMPORTANT: This is a RULES-BASED engine. It is NOT an LLM and must never
 * be advertised as one.
 */
interface LocalAIProvider {

    /** Short display name shown in the UI, e.g. "Craft Rules" or "Local Llama". */
    val displayName: String

    /** True if the provider performs generative inference (an actual model). */
    val isGenerative: Boolean

    /** Explain a code fragment in natural language. Returns human-readable text. */
    suspend fun explain(language: String, code: String): AiResult

    /** Produce lint diagnostics for a file. */
    suspend fun lint(language: String, code: String): List<Diagnostic>

    /** Suggest code changes (local heuristics only for the rules engine). */
    suspend fun suggest(language: String, code: String): List<Suggestion>

    /** Format a code fragment per the project's style. */
    suspend fun format(language: String, code: String): String

    /** Generate a small template / snippet for a language and intent. */
    suspend fun generateTemplate(intent: String, language: String): String

    /** Run project-wide diagnostics. */
    suspend fun analyzeProject(files: Map<String, String>): ProjectReport
}

data class AiResult(val text: String, val confidence: Float = 0f)
data class Diagnostic(
    val line: Int,
    val column: Int = 0,
    val severity: Severity,
    val message: String,
    val ruleId: String
)
enum class Severity { ERROR, WARNING, INFO }

data class Suggestion(
    val title: String,
    val description: String,
    val language: String,
    val replacementRange: IntRange? = null,
    val replacement: String? = null
)

data class ProjectReport(
    val totalFiles: Int,
    val totalErrors: Int,
    val totalWarnings: Int,
    val issues: List<Diagnostic>,
    val summary: String
)
