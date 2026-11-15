package com.webcraft.ide.io

import java.io.File
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ZipIO {

    /** Export a project (root directory) into a ZIP. */
    fun exportDirectory(root: File, destZip: File) {
        destZip.parentFile?.mkdirs()
        ZipOutputStream(destZip.outputStream().buffered()).use { zos ->
            root.walkTopDown().filter { it.isFile }.forEach { file ->
                val rel = file.toRelativeString(root).replace(File.separatorChar, '/')
                zos.putNextEntry(ZipEntry(rel))
                file.inputStream().use { it.copyTo(zos) }
                zos.closeEntry()
            }
        }
    }

    /** Import a ZIP into a destination directory. Returns relative paths written. */
    fun importZip(stream: InputStream, destRoot: File): List<String> {
        destRoot.mkdirs()
        val written = mutableListOf<String>()
        ZipInputStream(stream.buffered()).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                val name = entry.name
                // Guard against path traversal
                if (name.contains("..") || name.startsWith("/")) {
                    zis.closeEntry(); entry = zis.nextEntry; continue
                }
                val target = File(destRoot, name)
                if (entry.isDirectory) {
                    target.mkdirs()
                } else {
                    target.parentFile?.mkdirs()
                    target.outputStream().use { zis.copyTo(it) }
                    written.add(name)
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return written
    }
}
