package com.intima.log.data.repo

import androidx.room.withTransaction
import com.intima.log.data.db.IntimaDatabase
import com.intima.log.data.db.PartnerDao
import com.intima.log.data.db.RecordDao
import com.intima.log.data.db.RecordPartnerCrossRef
import com.intima.log.data.db.SexRecord
import com.intima.log.data.db.SexRecordWithPartners
import com.intima.log.domain.usecase.RecordSplitter
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordRepositoryImpl @Inject constructor(
    private val db: IntimaDatabase,
    private val recordDao: RecordDao,
    private val partnerDao: PartnerDao,
) : RecordRepository {

    override fun observeAll(): Flow<List<SexRecordWithPartners>> = recordDao.observeAllWithPartners()
    override fun observeRange(from: Long, until: Long): Flow<List<SexRecordWithPartners>> =
        recordDao.observeRangeWithPartners(from, until)
    override fun observeOngoing(): Flow<SexRecord?> = recordDao.observeOngoing()
    override fun observeCountInRange(from: Long, until: Long): Flow<Int> =
        recordDao.observeCountInRange(from, until)

    override suspend fun countInRangeOnce(from: Long, until: Long): Int = recordDao.countInRange(from, until)

    override suspend fun byId(id: Long): SexRecordWithPartners? = recordDao.byIdWithPartners(id)
    override suspend fun ongoingRecord(): SexRecord? = recordDao.ongoing()

    /**
     * 事务核心：withTransaction 内写入记录 + 重建关联表 + 维护 participantCount。
     * 关联表采用「先删后插」重建，保证 joinOrder / isPrimary 与主键唯一约束。
     */
    override suspend fun save(draft: RecordDraft): Long = db.withTransaction {
        val now = System.currentTimeMillis()
        val distinctIds = draft.partnerIds.distinct()

        // ---- 跨日拆分：[start, end) 按本地自然日 00:00 切成多行，同组共享 splitGroupId ----
        // 进行中记录（endTime=null）不拆分，endTime 行存 null；结束时由 finishOngoing 补拆
        val segments = RecordSplitter.split(
            draft.startTime,
            draft.endTime ?: draft.startTime,
            groupId = draft.splitGroupId,
        )

        // 编辑已有记录：若原来属于某拆分组而新区间不再跨日/组变化，先清掉旧组的其余段
        if (draft.recordId != 0L) {
            val old = recordDao.byId(draft.recordId)
            val oldGroup = old?.splitGroupId
            if (oldGroup != null && (segments.size <= 1 || segments.first().splitGroupId != oldGroup)) {
                recordDao.deleteBySplitGroupId(oldGroup)
            }
        }

        val single = segments.size == 1
        var firstId = 0L
        segments.forEachIndexed { segIdx, seg ->
            val row = SexRecord(
                id = if (single) draft.recordId else 0L,
                participantCount = distinctIds.size,
                startTime = seg.startMillis,
                endTime = draft.endTime?.let { seg.endMillis },
                rating = draft.rating,
                moodTags = draft.moodTag,
                customProtections = draft.customProtections.joinToString(","),
                customPlaces = draft.customPlaces.joinToString(","),
                positions = draft.positions.joinToString(","),
                toys = draft.toys.joinToString(","),
                note = draft.note,
                isBackfill = draft.isBackfill,
                // 高潮次数语义：跨日拆分时第一天段(splitIndex=0)记 0（空），完整计数挂在后续段上
                climaxesJson = (if (!single && seg.splitIndex == 0) emptyMap() else draft.climaxes)
                    .filterValues { it != 0 }
                    .takeIf { it.isNotEmpty() }
                    ?.let { kotlinx.serialization.json.Json.encodeToString(it) } ?: "",
                splitGroupId = if (single) null else seg.splitGroupId,
                splitIndex = seg.splitIndex,
                createdAt = if (draft.recordId != 0L && segIdx == 0) {
                    recordDao.byId(draft.recordId)?.createdAt ?: now
                } else now,
                updatedAt = now,
            )
            val id: Long
            if (single && draft.recordId != 0L) {
                recordDao.update(row)
                recordDao.deleteLinksFor(draft.recordId)
                id = draft.recordId
            } else {
                id = recordDao.insert(row)
            }
            if (segIdx == 0) firstId = id
            distinctIds.forEachIndexed { index, partnerId ->
                recordDao.insertLink(
                    RecordPartnerCrossRef(
                        recordId = id,
                        partnerId = partnerId,
                        joinOrder = index,
                        isPrimary = partnerId == draft.primaryPartnerId
                    )
                )
            }
        }
        firstId
    }

    override suspend fun startOngoing(partnerIds: List<Long>): Long = db.withTransaction {
        // 已有进行中的先结束，避免双开
        recordDao.ongoing()?.let {
            recordDao.finishRecord(it.id, System.currentTimeMillis(), System.currentTimeMillis())
        }
        save(
            RecordDraft(
                partnerIds = partnerIds,
                startTime = System.currentTimeMillis(),
                endTime = null
            )
        )
    }

    override suspend fun finishOngoing(endTime: Long): Long? {
        val ongoing = recordDao.ongoing() ?: return null
        val fixedEnd = if (endTime <= ongoing.startTime) ongoing.startTime + 60_000L else endTime
        // 跨日计时：结束时按自然日拆分（原行改为第一段，多余段插入）
        val segments = RecordSplitter.split(ongoing.startTime, fixedEnd)
        if (segments.size <= 1) {
            recordDao.finishRecord(ongoing.id, fixedEnd, System.currentTimeMillis())
            return ongoing.id
        }
        db.withTransaction {
            val first = segments.first()
            recordDao.finishRecord(ongoing.id, first.endMillis, System.currentTimeMillis())
            recordDao.updateSplitGroup(
                id = ongoing.id,
                splitGroupId = first.splitGroupId,
                splitIndex = first.splitIndex,
                updatedAt = System.currentTimeMillis(),
            )
            // 高潮次数语义：跨日拆分时第一天段清空计数，完整计数挂到后续段（次日段）
            if (ongoing.climaxesJson.isNotBlank()) {
                recordDao.clearClimaxes(ongoing.id, System.currentTimeMillis())
            }
            segments.drop(1).forEach { seg ->
                recordDao.insert(
                    SexRecord(
                        participantCount = ongoing.participantCount,
                        startTime = seg.startMillis,
                        endTime = seg.endMillis,
                        customProtections = ongoing.customProtections,
                        customPlaces = ongoing.customPlaces,
                        positions = ongoing.positions,
                        toys = ongoing.toys,
                        note = ongoing.note,
                        climaxesJson = ongoing.climaxesJson,
                        splitGroupId = seg.splitGroupId,
                        splitIndex = seg.splitIndex,
                    )
                )
            }
        }
        return ongoing.id
    }

    override suspend fun delete(id: Long) {
        // 拆分组联动删除：删任意一段 → 整组移除
        val target = recordDao.byId(id) ?: return
        target.splitGroupId?.let { recordDao.deleteBySplitGroupId(it) } ?: recordDao.delete(id)
    }

    override suspend fun deleteAll() {
        recordDao.deleteAll()
    }
    override suspend fun count(): Int = recordDao.count()
    override suspend fun listAllWithPartners(): List<SexRecordWithPartners> = recordDao.listAllWithPartners()
    override suspend fun recordsWithExactPartners(partnerIds: List<Long>): List<SexRecordWithPartners> {
        if (partnerIds.isEmpty()) return emptyList()
        return recordDao.recordsWithExactPartners(partnerIds, partnerIds.size)
            .mapNotNull { recordDao.byIdWithPartners(it) }
    }

    override suspend fun recordsContaining(partnerId: Long): List<SexRecordWithPartners> =
        recordDao.recordsContaining(partnerId).mapNotNull { recordDao.byIdWithPartners(it) }

    override suspend fun splitGroupSiblings(groupId: String): List<SexRecord> =
        recordDao.bySplitGroupId(groupId).sortedBy { it.splitIndex }

    override suspend fun partnerStats() = recordDao.partnerStats()
    override suspend fun partnerPersonTimes() = recordDao.partnerPersonTimes()
        override suspend fun multiPartnerBuckets() = recordDao.multiPartnerBuckets()
    override suspend fun avgDurationSince(from: Long) = recordDao.avgDurationSince(from)
    override suspend fun rawTimesForHeatmap(from: Long) = recordDao.rawTimesForHeatmap(from)
}
