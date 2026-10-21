package com.intima.log.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** 自定义标签（体位 / 玩具共用表）。kind: "position" | "toy" */
@Entity(tableName = "custom_tags", indices = [androidx.room.Index(value = ["kind", "name"], unique = true)])
data class CustomTagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: String,
    val name: String,
)

@Dao
interface TagDao {
    @Query("SELECT * FROM custom_tags WHERE kind = :kind ORDER BY id ASC")
    fun observeKind(kind: String): Flow<List<CustomTagEntity>>

    @Query("SELECT * FROM custom_tags WHERE kind = :kind ORDER BY id ASC")
    suspend fun listKind(kind: String): List<CustomTagEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(tag: CustomTagEntity): Long

    /** 便捷：同 kind 下同名去重插入 */
    suspend fun insertIgnore(kind: String, name: String) {
        insertIgnore(CustomTagEntity(kind = kind, name = name))
    }

    @Query("UPDATE custom_tags SET name = :newName WHERE id = :id")
    suspend fun rename(id: Long, newName: String)

    @Query("DELETE FROM custom_tags WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT name FROM custom_tags WHERE kind = :kind AND name = :name LIMIT 1")
    suspend fun findName(kind: String, name: String): String?

    /** 备份恢复：全量读取 / 清空 / 按 id 写回 */
    @Query("SELECT * FROM custom_tags ORDER BY id ASC")
    suspend fun listAll(): List<CustomTagEntity>

    @Query("DELETE FROM custom_tags")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReplace(tag: CustomTagEntity): Long
}
