package com.intima.log.data.backup

import android.content.Context
import android.net.Uri
import com.intima.log.data.db.Partner
import com.intima.log.data.db.Protection
import com.intima.log.data.db.Place
import com.intima.log.data.db.RecordPartnerCrossRef
import com.intima.log.data.db.SexRecord
import com.intima.log.data.repo.PartnerRepository
import com.intima.log.data.repo.RecordDraft
import com.intima.log.data.repo.RecordRepository
import com.intima.log.data.security.SecurityManager
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** 导出模型（含关联表） */
@Serializable
data class BackupPayload(
    val version: Int = 1,
    val exportedAt: Long,
    val partners: List<BackupPartner>,
    val records: List<BackupRecord>,
)

@Serializable
data class BackupPartner(
    val id: Long,
    val nickname: String,
    val alias: String? = null,
    val emoji: String = "🙂",
    val colorIndex: Int = 0,
    val gender: String = "UNDISCLOSED",
    val isRegular: Boolean = true,
    val archived: Boolean = false,
    val isImported: Boolean = false,
    val createdAt: Long,
)

@Serializable
data class BackupRecord(
    val id: Long,
    val startTime: Long,
    val endTime: Long? = null,
    val protection: String = "NOT_RECORDED",
    val climax: Int = -1,
    val rating: Int = 0,
    val moodTags: String = "",
    val protections: String = "",   // 逗号分隔 Protection.name（v3 备份起）
    val customProtections: String = "",  // 逗号分隔自定义保护措施（v5 备份起）
    val customPlaces: String = "",       // 逗号分隔自定义地点（v5 备份起）
    val positions: String = "",
    val toys: String = "",
    val place: String = "NOT_RECORDED",
    val note: String = "",
    val isBackfill: Boolean = false,
    val createdAt: Long,
    val partnerIds: List<Long> = emptyList(),   // 按参与顺序
)

@Singleton
class BackupManager @Inject constructor(
    private val recordRepository: RecordRepository,
    private val partnerRepository: PartnerRepository,
) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    /** 导出为加密 .intima 字节 */
    suspend fun exportEncrypted(pin: String): ByteArray {
        val partners = partnerRepository.listActive().map { p ->
            BackupPartner(p.id, p.nickname, p.alias, p.emoji, p.colorIndex, p.gender.name, p.isRegular, p.archived, p.isImported, p.createdAt)
        }
        val records = recordRepository.listAllWithPartners().map { r ->
            BackupRecord(
                id = r.record.id,
                startTime = r.record.startTime,
                endTime = r.record.endTime,
                protection = r.record.protection.name,
                climax = r.record.climax,
                rating = r.record.rating,
                moodTags = r.record.moodTags,
                protections = r.record.protections,
                customProtections = r.record.customProtections,
                customPlaces = r.record.customPlaces,
                positions = r.record.positions,
                toys = r.record.toys,
                place = r.record.place.name,
                note = r.record.note,
                isBackfill = r.record.isBackfill,
                createdAt = r.record.createdAt,
                partnerIds = r.partners.map { it.id },
            )
        }
        val payload = BackupPayload(exportedAt = System.currentTimeMillis(), partners = partners, records = records)
        return SecurityManager.encryptExport(json.encodeToString(payload).toByteArray(Charsets.UTF_8), pin)
    }

    /**
     * 导入：partnerId 不存在的创建占位伴侣（导入-未知N，isImported=true）。
     * 返回 (导入记录数, 创建占位数)。
     */
    suspend fun importEncrypted(data: ByteArray, pin: String): Pair<Int, Int> {
        val plain = SecurityManager.decryptExport(data, pin)
        val payload = json.decodeFromString<BackupPayload>(plain.toString(Charsets.UTF_8))

        val existingIds = partnerRepository.listActive().map { it.id }.toSet()
        val idMap = mutableMapOf<Long, Long>()   // 旧 id → 新 id
        var placeholders = 0

        // 伴侣映射：同 id 直接映射；缺失的建占位
        payload.partners.forEach { bp ->
            if (bp.id in existingIds) {
                idMap[bp.id] = bp.id
            } else {
                val newId = partnerRepository.save(
                    Partner(
                        nickname = "导入-未知${++placeholders}",
                        emoji = bp.emoji,
                        colorIndex = bp.colorIndex,
                        gender = runCatching { com.intima.log.data.db.Gender.valueOf(bp.gender) }
                            .getOrDefault(com.intima.log.data.db.Gender.UNDISCLOSED),
                        isImported = true,
                        createdAt = bp.createdAt,
                    )
                )
                idMap[bp.id] = newId
            }
        }

        var imported = 0
        payload.records.forEach { br ->
            val draft = RecordDraft(
                partnerIds = br.partnerIds.mapNotNull { idMap[it] },
                primaryPartnerId = br.partnerIds.firstOrNull()?.let { idMap[it] },
                startTime = br.startTime,
                endTime = br.endTime,
                protection = Protection.NOT_RECORDED, // 兼容旧列；多选 protections 才是权威
                climax = br.climax,
                rating = br.rating,
                moodTag = br.moodTags,
                protections = br.protections.split(",").filter { it.isNotBlank() }
                    .mapNotNull { n ->
                        // 旧备份的 PILL → 紧急避孕药
                        when (n) {
                            "PILL" -> Protection.PILL_URGENT
                            else -> runCatching { Protection.valueOf(n) }.getOrNull()
                        }
                    },
                customProtections = br.customProtections.split(",").filter { it.isNotBlank() },
                positions = br.positions.split(",").filter { it.isNotBlank() },
                toys = br.toys.split(",").filter { it.isNotBlank() },
                place = runCatching { Place.valueOf(br.place) }.getOrDefault(Place.NOT_RECORDED),
                customPlaces = br.customPlaces.split(",").filter { it.isNotBlank() },
                note = br.note,
                isBackfill = true,
            )
            recordRepository.save(draft)
            imported++
        }
        return imported to placeholders
    }

    /** CSV 明文导出（可选脱敏：P1/P2… 代替伴侣名） */
    suspend fun exportCsv(anonymize: Boolean): String {
        val sb = StringBuilder()
        sb.appendLine("start_time,end_time,duration_seconds,protection,place,rating,mood_tags,note,partners")
        val all = recordRepository.listAllWithPartners()
        val anonMap = mutableMapOf<Long, String>()
        all.forEach { item ->
            val partnerNames = item.partners.map { p ->
                if (!anonymize) p.nickname
                else anonMap.getOrPut(p.id) { "P${anonMap.size + 1}" }
            }
            val fmt = { ms: Long -> Instant.ofEpochMilli(ms).toString() }
            sb.appendLine(
                listOf(
                    fmt(item.record.startTime),
                    item.record.endTime?.let(fmt) ?: "",
                    item.record.durationSeconds?.toString() ?: "",
                    item.record.protection.name,
                    item.record.place.name,
                    item.record.rating.toString(),
                    item.record.moodTags,
                    item.record.note.replace(",", "，"),
                    partnerNames.joinToString("+"),
                ).joinToString(",")
            )
        }
        return sb.toString()
    }
}
