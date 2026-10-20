package com.intima.log.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text as M3Text
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intima.log.data.db.SexRecordWithPartners
import com.intima.log.ui.components.AvatarGroup
import com.intima.log.ui.theme.IntimaColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.intima.log.ui.theme.LocalIntimaPalette

private val hm: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** 首页导航小按钮 */
@Composable
fun NavIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color(0x0D000000))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = desc, tint = LocalIntimaPalette.current.text, modifier = Modifier.size(22.dp))
    }
}

/** 指标卡 */
@Composable
fun MetricCard(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(LocalIntimaPalette.current.card, RoundedCornerShape(16.dp))
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        M3Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = LocalIntimaPalette.current.text)
        M3Text(label, fontSize = 12.sp, color = LocalIntimaPalette.current.textHint, modifier = Modifier.padding(top = 4.dp))
    }
}

/** 大圆「开始/结束」按钮：按下轻微 scale */
@Composable
fun BigRecordButton(
    ongoing: Boolean,
    onClick: () -> Unit,
    onQuickAdd: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, tween(120), label = "scale")

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .scale(scale)
                .background(
                    if (ongoing) Color(0xFFE94634) else IntimaColors.Blue,
                    CircleShape
                )
                .clickable(interactionSource = interaction, indication = null, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    if (ongoing) Icons.Rounded.Stop else Icons.Rounded.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp),
                )
                M3Text(
                    if (ongoing) "结束" else "开始记录",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        M3Text(
            "点击填写详情 →",
            fontSize = 13.sp,
            color = Color(0xFF7B8CDE),
            modifier = Modifier
                .padding(top = 12.dp)
                .clickable(onClick = onQuickAdd),
        )
    }
}

/** 时间轴单条记录 */
@Composable
fun RecordRow(
    item: SexRecordWithPartners,
    strongDisguise: Boolean,
    onClick: () -> Unit,
) {
    val zone = ZoneId.systemDefault()
    val start = Instant.ofEpochMilli(item.record.startTime).atZone(zone)
    val end = item.record.endTime?.let { Instant.ofEpochMilli(it).atZone(zone) }
    val duration = item.record.durationSeconds

    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(LocalIntimaPalette.current.card)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 左侧时间
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            M3Text(start.format(hm), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            M3Text(
                end?.let { "→ ${it.format(hm)}" } ?: "进行中",
                fontSize = 11.sp,
                color = if (end == null) Color(0xFFE94634) else LocalIntimaPalette.current.textHint,
            )
        }
        // AvatarGroup / 强伪装中性
        if (strongDisguise && item.record.participantCount >= 2) {
            M3Text(
                "活动 · ${item.record.participantCount} 人",
                fontSize = 14.sp,
                color = LocalIntimaPalette.current.textSub,
            )
        } else {
            AvatarGroup(partners = item.partners, size = 30.dp)
        }
        Box(modifier = Modifier.weight(1f))
        // 右侧时长/措施
        Column(horizontalAlignment = Alignment.End) {
            M3Text(
                duration?.let { "${it / 60} 分钟" } ?: "计时中",
                fontSize = 13.sp,
                color = LocalIntimaPalette.current.textSub,
            )
            M3Text(
                item.record.customProtections.split(",").filter { it.isNotBlank() }.joinToString(" · ").ifEmpty { "" },
                fontSize = 11.sp,
                color = Color(0xFF03B585),
            )
        }
        Icon(
            Icons.Rounded.Edit,
            contentDescription = "编辑",
            tint = LocalIntimaPalette.current.textHint,
            modifier = Modifier.size(16.dp),
        )
    }
}
