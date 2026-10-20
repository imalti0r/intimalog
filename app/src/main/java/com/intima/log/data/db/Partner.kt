package com.intima.log.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** 性别 */
enum class Gender { MALE, FEMALE, OTHER, UNDISCLOSED }

/** 保护措施 */
enum class Protection { NONE, CONDOM, PILL_SHORT, PILL_LONG, PILL_URGENT, OTHER, NOT_RECORDED }

/** PILL 兼容：旧数据 PILL → 紧急避孕药显示 */
fun Protection.labelOrLegacy(): Protection = this

fun Protection.displayLabel(): String = when (this) {
    Protection.PILL_SHORT -> "短效避孕药"
    Protection.PILL_LONG -> "长效避孕药"
    Protection.PILL_URGENT -> "紧急避孕药"
    else -> this.name
}

/** 地点标签 */
enum class Place { HOME, HOTEL, OTHER, NOT_RECORDED }

/** 统计口径：SESSION=场次，PERSON_TIME=人次 */
enum class StatsBasis { SESSION, PERSON_TIME }

/**
 * 性伴侣。软删除（archived=true）后不参与新记录，但保留历史关联。
 */
@Entity(
    tableName = "partners",
    indices = [Index("nickname")]
)
data class Partner(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nickname: String,                 // 必填昵称
    val alias: String? = null,            // 别名/备注
    val emoji: String = "🙂",             // 头像 Emoji
    val colorIndex: Int = 0,              // 头像底色索引（0~7）
    val gender: Gender = Gender.UNDISCLOSED,
    val isRegular: Boolean = true,        // 固定伴侣
    val archived: Boolean = false,        // 软删除
    val hidden: Boolean = false,          // 隐藏：不出现在记录选择器，仍占列表（半透明）
    val isImported: Boolean = false,      // 导入占位
    val createdAt: Long = System.currentTimeMillis(),
)
