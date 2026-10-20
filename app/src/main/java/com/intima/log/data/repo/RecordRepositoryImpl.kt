package com.intima.log.data.repo

import androidx.room.withTransaction
import com.intima.log.data.db.IntimaDatabase
import com.intima.log.data.db.PartnerDao
import com.intima.log.data.db.Protection
import com.intima.log.data.db.Place
import com.intima.log.data.db.RecordDao
import com.intima.log.data.db.RecordPartnerCrossRef
import com.intima.log.data.db.SexRecord
import com.intima.log.data.db.SexRecordWithPartners
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
        val record = SexRecord(
            id = draft.recordId,
            participantCount = distinctIds.size,
            startTime = draft.startTime,
            endTime = draft.endTime,
            protection = draft.protections.firstOrNull() ?: draft.protection,
            climax = draft.climax,
            rating = draft.rating,
            moodTags = draft.moodTag,
            place = draft.place,
            protections = draft.protections.joinToString(",") { it.name },
            customProtections = draft.customProtections.joinToString(","),
            customPlaces = draft.customPlaces.joinToString(","),
            positions = draft.positions.joinToString(","),
            toys = draft.toys.joinToString(","),
            note = draft.note,
            isBackfill = draft.isBackfill,
            updatedAt = now,
        )
        val recordId = if (draft.recordId == 0L) {
            recordDao.insert(record)
        } else {
            // 保留原 createdAt
            val old = recordDao.byId(draft.recordId)
            recordDao.update(record.copy(createdAt = old?.createdAt ?: now))
            recordDao.deleteLinksFor(draft.recordId)
            draft.recordId
        }
        distinctIds.forEachIndexed { index, partnerId ->
            recordDao.insertLink(
                RecordPartnerCrossRef(
                    recordId = recordId,
                    partnerId = partnerId,
                    joinOrder = index,
                    isPrimary = partnerId == draft.primaryPartnerId
                )
            )
        }
        recordId
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
        recordDao.finishRecord(ongoing.id, fixedEnd, System.currentTimeMillis())
        return ongoing.id
    }

    override suspend fun delete(id: Long) {
        recordDao.delete(id)
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

    override suspend fun partnerStats() = recordDao.partnerStats()
    override suspend fun partnerPersonTimes() = recordDao.partnerPersonTimes()
    override suspend fun protectionCounts() = recordDao.protectionCounts()
    override suspend fun multiPartnerBuckets() = recordDao.multiPartnerBuckets()
    override suspend fun avgDurationSince(from: Long) = recordDao.avgDurationSince(from)
    override suspend fun rawTimesForHeatmap(from: Long) = recordDao.rawTimesForHeatmap(from)
}
