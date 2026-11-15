package com.webcraft.ide.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE isFavorite = 1 ORDER BY updatedAt DESC")
    fun observeFavorites(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<ProjectEntity?>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ProjectEntity?

    @Query("SELECT * FROM projects WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): ProjectEntity?

    @Query("SELECT * FROM projects ORDER BY updatedAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 10): List<ProjectEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(project: ProjectEntity): Long

    @Update
    suspend fun update(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM projects")
    suspend fun count(): Int
}

@Dao
interface FileDao {
    @Query("SELECT * FROM files WHERE projectId = :projectId ORDER BY isDirectory DESC, path ASC")
    fun observeForProject(projectId: Long): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE projectId = :projectId AND path = :path LIMIT 1")
    suspend fun find(projectId: Long, path: String): FileEntity?

    @Query("SELECT * FROM files WHERE projectId = :projectId")
    suspend fun allForProject(projectId: Long): List<FileEntity>

    @Query("SELECT * FROM files WHERE projectId = :projectId AND isDirectory = 0")
    suspend fun filesForProject(projectId: Long): List<FileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(file: FileEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(files: List<FileEntity>)

    @Update
    suspend fun update(file: FileEntity)

    @Query("DELETE FROM files WHERE projectId = :projectId AND path LIKE :prefix || '%'")
    suspend fun deleteByPrefix(projectId: Long, prefix: String)

    @Query("DELETE FROM files WHERE projectId = :projectId AND path = :path")
    suspend fun delete(projectId: Long, path: String)

    @Query("DELETE FROM files WHERE projectId = :projectId")
    suspend fun deleteAllForProject(projectId: Long)

    @Query("SELECT COUNT(*) FROM files WHERE projectId = :projectId")
    suspend fun count(projectId: Long): Int
}

@Dao
interface TabDao {
    @Query("SELECT * FROM tabs WHERE projectId = :projectId ORDER BY `order` ASC")
    fun observeForProject(projectId: Long): Flow<List<TabEntity>>

    @Query("SELECT * FROM tabs WHERE projectId = :projectId ORDER BY `order` ASC")
    suspend fun getForProject(projectId: Long): List<TabEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tab: TabEntity): Long

    @Update
    suspend fun update(tab: TabEntity)

    @Query("DELETE FROM tabs WHERE projectId = :projectId AND filePath = :path")
    suspend fun delete(projectId: Long, path: String)

    @Query("DELETE FROM tabs WHERE projectId = :projectId")
    suspend fun deleteAllForProject(projectId: Long)
}
