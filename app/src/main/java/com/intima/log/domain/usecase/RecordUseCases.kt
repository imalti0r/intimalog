package com.intima.log.domain.usecase

import com.intima.log.data.db.SexRecordWithPartners
import com.intima.log.data.repo.RecordRepository
import com.intima.log.data.repo.RecordDraft
import com.intima.log.data.db.SceneTemplate
import com.intima.log.data.repo.TemplateRepository
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** 保存记录（事务：记录+关联+participantCount） */
@Singleton
class SaveRecordWithPartnersUseCase @Inject constructor(
    private val recordRepository: RecordRepository,
) {
    suspend operator fun invoke(draft: RecordDraft): Long = recordRepository.save(draft)
    suspend fun startOngoing(partnerIds: List<Long> = emptyList()): Long =
        recordRepository.startOngoing(partnerIds)
    suspend fun finishOngoing(): Long? = recordRepository.finishOngoing()
}

/** 组合排行：把记录聚合成搭档组合并排序 */
data class CombinationRank(
    val partnerIds: List<Long>,
    val sessionCount: Int,
    val avgDurationSeconds: Long?,
    val avgRating: Double?,
)

@Singleton
class GetCombinationStatsUseCase @Inject constructor(
    private val recordRepository: RecordRepository,
) {
    /** 全部组合，按场次降序。单人（0/1 位伴侣）不视为组合。 */
    suspend operator fun invoke(topN: Int = 0): List<CombinationRank> {
        val all = recordRepository.listAllWithPartners()
        return aggregate(all).let { if (topN > 0) it.take(topN) else it }
    }

    /** 某伴侣的 Top 组合（含 2 人及以上） */
    suspend fun forPartner(partnerId: Long, topN: Int = 5): List<CombinationRank> {
        val records = recordRepository.recordsContaining(partnerId)
        return aggregate(records).take(topN)
    }

    private fun aggregate(records: List<SexRecordWithPartners>): List<CombinationRank> {
        data class Entry(val ids: List<Long>, val record: com.intima.log.data.db.SexRecord)
        val entries = records.map { r -> Entry(r.partners.map { it.id }.sorted(), r.record) }
            .filter { it.ids.size >= 2 }
        return entries
            .groupBy { it.ids }
            .map { (ids, group) ->
                val durations = group.mapNotNull { it.record.durationSeconds }
                val ratings = group.map { it.record.rating }.filter { it > 0 }
                CombinationRank(
                    partnerIds = ids,
                    sessionCount = group.size,
                    avgDurationSeconds = if (durations.isEmpty()) null else durations.average().toLong(),
                    avgRating = if (ratings.isEmpty()) null else ratings.average(),
                )
            }
            .sortedByDescending { it.sessionCount }
    }
}

/** 场景模板套用：只取组合/措施/评分，绝不覆盖时间 */
data class TemplateApplication(
    val partnerIds: List<Long>,
    val rating: Int,
)

@Singleton
class ApplyTemplateUseCase @Inject constructor(
    private val templateRepository: TemplateRepository,
) {
    suspend operator fun invoke(templateId: Long): TemplateApplication? {
        val t: SceneTemplate = templateRepository.byId(templateId) ?: return null
        return TemplateApplication(
            partnerIds = parseIds(t.partnerIdsJson),
            rating = t.rating,
        )
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun parseIds(jsonStr: String): List<Long> =
            runCatching { json.decodeFromString<List<Long>>(jsonStr) }.getOrDefault(emptyList())

        fun encodeIds(ids: List<Long>): String = json.encodeToString(ids)
    }
}
