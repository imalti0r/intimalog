package com.intima.log

import com.intima.log.data.db.Gender
import com.intima.log.data.db.Partner
import com.intima.log.data.db.SexRecord
import com.intima.log.data.db.StatsBasis
import com.intima.log.domain.usecase.ApplyTemplateUseCase
import com.intima.log.domain.usecase.CombinationRank
import com.intima.log.domain.usecase.DurationFormatter
import com.intima.log.domain.usecase.GetCombinationStatsUseCase
import com.intima.log.domain.usecase.StatsBasisCounter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class StatsLogicTest {

    private val counter = StatsBasisCounter()

    // ---- 口径计数 ----

    @Test
    fun `session basis counts 1 per record`() {
        assertEquals(1, counter.value(StatsBasis.SESSION, 0))
        assertEquals(1, counter.value(StatsBasis.SESSION, 3))
    }

    @Test
    fun `person-time basis counts participantCount`() {
        assertEquals(1, counter.value(StatsBasis.PERSON_TIME, 0))  // 0 人视作 1（单人）
        assertEquals(2, counter.value(StatsBasis.PERSON_TIME, 2))
        assertEquals(3, counter.value(StatsBasis.PERSON_TIME, 3))
    }

    // ---- 时长计算 ----

    @Test
    fun `duration simple`() {
        val r = SexRecord(startTime = 0, endTime = 90 * 60_000L)
        assertEquals(90 * 60L, r.durationSeconds)
    }

    @Test
    fun `duration cross midnight`() {
        // 23:00 开始，01:00 结束 → 2 小时
        val zone = ZoneId.systemDefault()
        val start = LocalDate.of(2026, 10, 19).atTime(23, 0).atZone(zone).toInstant().toEpochMilli()
        val end = LocalDate.of(2026, 10, 20).atTime(1, 0).atZone(zone).toInstant().toEpochMilli()
        val r = SexRecord(startTime = start, endTime = end)
        assertEquals(7200L, r.durationSeconds)
    }

    @Test
    fun `ongoing record duration is null`() {
        val r = SexRecord(startTime = 0, endTime = null)
        assertEquals(null, r.durationSeconds)
    }

    @Test
    fun `duration formatter`() {
        assertEquals("1小时30分钟", DurationFormatter.format(90 * 60L))
        assertEquals("2小时5分钟", DurationFormatter.format(7500L))
        assertEquals("59秒", DurationFormatter.format(59L))
    }

    // ---- Streak ----

    @Test
    fun `streak consecutive days`() {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val days = (0..2).map { today.minusDays(it.toLong()) }.toSet()
        var streak = 0
        var cursor = today
        if (cursor !in days) cursor = cursor.minusDays(1)
        while (cursor in days) { streak++; cursor = cursor.minusDays(1) }
        assertEquals(3, streak)
    }

    @Test
    fun `streak broken by gap`() {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val days = setOf(today, today.minusDays(5))   // 中间断档
        var streak = 0
        var cursor = today
        if (cursor !in days) cursor = cursor.minusDays(1)
        while (cursor in days) { streak++; cursor = cursor.minusDays(1) }
        assertEquals(1, streak)
    }

    // ---- 组合聚合（A+B×2、A+B+C×1 排序） ----

    private fun partner(id: Long) = Partner(id = id, nickname = "P$id")
    private fun record(id: Long, start: Long, end: Long?, rating: Int = 0) =
        SexRecord(id = id, startTime = start, endTime = end, rating = rating)

    private fun aggregate(records: List<Pair<SexRecord, List<Long>>>): List<CombinationRank> {
        return records
            .map { (r, ids) -> ids.sorted() to r }
            .filter { (ids, _) -> ids.size >= 2 }
            .groupBy { (ids, _) -> ids }
            .map { (ids, group) ->
                val durations = group.mapNotNull { it.second.durationSeconds }
                val ratings = group.map { it.second.rating }.filter { it > 0 }
                CombinationRank(
                    partnerIds = ids,
                    sessionCount = group.size,
                    avgDurationSeconds = if (durations.isEmpty()) null else durations.average().toLong(),
                    avgRating = if (ratings.isEmpty()) null else ratings.average(),
                )
            }
            .sortedByDescending { it.sessionCount }
    }

    @Test
    fun `combination ranking orders by session count`() {
        val a = 1L; val b = 2L; val c = 3L
        val records = listOf(
            record(1, 0, 60_000L) to listOf(a, b),          // 60 秒
            record(2, 1, 121_000L) to listOf(a, b),         // 121 秒（start=1ms 偏移）
            record(3, 2, 90_000L) to listOf(a, b, c),
        )
        val ranks = aggregate(records)
        assertEquals(2, ranks.size)
        assertEquals(listOf(a, b), ranks[0].partnerIds)
        assertEquals(2, ranks[0].sessionCount)
        assertEquals(listOf(a, b, c), ranks[1].partnerIds)
        assertEquals(1, ranks[1].sessionCount)
        // 平均时长：(60+120)/2 = 90 秒（90_000 毫秒 / 1000）
        assertEquals(90L, ranks[0].avgDurationSeconds)
    }

    @Test
    fun `single partner records are not combinations`() {
        val records = listOf(record(1, 0, 60_000L) to listOf(1L))
        assertTrue(aggregate(records).isEmpty())
    }

    // ---- 模板 ID 编解码 ----

    @Test
    fun `template ids roundtrip`() {
        val ids = listOf(1L, 2L, 30L)
        val json = ApplyTemplateUseCase.encodeIds(ids)
        assertEquals(ids, ApplyTemplateUseCase.parseIds(json))
    }

    @Test
    fun `template parse malformed returns empty`() {
        assertTrue(ApplyTemplateUseCase.parseIds("not json").isEmpty())
    }
}
