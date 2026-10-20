package com.intima.log.domain.usecase

import com.intima.log.data.db.StatsBasis
import javax.inject.Inject
import javax.inject.Singleton

/** 时长格式化工具 */
@Singleton
object DurationFormatter {
    fun format(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        return when {
            h > 0 -> "${h}小时${m}分钟"
            m > 0 -> "${m}分钟"
            else -> "${seconds}秒"
        }
    }

    fun shortFormat(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        return if (h > 0) "${h}h${m}m" else "${m}m"
    }
}

/** 统计口径辅助：SESSION 记 1，PERSON_TIME 记 participantCount */
@Singleton
class StatsBasisCounter @Inject constructor() {
    fun value(basis: StatsBasis, participantCount: Int): Int = when (basis) {
        StatsBasis.SESSION -> 1
        StatsBasis.PERSON_TIME -> participantCount.coerceAtLeast(1)  // 0 人视作 1（单人）
    }

    fun unitLabel(basis: StatsBasis): String = when (basis) {
        StatsBasis.SESSION -> "场"
        StatsBasis.PERSON_TIME -> "人次"
    }
}
