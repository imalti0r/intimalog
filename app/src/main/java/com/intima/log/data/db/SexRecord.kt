package com.intima.log.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 亲密记录。endTime=null 表示进行中。
 * participantCount 为冗余字段，由 Repository 在事务内维护，用于快速统计；
 * 0 表示「未指定参与者/单人」。
 */
@Entity(
    tableName = "sex_records",
    indices = [Index("startTime"), Index("participantCount")]
)
data class SexRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val participantCount: Int = 0,
    val startTime: Long,
    val endTime: Long? = null,            // null = 进行中
    val protection: Protection = Protection.NOT_RECORDED,
    val climax: Int = -1,                 // -1 不记录 / 0 否 / 1 是
    val rating: Int = 0,                  // 满意度 1~5，0=未评
    val moodTags: String = "",            // 单个情绪标签（单选）
    val place: Place = Place.NOT_RECORDED,
    val protections: String = "",         // 逗号分隔的多选保护措施（Protection.name）
    val positions: String = "",           // 逗号分隔的自定义体位
    val toys: String = "",                // 逗号分隔的自定义玩具
    val note: String = "",
    val isBackfill: Boolean = false,      // 手动补录
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
) {
    /** 时长秒数；进行中返回 null */
    val durationSeconds: Long?
        get() = endTime?.let { ((it - startTime) / 1000).coerceAtLeast(0) }
}

/** 情绪标签常量 */
object MoodTags {
    const val PLEASANT = "愉悦"
    const val TIRED = "疲惫"
    const val INTIMATE = "亲密"
    const val SO_SO = "一般"
    val ALL = listOf(PLEASANT, TIRED, INTIMATE, SO_SO)
}
