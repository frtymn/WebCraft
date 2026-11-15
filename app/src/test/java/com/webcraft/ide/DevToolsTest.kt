package com.webcraft.ide

import com.webcraft.ide.tools.*
import com.webcraft.ide.git.GitManagerStatics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.zip.ZipInputStream
import com.webcraft.ide.io.ZipIO


class DevToolsTest {

    @Test
    fun `json pretty print indents objects`() {
        val input = "{\"a\":1,\"b\":{\"c\":2}}"
        val out = JsonTools.prettyPrint(input)
        assertTrue(out.contains("\n  \"a\""))
        assertTrue(out.contains("\n    \"c\""))
    }

    @Test
    fun `json minify removes whitespace outside strings`() {
        val input = """{ "a" : "hello world" , "b" : 1 }"""
        val out = JsonTools.minify(input)
        assertEquals("""{"a":"hello world","b":1}""", out)
    }

    @Test
    fun `base64 roundtrip`() {
        val original = "Hello, WebCraft!"
        val encoded = Base64Tools.encode(original)
        assertEquals(original, Base64Tools.decode(encoded))
    }

    @Test
    fun `base64 url safe roundtrip`() {
        val original = "some/url+with=special"
        val encoded = Base64Tools.encodeUrlSafe(original)
        assertEquals(original, Base64Tools.decodeUrlSafe(encoded))
    }

    @Test
    fun `url encode decode roundtrip`() {
        val original = "https://example.com/search?q=web+craft&lang=en"
        val encoded = UrlTools.encode(original)
        // decoded should equal original (space handling differences aside)
        assertEquals(original, UrlTools.decode(encoded))
    }

    @Test
    fun `uuid v4 has correct format and version`() {
        val uuid = UuidTools.v4()
        assertEquals(36, uuid.length)
        assertEquals(4, uuid.count { it == '-' })
        // UUID string format: xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx
        assertEquals('4', uuid[14])
        // variant nibble should be 8, 9, a, or b
        val variantChar = uuid[19]
        assertTrue(variantChar in "89ab")
    }

    @Test
    fun `uuid v7 has correct version`() {
        val uuid = UuidTools.v7()
        assertEquals('7', uuid[14])
        assertEquals(36, uuid.length)
    }

    @Test
    fun `uuid v7 has timestamp prefix`() {
        val uuid = UuidTools.v7()
        // First 12 hex chars should decode to a unix ms timestamp near now
        val hex = uuid.substring(0, 13).replace("-", "")
        val ts = hex.toLong(16)
        val now = System.currentTimeMillis()
        assertTrue(Math.abs(now - ts) < 1000)
    }

    @Test
    fun `uuid v4 uniqueness over many samples`() {
        val set = mutableSetOf<String>()
        repeat(100) { set.add(UuidTools.v4()) }
        assertEquals(100, set.size)
    }

    @Test
    fun `epoch to iso conversion`() {
        // 2026-01-01T00:00:00Z = 1767225600 seconds
        val iso = TimestampTools.epochToIso(1767225600)
        assertEquals("2026-01-01T00:00:00Z", iso)
    }

    @Test
    fun `iso to epoch conversion`() {
        val epoch = TimestampTools.isoToEpoch("2026-01-01T00:00:00Z")
        assertEquals(1767225600L, epoch)
    }

    @Test
    fun `iso to epoch ms conversion`() {
        val ms = TimestampTools.isoToEpochMs("2026-01-01T00:00:00Z")
        assertEquals(1767225600000L, ms)
    }

    @Test
    fun `regex finds matches with position`() {
        val matches = RegexTools.findMatches("""\d+""", "abc 123 def 456")
        assertEquals(2, matches.size)
        assertEquals("123" to 4, matches[0])
        assertEquals("456" to 12, matches[1])
    }

    @Test
    fun `regex flags are applied`() {
        val matches = RegexTools.findMatches("abc", "ABC abc", java.util.regex.Pattern.CASE_INSENSITIVE)
        assertEquals(2, matches.size)
    }

    @Test
    fun `regex invalid pattern returns empty`() {
        val matches = RegexTools.findMatches("[unclosed", "test")
        assertTrue(matches.isEmpty())
        assertFalse(RegexTools.isValid("[unclosed"))
    }

    @Test
    fun `color hex to rgb and back`() {
        val rgb = ColorTools.hexToRgb("#FF7B54")
        assertNotNull(rgb)
        assertEquals(255, rgb!!.r)
        assertEquals(123, rgb.g)
        assertEquals(84, rgb.b)
        assertEquals("#FF7B54", ColorTools.rgbToHex(rgb.r, rgb.g, rgb.b))
    }

    @Test
    fun `color shorthand hex to rgb`() {
        val rgb = ColorTools.hexToRgb("#FA3")
        assertNotNull(rgb)
        assertEquals(255, rgb!!.r)
        assertEquals(170, rgb.g)
    }

    @Test
    fun `color invalid hex returns null`() {
        assertEquals(null, ColorTools.hexToRgb("#XYZ"))
        assertEquals(null, ColorTools.hexToRgb("#TOOLONGHEX"))
        assertEquals(null, ColorTools.hexToRgb(""))
        assertEquals(null, ColorTools.hexToRgb("#1234")) // 4 chars is invalid
    }

    @Test
    fun `color contrast ratio calculated`() {
        val ratio = ColorTools.contrastRatio("#FFFFFF", "#000000")
        assertNotNull(ratio)
        assertTrue(ratio!! > 20.0)
    }

    @Test
    fun `zip export and import roundtrip`() {
        val temp = File.createTempFile("webcraft", "").apply { delete() }
        temp.mkdirs()
        // create files
        File(temp, "index.html").writeText("<h1>Hello</h1>")
        val sub = File(temp, "js").apply { mkdirs() }
        File(sub, "app.js").writeText("console.log('hi');")

        // Export
        val zipFile = File(temp.parentFile, "test-export.zip")
        ZipIO.exportDirectory(temp, zipFile)
        assertTrue(zipFile.exists())
        assertTrue(zipFile.length() > 100)

        // Import into a new directory
        val dest = File(temp.parentFile, "imported-${System.currentTimeMillis()}")
        dest.mkdirs()
        val written = ZipIO.importZip(zipFile.inputStream(), dest)
        assertEquals(setOf("index.html", "js/app.js"), written.toSet())

        assertEquals("<h1>Hello</h1>", File(dest, "index.html").readText())
        assertEquals("console.log('hi');", File(dest, "js/app.js").readText())

        // Cleanup
        temp.deleteRecursively()
        zipFile.delete()
        dest.deleteRecursively()
    }

    @Test
    fun `zip import guards path traversal`() {
        // Create a zip with a traversal entry
        val zipFile = File.createTempFile("evil", ".zip")
        val zos = java.util.zip.ZipOutputStream(zipFile.outputStream())
        zos.putNextEntry(java.util.zip.ZipEntry("../../etc/passwd"))
        zos.write("malicious".toByteArray())
        zos.closeEntry()
        zos.close()

        val dest = File("/tmp/test-import-safe-${System.currentTimeMillis()}")
        dest.mkdirs()
        val written = ZipIO.importZip(zipFile.inputStream(), dest)
        assertTrue(written.isEmpty()) // traversal entries should be skipped
        dest.deleteRecursively()
        zipFile.delete()
    }

    @Test
    fun `git diff detects added modified deleted`() {
        val before = mapOf("a.txt" to "1", "b.txt" to "2")
        val after = mapOf("a.txt" to "1-changed", "c.txt" to "3")
        val diff = GitManagerStatics.diffMaps(before, after)
        assertEquals("modified", diff["a.txt"])
        assertEquals("deleted", diff["b.txt"])
        assertEquals("added", diff["c.txt"])
    }

    @Test
    fun `git filter ignores patterns`() {
        val files = mapOf(
            "index.html" to "<p/>",
            "node_modules/lib.js" to "x",
            ".git" to "",
            "dist/bundle.js" to "x"
        )
        val filtered = GitManagerStatics.filterIgnored(files, listOf("node_modules", "dist"))
        assertEquals(setOf("index.html", ".git"), filtered.keys)
    }
}

