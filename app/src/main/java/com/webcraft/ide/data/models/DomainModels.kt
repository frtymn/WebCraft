package com.webcraft.ide.data.models

import com.webcraft.ide.data.db.*
import java.util.Date

data class Project(
    val id: Long,
    val name: String,
    val description: String,
    val createdAt: Date,
    val updatedAt: Date,
    val isFavorite: Boolean,
    val templateId: String?,
    val rootPath: String,
    val hasGit: Boolean,
    val gitRemote: String,
    val gitBranch: String
)

data class ProjectFile(
    val id: Long,
    val projectId: Long,
    val path: String,
    val content: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Date
)

data class Tab(
    val id: Long,
    val projectId: Long,
    val filePath: String,
    val order: Int,
    val isPinned: Boolean,
    val openedAt: Date
)

// ── Mappers ───────────────────────────────────────────────────────

fun ProjectEntity.toDomain() = Project(
    id = id, name = name, description = description,
    createdAt = createdAt, updatedAt = updatedAt,
    isFavorite = isFavorite, templateId = templateId,
    rootPath = rootPath, hasGit = hasGit,
    gitRemote = gitRemote, gitBranch = gitBranch
)

fun FileEntity.toDomain() = ProjectFile(
    id = id, projectId = projectId, path = path,
    content = content, isDirectory = isDirectory,
    size = size, lastModified = lastModified
)

fun TabEntity.toDomain() = Tab(
    id = id, projectId = projectId, filePath = filePath,
    order = order, isPinned = isPinned, openedAt = openedAt
)
