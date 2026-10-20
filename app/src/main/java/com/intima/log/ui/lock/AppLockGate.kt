package com.intima.log.ui.lock

import androidx.activity.compose.LocalActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.intima.log.data.repo.ProfileRepository
import com.intima.log.ui.components.PinPad
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.TextButton
import java.util.concurrent.Executor
import com.intima.log.ui.theme.LocalIntimaPalette

/**
 * 应用锁门控：前台切回时需 PIN / 生物识别。
 * 同时按 screenshotAllowed 控制 FLAG_SECURE。
 */
@Composable
fun AppLockGate(
    enabled: Boolean,
    profileRepository: ProfileRepository? = null,
    screenshotAllowed: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var unlocked by remember { mutableStateOf(!enabled) }
    var lastPauseAt by remember { mutableStateOf(0L) }

    // FLAG_SECURE：响应 screenshotAllowed 变化（默认禁止截屏）
    DisposableEffect(screenshotAllowed) {
        val window = (activity as? AppCompatActivity)?.window
        if (!screenshotAllowed) {
            window?.setFlags(
                android.view.WindowManager.LayoutParams.FLAG_SECURE,
                android.view.WindowManager.LayoutParams.FLAG_SECURE
            )
        } else {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose { }
    }

    // 切后台后回来重新上锁
    DisposableEffect(lifecycleOwner, enabled) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> lastPauseAt = System.currentTimeMillis()
                Lifecycle.Event.ON_RESUME -> {
                    if (enabled && lastPauseAt > 0 &&
                        System.currentTimeMillis() - lastPauseAt > 3_000
                    ) unlocked = false
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(enabled) { if (!enabled) unlocked = true }

    if (unlocked) {
        content()
    } else {
        LockScreen(
            onUnlocked = { unlocked = true },
            onPinCheck = { pin ->
                context.getSharedPreferences("intima_pin", android.content.Context.MODE_PRIVATE)
                    .let { prefs ->
                        val hash = prefs.getString("pin_hash", null) ?: return@let false
                        val salt = prefs.getString("pin_salt", null) ?: return@let false
                        com.intima.log.data.security.SecurityManager.verifyPin(pin, hash, salt)
                    }
            }
        )
    }
}

@Composable
private fun LockScreen(onUnlocked: () -> Unit, onPinCheck: (String) -> Boolean) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalIntimaPalette.current.bg)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("输入 PIN 解锁", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Box(
            modifier = Modifier
                .padding(vertical = 24.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = buildString { repeat(pin.length) { append("●") } },
                fontSize = 32.sp,
                letterSpacing = 8.sp,
                color = if (error) Color(0xFFE94634) else LocalIntimaPalette.current.text
            )
        }
        PinPad(
            onDigit = { d ->
                if (pin.length < 6) pin += d
                error = false
            },
            onBackspace = { pin = pin.dropLast(1) },
        )
        Button(
            modifier = Modifier
                .padding(top = 24.dp)
                .fillMaxWidth(),
            onClick = {
                if (onPinCheck(pin)) {
                    onUnlocked()
                } else {
                    error = true
                    pin = ""
                }
            }
        ) { Text("解锁", color = Color.White) }

        // 生物识别（可用时）
        val fa = LocalActivity.current as? FragmentActivity
        if (fa != null) {
            val canBiometric = remember {
                BiometricManager.from(fa).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
                    BiometricManager.BIOMETRIC_SUCCESS
            }
            if (canBiometric) {
                TextButton(
                    text = "使用生物识别",
                    modifier = Modifier.padding(top = 12.dp),
                    onClick = { showBiometric(fa, onUnlocked) }
                )
            }
        }
    }
    // 满位自动校验
    LaunchedEffect(pin) {
        if (pin.length in 4..6 && pin.length == 6) {
            if (onPinCheck(pin)) onUnlocked() else { error = true; pin = "" }
        }
    }
}

private fun showBiometric(activity: FragmentActivity, onUnlocked: () -> Unit) {
    val executor: Executor = ContextCompat.getMainExecutor(activity)
    val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            onUnlocked()
        }
    })
    prompt.authenticate(
        BiometricPrompt.PromptInfo.Builder()
            .setTitle("解锁")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()
    )
}
