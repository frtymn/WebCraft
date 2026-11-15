package com.webcraft.ide

import com.webcraft.ide.data.templates.ProjectTemplates
import com.webcraft.ide.data.templates.TemplateFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectTemplatesTest {

    @Test
    fun `all eight templates are defined`() {
        assertEquals(8, ProjectTemplates.all.size)
    }

    @Test
    fun `every template has an index or entry file`() {
        ProjectTemplates.all.forEach { t ->
            val hasEntry = t.files.any { f -> f.path.endsWith("index.html") || f.path.endsWith(".html") }
            assertTrue("Template ${t.id} should have an HTML entry file", hasEntry)
        }
    }

    @Test
    fun `every template file has non-empty content`() {
        ProjectTemplates.all.forEach { t ->
            t.files.forEach { f: TemplateFile ->
                assertTrue("File ${t.id}/${f.path} should have content", f.content.isNotBlank())
            }
        }
    }

    @Test
    fun `template by name returns correct template`() {
        val t = ProjectTemplates.byName("portfolio")
        assertNotNull(t)
        assertEquals("Portfolio", t!!.name)
        assertEquals("portfolio", t.id)
    }

    @Test
    fun `template by name returns null for unknown id`() {
        assertEquals(null, ProjectTemplates.byName("nonexistent"))
    }

    @Test
    fun `portfolio template has linked css and js`() {
        val t = ProjectTemplates.byName("portfolio")!!
        val paths = t.files.map { it.path }
        assertTrue(paths.contains("index.html"))
        assertTrue(paths.contains("style.css"))
        assertTrue(paths.contains("script.js"))
    }

    @Test
    fun `blog template has multiple posts`() {
        val t = ProjectTemplates.byName("blog")!!
        assertTrue(t.files.map { it.path }.containsAll(listOf("index.html", "post1.html", "post2.html")))
    }

    @Test
    fun `css playground template has interactive controls`() {
        val t = ProjectTemplates.byName("css-playground")!!
        val html = t.files.first { it.path == "index.html" }.content
        assertTrue(html.contains("<input type=\"range\""))
        assertTrue(html.contains("<select"))
    }

    @Test
    fun `documentation template has sidebar nav`() {
        val t = ProjectTemplates.byName("docs")!!
        val html = t.files.first { it.path == "index.html" }.content
        assertTrue(html.contains("class=\"sidebar\""))
    }

    @Test
    fun `dashboard template has stats and chart`() {
        val t = ProjectTemplates.byName("dashboard")!!
        val html = t.files.first { it.path == "index.html" }.content
        assertTrue(html.contains("class=\"stats\""))
        assertTrue(html.contains("svg"))
    }
}
