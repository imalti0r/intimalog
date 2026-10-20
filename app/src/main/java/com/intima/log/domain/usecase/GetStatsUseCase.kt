package com.intima.log.domain.usecase

import com.intima.log.data.db.SexRecordWithPartners
import com.intima.log.data.repo.RecordRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/** 首页顶部指标 */
data class HomeMetrics(
    val todaySessions: Int,          // 今日场次（恒按场次）
    val todayPersonTimes: Int,       // 今日人次
    val weekSessions: Int,           // 本周（近 7 天）按口径
    val avgDurationSeconds: Long?,   // 平均时长（恒按场次）
    val basis: StatsBasisValue,
)

data class StatsBasisValue(val basis: com.intima.log.data.db.StatsBasis)

@Singleton
class GetHomeMetricsUseCase @Inject constructor(
    private val recordRepository: RecordRepository,
    private val counter: StatsBasisCounter,
) {
    suspend operator fun invoke(basis: com.intima.log.data.db.StatsBasis): HomeMetrics {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val dayStart = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val weekStart = today.minusDays(6).atStartOfDay(zone).toInstant().toEpochMilli()
        val all = recordRepository.listAllWithPartners()
        val todayRecords = all.filter { it.record.startTime >= dayStart }
        val weekRecords = all.filter { it.record.startTime >= weekStart }

        val todayPersonTimes = todayRecords.sumOf { counter.value(basis, it.record.participantCount) }
        val weekByBasis = weekRecords.sumOf { counter.value(basis, it.record.participantCount) }
        val durations = todayRecords.mapNotNull { it.record.durationSeconds }
        val avg = if (durations.isEmpty()) null else durations.average().toLong()

        return HomeMetrics(
            todaySessions = todayRecords.size,
            todayPersonTimes = todayPersonTimes,
            weekSessions = weekByBasis,
            avgDurationSeconds = avg,
            basis = StatsBasisValue(basis),
        )
    }
}

/** 日历月视图：日 → 场次数 */
@Singleton
class GetMonthCalendarUseCase @Inject constructor(
    private val getRecords: GetRecordsUseCase,
) {
    suspend operator fun invoke(month: YearMonth): Map<LocalDate, Int> {
        val zone = ZoneId.systemDefault()
        val from = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val until = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val records = getRecords.observeRangeOnce(from, until)
        return records.groupBy { Instant.ofEpochMilli(it.record.startTime).atZone(zone).toLocalDate() }
            .mapValues { it.value.size }
    }
}

/** Streak：连续记录天数 */
@Singleton
class GetStreakUseCase @Inject constructor(
    private val recordRepository: RecordRepository,
) {
    suspend operator fun invoke(): Int {
        val all = recordRepository.listAllWithPartners()
        if (all.isEmpty()) return 0
        val zone = ZoneId.systemDefault()
        val days = all.map { Instant.ofEpochMilli(it.record.startTime).atZone(zone).toLocalDate() }
            .toSet()
        var streak = 0
        var cursor = LocalDate.now(zone)
        if (cursor !in days) cursor = cursor.minusDays(1)   // 今日还没有不算断
        while (cursor in days) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }
}

/** 记录列表（组合筛选用） */
@Singleton
class GetRecordsUseCase @Inject constructor(
    private val recordRepository: RecordRepository,
) {
    fun observeAll(): Flow<List<SexRecordWithPartners>> = recordRepository.observeAll()
    suspend fun observeRangeOnce(from: Long, until: Long): List<SexRecordWithPartners> =
        recordRepository.listAllWithPartners().filter { it.record.startTime in from until until }
    suspend fun exactCombination(partnerIds: List<Long>) = recordRepository.recordsWithExactPartners(partnerIds)
    suspend fun containing(partnerId: Long) = recordRepository.recordsContaining(partnerId)
}
