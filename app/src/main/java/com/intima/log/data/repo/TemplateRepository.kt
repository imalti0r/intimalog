package com.intima.log.data.repo

import com.intima.log.data.db.SceneTemplate
import com.intima.log.data.db.TemplateDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TemplateRepository @Inject constructor(
    private val templateDao: TemplateDao,
) {
    fun observeAll(): Flow<List<SceneTemplate>> = templateDao.observeAll()

    suspend fun byId(id: Long): SceneTemplate? = templateDao.byId(id)

    suspend fun save(template: SceneTemplate): Long {
        // 上限 20 个：满员时挤掉最旧的未置顶模板
        if (template.id == 0L && templateDao.count() >= 20) {
            val oldest = templateDao.listOrdered().lastOrNull { !it.pinned } ?: return -1
            templateDao.delete(oldest.id)
        }
        return templateDao.insert(template)
    }

    suspend fun update(template: SceneTemplate) = templateDao.update(template)
    suspend fun delete(id: Long) = templateDao.delete(id)
}
