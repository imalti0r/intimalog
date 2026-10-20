package com.intima.log.domain.usecase

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

/**
 * 跨日记录拆分：按本地自然日 00:00 把 [start, end) 切成若干段。
 *
 * 不变量（由单元测试覆盖）：
 * - 每段 = 一个本地自然日内的区间，段与段之间无缝隙、无重叠；
 * - 所有段时长之和 == 原时长；
 * - 组内按时间升序 splitIndex = 0..n-1；同一组共享一个 splitGroupId；
 * - 起点/终点毫秒值不损失（拆分点恒为 00:00 整点毫秒）。
 */
object RecordSplitter {

    data class Segment(
        val startMillis: Long,
        val endMillis: Long,
        val splitGroupId: String,
        val splitIndex: Int,
    )

    /**
     * 拆分 [start, end]。end <= start、或同一天内 → 返回单个 Segment（splitIndex=0）。
     * @param groupId 显式传入用于「合并编辑」时保持组 id 稳定；不传则生成 UUID。
     */
    fun split(
        start: Long,
        end: Long,
        zone: ZoneId = ZoneId.systemDefault(),
        groupId: String? = null,
    ): List<Segment> {
        val gid = groupId ?: UUID.randomUUID().toString()
        val startDate = Instant.ofEpochMilli(start).atZone(zone).toLocalDate()
        val endDate = Instant.ofEpochMilli(end).atZone(zone).toLocalDate()

        val out = ArrayList<Segment>(2)
        var index = 0
        var cursor = start
        var day = startDate
        while (day <= endDate) {
            val dayEnd = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val segEnd = minOf(end, dayEnd)
            if (segEnd > cursor) {
                out.add(Segment(cursor, segEnd, gid, index++))
                cursor = segEnd
            }
            day = day.plusDays(1)
        }
        // 同日或区间无效：返回单段（保持「一条记录」语义，调用方无需判空）
        if (out.isEmpty()) {
            out.add(Segment(start, maxOf(end, start), gid, 0))
        }
        return out
    }

    /** 记录起始时间戳所属的本地自然日 */
    fun dateOf(millis: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
}
