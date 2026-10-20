package com.intima.log.data.repo

import com.intima.log.data.db.Partner
import com.intima.log.data.db.RecordPartnerCrossRef
import com.intima.log.data.db.SexRecord
import com.intima.log.data.db.SexRecordWithPartners

/** 保存记录的请求 */
data class RecordDraft(
    val recordId: Long = 0,               // 0 = 新建
    val partnerIds: List<Long> = emptyList(),  // 按参与顺序
    val primaryPartnerId: Long? = null,   // 主参与者（必须在 partnerIds 内）
    val startTime: Long,
    val endTime: Long? = null,
    val customProtections: List<String> = emptyList(),  // 自定义保护措施名（仅用户自定义）
    val rating: Int = 0,
    val moodTag: String = "",             // 情绪（单选）
    val customPlaces: List<String> = emptyList(),       // 自定义地点名（仅用户自定义）
    val positions: List<String> = emptyList(),  // 自定义体位
    val toys: List<String> = emptyList(),       // 自定义玩具
    val note: String = "",
    val isBackfill: Boolean = false,
    val climaxes: Map<String, Int> = emptyMap(),  // 每人高潮次数（key=顺序号或 "me"）
    val splitGroupId: String? = null,   // 编辑拆分组时保持组 id 稳定；新建跨日自动生成
)

interface RecordRepository {
    fun observeAll(): kotlinx.coroutines.flow.Flow<List<SexRecordWithPartners>>
    fun observeRange(from: Long, until: Long): kotlinx.coroutines.flow.Flow<List<SexRecordWithPartners>>
    fun observeOngoing(): kotlinx.coroutines.flow.Flow<SexRecord?>
    fun observeCountInRange(from: Long, until: Long): kotlinx.coroutines.flow.Flow<Int>
    suspend fun countInRangeOnce(from: Long, until: Long): Int {
        var v = 0
        observeCountInRange(from, until).collect { v = it }
        return v
    }

    suspend fun byId(id: Long): SexRecordWithPartners?
    suspend fun ongoingRecord(): SexRecord?

    /** 事务写入：记录 + 关联 + participantCount 原子维护。跨日区间自动按自然日拆分 */
    suspend fun save(draft: RecordDraft): Long

    /** 开始计时（endTime=null） */
    suspend fun startOngoing(partnerIds: List<Long> = emptyList()): Long

    /** 结束进行中的记录，返回结束的记录 id；无进行中记录返回 null */
    suspend fun finishOngoing(endTime: Long = System.currentTimeMillis()): Long?

    /** 删除记录；若属于拆分组则联动删除同组其余段 */
    suspend fun delete(id: Long)
    suspend fun deleteAll()
    suspend fun count(): Int
    suspend fun listAllWithPartners(): List<SexRecordWithPartners>

    // 组合查询
    suspend fun recordsWithExactPartners(partnerIds: List<Long>): List<SexRecordWithPartners>
    suspend fun recordsContaining(partnerId: Long): List<SexRecordWithPartners>

    /** 拆分组的全部段（按 splitIndex 升序） */
    suspend fun splitGroupSiblings(groupId: String): List<com.intima.log.data.db.SexRecord>
    suspend fun partnerStats(): List<com.intima.log.data.db.PartnerWithStats>
    suspend fun partnerPersonTimes(): List<com.intima.log.data.db.PartnerPersonTimeRow>
        suspend fun multiPartnerBuckets(): List<com.intima.log.data.db.BucketCountRow>
    suspend fun avgDurationSince(from: Long): Double?
    suspend fun rawTimesForHeatmap(from: Long): List<com.intima.log.data.db.TimeCountRow>
}
