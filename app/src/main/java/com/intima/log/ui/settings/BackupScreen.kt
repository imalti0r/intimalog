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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.intima.log.data.backup.BackupManager
import com.intima.log.data.repo.ProfileRepository
import com.intima.log.data.sync.WebDavClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.intima.log.ui.theme.LocalIntimaPalette
@HiltViewModel
class BackupViewModel @Inject constructor(
    val backupManager: BackupManager,
    val profileRepository: ProfileRepository,
) : ViewModel() {
    fun exportCsv(contentResolver: android.content.ContentResolver, uri: android.net.Uri, anonymize: Boolean, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val csv = backupManager.exportCsv(anonymize)
                contentResolver.openOutputStream(uri)?.use { it.write(csv.toByteArray(Charsets.UTF_8)) }
                onDone(true)
            } catch (e: Exception) {
                onDone(false)
            }
        }
    }

    fun import(contentResolver: android.content.ContentResolver, uri: android.net.Uri, pin: String, onDone: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: return@launch onDone("无法读取文件")
                val (records, placeholders) = backupManager.importEncrypted(bytes, pin)
                onDone("导入 $records 条记录，创建 $placeholders 个占位伴侣")
            } catch (e: Exception) {
                onDone("导入失败：${e.message}")
            }
        }
    }

    /** 导出加密 .intima 到 SAF Uri */
    fun launchExport(contentResolver: android.content.ContentResolver, uri: android.net.Uri, pin: String, onDone: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val bytes = backupManager.exportEncrypted(pin)
                contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                onDone("导出成功")
            } catch (e: Exception) {
                onDone("导出失败：${e.message}")
            }
        }
    }

    // ---- WebDAV 同步 ----

    fun saveWebDavConfig(url: String, user: String, password: String, dir: String, onDone: (String) -> Unit) {
        viewModelScope.launch {
            try {
                profileRepository.setWebDavConfig(url, user, password, dir)
                onDone("WebDAV 配置已保存（密码经 Keystore 加密存储）")
            } catch (e: Exception) {
                onDone("保存失败：${e.message}")
            }
        }
    }

    /** 上传加密备份到 WebDAV（文件名 intima-backup.intima） */
    fun webDavUpload(pin: String, onDone: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val cfg = profileRepository.webDavConfigFlow().first()
                    ?: return@launch onDone("请先填写 WebDAV 配置")
                val bytes = backupManager.exportEncrypted(pin)
                val result = WebDavClient(cfg).upload("intima-backup.intima", bytes)
                onDone(
                    when (result) {
                        is WebDavClient.Result.Ok -> "同步成功（${bytes.size / 1024} KB）"
                        is WebDavClient.Result.Fail -> result.message
                        is WebDavClient.Result.Error -> result.message
                    }
                )
            } catch (e: Exception) {
                onDone("同步失败：${e.message}")
            }
        }
    }

    /** 从 WebDAV 下载并恢复（覆盖导入） */
    fun webDavRestore(pin: String, onDone: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val cfg = profileRepository.webDavConfigFlow().first()
                    ?: return@launch onDone("请先填写 WebDAV 配置")
                val client = WebDavClient(cfg)
                val dl = client.download("intima-backup.intima")
                val bytes = when (dl) {
                    is WebDavClient.Result.Ok -> dl.bytes
                        ?: return@launch onDone("下载内容为空")
                    is WebDavClient.Result.Fail -> return@launch onDone(dl.message)
                    is WebDavClient.Result.Error -> return@launch onDone(dl.message)
                }
                val (records, placeholders) = backupManager.importEncrypted(bytes, pin)
                onDone("恢复完成：$records 条记录，$placeholders 个占位伴侣")
            } catch (e: Exception) {
                onDone("恢复失败：${e.message}")
            }
        }
    }

    /** 查看远端备份信息 */
    fun webDavStat(onDone: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val cfg = profileRepository.webDavConfigFlow().first()
                    ?: return@launch onDone("未配置")
                when (val r = WebDavClient(cfg).stat("intima-backup.intima")) {
                    is WebDavClient.Result.Ok -> onDone(r.message.ifBlank { "存在" })
                    is WebDavClient.Result.Fail -> if (r.code == 404) onDone("远端暂无备份") else onDone(r.message)
                    is WebDavClient.Result.Error -> onDone(r.message)
                }
            } catch (e: Exception) {
                onDone("查询失败：${e.message}")
            }
        }
    }
}

/** 备份页（从设置页相关文案进入，此处作为独立入口由导航调用） */
@Composable
fun BackupScreen(viewModel: BackupViewModel = hiltViewModel()) {
    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var anonymize by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf("") }
    var davUrl by remember { mutableStateOf("") }
    var davUser by remember { mutableStateOf("") }
    var davPass by remember { mutableStateOf("") }
    var davDir by remember { mutableStateOf("intimalog") }
    var davMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.profileRepository.webDavConfigFlow().first()?.let {
            davUrl = it.url; davUser = it.user; davDir = it.dir
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null && pin.length >= 4) {
            viewModel.launchExport(context.contentResolver, uri, pin) { message = it }
        }
    }
    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            viewModel.exportCsv(context.contentResolver, uri, anonymize) { ok ->
                message = if (ok) "CSV 导出成功" else "CSV 导出失败"
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && pin.length >= 4) {
            viewModel.import(context.contentResolver, uri, pin) { message = it }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalIntimaPalette.current.bg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        M3Text("备份与导出", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        com.intima.log.ui.components.MiuixTextField(
            value = pin,
            onValueChange = { pin = it.filter(Char::isDigit).take(6) },
            label = "导出/导入密码（4-6 位）",
        )
        ActionRow("导出加密 .intima", enabled = pin.length >= 4) { exportLauncher.launch("intima_backup.intima") }
        Row(verticalAlignment = Alignment.CenterVertically) {
            M3Text("CSV 脱敏（P1/P2 代替伴侣名）", fontSize = 14.sp, modifier = Modifier.weight(1f))
            top.yukonga.miuix.kmp.basic.Switch(checked = anonymize, onCheckedChange = { anonymize = it })
        }
        ActionRow("导出 CSV（需二次确认输入密码）", enabled = pin.length >= 4) { csvLauncher.launch("intima_export.csv") }
        ActionRow("从 .intima 导入", enabled = pin.length >= 4) { importLauncher.launch(arrayOf("*/*")) }
        if (message.isNotEmpty()) {
            M3Text(message, fontSize = 13.sp, color = Color(0xFF03B585))
        }
        M3Text(
            "导出文件为 AES-256-GCM 加密，密码丢失无法恢复。导入时缺失的伴侣将创建「导入-未知N」占位，可在伴侣页手动改名绑定。",
            fontSize = 12.sp, color = LocalIntimaPalette.current.textHint, lineHeight = 18.sp,
        )

        // ---- WebDAV 同步 ----
        M3Text("WebDAV 同步", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        com.intima.log.ui.components.MiuixTextField(
            value = davUrl,
            onValueChange = { davUrl = it },
            label = "服务器地址（https://dav.example.com/dav）",
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            com.intima.log.ui.components.MiuixTextField(
                value = davUser,
                onValueChange = { davUser = it },
                label = "账号",
                modifier = Modifier.weight(1f),
            )
            com.intima.log.ui.components.MiuixTextField(
                value = davPass,
                onValueChange = { davPass = it },
                label = "密码",
                modifier = Modifier.weight(1f),
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
            )
        }
        com.intima.log.ui.components.MiuixTextField(
            value = davDir,
            onValueChange = { davDir = it },
            label = "远端目录（默认 intimalog）",
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(LocalIntimaPalette.current.card)
                    .clickable {
                        if (davUrl.isBlank() || davUser.isBlank() || davPass.isBlank()) {
                            davMessage = "地址/账号/密码均必填"
                        } else {
                            viewModel.saveWebDavConfig(davUrl, davUser, davPass, davDir) { davMessage = it }
                        }
                    }
                    .padding(14.dp),
            ) { M3Text("保存配置", fontSize = 14.sp, color = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.primary) }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(LocalIntimaPalette.current.card)
                    .clickable { viewModel.webDavStat { davMessage = "远端：$it" } }
                    .padding(14.dp),
            ) { M3Text("查看远端备份", fontSize = 14.sp, color = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.primary) }
        }
        ActionRow("上传备份到 WebDAV", enabled = pin.length >= 4) {
            viewModel.webDavUpload(pin) { davMessage = it }
        }
        ActionRow("从 WebDAV 恢复（覆盖导入）", enabled = pin.length >= 4) {
            viewModel.webDavRestore(pin) { davMessage = it }
        }
        if (davMessage.isNotEmpty()) {
            M3Text(davMessage, fontSize = 13.sp, color = Color(0xFF03B585), lineHeight = 18.sp)
        }
        M3Text(
            "备份文件与本地导出同样为 AES-256-GCM 加密后再上传；密码经 Android Keystore 加密保存在本机。仅在你主动点击同步时联网。",
            fontSize = 12.sp, color = LocalIntimaPalette.current.textHint, lineHeight = 18.sp,
        )
    }
}

@Composable
private fun ActionRow(text: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (enabled) LocalIntimaPalette.current.card else LocalIntimaPalette.current.chipBg, RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(16.dp),
    ) {
        M3Text(text, fontSize = 15.sp, color = if (enabled) LocalIntimaPalette.current.text else LocalIntimaPalette.current.textHint)
    }
}
