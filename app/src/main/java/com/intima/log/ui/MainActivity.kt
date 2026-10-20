package com.intima.log.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.intima.log.data.repo.ProfileRepository
import com.intima.log.ui.theme.ThemeMode
import com.intima.log.ui.lock.AppLockGate
import com.intima.log.ui.onboarding.OnboardingScreen
import com.intima.log.ui.theme.IntimaTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 主入口。应用锁门控：lockEnabled 时先验证 PIN/生物识别，再放行内容。
 * FLAG_SECURE 由 AppLockGate 内按 screenshotAllowed 动态设置。
 *
 * 主题/锁状态全部响应式 collect：设置页改动即时生效，无需重建 Activity。
 * 三态主题（跟随系统/浅色/深色）由 IntimaTheme 解析；系统栏图标反色见 Theme.kt。
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var profileRepository: ProfileRepository

    private var ready by mutableStateOf(false)
    private var lockEnabled by mutableStateOf(false)
    private var onboardingDone by mutableStateOf(true)
    private var screenshotAllowed by mutableStateOf(false)
    private var themeMode by mutableStateOf(ThemeMode.SYSTEM)

    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // 状态就绪前保持系统启动画面，避免空帧闪屏
        splash.setKeepOnScreenCondition { !ready }
        lifecycleScope.launch {
            launch {
                // 锁与截屏策略：设置页切换即时生效
                profileRepository.profile.collectLatest { p ->
                    lockEnabled = p.lockEnabled && profileRepository.hasPin()
                    onboardingDone = p.onboardingDone
                    screenshotAllowed = p.screenshotAllowed
                }
            }
            launch {
                // 主题三态即时生效
                profileRepository.themeModeFlow().collect { themeMode = it }
            }
            ready = true
        }
        requestNotificationPermissionIfNeeded()
        setContent {
            IntimaTheme(themeMode = themeMode) {
                if (ready) {
                    if (!onboardingDone) {
                        OnboardingScreen(onDone = { onboardingDone = true })
                    } else {
                        AppLockGate(enabled = lockEnabled, screenshotAllowed = screenshotAllowed) {
                            IntimaNavHost()
                        }
                    }
                }
            }
        }
    }

    /** Android 13+ 首次启动请求通知权限（计时前台服务需要可见通知） */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            runCatching { notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS) }
        }
    }
}
