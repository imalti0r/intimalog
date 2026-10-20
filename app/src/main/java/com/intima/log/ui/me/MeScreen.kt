package com.intima.log.ui.me

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Toys
import androidx.compose.material3.Icon
import androidx.compose.material3.Text as M3Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intima.log.ui.theme.LocalIntimaPalette

/**
 * 「我的」页：伴侣、模板、日历、统计、设置的聚合入口。
 */
@Composable
fun MeScreen(
    onNavigate: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        M3Text("我的", fontSize = 28.sp, color = LocalIntimaPalette.current.text)

        // 头像卡片
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(LocalIntimaPalette.current.card, RoundedCornerShape(20.dp))
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Color(0xFFE8F0FE), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                M3Text("😊", fontSize = 28.sp)
            }
            Column {
                M3Text("亲密记录", fontSize = 18.sp, color = LocalIntimaPalette.current.text)
                M3Text(
                    "数据仅保存在本机 · 加密存储",
                    fontSize = 12.sp,
                    color = LocalIntimaPalette.current.textHint,
                )
            }
        }

        // 功能入口组
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(LocalIntimaPalette.current.card, RoundedCornerShape(20.dp))
                .padding(vertical = 4.dp),
        ) {
            MeRow(Icons.Rounded.People, "伴侣管理", "伴侣档案与组合视图") { onNavigate("partners") }
            MeRow(Icons.Rounded.SelfImprovement, "体位管理", "自定义体位标签") { onNavigate("positions") }
            MeRow(Icons.Rounded.Toys, "玩具管理", "自定义玩具标签") { onNavigate("toys") }
            MeRow(Icons.Rounded.Bookmark, "场景模板", "常用组合一键套用") { onNavigate("templates") }
            MeRow(Icons.Rounded.BarChart, "统计", "趋势、占比与排行") { onNavigate("stats") }
            MeRow(Icons.Rounded.Bookmark, "备份与同步", "导出加密备份 · WebDAV") { onNavigate("backup") }
            MeRow(Icons.Rounded.Settings, "设置", "应用锁与伪装") { onNavigate("settings") }
        }

        Spacer(modifier = Modifier.height(12.dp))
        M3Text(
            "本应用不申请网络权限\n数据绝不离开设备",
            fontSize = 12.sp,
            color = LocalIntimaPalette.current.textHint,
            lineHeight = 18.sp,
            modifier = Modifier.padding(bottom = 16.dp),
        )
    }
}

@Composable
private fun MeRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color(0xFFF4F5F7), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = title, tint = Color(0xFF3482FF))
        }
        Column(modifier = Modifier.weight(1f)) {
            M3Text(title, fontSize = 15.sp, color = LocalIntimaPalette.current.text)
            M3Text(subtitle, fontSize = 12.sp, color = LocalIntimaPalette.current.textHint)
        }
        Icon(
            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = LocalIntimaPalette.current.textHint,
        )
    }
}
