package com.webcraft.ide

import com.webcraft.ide.ai.RulesBasedProvider
import com.webcraft.ide.ai.Severity
import com.webcraft.ide.ai.LocalAIRegistry
import com.webcraft.ide.tools.JsonTools
import com.webcraft.ide.tools.Base64Tools
import com.webcraft.ide.tools.UrlTools
import com.webcraft.ide.tools.UuidTools
import com.webcraft.ide.tools.TimestampTools
import com.webcraft.ide.tools.ColorTools
import com.webcraft.ide.git.GitManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class RulesBasedProviderTest {

    private val provider = RulesBasedProvider()

    @Test
    fun `explain detects html structure`() = runBlocking {
        val result = provider.explain("html", "<!DOCTYPE html><html><head></head><body><div>hi</div></body></html>")
        assertTrue(result.text.isNotBlank())
        assertTrue(result.text.contains("Language:"))
    }

    @Test
    fun `lint flags missing viewport in html`() = runBlocking {
        val html = "<!DOCTYPE html><html><head><title>t</title></head><body><p>x</p></body></html>"
        val diags = provider.lint("html", html)
        assertTrue(diags.any { it.severity == Severity.WARNING && it.ruleId == "html/viewport" })
    }

    @Test
    fun `lint flags img without alt`() = runBlocking {
        val html = "<!DOCTYPE html><html lang=\"en\"><head><meta name=\"viewport\"></head><body><img src=\"x.png\"></body></html>"
        val diags = provider.lint("html", html)
        assertTrue(diags.any { it.ruleId == "html/img-alt" })
    }

    @Test
    fun `lint flags var in javascript`() = runBlocking {
        val js = "var x = 1;\nconsole.log(x);\n"
        val diags = provider.lint("javascript", js)
        assertTrue(diags.any { it.ruleId == "js/no-var" })
    }

    @Test
    fun `lint flags eval in javascript`() = runBlocking {
        val js = "eval('1 + 1');\n"
        val diags = provider.lint("javascript", js)
        assertTrue(diags.any { it.severity == Severity.ERROR && it.ruleId == "js/no-eval" })
    }

    @Test
    fun `lint detects unbalanced braces`() = runBlocking {
        val js = "function a() {\n  return 1;\n"
        val diags = provider.lint("javascript", js)
        assertTrue(diags.any { it.ruleId == "js/unbalanced-braces" })
    }

    @Test
    fun `lint flags undefined css variable`() = runBlocking {
        val css = ".a { color: var(--undefined-color); }\n"
        val diags = provider.lint("css", css)
        assertTrue(diags.any { it.ruleId == "css/undefined-var" && it.severity == Severity.ERROR })
    }

    @Test
    fun `lint flags duplicate css selector`() = runBlocking {
        val css = ".a { color: red; }\n.a { background: blue; }\n"
        val diags = provider.lint("css", css)
        assertTrue(diags.any { it.ruleId == "css/duplicate-selector" })
    }

    @Test
    fun `lint flags trailing comma in json`() = runBlocking {
        val json = "{\"a\": 1,\"b\": 2,}"
        val diags = provider.lint("json", json)
        assertTrue(diags.any { it.ruleId == "json/trailing-comma" && it.severity == Severity.ERROR })
    }

    @Test
    fun `lint accepts valid json`() = runBlocking {
        val json = "{\"a\": 1, \"b\": [1, 2, 3]}"
        val diags = provider.lint("json", json)
        assertTrue(diags.none { it.severity == Severity.ERROR })
    }

    @Test
    fun `format json pretty prints`() = runBlocking {
        val result = provider.format("json", "{\"a\":1,\"b\":{\"c\":2}}")
        assertTrue(result.contains("\n  \"a\": 1,"))
        assertTrue(result.contains("\n    \"c\": 2"))
    }

    @Test
    fun `format css indents`() = runBlocking {
        val result = provider.format("css", ".a{color:red;font-size:14px}")
        // at minimum should contain newlines and indentation
        assertTrue(result.lines().size >= 3)
    }

    @Test
    fun `suggest recommends replacing var`() = runBlocking {
        val js = "var x = 1;"
        val suggestions = provider.suggest("javascript", js)
        assertTrue(suggestions.any { it.title.contains("var") })
    }

    @Test
    fun `analyze project aggregates counts`() = runBlocking {
        val files = mapOf(
            "index.html" to "<!DOCTYPE html><html><head></head><body><p>x</p></body></html>",
            "style.css" to ".a { color: var(--x); }",
            "app.js" to "var a = 1;"
        )
        val report = provider.analyzeProject(files)
        assertEquals(3, report.totalFiles)
        assertTrue(report.totalWarnings > 0)
    }

    @Test
    fun `generate template for known intents`() = runBlocking {
        val fetch = provider.generateTemplate("fetch", "javascript")
        assertTrue(fetch.contains("async function fetchJson"))
        val debounce = provider.generateTemplate("debounce", "javascript")
        assertTrue(debounce.contains("function debounce"))
    }

    @Test
    fun `generate template for unknown intent returns message`() = runBlocking {
        val result = provider.generateTemplate("nonexistent", "javascript")
        assertTrue(result.startsWith("// No template"))
    }

    @Test
    fun `provider is not advertised as generative`() {
        assertFalse(provider.isGenerative)
        assertEquals("Craft Rules", provider.displayName)
    }

    @Test
    fun `registry default provider is rules-based`() {
        val p = LocalAIRegistry.active()
        assertFalse(p.isGenerative)
    }
}
