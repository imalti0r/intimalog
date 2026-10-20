package com.intima.log.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PartnerDao {
    @Query("SELECT * FROM partners WHERE archived = 0 AND hidden = 0 ORDER BY createdAt ASC")
    fun observeActive(): Flow<List<Partner>>

    @Query("SELECT * FROM partners ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<Partner>>

    @Query("SELECT * FROM partners WHERE archived = 0 ORDER BY createdAt ASC")
    suspend fun listActive(): List<Partner>

    @Query("SELECT * FROM partners WHERE id = :id")
    suspend fun byId(id: Long): Partner?

    @Query("SELECT * FROM partners WHERE id IN (:ids)")
    suspend fun byIds(ids: List<Long>): List<Partner>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(partner: Partner): Long

    @Update
    suspend fun update(partner: Partner)

    /** 软删除（归档）。有记录引用时是唯一允许的删除方式。 */
    @Query("UPDATE partners SET archived = 1 WHERE id = :id")
    suspend fun archive(id: Long)

    @Query("UPDATE partners SET archived = 0 WHERE id = :id")
    suspend fun unarchive(id: Long)

    @Query("UPDATE partners SET hidden = 1 WHERE id = :id")
    suspend fun hide(id: Long)

    @Query("UPDATE partners SET hidden = 0 WHERE id = :id")
    suspend fun unhide(id: Long)

    @Query("SELECT COUNT(*) FROM record_partner_cross_ref WHERE partnerId = :partnerId")
    suspend fun usageCount(partnerId: Long): Int

    /** 彻底删除用的占位行（id=-1「已删除」），已存在则忽略 */
    @Query(
        "INSERT OR IGNORE INTO partners (id, nickname, emoji, colorIndex, gender, isRegular, archived, hidden, isImported, createdAt) " +
            "VALUES (-1, '已删除', '🗑️', 0, 'UNDISCLOSED', 0, 1, 1, 0, 0)"
    )
    suspend fun ensureDeletedPlaceholder()

    /** 把某伴侣的历史引用全部改指向占位行 */
    @Query("UPDATE record_partner_cross_ref SET partnerId = -1 WHERE partnerId = :id")
    suspend fun repointRefsToPlaceholder(id: Long)

    @Query("DELETE FROM partners WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** 占位行自愈：强制 archived=1 / hidden=1（防止误操作把占位行当普通伴侣恢复） */
    @Query("UPDATE partners SET archived = 1, hidden = 1 WHERE id = -1")
    suspend fun quarantinePlaceholder()
}

@Dao
interface RecordDao {
    @Transaction
    @Query("SELECT * FROM sex_records ORDER BY startTime DESC")
    fun observeAllWithPartners(): Flow<List<SexRecordWithPartners>>

    @Transaction
    @Query("SELECT * FROM sex_records WHERE startTime >= :from AND startTime < :until ORDER BY startTime DESC")
    fun observeRangeWithPartners(from: Long, until: Long): Flow<List<SexRecordWithPartners>>

    @Transaction
    @Query("SELECT * FROM sex_records WHERE id = :id")
    suspend fun byIdWithPartners(id: Long): SexRecordWithPartners?

    @Transaction
    @Query("SELECT * FROM sex_records ORDER BY startTime DESC")
    suspend fun listAllWithPartners(): List<SexRecordWithPartners>

    @Query("SELECT * FROM sex_records WHERE endTime IS NULL LIMIT 1")
    suspend fun ongoing(): SexRecord?

    @Query("SELECT * FROM sex_records WHERE endTime IS NULL LIMIT 1")
    fun observeOngoing(): Flow<SexRecord?>

    @Query("SELECT COUNT(*) FROM sex_records WHERE startTime >= :from AND startTime < :until")
    suspend fun countInRange(from: Long, until: Long): Int

    @Query("SELECT COUNT(*) FROM sex_records WHERE startTime >= :from AND startTime < :until")
    fun observeCountInRange(from: Long, until: Long): Flow<Int>

    @Query("SELECT * FROM sex_records WHERE id = :id")
    suspend fun byId(id: Long): SexRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: SexRecord): Long

    @Update
    suspend fun update(record: SexRecord)

    @Query("UPDATE sex_records SET endTime = :endTime, updatedAt = :updatedAt WHERE id = :id AND endTime IS NULL")
    suspend fun finishRecord(id: Long, endTime: Long, updatedAt: Long): Int

    @Query("DELETE FROM sex_records WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM sex_records")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLink(ref: RecordPartnerCrossRef): Long

    @Query("DELETE FROM record_partner_cross_ref WHERE recordId = :recordId")
    suspend fun deleteLinksFor(recordId: Long)

    @Query("SELECT COUNT(*) FROM sex_records")
    suspend fun count(): Int

    @Query("SELECT AVG(endTime - startTime) FROM sex_records WHERE endTime IS NOT NULL AND startTime >= :from")
    suspend fun avgDurationSince(from: Long): Double?

    // ---- 统计（口径参数化在 Repository/UseCase 层做，DAO 提供原始聚合） ----

    /** 按天聚合场次（本地时区由调用方传入日的起止，逐日查询太慢；这里直接拉原始行） */
    @Query("SELECT startTime, participantCount FROM sex_records WHERE startTime >= :from AND endTime IS NOT NULL OR id IN (SELECT id FROM sex_records WHERE endTime IS NULL)")
    suspend fun rawTimesForHeatmap(from: Long): List<TimeCountRow>

    /** 组合统计：同场次的伴侣集合 */
    @Query(
        """
        SELECT c.recordId, GROUP_CONCAT(c.partnerId, ',') AS partnerIds, COUNT(*) AS size
        FROM record_partner_cross_ref c
        GROUP BY c.recordId
        HAVING COUNT(*) >= 1
        """
    )
    suspend fun allRecordCombinations(): List<CombinationRow>

    /** 精确匹配组合：包含且仅包含给定参与者的记录 id */
    @Query(
        """
        SELECT recordId FROM record_partner_cross_ref
        WHERE recordId IN (
            SELECT recordId FROM record_partner_cross_ref
            WHERE partnerId IN (:partnerIds)
            GROUP BY recordId
            HAVING COUNT(DISTINCT partnerId) = :count
        )
        GROUP BY recordId
        HAVING COUNT(DISTINCT partnerId) = :count
        """
    )
    suspend fun recordsWithExactPartners(partnerIds: List<Long>, count: Int): List<Long>

    /** 包含某人（含单人场）的记录 */
    @Query("SELECT DISTINCT recordId FROM record_partner_cross_ref WHERE partnerId = :partnerId")
    suspend fun recordsContaining(partnerId: Long): List<Long>

    /** 每位伴侣的参与人次统计 */
    @Query(
        """
        SELECT p.*, COUNT(c.recordId) AS sessionCount,
               IFNULL(SUM(r.endTime - r.startTime), 0) AS totalDurationSeconds,
               MAX(r.startTime) AS lastTime
        FROM partners p
        LEFT JOIN record_partner_cross_ref c ON c.partnerId = p.id
        LEFT JOIN sex_records r ON r.id = c.recordId
        WHERE p.archived = 0
        GROUP BY p.id
        ORDER BY sessionCount DESC
        """
    )
    suspend fun partnerStats(): List<PartnerWithStats>

    /** 参与人次分布（含已归档伴侣的历史数据） */
    @Query(
        """
        SELECT c.partnerId AS partnerId, COUNT(*) AS personTimes
        FROM record_partner_cross_ref c
        GROUP BY c.partnerId
        ORDER BY personTimes DESC
        """
    )
    suspend fun partnerPersonTimes(): List<PartnerPersonTimeRow>

    @Query("SELECT protection, COUNT(*) AS cnt FROM sex_records GROUP BY protection")
    suspend fun protectionCounts(): List<ProtectionCountRow>

    @Query(
        """
        SELECT CASE
            WHEN participantCount <= 1 THEN 0
            WHEN participantCount = 2 THEN 1
            ELSE 2 END AS bucket, COUNT(*) AS cnt
        FROM sex_records GROUP BY bucket
        """
    )
    suspend fun multiPartnerBuckets(): List<BucketCountRow>
}

data class TimeCountRow(val startTime: Long, val participantCount: Int)
data class CombinationRow(val recordId: Long, val partnerIds: String, val size: Int)
data class PartnerPersonTimeRow(val partnerId: Long, val personTimes: Int)
data class ProtectionCountRow(val protection: Protection, val cnt: Int)
data class BucketCountRow(val bucket: Int, val cnt: Int)

@Dao
interface ProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun observe(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun get(): UserProfile?

    @Upsert
    suspend fun upsert(profile: UserProfile)

    @Query("UPDATE user_profile SET pinHash = :hash, pinSalt = :salt WHERE id = 1")
    suspend fun setPin(hash: String, salt: String)
}

@Dao
interface CycleDao {
    @Query("SELECT * FROM cycle_logs ORDER BY startDate DESC")
    fun observeAll(): Flow<List<CycleLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: CycleLog): Long

    @Query("DELETE FROM cycle_logs WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface TemplateDao {
    @Query("SELECT * FROM scene_templates ORDER BY pinned DESC, sortOrder ASC, createdAt ASC")
    fun observeAll(): Flow<List<SceneTemplate>>

    @Query("SELECT * FROM scene_templates ORDER BY pinned DESC, sortOrder ASC, createdAt ASC")
    suspend fun listOrdered(): List<SceneTemplate>

    @Query("SELECT * FROM scene_templates WHERE id = :id")
    suspend fun byId(id: Long): SceneTemplate?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(template: SceneTemplate): Long

    @Update
    suspend fun update(template: SceneTemplate)

    @Query("DELETE FROM scene_templates WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM scene_templates")
    suspend fun count(): Int
}
