package com.intima.log.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.intima.log.data.repo.ProfileRepository
import com.intima.log.ui.theme.ThemeMode
import com.intima.log.ui.lock.AppLockGate
import com.intima.log.ui.onboarding.OnboardingScreen
import com.intima.log.ui.theme.IntimaTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 主入口。应用锁门控：lockEnabled 时先验证 PIN/生物识别，再放行内容。
 * FLAG_SECURE 由 AppLockGate 内按 screenshotAllowed 动态设置。
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var profileRepository: ProfileRepository

    private var ready by mutableStateOf(false)
    private var lockEnabled by mutableStateOf(false)
    private var onboardingDone by mutableStateOf(true)
    private var screenshotAllowed by mutableStateOf(false)
    private var themeMode by mutableStateOf(ThemeMode.SYSTEM)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleScope.launch {
            val profile = profileRepository.profile.first()
            lockEnabled = profile.lockEnabled && profileRepository.hasPin()
            onboardingDone = profile.onboardingDone
            screenshotAllowed = profile.screenshotAllowed
            ready = true
            // 夜间模式设置变化即时生效（collectForever 持续订阅）
            profileRepository.themeModeFlow().collect { themeMode = it }
        }
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
}
