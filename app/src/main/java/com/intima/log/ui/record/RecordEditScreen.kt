package com.intima.log.ui.record

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text as M3Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.intima.log.data.db.MoodTags
import com.intima.log.ui.components.MiuixChip
import com.intima.log.ui.components.MiuixConfirmDialog
import com.intima.log.ui.components.MiuixDatePickerDialog
import com.intima.log.ui.components.MiuixTextField
import com.intima.log.ui.components.MiuixTimePickerDialog
import com.intima.log.ui.components.PartnerMultiSelect
import com.intima.log.ui.theme.LocalIntimaPalette
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val hm: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val ymd: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

/**
 * 记录编辑 / 新建页。
 * 功能①：发生日期可选（Miuix 滚轮日期选择器，状态存 ViewModel SavedStateHandle）。
 * 功能②：拆分组进入时载入整组区间，保存自动重拆；删除联动整组。
 */
@Composable
fun RecordEditScreen(
    onDone: () -> Unit,
    onManageTags: (String) -> Unit = {},
    viewModel: RecordEditViewModel = hiltViewModel(),
) {
    val state = viewModel.state
    val palette = LocalIntimaPalette.current
    var showStartPicker by rememberSaveable { mutableStateOf(false) }
    var showDurationPicker by rememberSaveable { mutableStateOf(false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var deleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(state.saved) { if (state.saved) onDone() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "返回",
                tint = palette.text,
                modifier = Modifier
                    .size(26.dp)
                    .clickable { onDone() },
            )
            M3Text(
                if (state.recordId == 0L) "新的记录" else "编辑记录",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = palette.text,
                modifier = Modifier.padding(start = 10.dp),
            )
            Spacer(Modifier.weight(1f))
            M3Text(
                state.startTimeText,
                fontSize = 13.sp,
                color = palette.textHint,
            )
        }

        // 拆分组提示（功能②）：合并编辑整组
        if (state.isSplitGroup) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.chipBg)
                    .padding(12.dp),
            ) {
                M3Text(
                    "该记录跨日拆分为多条，正在合并编辑整组；保存后按新时间重新拆分",
                    fontSize = 13.sp,
                    color = palette.textSub,
                    lineHeight = 19.sp,
                )
            }
        }

        // 进行中提示
        if (state.ongoing) {
            Column(modifier = Modifier.fillMaxWidth()) {
                M3Text("⏱ 计时进行中 · 已 ${state.elapsedText}", fontSize = 14.sp, color = palette.text)
                Button(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth(),
                    onClick = { viewModel.finishNow() },
                ) { M3Text("结束并保存", color = Color.White) }
            }
        } else {
            // 发生日期 + 开始时间 + 持续时间（功能①：日期选择器）
            SectionTitle("发生时间")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.card)
                        .clickable { showDatePicker = true }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    M3Text(
                        Instant.ofEpochMilli(state.startTime)
                            .atZone(ZoneId.systemDefault()).toLocalDate().format(ymd),
                        fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = palette.text,
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.card)
                        .clickable { showStartPicker = true }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    M3Text(
                        Instant.ofEpochMilli(state.startTime)
                            .atZone(ZoneId.systemDefault()).format(hm),
                        fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = palette.text,
                    )
                }
                M3Text("持续", fontSize = 14.sp, color = palette.textSub)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.card)
                        .clickable { showDurationPicker = true }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    M3Text(
                        viewModel.durationMinutes.let { if (it > 0) "${it} 分钟" else "点选" },
                        fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = palette.text,
                    )
                }
            }
        }

        // 参与者（➕ 跳伴侣管理）
        SectionTitleWithAdd("参与者" + if (!state.multiPartner) "（单人模式）" else "（可多选）") {
            onManageTags("partner")
        }
        Column(modifier = Modifier.fillMaxWidth()) {
            if (state.partners.isEmpty()) {
                M3Text(
                    "还没有伴侣档案，可跳过保存为「未指定」",
                    fontSize = 13.sp,
                    color = palette.textHint,
                )
            } else {
                PartnerMultiSelect(
                    partners = state.partners,
                    selectedIds = state.selectedIds,
                    multi = state.multiPartner,
                    onSelectionChange = viewModel::onSelectionChange,
                )
            }
            if (state.selectedIds.isNotEmpty()) {
                M3Text(
                    "已选 ${state.selectedIds.size} 人 · 首位为主参与者",
                    fontSize = 12.sp,
                    color = Color(0xFF03B585),
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        // 保护措施
        SectionTitleWithAdd("保护措施") { onManageTags("protection") }
        ChipFlow(
            items = state.knownCustomProtections,
            selected = state.customProtections,
            onToggle = { viewModel.toggleCustomProtection(it) },
            emptyText = "暂无保护措施，点右上 ➕ 添加",
        )

        // 体位
        SectionTitleWithAdd("体位") { onManageTags("position") }
        ChipFlow(
            items = state.knownPositions,
            selected = state.positions.toSet(),
            onToggle = { viewModel.togglePosition(it) },
            emptyText = "暂无体位，点右上 ➕ 添加",
        )

        // 玩具
        SectionTitleWithAdd("玩具") { onManageTags("toy") }
        ChipFlow(
            items = state.knownToys,
            selected = state.toys.toSet(),
            onToggle = { viewModel.toggleToy(it) },
            emptyText = "暂无玩具，点右上 ➕ 添加",
        )

        // 满意度
        SectionTitle("满意度")
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            (1..5).forEach { star ->
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { viewModel.setRating(star) }
                        .padding(4.dp),
                ) {
                    Icon(
                        Icons.Rounded.Star,
                        contentDescription = "$star 星",
                        tint = if (star <= state.rating) Color(0xFFFFB300) else palette.textHint.copy(alpha = 0.4f),
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
        }

        // 高潮次数（按参与者选择顺序 + 「我」，加减号记录；统计页汇总展示）
        ClimaxSection(
            selectedIds = state.selectedIds.toList(),
            partners = state.partners,
            climaxes = state.climaxes,
            onBump = viewModel::bumpClimax,
        )

        // 情绪标签（单选）
        SectionTitle("情绪")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MoodTags.ALL.forEach { tag ->
                MiuixChip(
                    text = tag,
                    selected = tag == state.moodTag,
                    onClick = { viewModel.setMood(tag) },
                )
            }
        }

        // 地点
        SectionTitleWithAdd("地点") { onManageTags("place") }
        ChipFlow(
            items = state.knownCustomPlaces,
            selected = state.customPlaces,
            onToggle = { viewModel.toggleCustomPlace(it) },
            emptyText = "暂无地点，点右上 ➕ 添加",
        )

        // 备注
        MiuixTextField(
            value = state.note,
            onValueChange = viewModel::setNote,
            label = "备注（可选）",
        )

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            onClick = viewModel::save,
            colors = top.yukonga.miuix.kmp.basic.ButtonDefaults.buttonColors(
                color = Color(0xFFE85A8A),
            ),
        ) { M3Text("保存", color = Color.White, fontSize = 16.sp) }

        if (state.recordId != 0L) {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { deleteConfirm = true },
            ) { M3Text("删除记录", color = Color(0xFFE94634)) }
        }
        Spacer(Modifier.height(40.dp))
    }

    // ---- 弹窗区（Miuix 风格）----

    val dateShow = remember { mutableStateOf(false) }
    LaunchedEffect(showDatePicker) { dateShow.value = showDatePicker }
    LaunchedEffect(dateShow.value) { showDatePicker = dateShow.value }
    MiuixDatePickerDialog(
        show = dateShow,
        initialDate = Instant.ofEpochMilli(state.startTime).atZone(ZoneId.systemDefault()).toLocalDate(),
        onConfirm = viewModel::setStartDate,
    )

    val timeShow = remember { mutableStateOf(false) }
    LaunchedEffect(showStartPicker) { timeShow.value = showStartPicker }
    LaunchedEffect(timeShow.value) { showStartPicker = timeShow.value }
    MiuixTimePickerDialog(
        show = timeShow,
        initialHour = Instant.ofEpochMilli(state.startTime).atZone(ZoneId.systemDefault()).hour,
        initialMinute = Instant.ofEpochMilli(state.startTime).atZone(ZoneId.systemDefault()).minute,
        onConfirm = viewModel::setStart,
    )

    if (showDurationPicker) {
        val durations = listOf(5, 10, 15, 20, 30, 45, 60, 90, 120)
        var customText by remember { mutableStateOf("") }
        androidx.compose.ui.window.Dialog(onDismissRequest = { showDurationPicker = false }) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .background(palette.card, RoundedCornerShape(22.dp))
                    .padding(20.dp),
            ) {
                M3Text("持续时间", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = palette.text)
                Spacer(Modifier.height(12.dp))
                durations.chunked(3).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        rowItems.forEach { min ->
                            val sel = viewModel.durationMinutes == min
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (sel) MiuixTheme.colorScheme.primary else palette.chipBg)
                                    .clickable {
                                        viewModel.setDurationMinutes(min)
                                        showDurationPicker = false
                                    }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                M3Text(
                                    "$min 分",
                                    fontSize = 14.sp,
                                    color = if (sel) Color.White else palette.text,
                                )
                            }
                        }
                        repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.height(10.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MiuixTextField(
                        value = customText,
                        onValueChange = { customText = it.filter { c -> c.isDigit() }.take(4) },
                        label = "自定义分钟",
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MiuixTheme.colorScheme.primary)
                            .clickable(enabled = customText.toIntOrNull() in 1..1440) {
                                viewModel.setDurationMinutes(customText.toInt())
                                showDurationPicker = false
                            }
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                    ) {
                        M3Text("确定", color = Color.White, fontSize = 14.sp)
                    }
                }
            }
        }
    }

    // 删除确认（拆分组时联动删除整组）
    val delShow = remember { mutableStateOf(false) }
    LaunchedEffect(deleteConfirm) { delShow.value = deleteConfirm }
    LaunchedEffect(delShow.value) { deleteConfirm = delShow.value }
    MiuixConfirmDialog(
        show = delShow,
        title = "删除记录？",
        body = if (state.isSplitGroup) "该记录属于跨日拆分组，删除将移除整组所有分段。"
        else "删除后不可恢复。",
        confirmText = "删除",
        danger = true,
        onConfirm = viewModel::delete,
    )
}

@Composable
private fun SectionTitle(text: String) {
    M3Text(
        text,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color = LocalIntimaPalette.current.textSub,
    )
}

/** 高潮次数：每人一行（「我」+ 参与者按选择顺序），+/− 按钮记录 */
@Composable
private fun ClimaxSection(
    selectedIds: List<Long>,
    partners: List<com.intima.log.data.db.Partner>,
    climaxes: Map<String, Int>,
    onBump: (String, Int) -> Unit,
) {
    val palette = LocalIntimaPalette.current
    val partnerById = partners.associateBy { it.id }
    // 展示顺序：「我」→ 参与者按选择顺序
    val rows: List<Pair<String, String>> =   // key → 显示名
        listOf("me" to "我") + selectedIds.mapIndexed { idx, id ->
            idx.toString() to (partnerById[id]?.let { "${it.emoji} ${it.nickname}" } ?: "参与者${idx + 1}")
        }

    Column {
        SectionTitle("高潮次数")
        Spacer(Modifier.height(6.dp))
        if (selectedIds.isEmpty()) {
            M3Text(
                "未选择参与者 · 仍可记录「我」的次数",
                fontSize = 12.sp, color = palette.textHint,
            )
        }
        rows.forEach { (key, name) ->
            val count = climaxes[key] ?: 0
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                M3Text(name, fontSize = 14.sp, color = palette.text, modifier = Modifier.weight(1f))
                // −
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(palette.chipBg)
                        .clickable(enabled = count > 0) { onBump(key, -1) },
                    contentAlignment = Alignment.Center,
                ) {
                    M3Text("−", fontSize = 17.sp, color = if (count > 0) palette.text else palette.textHint)
                }
                M3Text(
                    "%02d".format(count.coerceAtMost(99)),
                    fontSize = 16.sp, fontWeight = FontWeight.Bold, color = palette.text,
                    modifier = Modifier.width(24.dp),   // 固定两字符宽，加减时控件不跳动
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                // ＋
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(palette.chipBg)
                        .clickable { onBump(key, +1) },
                    contentAlignment = Alignment.Center,
                ) {
                    M3Text("＋", fontSize = 15.sp, color = palette.text)
                }
            }
        }
    }
}

/** 标题 + 右侧 ➕ 自定义管理入口 */
@Composable
private fun SectionTitleWithAdd(text: String, onAdd: () -> Unit) {
    val palette = LocalIntimaPalette.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        M3Text(
            text,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = palette.textSub,
        )
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(palette.chipBg)
                .clickable(onClick = onAdd),
            contentAlignment = Alignment.Center,
        ) {
            M3Text("＋", fontSize = 15.sp, color = palette.text)
        }
    }
}

/** 流式 Chip 布局：一行尽量多排，放不下换行 */
@Composable
private fun ChipFlow(
    items: List<String>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    emptyText: String,
) {
    if (items.isEmpty() && emptyText.isNotEmpty()) {
        M3Text(emptyText, fontSize = 13.sp, color = LocalIntimaPalette.current.textHint)
        return
    }
    androidx.compose.foundation.layout.FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEach { id ->
            MiuixChip(
                text = id,
                selected = id in selected,
                onClick = { onToggle(id) },
            )
        }
    }
}
