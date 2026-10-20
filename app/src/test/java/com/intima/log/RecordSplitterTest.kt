package com.intima.log

import com.intima.log.domain.usecase.RecordSplitter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * 跨日拆分核心逻辑单测。
 * 用固定时区（GMT+8）避免宿主机时区影响断言。
 */
class RecordSplitterTest {

    private val zone: ZoneId = ZoneId.of("Asia/Shanghai")

    private fun at(date: LocalDate, hour: Int, minute: Int): Long =
        date.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun `same day record returns single segment`() {
        val start = at(LocalDate.of(2026, 10, 20), 22, 0)
        val end = at(LocalDate.of(2026, 10, 20), 23, 30)
        val segs = RecordSplitter.split(start, end, zone)
        assertEquals(1, segs.size)
        assertEquals(start, segs[0].startMillis)
        assertEquals(end, segs[0].endMillis)
    }

    @Test
    fun `end before start clamps to single zero-length segment`() {
        val start = at(LocalDate.of(2026, 10, 20), 23, 0)
        val end = at(LocalDate.of(2026, 10, 20), 22, 0)
        val segs = RecordSplitter.split(start, end, zone)
        assertEquals(1, segs.size)
        assertEquals(0L, segs[0].endMillis - segs[0].startMillis)
    }

    @Test
    fun `cross midnight splits into two segments at 00-00`() {
        val d1 = LocalDate.of(2026, 10, 19)
        val d2 = LocalDate.of(2026, 10, 20)
        val start = at(d1, 23, 0)
        val end = at(d2, 1, 0)
        val segs = RecordSplitter.split(start, end, zone)

        assertEquals(2, segs.size)
        // 第一段：23:00 → 次日 00:00
        assertEquals(start, segs[0].startMillis)
        assertEquals(at(d2, 0, 0), segs[0].endMillis)
        // 第二段：00:00 → 01:00
        assertEquals(at(d2, 0, 0), segs[1].startMillis)
        assertEquals(end, segs[1].endMillis)
        // 时长守恒：2 小时
        val totalSec = segs.sumOf { (it.endMillis - it.startMillis) / 1000 }
        assertEquals(7200L, totalSec)
        // 组 id 与序号
        val gid = segs[0].splitGroupId
        assertTrue(gid.isNotBlank())
        assertEquals(gid, segs[1].splitGroupId)
        assertEquals(0, segs[0].splitIndex)
        assertEquals(1, segs[1].splitIndex)
    }

    @Test
    fun `two nights splits into three segments`() {
        val d1 = LocalDate.of(2026, 10, 19)
        val d3 = LocalDate.of(2026, 10, 21)
        val start = at(d1, 23, 0)
        val end = at(d3, 2, 0)
        val segs = RecordSplitter.split(start, end, zone)

        assertEquals(3, segs.size)
        // 无缝衔接
        assertEquals(segs[0].endMillis, segs[1].startMillis)
        assertEquals(segs[1].endMillis, segs[2].startMillis)
        // 时长守恒
        val totalSec = segs.sumOf { (it.endMillis - it.startMillis) / 1000 }
        assertEquals((end - start) / 1000, totalSec)
        // 序号连续
        assertEquals(listOf(0, 1, 2), segs.map { it.splitIndex })
    }

    @Test
    fun `exact midnight boundary belongs to next day`() {
        val d1 = LocalDate.of(2026, 10, 19)
        val d2 = LocalDate.of(2026, 10, 20)
        // 22:00 开始、次日 00:00 精确结束 → 全部属于第一天，不产生空段
        val start = at(d1, 22, 0)
        val end = at(d2, 0, 0)
        val segs = RecordSplitter.split(start, end, zone)
        assertEquals(1, segs.size)
        assertEquals(start, segs[0].startMillis)
        assertEquals(end, segs[0].endMillis)
    }

    @Test
    fun `explicit groupId is preserved`() {
        val start = at(LocalDate.of(2026, 10, 19), 23, 30)
        val end = at(LocalDate.of(2026, 10, 20), 0, 30)
        val segs = RecordSplitter.split(start, end, zone, groupId = "fixed-group")
        assertEquals(2, segs.size)
        assertTrue(segs.all { it.splitGroupId == "fixed-group" })
    }

    @Test
    fun `no millisecond loss`() {
        val start = at(LocalDate.of(2026, 10, 19), 23, 59) + 999
        val end = at(LocalDate.of(2026, 10, 20), 0, 1) + 123
        val segs = RecordSplitter.split(start, end, zone)
        assertEquals(start, segs.first().startMillis)
        assertEquals(end, segs.last().endMillis)
        val totalMs = segs.sumOf { it.endMillis - it.startMillis }
        assertEquals(end - start, totalMs)
    }

    @Test
    fun `dateOf returns local date`() {
        assertEquals(
            LocalDate.of(2026, 10, 19),
            RecordSplitter.dateOf(at(LocalDate.of(2026, 10, 19), 23, 59), zone),
        )
    }
}
