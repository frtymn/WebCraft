package com.webcraft.ide.data.repo

import com.webcraft.ide.data.db.*
import com.webcraft.ide.data.fs.VirtualFileSystem
import com.webcraft.ide.data.models.*
import com.webcraft.ide.data.templates.ProjectTemplates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.*

class ProjectRepository(
    private val db: WebCraftDatabase,
    private val vfs: VirtualFileSystem
) {
    private val projectDao = db.projectDao()
    private val fileDao = db.fileDao()
    private val tabDao = db.tabDao()

    fun observeProjects(): Flow<List<Project>> =
        projectDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeFavorites(): Flow<List<Project>> =
        projectDao.observeFavorites().map { list -> list.map { it.toDomain() } }

    fun observeProject(id: Long): Flow<Project?> =
        projectDao.observeById(id).map { it?.toDomain() }

    suspend fun getProject(id: Long): Project? = projectDao.getById(id)?.toDomain()

    suspend fun getRecent(limit: Int = 10): List<Project> =
        projectDao.getRecent(limit).map { it.toDomain() }

    suspend fun createProject(name: String, description: String = "", templateId: String? = null): Result<Project> =
        withContext(Dispatchers.IO) {
            runCatching {
                val entity = ProjectEntity(name = name, description = description, templateId = templateId)
                val id = projectDao.insert(entity)
                // Create on-disk root
                val rootDir = vfs.createProjectRoot(name)
                projectDao.update(entity.copy(id = id, rootPath = rootDir.absolutePath))
                // Materialize template files if requested
                templateId?.let { tid ->
                    val template = ProjectTemplates.byName(tid)
                    if (template != null) {
                        for (file in template.files) {
                            val rel = if (file.path.startsWith("/")) file.path.substring(1) else file.path
                            val absPath = vfs.abs(rootDir.absolutePath, rel)
                            vfs.write(absPath, file.content)
                            fileDao.insert(FileEntity(
                                projectId = id,
                                path = rel,
                                content = file.content,
                                isDirectory = file.path.endsWith("/"),
                                size = file.content.toByteArray().size.toLong()
                            ))
                        }
                    }
                }
                projectDao.getById(id)!!.toDomain()
            }
        }

    suspend fun renameProject(id: Long, newName: String): Result<Project> = withContext(Dispatchers.IO) {
        runCatching {
            val p = projectDao.getById(id) ?: error("Project not found")
            vfs.renameProjectRoot(p.rootPath, p.name, newName)
            projectDao.update(p.copy(name = newName, updatedAt = Date()))
            projectDao.getById(id)!!.toDomain()
        }
    }

    suspend fun deleteProject(id: Long): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val p = projectDao.getById(id) ?: error("Project not found")
            fileDao.deleteAllForProject(id)
            tabDao.deleteAllForProject(id)
            projectDao.delete(id)
            vfs.deleteProjectRoot(p.rootPath)
        }
    }

    suspend fun duplicateProject(id: Long): Result<Project> = withContext(Dispatchers.IO) {
        runCatching {
            val src = projectDao.getById(id) ?: error("Project not found")
            val files = fileDao.allForProject(id)
            val tabs = tabDao.getForProject(id)
            val newId = projectDao.insert(src.copy(
                id = 0,
                name = "${src.name} (copy)",
                createdAt = Date(),
                updatedAt = Date(),
                isFavorite = false
            ))
            val newRoot = vfs.createProjectRoot("${src.name} (copy)")
            projectDao.update(src.copy(id = newId, name = "${src.name} (copy)", rootPath = newRoot.absolutePath, createdAt = Date(), updatedAt = Date()))
            files.forEach { f ->
                vfs.write(vfs.abs(newRoot.absolutePath, f.path), f.content)
                fileDao.insert(f.copy(id = 0, projectId = newId))
            }
            tabs.forEach { t ->
                tabDao.insert(t.copy(id = 0, projectId = newId))
            }
            projectDao.getById(newId)!!.toDomain()
        }
    }

    suspend fun setFavorite(id: Long, favorite: Boolean) = withContext(Dispatchers.IO) {
        val p = projectDao.getById(id) ?: return@withContext
        projectDao.update(p.copy(isFavorite = favorite, updatedAt = Date()))
    }

    // ── File operations ────────────────────────────────────────────

    fun observeFiles(projectId: Long): Flow<List<ProjectFile>> =
        fileDao.observeForProject(projectId).map { list -> list.map { it.toDomain() } }

    suspend fun listFiles(projectId: Long): List<ProjectFile> =
        fileDao.allForProject(projectId).map { it.toDomain() }

    suspend fun readFile(projectId: Long, path: String): String? = withContext(Dispatchers.IO) {
        fileDao.find(projectId, path)?.content
    }

    suspend fun writeFile(projectId: Long, path: String, content: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val p = projectDao.getById(projectId) ?: error("Project not found")
                vfs.write(vfs.abs(p.rootPath, path), content)
                val existing = fileDao.find(projectId, path)
                val size = content.toByteArray().size.toLong()
                if (existing != null) {
                    fileDao.update(existing.copy(content = content, size = size, lastModified = Date()))
                } else {
                    fileDao.insert(FileEntity(
                        projectId = projectId, path = path, content = content,
                        isDirectory = false, size = size, lastModified = Date()
                    ))
                }
                projectDao.update(p.copy(updatedAt = Date()))
            }
        }

    suspend fun createFile(projectId: Long, path: String, isDirectory: Boolean): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val p = projectDao.getById(projectId) ?: error("Project not found")
                val abs = vfs.abs(p.rootPath, path)
                if (isDirectory) {
                    vfs.mkdir(abs)
                    fileDao.insert(FileEntity(projectId = projectId, path = path, isDirectory = true))
                } else {
                    vfs.write(abs, "")
                    fileDao.insert(FileEntity(projectId = projectId, path = path, content = "", isDirectory = false))
                }
                projectDao.update(p.copy(updatedAt = Date()))
            }
        }

    suspend fun deleteFile(projectId: Long, path: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val p = projectDao.getById(projectId) ?: error("Project not found")
            vfs.delete(vfs.abs(p.rootPath, path))
            fileDao.deleteByPrefix(projectId, path)
            fileDao.delete(projectId, path)
            projectDao.update(p.copy(updatedAt = Date()))
        }
    }

    suspend fun renameFile(projectId: Long, oldPath: String, newPath: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val p = projectDao.getById(projectId) ?: error("Project not found")
                vfs.rename(vfs.abs(p.rootPath, oldPath), vfs.abs(p.rootPath, newPath))
                val f = fileDao.find(projectId, oldPath)
                if (f != null) {
                    fileDao.delete(projectId, oldPath)
                    fileDao.insert(f.copy(id = 0, path = newPath))
                }
                projectDao.update(p.copy(updatedAt = Date()))
            }
        }

    suspend fun searchFiles(projectId: Long, query: String): List<Pair<ProjectFile, String>> =
        withContext(Dispatchers.IO) {
            if (query.isBlank()) return@withContext emptyList()
            val files = fileDao.filesForProject(projectId)
            files.mapNotNull { f ->
                val lines = f.content.lines()
                val matched = lines.mapIndexedNotNull { i, line ->
                    if (line.contains(query, ignoreCase = true))
                        "${i + 1}: ${line.trim().take(80)}" else null
                }
                if (matched.isEmpty()) null else f.toDomain() to matched.joinToString("\n")
            }
        }

    // ── Tabs ───────────────────────────────────────────────────────

    fun observeTabs(projectId: Long): Flow<List<Tab>> =
        tabDao.observeForProject(projectId).map { list -> list.map { it.toDomain() } }

    suspend fun openTab(projectId: Long, path: String) = withContext(Dispatchers.IO) {
        val tabs = tabDao.getForProject(projectId)
        if (tabs.none { it.filePath == path }) {
            tabDao.insert(TabEntity(projectId = projectId, filePath = path, order = tabs.size))
        }
    }

    suspend fun closeTab(projectId: Long, path: String) = withContext(Dispatchers.IO) {
        tabDao.delete(projectId, path)
    }

    suspend fun closeAllTabs(projectId: Long) = withContext(Dispatchers.IO) {
        tabDao.deleteAllForProject(projectId)
    }

    // ── Import / Export ────────────────────────────────────────────

    suspend fun importFromDirectory(projectName: String, sourceRoot: java.io.File): Result<Project> =
        withContext(Dispatchers.IO) {
            runCatching {
                val rootDir = vfs.createProjectRoot(projectName)
                val id = projectDao.insert(ProjectEntity(name = projectName, rootPath = rootDir.absolutePath))
                val all = sourceRoot.walkTopDown().filter { it.isFile }.toList()
                for (file in all) {
                    val rel = file.toRelativeString(sourceRoot)
                    val content = file.readText()
                    vfs.write(vfs.abs(rootDir.absolutePath, rel), content)
                    fileDao.insert(FileEntity(
                        projectId = id, path = rel, content = content,
                        size = content.toByteArray().size.toLong()
                    ))
                }
                projectDao.getById(id)!!.toDomain()
            }
        }

    suspend fun exportToDirectory(projectId: Long, destRoot: java.io.File): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val p = projectDao.getById(projectId) ?: error("Project not found")
                val files = fileDao.allForProject(projectId)
                for (f in files) {
                    val target = vfs.abs(destRoot.absolutePath, f.path)
                    if (f.isDirectory) vfs.mkdir(target) else vfs.write(target, f.content)
                }
            }
        }

    suspend fun backupAll(destZip: java.io.File): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val projects = projectDao.getRecent(Int.MAX_VALUE)
            val zipOut = java.util.zip.ZipOutputStream(destZip.outputStream())
            for (p in projects) {
                val files = fileDao.allForProject(p.id)
                for (f in files) {
                    val entry = java.util.zip.ZipEntry("projects/${p.name}/${f.path}")
                    zipOut.putNextEntry(entry)
                    if (!f.isDirectory) zipOut.write(f.content.toByteArray())
                    zipOut.closeEntry()
                }
            }
            zipOut.close()
        }
    }
}
