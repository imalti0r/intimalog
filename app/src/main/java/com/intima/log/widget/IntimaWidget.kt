package com.intima.log.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.RowScope
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.intima.log.data.db.SexRecordWithPartners
import com.intima.log.data.repo.RecordRepository
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * IntimaLog 桌面小组件（Glance）。
 * 单一 Provider 自适应 2x2 / 4x2 / 4x4（由桌面决定显示区域）。
 */
class IntimaWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val recordRepo = WidgetGraph.recordRepository(context)
        val profileRepo = WidgetGraph.profileRepository(context)

        val ongoingNow = recordRepo.ongoingRecord() != null
        val todayCount = countToday(recordRepo)
        val todayList = todayRecords(recordRepo)
        provideContent {
            GlanceTheme {
                WidgetContent(
                    todaySessions = todayCount,
                    todayPersonTimes = 0,   // 简化：小组件显示场次
                    records = todayList,
                    hasOngoing = ongoingNow,
                )
            }
        }
    }

    private suspend fun countToday(repo: com.intima.log.data.repo.RecordRepository): Int {
        val zone = ZoneId.systemDefault()
        val start = java.time.LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
        return repo.countInRangeOnce(start, System.currentTimeMillis())
    }

    private suspend fun todayRecords(repo: com.intima.log.data.repo.RecordRepository): List<SexRecordWithPartners> {
        val zone = ZoneId.systemDefault()
        val start = java.time.LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
        return repo.listAllWithPartners().filter { it.record.startTime >= start }
            .sortedByDescending { it.record.startTime }
            .take(3)
    }
}

@Composable
private fun WidgetContent(
    todaySessions: Int,
    todayPersonTimes: Int,
    records: List<SexRecordWithPartners>,
    hasOngoing: Boolean,
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "今日 $todaySessions 场",
                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                modifier = GlanceModifier.defaultWeight(),
            )
            // 开始/结束按钮
            Box(
                modifier = GlanceModifier
                    .background(GlanceTheme.colors.primary)
                    .cornerRadius(16.dp)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .clickable(actionRunCallback<ToggleRecordCallback>()),
            ) {
                Text(
                    if (hasOngoing) "结束" else "开始",
                    style = TextStyle(color = GlanceTheme.colors.onPrimary, fontSize = 12.sp),
                )
            }
        }
        Spacer(modifier = GlanceModifier.height(8.dp))
        records.take(3).forEach { item ->
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    formatTime(item.record.startTime),
                    style = TextStyle(fontSize = 11.sp, color = ColorProvider(Color(0xFF888888))),
                )
                Spacer(modifier = GlanceModifier.width(8.dp))
                Text(
                    if (item.partners.isEmpty()) "未指定"
                    else item.partners.joinToString("+") { it.nickname },
                    style = TextStyle(fontSize = 12.sp),
                    modifier = GlanceModifier.defaultWeight(),
                    maxLines = 1,
                )
            }
        }
        if (records.isEmpty()) {
            Text(
                "今天还没有记录",
                style = TextStyle(fontSize = 12.sp, color = ColorProvider(Color(0xFF999999))),
            )
        }
    }
}

private fun formatTime(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("HH:mm"))

class IntimaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = IntimaWidget()
}

/** 小组件「开始/结束」点击回调：直接操作 Repository 并刷新 */
class ToggleRecordCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val recordRepository = WidgetGraph.recordRepository(context)
        val ongoing = recordRepository.ongoingRecord()
        if (ongoing != null) {
            recordRepository.finishOngoing()
        } else {
            recordRepository.startOngoing()
        }
        IntimaWidget().update(context, glanceId)
    }
}