package com.intima.log.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog as M3AlertDialog
import androidx.compose.material3.Text as M3Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.intima.log.ui.theme.LocalDarkTheme
import com.intima.log.ui.theme.LocalIntimaPalette
import top.yukonga.miuix.kmp.basic.NumberPicker
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Miuix 风格组件封装（MiuixXxx 命名）。
 * 规则：Miuix 原生有的直接用；Miuix 没有的（日期选择、确认弹窗、时间选择）
 * 用 Compose 基元按 Miuix 视觉规范封装。仅统一视觉，不改交互语义。
 */

/** Miuix 风格胶囊选择 Chip（选中=主色底白字） */
@Composable
fun MiuixChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedColor: Color = MiuixTheme.colorScheme.primary,
) {
    val palette = LocalIntimaPalette.current
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(if (selected) selectedColor else palette.chipBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        M3Text(
            text,
            color = if (selected) Color.White else palette.text,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

/** Miuix 风格圆角输入框（label 作为占位符浮起，0.8.8 稳定 API） */
@Composable
fun MiuixTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
    hint: String = "",
    singleLine: Boolean = true,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation =
        androidx.compose.ui.text.input.VisualTransformation.None,
    keyboardOptions: androidx.compose.foundation.text.KeyboardOptions =
        androidx.compose.foundation.text.KeyboardOptions.Default,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = label,
        singleLine = singleLine,
        // 输入文字显式随主题（Miuix TextField 默认 textStyle 不取 LocalContentColor，深色下会是黑字）
        textStyle = androidx.compose.ui.text.TextStyle(color = LocalIntimaPalette.current.text),
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
    )
}

/**
 * Miuix 风格确认弹窗：圆角卡片、居中标题、底部双按钮。
 * Miuix extra.SuperDialog 的可用签名随版本变化较大，这里按其视觉
 * （卡片圆角、按钮排版、dim 遮罩）用基元封装，接口稳定。
 */
@Composable
fun MiuixConfirmDialog(
    show: MutableState<Boolean>,
    title: String,
    body: String,
    confirmText: String = "确定",
    dismissText: String = "取消",
    danger: Boolean = false,
    onConfirm: () -> Unit,
) {
    if (!show.value) return
    val palette = LocalIntimaPalette.current
    Dialog(
        onDismissRequest = { show.value = false },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .fillMaxWidth()
                .background(palette.card, RoundedCornerShape(22.dp))
                .padding(24.dp),
        ) {
            M3Text(
                title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = palette.text,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            M3Text(
                body,
                fontSize = 14.sp,
                color = palette.textSub,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 20.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.chipBg)
                        .clickable { show.value = false }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    M3Text(dismissText, fontSize = 15.sp, color = palette.textSub)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (danger) Color(0xFFE94634) else MiuixTheme.colorScheme.primary)
                        .clickable { show.value = false; onConfirm() }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    M3Text(confirmText, fontSize = 15.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/** Miuix 风格单输入弹窗（新增/重命名标签等） */
@Composable
fun MiuixInput(
    show: MutableState<Boolean>,
    title: String,
    initial: String = "",
    label: String = "",
    hint: String = "",
    confirmText: String = "保存",
    onConfirm: (String) -> Unit,
) {
    if (!show.value) return
    var text by remember(show) { mutableStateOf(initial) }
    val palette = LocalIntimaPalette.current
    Dialog(
        onDismissRequest = { show.value = false },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .fillMaxWidth()
                .background(palette.card, RoundedCornerShape(22.dp))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            M3Text(
                title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = palette.text,
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
            )
            MiuixTextField(
                value = text,
                onValueChange = { text = it },
                label = label,
                hint = hint,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.chipBg)
                        .clickable { show.value = false }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) { M3Text("取消", fontSize = 15.sp, color = palette.textSub) }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (text.isBlank()) palette.chipBg else MiuixTheme.colorScheme.primary)
                        .clickable(enabled = text.isNotBlank()) { show.value = false; onConfirm(text) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) { M3Text(confirmText, fontSize = 15.sp, color = if (text.isBlank()) palette.textHint else Color.White) }
            }
        }
    }
}

/**
 * Miuix 风格三列滚轮选择器（年/月/日）。
 * Miuix NumberPicker 提供 scope/ state 的 onValueChange 变体；为兼容 0.8.8 稳定 API，
 * 这里用其「值 + 范围」滚动语义：每次变更回调新值。
 */
@Composable
fun MiuixDatePickerDialog(
    show: MutableState<Boolean>,
    initialDate: java.time.LocalDate,
    onConfirm: (java.time.LocalDate) -> Unit,
    title: String = "选择日期",
) {
    if (!show.value) return
    val palette = LocalIntimaPalette.current
    var year by remember(show) { mutableStateOf(initialDate.year) }
    var month by remember(show) { mutableStateOf(initialDate.monthValue) }
    var day by remember(show) { mutableStateOf(initialDate.dayOfMonth) }
    val minYear = 2000
    val maxYear = java.time.LocalDate.now().plusYears(1).year

    fun clampDay() {
        val maxDay = java.time.YearMonth.of(year, month).lengthOfMonth()
        if (day > maxDay) day = maxDay
    }

    Dialog(
        onDismissRequest = { show.value = false },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .background(palette.card, RoundedCornerShape(22.dp))
                .padding(20.dp),
        ) {
            M3Text(
                title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = palette.text,
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
            )
            // 可选范围提示（日期上下限）
            M3Text(
                "可选范围 $minYear ~ $maxYear 年",
                fontSize = 11.sp,
                color = palette.textHint,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .height(160.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                WheelColumn(
                    modifier = Modifier.weight(1f),
                    unit = "年",
                    value = year,
                    range = minYear..maxYear,
                    onValueChange = { year = it; clampDay() },
                )
                WheelColumn(
                    modifier = Modifier.weight(1f),
                    unit = "月",
                    value = month,
                    range = 1..12,
                    onValueChange = { month = it; clampDay() },
                )
                WheelColumn(
                    modifier = Modifier.weight(1f),
                    unit = "日",
                    value = day,
                    range = 1..java.time.YearMonth.of(year, month).lengthOfMonth(),
                    onValueChange = { day = it },
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.chipBg)
                        .clickable { show.value = false }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) { M3Text("取消", fontSize = 15.sp, color = palette.textSub) }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MiuixTheme.colorScheme.primary)
                        .clickable {
                            show.value = false
                            onConfirm(java.time.LocalDate.of(year, month, day))
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) { M3Text("确定", fontSize = 15.sp, color = Color.White, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

/** 单列滚轮：Miuix NumberPicker。label 为每行格式化器（默认数字本身），unit 显示列底部单位 */
@Composable
private fun WheelColumn(
    modifier: Modifier,
    unit: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
) {
    NumberPicker(
        value = value,
        onValueChange = { onValueChange(it) },
        modifier = modifier,
        range = range.first..range.last,
        label = { it.toString() },   // 每行显示数字本身（修复：此前误传单位导致全列显示「年/月/日」）
    )
}

/**
 * Miuix 风格时间选择弹窗：时/分 双列循环滚轮（NumberPicker wrapAround），与日期选择器同视觉。
 */
@Composable
fun MiuixTimePickerDialog(
    show: MutableState<Boolean>,
    initialHour: Int,
    initialMinute: Int,
    title: String = "选择时间",
    onConfirm: (Int, Int) -> Unit,
) {
    if (!show.value) return
    val palette = LocalIntimaPalette.current
    var hour by remember(show) { mutableStateOf(initialHour) }
    var minute by remember(show) { mutableStateOf(initialMinute) }

    Dialog(
        onDismissRequest = { show.value = false },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .background(palette.card, RoundedCornerShape(22.dp))
                .padding(20.dp),
        ) {
            M3Text(
                title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = palette.text,
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                NumberPicker(
                    value = hour,
                    onValueChange = { hour = it },
                    modifier = Modifier.weight(1f),
                    range = 0..23,
                    label = { "%02d".format(it) },
                )
                NumberPicker(
                    value = minute,
                    onValueChange = { minute = it },
                    modifier = Modifier.weight(1f),
                    range = 0..59,
                    label = { "%02d".format(it) },
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.chipBg)
                        .clickable { show.value = false }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) { M3Text("取消", fontSize = 15.sp, color = palette.textSub) }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MiuixTheme.colorScheme.primary)
                        .clickable { show.value = false; onConfirm(hour, minute) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) { M3Text("确定", fontSize = 15.sp, color = Color.White, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}
