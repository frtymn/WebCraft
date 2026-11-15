package com.webcraft.ide.data.fs

import android.content.Context
import java.io.File

class VirtualFileSystem(context: Context) {

    private val baseDir: File = File(context.filesDir, "projects").apply {
        if (!exists()) mkdirs()
    }

    fun createProjectRoot(name: String): File {
        val sanitized = sanitize(name)
        var dir = File(baseDir, sanitized)
        var counter = 1
        while (dir.exists()) {
            dir = File(baseDir, "${sanitized}_${counter++}")
        }
        dir.mkdirs()
        return dir
    }

    fun renameProjectRoot(oldRoot: String, oldName: String, newName: String) {
        val old = File(oldRoot)
        if (!old.exists()) return
        val target = File(baseDir, sanitize(newName))
        if (target.exists()) {
            old.renameTo(File(baseDir, "${sanitize(newName)}_${System.currentTimeMillis()}"))
        } else {
            old.renameTo(target)
        }
    }

    fun deleteProjectRoot(rootPath: String) {
        val dir = File(rootPath)
        if (dir.exists()) dir.deleteRecursively()
    }

    fun abs(rootPath: String, relative: String): File {
        val rel = relative.trimStart('/')
        val f = File(rootPath, rel)
        f.parentFile?.mkdirs()
        return f
    }

    fun write(file: File, content: String) {
        file.parentFile?.mkdirs()
        file.writeText(content)
    }

    fun mkdir(dir: File) {
        dir.mkdirs()
    }

    fun read(file: File): String = if (file.exists()) file.readText() else ""

    fun delete(file: File) {
        if (file.exists()) {
            if (file.isDirectory) file.deleteRecursively() else file.delete()
        }
    }

    fun rename(from: File, to: File) {
        to.parentFile?.mkdirs()
        if (from.exists()) from.renameTo(to)
    }

    fun listAll(rootPath: String): List<String> {
        val root = File(rootPath)
        if (!root.exists()) return emptyList()
        return root.walkTopDown().filter { it.isFile }
            .map { it.toRelativeString(root).replace(File.separatorChar, '/') }
            .toList()
    }

    private fun sanitize(name: String): String =
        name.replace(Regex("[^a-zA-Z0-9_\\-. ]"), "_").take(64).ifBlank { "project" }
}
