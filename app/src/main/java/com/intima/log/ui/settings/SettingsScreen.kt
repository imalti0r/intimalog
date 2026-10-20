package com.intima.log.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text as M3Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.intima.log.data.repo.PartnerRepository
import com.intima.log.data.repo.ProfileRepository
import com.intima.log.data.repo.RecordRepository
import com.intima.log.domain.usecase.StatsBasisCounter
import com.intima.log.ui.theme.LocalIntimaPalette
import com.intima.log.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.intima.log.ui.theme.LocalIntimaPalette

@HiltViewModel
class SettingsViewModel @Inject constructor(
    val profileRepository: ProfileRepository,
    private val recordRepository: RecordRepository,
    private val partnerRepository: PartnerRepository,
) : ViewModel() {

    fun setLock(enabled: Boolean, onNeedPin: () -> Unit) {
        viewModelScope.launch {
            if (enabled && !profileRepository.hasPin()) onNeedPin()
            else profileRepository.setLockEnabled(enabled)
        }
    }

    suspend fun setPin(pin: String) = profileRepository.setPin(pin)

    /** 设置 PIN 并开启应用锁 */
    fun setPinAndEnable(pin: String) {
        viewModelScope.launch {
            profileRepository.setPin(pin)
            profileRepository.setLockEnabled(true)
        }
    }

    fun setMultiPartner(enabled: Boolean) = viewModelScope.launch { profileRepository.setMultiPartnerEnabled(enabled) }
    fun setDisguise(enabled: Boolean) = viewModelScope.launch {
        profileRepository.setDisguise(enabled)
        profileRepository.applyLauncherAlias(enabled)
    }
    fun setStrongDisguise(enabled: Boolean) = viewModelScope.launch { profileRepository.setStrongDisguise(enabled) }
    fun setScreenshotAllowed(allowed: Boolean) = viewModelScope.launch { profileRepository.setScreenshotAllowed(allowed) }
    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { profileRepository.setThemeMode(mode) }

    /** 一键清除全部数据（长按标题 5 次入口） */
    fun wipeAll() {
        viewModelScope.launch {
            recordRepository.deleteAll()
        }
    }
}

/** 设置页（含伪装、应用锁、导出入口由 BackupScreen 承接） */
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    var multi by remember { mutableStateOf(true) }
    var disguise by remember { mutableStateOf(false) }
    var strong by remember { mutableStateOf(false) }
    var shot by remember { mutableStateOf(false) }
    var lock by remember { mutableStateOf(false) }
    var themeMode by remember { mutableStateOf(ThemeMode.SYSTEM) }
    var showPinSetup by remember { mutableStateOf(false) }
    var wipeConfirm by remember { mutableStateOf(false) }
    var titleTaps by remember { mutableStateOf(0) }
    var lastTapAt by remember { mutableStateOf(0L) }

    LaunchedEffect(Unit) {
        val p = viewModel.profileRepository.profile.first()
        multi = p.enableMultiPartner
        disguise = p.disguiseEnabled
        strong = p.strongDisguise
        shot = p.screenshotAllowed
        lock = p.lockEnabled
    }
    LaunchedEffect(Unit) {
        viewModel.profileRepository.themeModeFlow().collect { themeMode = it }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalIntimaPalette.current.bg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // 连点 5 次标题 → 一键清除入口
        M3Text(
            "设置",
            fontSize = 28.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable {
                val now = System.currentTimeMillis()
                if (now - lastTapAt > 3_000) titleTaps = 0
                lastTapAt = now
                if (++titleTaps >= 5) { titleTaps = 0; wipeConfirm = true }
            },
        )

        SettingSwitch("应用锁（PIN）", lock) { enabled ->
            if (enabled && !viewModel.profileRepository.hasPin()) {
                showPinSetup = true
            } else {
                lock = enabled
                viewModel.setLock(enabled) {}
            }
        }
        SettingSwitch("多人模式", multi) {
            multi = it; viewModel.setMultiPartner(it)
        }
        SettingSwitch("伪装模式（应用名「日常」）", disguise) {
            disguise = it; viewModel.setDisguise(it)
        }
        SettingSwitch("强伪装（多人场显示为「活动」）", strong) {
            strong = it; viewModel.setStrongDisguise(it)
        }
        SettingSwitch("允许截屏（默认禁止）", shot) {
            shot = it; viewModel.setScreenshotAllowed(it)
        }

        M3Text(
            "导出与备份在「备份」页 · 数据库已加密存储 · 本应用不申请网络权限，数据绝不离开设备",
            fontSize = 12.sp, color = LocalIntimaPalette.current.textHint, lineHeight = 18.sp,
            modifier = Modifier.padding(top = 12.dp),
        )
        M3Text(
            "版本 ${com.intima.log.BuildConfig.VERSION_NAME} (${com.intima.log.BuildConfig.VERSION_CODE})",
            fontSize = 12.sp, color = LocalIntimaPalette.current.textHint,
            modifier = Modifier.padding(top = 4.dp),
        )
    }

    if (showPinSetup) {
        PinSetupDialog(
            onDismiss = { showPinSetup = false },
            onSet = { pin ->
                viewModel.setPinAndEnable(pin)
                showPinSetup = false
                lock = true
            },
        )
    }
}

// 占位：见 PinSetupDialog 实现
@Composable
private fun PinSetupDialog(onDismiss: () -> Unit, onSet: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(LocalIntimaPalette.current.card, RoundedCornerShape(24.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            M3Text("设置 PIN（4-6 位数字）", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            androidx.compose.material3.OutlinedTextField(
                value = pin, onValueChange = { pin = it.filter(Char::isDigit).take(6) },
                label = { M3Text("PIN") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
            )
            androidx.compose.material3.OutlinedTextField(
                value = confirm, onValueChange = { confirm = it.filter(Char::isDigit).take(6) },
                label = { M3Text("确认 PIN") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
            )
            val valid = pin.length in 4..6 && pin == confirm
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                M3Text("取消", color = LocalIntimaPalette.current.textHint, modifier = Modifier.clickable { onDismiss() }.padding(8.dp))
                M3Text(
                    "确定",
                    color = if (valid) Color(0xFF3482FF) else LocalIntimaPalette.current.textHint,
                    modifier = Modifier.clickable(enabled = valid) { onSet(pin) }.padding(8.dp),
                )
            }
        }
    }
}

@Composable
private fun SettingSwitch(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(LocalIntimaPalette.current.card, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        M3Text(title, fontSize = 15.sp, modifier = Modifier.weight(1f))
        top.yukonga.miuix.kmp.basic.Switch(checked = checked, onCheckedChange = onChange)
    }
}
