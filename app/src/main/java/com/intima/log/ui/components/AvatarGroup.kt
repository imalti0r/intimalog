package com.intima.log.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intima.log.data.db.Partner
import com.intima.log.ui.theme.LocalIntimaPalette
import com.intima.log.ui.theme.avatarColor

/**
 * AvatarGroup：圆形头像叠放（偏移 -8dp，2dp 描边，溢出 +N）。
 * partnerSize 为 0 时显示灰色占位。
 */
@Composable
fun AvatarGroup(
    partners: List<Partner>,
    modifier: Modifier = Modifier,
    maxVisible: Int = 3,
    size: Dp = 32.dp,
    borderColor: Color = LocalIntimaPalette.current.card,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        if (partners.isEmpty()) {
            EmptyAvatar(size)
            return@Row
        }
        val visible = partners.take(maxVisible)
        visible.forEachIndexed { index, partner ->
            PartnerAvatar(
                partner = partner,
                size = size,
                borderColor = borderColor,
                modifier = if (index == 0) Modifier else Modifier.offset((-8 * index).dp, 0.dp)
            )
        }
        if (partners.size > maxVisible) {
            OverflowAvatar(
                count = partners.size - maxVisible,
                size = size,
                color = avatarColor(partners.last().colorIndex),
                borderColor = borderColor,
                modifier = Modifier.offset((-8 * visible.size).dp, 0.dp)
            )
        }
    }
}

@Composable
fun PartnerAvatar(
    partner: Partner,
    size: Dp,
    borderColor: Color = LocalIntimaPalette.current.card,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(avatarColor(partner.colorIndex), CircleShape)
            .border(2.dp, borderColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(partner.emoji, fontSize = (size.value * 0.5f).sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun OverflowAvatar(
    count: Int,
    size: Dp,
    color: Color,
    borderColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(color, CircleShape)
            .border(2.dp, borderColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text("+${count.coerceAtMost(9)}", color = Color.White, fontSize = (size.value * 0.36f).sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun EmptyAvatar(size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .background(Color(0x1A808080), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // 「？」全角字形自带左右留白且分布不对称，圆内直接 Center 会视觉偏左：
        // 文本撑满整圆宽度 + textAlign 居中，再微调垂直位置抵消字形底部留白
        Text(
            "？",
            color = Color(0xFF9E9E9E),
            fontSize = (size.value * 0.5f).sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (size.value * 0.06f).dp),
        )
    }
}
