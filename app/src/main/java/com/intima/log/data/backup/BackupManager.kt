package com.intima.log.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.intima.log.data.db.CustomTagEntity
import com.intima.log.data.db.IntimaDatabase
import com.intima.log.data.db.Partner
import com.intima.log.data.db.RecordPartnerCrossRef
import com.intima.log.data.db.SceneTemplate
import com.intima.log.data.db.SexRecord
import com.intima.log.data.repo.PartnerRepository
import com.intima.log.data.repo.RecordRepository
import com.intima.log.data.security.SecurityManager
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** 导出模型（v2：全量六类 —— 伴侣 / 记录 / 自定义标签四路 / 模板 / 用户资料） */
@Serializable
data class BackupPayload(
    val version: Int = 2,
    val exportedAt: Long,
    val partners: List<BackupPartner>,
    val records: List<BackupRecord>,
    val tags: List<BackupTag> = emptyList(),          // custom_tags 全部 kind
    val templates: List<BackupTemplate> = emptyList(),// 场景模板
    val profile: BackupProfile? = null,               // 用户资料（单行）
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
    val rating: Int = 0,
    val moodTags: String = "",
    val customProtections: String = "",  // 逗号分隔自定义保护措施（仅用户自定义）
    val customPlaces: String = "",       // 逗号分隔自定义地点（仅用户自定义）
    val positions: String = "",
    val toys: String = "",
    val note: String = "",
    val isBackfill: Boolean = false,
    val createdAt: Long,
    val partnerIds: List<Long> = emptyList(),   // 按参与顺序
    val climaxesJson: String = "",              // 每人高潮次数 JSON（key=记录内顺序号，"me"=用户）
    val splitGroupId: String? = null,           // 跨日拆分组（恢复时原样回填）
    val splitIndex: Int = 0,
)

@Serializable
data class BackupTag(val id: Long, val kind: String, val name: String)

@Serializable
data class BackupTemplate(
    val id: Long,
    val name: String,
    val partnerIdsJson: String,          // JSON 数组 [1,2,3]
    val rating: Int = 0,
    val sortOrder: Int = 0,
    val pinned: Boolean = false,
    val createdAt: Long,
)

@Serializable
data class BackupProfile(
    val gender: String = "UNDISCLOSED",
    val birthYear: Int? = null,
)

@Singleton
class BackupManager @Inject constructor(
    private val db: IntimaDatabase,
    private val recordRepository: RecordRepository,
    private val partnerRepository: PartnerRepository,
) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    /** 导出为加密 .intima 字节（全量：伴侣 + 记录 + 四路自定义标签 + 模板 + 用户资料） */
    suspend fun exportEncrypted(pin: String): ByteArray {
        val partners = db.partnerDao().listActive().map { p ->
            BackupPartner(p.id, p.nickname, p.alias, p.emoji, p.colorIndex, p.gender.name, p.isRegular, p.archived, p.isImported, p.createdAt)
        }
        val records = db.recordDao().listAllWithPartners().map { r ->
            BackupRecord(
                id = r.record.id,
                startTime = r.record.startTime,
                endTime = r.record.endTime,
                rating = r.record.rating,
                moodTags = r.record.moodTags,
                customProtections = r.record.customProtections,
                customPlaces = r.record.customPlaces,
                positions = r.record.positions,
                toys = r.record.toys,
                note = r.record.note,
                isBackfill = r.record.isBackfill,
                createdAt = r.record.createdAt,
                partnerIds = r.partners.map { it.id },
                climaxesJson = r.record.climaxesJson,
                splitGroupId = r.record.splitGroupId,
                splitIndex = r.record.splitIndex,
            )
        }
        val tags = db.tagDao().listAll().map { BackupTag(it.id, it.kind, it.name) }
        val templates = db.templateDao().listAll().map {
            BackupTemplate(it.id, it.name, it.partnerIdsJson, it.rating, it.sortOrder, it.pinned, it.createdAt)
        }
        val profile = db.profileDao().get()?.let {
            BackupProfile(it.gender.name, it.birthYear)
        }
        val payload = BackupPayload(
            exportedAt = System.currentTimeMillis(),
            partners = partners,
            records = records,
            tags = tags,
            templates = templates,
            profile = profile,
        )
        return SecurityManager.encryptExport(json.encodeToString(payload).toByteArray(Charsets.UTF_8), pin)
    }

    /**
     * 恢复（覆盖式）：单事务内清空本地全部业务数据（记录+关联+伴侣+四路标签+模板+用户资料），
     * 再按备份原样写入。伴侣/记录/标签/模板保留原 id（跨设备恢复后数据完全一致）。
     * 返回 (恢复记录数, 恢复伴侣数)。
     */
    suspend fun restoreEncrypted(data: ByteArray, pin: String): Pair<Int, Int> {
        val plain = SecurityManager.decryptExport(data, pin)
        val payload = json.decodeFromString<BackupPayload>(plain.toString(Charsets.UTF_8))

        db.withTransaction {
            // ---- 清库（关联表随记录 CASCADE，显式删更稳） ----
            db.recordDao().deleteAllLinks()
            db.recordDao().deleteAll()
            db.partnerDao().deleteAll()
            db.tagDao().deleteAll()
            db.templateDao().deleteAll()

            // ---- 写伴侣（保留原 id） ----
            payload.partners.forEach { bp ->
                db.partnerDao().insert(
                    Partner(
                        id = bp.id,
                        nickname = bp.nickname,
                        alias = bp.alias,
                        emoji = bp.emoji,
                        colorIndex = bp.colorIndex,
                        gender = runCatching { com.intima.log.data.db.Gender.valueOf(bp.gender) }
                            .getOrDefault(com.intima.log.data.db.Gender.UNDISCLOSED),
                        isRegular = bp.isRegular,
                        archived = bp.archived,
                        isImported = bp.isImported,
                        createdAt = bp.createdAt,
                    )
                )
            }
            // 占位「已删除」行自愈（若备份链路里曾有 purge，保持其隔离态）
            db.partnerDao().ensureDeletedPlaceholder()
            db.partnerDao().quarantinePlaceholder()

            // ---- 写记录（保留原 id）+ 重建关联表 ----
            payload.records.forEach { br ->
                db.recordDao().insert(
                    SexRecord(
                        id = br.id,
                        participantCount = br.partnerIds.size,
                        startTime = br.startTime,
                        endTime = br.endTime,
                        rating = br.rating,
                        moodTags = br.moodTags,
                        customProtections = br.customProtections,
                        customPlaces = br.customPlaces,
                        positions = br.positions,
                        toys = br.toys,
                        note = br.note,
                        isBackfill = br.isBackfill,
                        climaxesJson = br.climaxesJson,
                        splitGroupId = br.splitGroupId,
                        splitIndex = br.splitIndex,
                        createdAt = br.createdAt,
                        updatedAt = System.currentTimeMillis(),
                    )
                )
                br.partnerIds.forEachIndexed { order, pid ->
                    db.recordDao().insertLink(RecordPartnerCrossRef(br.id, pid, order, order == 0))
                }
            }

            // ---- 写自定义标签（体位/玩具/保护措施/地点，保留原 id） ----
            payload.tags.forEach { t ->
                db.tagDao().insertReplace(CustomTagEntity(id = t.id, kind = t.kind, name = t.name))
            }

            // ---- 写模板（保留原 id；伴侣组合按 id 直接引用，恢复在同一事务内自洽） ----
            payload.templates.forEach { t ->
                db.templateDao().insert(
                    SceneTemplate(
                        id = t.id,
                        name = t.name,
                        partnerIdsJson = t.partnerIdsJson,
                        rating = t.rating,
                        sortOrder = t.sortOrder,
                        pinned = t.pinned,
                        createdAt = t.createdAt,
                    )
                )
            }

            // ---- 写用户资料（仅 gender/birthYear；PIN、伪装等本机设置不动） ----
            payload.profile?.let { p ->
                db.profileDao().ensureRow()
                db.profileDao().restoreProfile(p.gender, p.birthYear)
            }
        }
        return payload.records.size to payload.partners.size
    }

    /** CSV 明文导出（可选脱敏：P1/P2… 代替伴侣名） */
    suspend fun exportCsv(anonymize: Boolean): String {
        val sb = StringBuilder()
        sb.appendLine("start_time,end_time,duration_seconds,protections,places,rating,mood_tags,note,partners")
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
                    item.record.customProtections,
                    item.record.customPlaces,
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
