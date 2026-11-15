package com.webcraft.ide

import com.webcraft.ide.io.ZipIO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ZipIOTest {

    @Test
    fun `export directory creates valid zip with correct structure`() {
        val src = createTempDir("src")
        File(src, "index.html").writeText("<html></html>")
        File(src, "css").mkdirs()
        File(src, "css/style.css").writeText("body{}")
        File(src, "js").mkdirs()
        File(src, "js/app.js").writeText("console.log(1);")

        val zip = File(createTempDir("z"), "p.zip")
        ZipIO.exportDirectory(src, zip)
        assertTrue(zip.exists())
        assertTrue(zip.length() > 200)

        val dest = createTempDir("out")
        val written = ZipIO.importZip(zip.inputStream(), dest)
        assertEquals(setOf("index.html", "css/style.css", "js/app.js"), written.toSet())
        assertEquals("<html></html>", File(dest, "index.html").readText())
    }

    @Test
    fun `import preserves folder structure`() {
        val src = createTempDir("s")
        File(src, "deep").mkdirs()
        File(src, "deep/nested/final.txt").apply { parentFile.mkdirs(); writeText("x") }

        val zip = File(createTempDir("z2"), "p.zip")
        ZipIO.exportDirectory(src, zip)

        val dest = createTempDir("out2")
        val written = ZipIO.importZip(zip.inputStream(), dest)
        assertTrue(written.contains("deep/nested/final.txt"))
        assertEquals("x", File(dest, "deep/nested/final.txt").readText())
    }

    @Test
    fun `import rejects absolute path entries`() {
        val zip = File(createTempDir("z3"), "evil.zip")
        val zos = java.util.zip.ZipOutputStream(zip.outputStream())
        zos.putNextEntry(java.util.zip.ZipEntry("/etc/passwd"))
        zos.write("x".toByteArray())
        zos.closeEntry()
        zos.close()

        val dest = createTempDir("out3")
        val written = ZipIO.importZip(zip.inputStream(), dest)
        assertTrue(written.isEmpty())
    }

    private fun createTempDir(prefix: String): File =
        File("/tmp/webcraft-test-$prefix-${System.nanoTime()}").apply { mkdirs() }
}
