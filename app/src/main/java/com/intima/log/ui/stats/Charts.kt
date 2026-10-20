package com.intima.log.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text as M3Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intima.log.ui.theme.LocalIntimaPalette

/** 统计口径切换 Segmented Control */
@Composable
fun StatsBasisSwitcher(
    basis: com.intima.log.data.db.StatsBasis,
    onChange: (com.intima.log.data.db.StatsBasis) -> Unit,
) {
    Row(
        modifier = Modifier
            .background(Color(0x0D000000), RoundedCornerShape(999.dp))
            .padding(4.dp),
    ) {
        listOf(
            com.intima.log.data.db.StatsBasis.SESSION to "场次",
            com.intima.log.data.db.StatsBasis.PERSON_TIME to "人次",
        ).forEach { (b, label) ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        if (basis == b) Color.White else Color.Transparent
                    )
                    .clickable { onChange(b) }
                    .padding(horizontal = 20.dp, vertical = 6.dp),
            ) {
                M3Text(
                    label,
                    fontSize = 13.sp,
                    fontWeight = if (basis == b) FontWeight.Bold else FontWeight.Normal,
                    color = if (basis == b) Color(0xFF1A1A1A) else Color(0xFF888888),
                )
            }
        }
    }
}

/** 环形图（保护措施 / 多人占比） */
data class PieSlice(val label: String, val value: Int, val color: Color)

@Composable
fun DonutChart(
    slices: List<PieSlice>,
    modifier: Modifier = Modifier,
    stroke: Float = 60f,
    centerText: String = "",
) {
    val total = slices.sumOf { it.value }.coerceAtLeast(1)
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(modifier = Modifier.size(150.dp)) {
            var startAngle = -90f
            val diameter = minOf(size.width, size.height) - stroke * 2
            val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
            slices.filter { it.value > 0 }.forEach { slice ->
                val sweep = 360f * slice.value / total
                drawArc(
                    color = slice.color,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(diameter, diameter),
                    style = Stroke(width = stroke),
                )
                startAngle += sweep
            }
        }
        if (centerText.isNotEmpty()) {
            M3Text(centerText, fontSize = 12.sp, color = Color(0xFF888888))
        }
        // 图例
        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            slices.forEach { slice ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).background(slice.color, CircleShape))
                    M3Text(
                        " ${slice.label} ${slice.value}",
                        fontSize = 11.sp, color = LocalIntimaPalette.current.textSub,
                    )
                }
            }
        }
    }
}

/** 简易折线图（近 N 天趋势） */
@Composable
fun LineChart(values: List<Int>, modifier: Modifier = Modifier, lineColor: Color = Color(0xFF3482FF)) {
    val maxV = (values.maxOrNull() ?: 1).coerceAtLeast(1)
    Canvas(modifier = modifier.fillMaxWidth().height(120.dp)) {
        if (values.size < 2) return@Canvas
        val stepX = size.width / (values.size - 1)
        var path = androidx.compose.ui.graphics.Path()
        values.forEachIndexed { i, v ->
            val x = i * stepX
            val y = size.height - (v.toFloat() / maxV) * size.height * 0.9f - 8f
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, lineColor, style = Stroke(width = 6f))
        // 端点
        val lastX = (values.size - 1) * stepX
        val lastY = size.height - (values.last().toFloat() / maxV) * size.height * 0.9f - 8f
        drawCircle(lineColor, radius = 8f, center = Offset(lastX, lastY))
    }
}

/** 横向条形图（参与者分布 / 组合排行） */
@Composable
fun HBarChart(
    rows: List<Pair<String, Int>>,
    modifier: Modifier = Modifier,
    barColor: Color = Color(0xFF3482FF),
) {
    val maxV = (rows.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { (label, v) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 标签列固定宽度区间：所有条形左端严格对齐；超长标签省略号截断
                M3Text(
                    label, fontSize = 12.sp, maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    color = LocalIntimaPalette.current.textSub,
                    modifier = Modifier
                        .widthIn(min = 88.dp, max = 132.dp)
                        .padding(end = 8.dp),
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(18.dp)
                        .background(Color(0x0D000000), RoundedCornerShape(9.dp)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(v.toFloat() / maxV)
                            .height(18.dp)
                            .background(barColor, RoundedCornerShape(9.dp)),
                    )
                }
                M3Text(" $v", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** GitHub 风格热力图（近一年，恒按场次） */
@Composable
fun Heatmap(dayCounts: Map<java.time.LocalDate, Int>, modifier: Modifier = Modifier) {
    val levels = listOf(Color(0xFFEBEDF0), Color(0xFF9BE9A8), Color(0xFF40C463), Color(0xFF30A14E), Color(0xFF216E39))
    val today = java.time.LocalDate.now()
    val start = today.minusDays(363)
    Canvas(modifier = modifier.fillMaxWidth().height(96.dp)) {
        val cell = size.width / 53f
        dayCounts.forEach { (date, count) ->
            val daysFromStart = java.time.temporal.ChronoUnit.DAYS.between(start, date)
            if (daysFromStart < 0 || daysFromStart > 364) return@forEach
            val col = daysFromStart / 7
            val row = date.dayOfWeek.value % 7
            val level = when {
                count == 0 -> 0
                count == 1 -> 1
                count == 2 -> 2
                count <= 4 -> 3
                else -> 4
            }
            drawRect(
                color = levels[level],
                topLeft = Offset(col * cell, row * (size.height / 7f)),
                size = Size(cell * 0.85f, size.height / 7f * 0.85f),
            )
        }
    }
}
