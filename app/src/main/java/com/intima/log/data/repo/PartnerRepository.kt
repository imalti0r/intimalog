package com.intima.log.data.repo

import com.intima.log.data.db.Partner
import com.intima.log.data.db.PartnerDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PartnerRepository @Inject constructor(
    private val partnerDao: PartnerDao,
) {
    fun observeActive(): Flow<List<Partner>> = partnerDao.observeActive()
    fun observeAll(): Flow<List<Partner>> = partnerDao.observeAll()

    suspend fun byId(id: Long): Partner? = partnerDao.byId(id)
    suspend fun byIds(ids: List<Long>): List<Partner> = partnerDao.byIds(ids)
    suspend fun listActive(): List<Partner> = partnerDao.listActive()

    /** 新增/编辑。被历史记录引用的伴侣只能归档，返回 false 表示不允许物理删除。 */
    suspend fun save(partner: Partner): Long = if (partner.id == 0L) {
        partnerDao.insert(partner)
    } else {
        partnerDao.update(partner)
        partner.id
    }

    suspend fun archive(id: Long) = partnerDao.archive(id)
    suspend fun unarchive(id: Long) = partnerDao.unarchive(id)
    suspend fun hide(id: Long) = partnerDao.hide(id)
    suspend fun unhide(id: Long) = partnerDao.unhide(id)

    /** 物理删除仅在无任何记录引用时允许 */
    suspend fun tryDelete(id: Long): Boolean {
        val used = partnerDao.usageCount(id) > 0
        if (used) return false
        partnerDao.archive(id) // 无引用时直接归档（等效移除出列表）
        return true
    }

    /**
     * 彻底删除：物理删除伴侣行。
     * 历史记录的引用改为指向特殊占位行（id=-1，昵称「已删除」，archived 永久隔离），
     * 记录的参与者列表与 participantCount 保持正确，统计不受影响。
     */
    suspend fun purge(id: Long) {
        partnerDao.ensureDeletedPlaceholder()
        partnerDao.repointRefsToPlaceholder(id)
        partnerDao.deleteById(id)
    }

    /** 占位伴侣行（「已删除」） */
    suspend fun placeholder(): Partner = partnerDao.byId(DELETED_PLACEHOLDER_ID)
        ?: Partner(id = DELETED_PLACEHOLDER_ID, nickname = "已删除", archived = true).also {
            partnerDao.insert(it)
        }

    /** 启动自愈：确保占位行始终处于隔离态 */
    suspend fun startupFix() = partnerDao.quarantinePlaceholder()

    companion object {
        const val DELETED_PLACEHOLDER_ID = -1L
    }
}
