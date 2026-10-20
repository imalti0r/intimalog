package com.intima.log.ui.quickadd

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text as M3Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.intima.log.data.repo.PartnerRepository
import com.intima.log.data.repo.ProfileRepository
import com.intima.log.data.repo.RecordDraft
import com.intima.log.data.repo.RecordRepository
import com.intima.log.service.TimingService
import com.intima.log.ui.components.PartnerMultiSelect
import com.intima.log.ui.theme.IntimaTheme
import com.intima.log.ui.theme.ThemeMode
import com.intima.log.widget.IntimaWidget
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.intima.log.ui.theme.LocalIntimaPalette
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 半透明快速记录：从小组件「+」进入，保存即回桌面，不进主界面。
 * 功能④：跟随应用三态主题（DataStore 读取），系统栏图标随主题反色（Theme.kt 统一处理）。
 */
@AndroidEntryPoint
class QuickAddActivity : AppCompatActivity() {

    @Inject lateinit var recordRepository: RecordRepository
    @Inject lateinit var partnerRepository: PartnerRepository
    @Inject lateinit var profileRepository: ProfileRepository

    private var partners by mutableStateOf(listOf<com.intima.log.data.db.Partner>())
    private var selected by mutableStateOf(setOf<Long>())
    private var multi by mutableStateOf(true)
    private var themeMode by mutableStateOf(ThemeMode.SYSTEM)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            profileRepository.profile.collectLatest { p ->
                multi = p.enableMultiPartner
            }
            launch { partners = partnerRepository.listActive() }
            launch {
                profileRepository.themeModeFlow().collect { themeMode = it }
            }
        }
        setContent {
            IntimaTheme(themeMode = themeMode) {
                QuickAddContent(
                    partners = partners,
                    selected = selected,
                    multi = multi,
                    onToggle = { id ->
                        selected = if (id in selected) selected - id else selected + id
                    },
                    onSave = { finishNow() },
                    onDismiss = { finish() },
                )
            }
        }
    }

    private fun finishNow() {
        lifecycleScope.launch {
            val now = System.currentTimeMillis()
            recordRepository.save(
                RecordDraft(
                    partnerIds = selected.toList(),
                    startTime = now - 30 * 60_000,   // 默认 30 分钟前开始
                    endTime = now,
                )
            )
            // 计时前台服务一并接管（B5 修复：有进行中记录时保持常驻通知）
            androidx.core.content.ContextCompat.startForegroundService(
                this@QuickAddActivity,
                android.content.Intent(this@QuickAddActivity, TimingService::class.java),
            )
            IntimaWidget().updateAll(applicationContext)
            finish()
        }
    }
}

@Composable
private fun QuickAddContent(
    partners: List<com.intima.log.data.db.Partner>,
    selected: Set<Long>,
    multi: Boolean,
    onToggle: (Long) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalIntimaPalette.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x66000000)),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.card, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            M3Text("快速记录", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = palette.text)
            if (partners.isEmpty()) {
                M3Text("未选择参与者（未指定）", fontSize = 13.sp, color = palette.textHint)
            } else {
                PartnerMultiSelect(
                    partners = partners,
                    selectedIds = selected,
                    multi = multi,
                    onSelectionChange = { ids ->
                        // 单选回调适配
                        ids.filter { it !in selected }.forEach(onToggle)
                        selected.filter { it !in ids }.forEach(onToggle)
                    },
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(palette.chipBg, RoundedCornerShape(16.dp))
                        .clickable(onClick = onDismiss)
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    M3Text("取消", color = palette.text)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(MiuixTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                        .clickable(onClick = onSave)
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    M3Text("保存", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
