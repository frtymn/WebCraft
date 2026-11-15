package com.webcraft.ide.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val isFavorite: Boolean = false,
    val templateId: String? = null,
    val rootPath: String = "",
    val hasGit: Boolean = false,
    val gitRemote: String = "",
    val gitBranch: String = "main"
)

@Entity(
    tableName = "files",
    indices = [Index("projectId"), Index(value = ["projectId", "path"], unique = true)]
)
data class FileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val path: String,
    val content: String = "",
    val isDirectory: Boolean = false,
    val size: Long = 0,
    val lastModified: Date = Date(),
    val createdAt: Date = Date()
)

@Entity(
    tableName = "tabs",
    indices = [Index("projectId")]
)
data class TabEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val filePath: String,
    val order: Int = 0,
    val isPinned: Boolean = false,
    val openedAt: Date = Date()
)
