package com.intima.log.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intima.log.data.db.Partner
import com.intima.log.ui.theme.avatarColor
import com.intima.log.ui.theme.LocalIntimaPalette

/**
 * 参与者多选 Chip（流式换行）。
 * multi=false 时退化为单选。
 */
@Composable
fun PartnerMultiSelect(
    partners: List<Partner>,
    selectedIds: Set<Long>,
    onSelectionChange: (Set<Long>) -> Unit,
    modifier: Modifier = Modifier,
    multi: Boolean = true,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        partners.forEach { partner ->
            val selected = partner.id in selectedIds
            Chip(
                partner = partner,
                selected = selected,
                onClick = {
                    onSelectionChange(
                        if (multi) {
                            if (selected) selectedIds - partner.id else selectedIds + partner.id
                        } else {
                            if (selected) emptySet() else setOf(partner.id)
                        }
                    )
                }
            )
        }
    }
}

@Composable
private fun Chip(partner: Partner, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) avatarColor(partner.colorIndex) else LocalIntimaPalette.current.chipBg
    val content = if (selected) Color.White else LocalIntimaPalette.current.text
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            "${partner.emoji} ${partner.nickname}",
            color = content,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}
