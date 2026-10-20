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
    val protection: com.intima.log.data.db.Protection = com.intima.log.data.db.Protection.NOT_RECORDED,
    val protections: List<com.intima.log.data.db.Protection> = emptyList(), // 多选保护措施
    val customProtections: List<String> = emptyList(),  // 自定义保护措施名
    val climax: Int = -1,
    val rating: Int = 0,
    val moodTag: String = "",             // 情绪（单选）
    val place: com.intima.log.data.db.Place = com.intima.log.data.db.Place.NOT_RECORDED,
    val customPlaces: List<String> = emptyList(),       // 自定义地点名
    val positions: List<String> = emptyList(),  // 自定义体位
    val toys: List<String> = emptyList(),       // 自定义玩具
    val note: String = "",
    val isBackfill: Boolean = false,
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

    /** 事务写入：记录 + 关联 + participantCount 原子维护 */
    suspend fun save(draft: RecordDraft): Long

    /** 开始计时（endTime=null） */
    suspend fun startOngoing(partnerIds: List<Long> = emptyList()): Long

    /** 结束进行中的记录，返回结束的记录 id；无进行中记录返回 null */
    suspend fun finishOngoing(endTime: Long = System.currentTimeMillis()): Long?

    suspend fun delete(id: Long)
    suspend fun deleteAll()
    suspend fun count(): Int
    suspend fun listAllWithPartners(): List<SexRecordWithPartners>

    // 组合查询
    suspend fun recordsWithExactPartners(partnerIds: List<Long>): List<SexRecordWithPartners>
    suspend fun recordsContaining(partnerId: Long): List<SexRecordWithPartners>
    suspend fun partnerStats(): List<com.intima.log.data.db.PartnerWithStats>
    suspend fun partnerPersonTimes(): List<com.intima.log.data.db.PartnerPersonTimeRow>
    suspend fun protectionCounts(): List<com.intima.log.data.db.ProtectionCountRow>
    suspend fun multiPartnerBuckets(): List<com.intima.log.data.db.BucketCountRow>
    suspend fun avgDurationSince(from: Long): Double?
    suspend fun rawTimesForHeatmap(from: Long): List<com.intima.log.data.db.TimeCountRow>
}
