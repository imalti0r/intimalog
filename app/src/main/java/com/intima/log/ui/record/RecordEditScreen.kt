package com.intima.log.ui.record

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text as M3Text
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.intima.log.data.db.MoodTags
import com.intima.log.data.db.Partner
import com.intima.log.data.db.Protection
import com.intima.log.data.db.Place
import com.intima.log.ui.components.PartnerMultiSelect
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.intima.log.ui.theme.LocalIntimaPalette

private val hm: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** 保护措施中文名 */
fun Protection.label(): String = when (this) {
    Protection.NONE -> "无措施"
    Protection.CONDOM -> "安全套"
    Protection.PILL_SHORT -> "短效避孕药"
    Protection.PILL_LONG -> "长效避孕药"
    Protection.PILL_URGENT -> "紧急避孕药"
    Protection.OTHER -> "其他"
    Protection.NOT_RECORDED -> "未记录"
}

fun Place.label(): String = when (this) {
    Place.HOME -> "家"
    Place.HOTEL -> "酒店"
    Place.OTHER -> "其他"
    Place.NOT_RECORDED -> "未记录"
}

/** 记录编辑 / 新建页 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun RecordEditScreen(
    onDone: () -> Unit,
    onManageTags: () -> Unit = {},
    viewModel: RecordEditViewModel = hiltViewModel(),
) {
    val state = viewModel.state
    var showStartPicker by remember { mutableStateOf(false) }
    var showDurationPicker by remember { mutableStateOf(false) }

    LaunchedEffect(state.saved) { if (state.saved) onDone() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            M3Text(
                if (state.recordId == 0L) "新的记录" else "编辑记录",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.weight(1f))
            M3Text(
                state.startTimeText,
                fontSize = 13.sp,
                color = LocalIntimaPalette.current.textHint,
            )
        }

        // 进行中提示
        if (state.ongoing) {
            Column(modifier = Modifier.fillMaxWidth()) {
                M3Text("⏱ 计时进行中 · 已 ${state.elapsedText}", fontSize = 14.sp)
                Button(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth(),
                    onClick = { viewModel.finishNow() },
                ) { M3Text("结束并保存", color = Color.White) }
            }
        } else {
            // 开始时间 + 持续时间（补录 / 编辑时可调；计时中的记录不可改）
            SectionTitle("开始时间")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(LocalIntimaPalette.current.card)
                        .clickable { showStartPicker = true }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    M3Text(
                        java.time.Instant.ofEpochMilli(state.startTime).atZone(java.time.ZoneId.systemDefault())
                            .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")),
                        fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    )
                }
                M3Text("持续", fontSize = 14.sp, color = LocalIntimaPalette.current.textSub)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(LocalIntimaPalette.current.card)
                        .clickable { showDurationPicker = true }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    M3Text(
                        viewModel.durationMinutes.let { if (it > 0) "${it} 分钟" else "点选" },
                        fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        // 参与者
        SectionTitle("参与者" + if (!state.multiPartner) "（单人模式）" else "（可多选）")
        Column(modifier = Modifier.fillMaxWidth()) {
            if (state.partners.isEmpty()) {
                M3Text(
                    "还没有伴侣档案，可跳过保存为「未指定」",
                    fontSize = 13.sp,
                    color = LocalIntimaPalette.current.textHint,
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

        // 保护措施（多选）
        SectionTitle("保护措施（可多选）")
        ChipFlow(
            items = Protection.entries.map { it.name },
            selected = state.protections.map { it.name }.toSet(),
            labelOf = { n -> runCatching { Protection.valueOf(n) }.getOrNull()?.label() ?: n },
            onToggle = { n -> viewModel.toggleProtection(runCatching { Protection.valueOf(n) }.getOrNull() ?: return@ChipFlow) },
            emptyText = "",
        )

        // 体位（管理页维护的标签，多选；末尾入口跳管理页）
        SectionTitle("体位")
        ChipFlow(
            items = state.knownPositions,
            selected = state.positions.toSet(),
            onToggle = { viewModel.togglePosition(it) },
            emptyText = "还没有体位，去「我的 → 体位管理」添加",
            manageHint = "管理",
            onManageTags = { onManageTags() },
        )

        // 玩具（管理页维护的标签，多选）
        SectionTitle("玩具")
        ChipFlow(
            items = state.knownToys,
            selected = state.toys.toSet(),
            onToggle = { viewModel.toggleToy(it) },
            emptyText = "还没有玩具，去「我的 → 玩具管理」添加",
            manageHint = "管理",
            onManageTags = { onManageTags() },
        )

        // 满意度
        SectionTitle("满意度")
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                (1..5).forEach { star ->
                    Box(
                        modifier = Modifier
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .clickable { viewModel.setRating(star) }
                            .padding(4.dp),
                    ) {
                        Icon(
                            Icons.Rounded.Star,
                            contentDescription = "$star 星",
                            tint = if (star <= state.rating) Color(0xFFFFB300) else LocalIntimaPalette.current.textHint.copy(alpha = 0.4f),
                            modifier = Modifier.height(32.dp).width(32.dp),
                        )
                    }
                }
            }
        }

        // 情绪标签（单选）
        SectionTitle("情绪")
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MoodTags.ALL.forEach { tag ->
                    SelectableChip(
                        text = tag,
                        selected = tag == state.moodTag,
                        onClick = { viewModel.setMood(tag) },
                    )
                }
            }
        }

        // 地点
        SectionTitle("地点")
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(Place.HOME, Place.HOTEL, Place.OTHER).forEach { place ->
                    SelectableChip(
                        text = place.label(),
                        selected = state.place == place,
                        onClick = { viewModel.setPlace(place) },
                    )
                }
            }
        }

        // 备注
        TextField(
            value = state.note,
            onValueChange = viewModel::setNote,
            label = "备注（可选）",
            modifier = Modifier.fillMaxWidth(),
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
                onClick = viewModel::delete,
            ) { M3Text("删除记录", color = Color(0xFFE94634)) }
        }
        Spacer(Modifier.height(40.dp))
    }

    if (showStartPicker) {
        val initial = java.time.Instant.ofEpochMilli(state.startTime).atZone(java.time.ZoneId.systemDefault())
        val timeState = rememberTimePickerState(initial.hour, initial.minute, is24Hour = true)
        androidx.compose.ui.window.Dialog(onDismissRequest = { showStartPicker = false }) {
            Column(
                modifier = Modifier
                    .background(LocalIntimaPalette.current.card, RoundedCornerShape(24.dp))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                M3Text("开始时间", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                androidx.compose.material3.TimePicker(state = timeState)
                Row(modifier = Modifier.fillMaxWidth()) {
                    Spacer(Modifier.weight(1f))
                    M3Text(
                        "取消", color = LocalIntimaPalette.current.textHint,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showStartPicker = false }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                    M3Text(
                        "确定", color = Color(0xFF3482FF), fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                viewModel.setStart(timeState.hour, timeState.minute)
                                showStartPicker = false
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }

    if (showDurationPicker) {
        val durations = listOf(5, 10, 15, 20, 30, 45, 60, 90, 120)
        var customText by remember { mutableStateOf("") }
        androidx.compose.ui.window.Dialog(onDismissRequest = { showDurationPicker = false }) {
            Column(
                modifier = Modifier
                    .background(LocalIntimaPalette.current.card, RoundedCornerShape(24.dp))
                    .padding(20.dp),
            ) {
                M3Text("持续时间", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = LocalIntimaPalette.current.text)
                Spacer(Modifier.height(12.dp))
                // 等宽 3 列网格
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
                                    .background(if (sel) Color(0xFF3482FF) else LocalIntimaPalette.current.chipBg)
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
                                    color = if (sel) Color.White else LocalIntimaPalette.current.text,
                                )
                            }
                        }
                        // 补齐空位保持等宽
                        repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.height(10.dp))
                }
                // 自定义输入
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    androidx.compose.material3.OutlinedTextField(
                        value = customText,
                        onValueChange = { customText = it.filter { c -> c.isDigit() }.take(4) },
                        placeholder = { M3Text("自定义分钟", fontSize = 13.sp, color = LocalIntimaPalette.current.textHint) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF3482FF))
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

@Composable
private fun SelectableChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MiuixTheme.colorScheme.primary else LocalIntimaPalette.current.chipBg
    val fg = if (selected) Color.White else LocalIntimaPalette.current.text
    Box(
        modifier = Modifier
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        M3Text(text, color = fg, fontSize = 14.sp)
    }
}

/** 流式 Chip 布局：一行尽量多排，放不下换行 */
@Composable
private fun ChipFlow(
    items: List<String>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    emptyText: String,
    manageHint: String? = null,
    onManageTags: () -> Unit = {},
    labelOf: (String) -> String = { it },
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
            SelectableChip(
                text = labelOf(id),
                selected = id in selected,
                onClick = { onToggle(id) },
            )
        }
        if (manageHint != null) {
            SelectableChip(text = manageHint, selected = false, onClick = onManageTags)
        }    }
}

/** 体位/玩具的内联小输入框（输入 + 回车确认） */
@Composable
private fun PositionToyInput(hint: String, onAdd: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    androidx.compose.material3.OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        placeholder = { M3Text(hint, fontSize = 13.sp, color = LocalIntimaPalette.current.textHint) },
        modifier = Modifier.width(110.dp),
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
        trailingIcon = {
            if (text.isNotBlank()) {
                M3Text(
                    "添加", fontSize = 13.sp, color = Color(0xFF3482FF),
                    modifier = Modifier
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .clickable { onAdd(text); text = "" }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                )
            }
        },
    )
}
