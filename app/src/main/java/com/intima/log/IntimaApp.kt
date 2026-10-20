package com.intima.log

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.intima.log.data.repo.ProfileRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class IntimaApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var profileRepository: ProfileRepository

    @Inject
    lateinit var partnerRepository: com.intima.log.data.repo.PartnerRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // 启动时按持久化的伪装设置同步桌面别名（防组件状态与设置漂移）
        appScope.launch {
            runCatching { profileRepository.applyLauncherAlias(profileRepository.profile.first().disguiseEnabled) }
        }
        // 占位「已删除」伴侣行自愈（防误恢复进正常列表）
        appScope.launch { runCatching { partnerRepository.startupFix() } }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
