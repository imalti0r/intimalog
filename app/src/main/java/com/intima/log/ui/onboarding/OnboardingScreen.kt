package com.intima.log.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text as M3Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intima.log.data.db.Gender
import com.intima.log.data.repo.ProfileRepository
import com.intima.log.ui.components.PinPad
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.intima.log.ui.theme.LocalIntimaPalette

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
) : ViewModel() {
    fun finish(gender: Gender, pin: String?) {
        viewModelScope.launch {
            profileRepository.setGender(gender)
            pin?.let { profileRepository.setPin(it) }
            profileRepository.setOnboardingDone(true)
        }
    }
}

/** 首次引导：性别 → PIN → 说明 */
@Composable
fun OnboardingScreen(
    onDone: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    var step by remember { mutableStateOf(0) }
    var gender by remember { mutableStateOf(Gender.UNDISCLOSED) }
    var pin by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalIntimaPalette.current.bg)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (step) {
            0 -> {
                Spacer(Modifier.padding(top = 60.dp))
                M3Text("欢迎使用", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                M3Text(
                    "所有数据仅保存在本机加密数据库\n不联网、不上传",
                    fontSize = 14.sp, color = LocalIntimaPalette.current.textHint, lineHeight = 22.sp,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(Gender.FEMALE to "女", Gender.MALE to "男", Gender.OTHER to "其他", Gender.UNDISCLOSED to "不透露").forEach { (g, label) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (gender == g) top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.primary else LocalIntimaPalette.current.card,
                                )
                                .clickable { gender = g }
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                        ) {
                            M3Text(label, color = if (gender == g) Color.White else LocalIntimaPalette.current.text, fontSize = 15.sp)
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                PrimaryButton("下一步") { step = 1 }
            }
            1 -> {
                Spacer(Modifier.padding(top = 40.dp))
                M3Text("设置 PIN（可选，4-6 位）", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                M3Text(
                    "用于应用锁。跳过则不启用锁",
                    fontSize = 13.sp, color = LocalIntimaPalette.current.textHint,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
                M3Text(
                    text = buildString { repeat(pin.length) { append("●") } },
                    fontSize = 30.sp, letterSpacing = 8.sp,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
                PinPad(
                    onDigit = { if (pin.length < 6) pin += it },
                    onBackspace = { pin = pin.dropLast(1) },
                )
                Spacer(Modifier.weight(1f))
                PrimaryButton(if (pin.isBlank()) "跳过" else "完成") {
                    viewModel.finish(gender, pin.ifBlank { null })
                    onDone()
                }
            }
        }
    }
}

@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(999.dp))
            .background(top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.primary)
            .clickable(onClick = onClick)
            .padding(vertical = 15.dp),
        contentAlignment = Alignment.Center,
    ) {
        M3Text(text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}
