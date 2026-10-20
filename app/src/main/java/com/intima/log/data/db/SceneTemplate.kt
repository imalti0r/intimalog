package com.intima.log.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 经期记录（可选模块，仅日期粒度） */
@Entity(tableName = "cycle_logs")
data class CycleLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startDate: Long,        // 当日 0 点时间戳
    val endDate: Long? = null,
)

/** 场景模板：参与者组合 + 保护措施 + 满意度 + 地点 */
@Entity(tableName = "scene_templates")
data class SceneTemplate(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val partnerIdsJson: String,           // JSON 数组 [1,2,3]
    val protection: String = "NOT_RECORDED",
    val rating: Int = 0,
    val place: Place = Place.NOT_RECORDED,
    val sortOrder: Int = 0,
    val pinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)
