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
    val rating: Int = 0,                  // 满意度 1~5，0=未评
    val moodTags: String = "",            // 单个情绪标签（单选）
    val customProtections: String = "",   // 逗号分隔的自定义保护措施名（仅用户自定义）
    val customPlaces: String = "",        // 逗号分隔的自定义地点名（仅用户自定义）
    val positions: String = "",           // 逗号分隔的自定义体位
    val toys: String = "",                // 逗号分隔的自定义玩具
    val note: String = "",
    val isBackfill: Boolean = false,      // 手动补录
    val climaxesJson: String = "",        // 每人高潮次数 JSON：{"0":2,"1":1,...}，key=参与者顺序号，"me"=用户
    val splitGroupId: String? = null,     // 跨日拆分组 id（同组 = 原本一条记录拆分而成）
    val splitIndex: Int = 0,              // 组内序号：0 = 第一天（start 为原记录起点）
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
