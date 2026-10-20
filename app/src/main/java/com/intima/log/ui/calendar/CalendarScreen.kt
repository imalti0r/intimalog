package com.intima.log.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text as M3Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.intima.log.data.db.SexRecordWithPartners
import com.intima.log.data.repo.RecordRepository
import com.intima.log.domain.usecase.GetMonthCalendarUseCase
import com.intima.log.domain.usecase.GetRecordsUseCase
import com.intima.log.ui.components.AvatarGroup
import com.intima.log.ui.home.RecordRow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import com.intima.log.ui.theme.LocalIntimaPalette

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val monthCalendar: GetMonthCalendarUseCase,
    private val getRecords: GetRecordsUseCase,
) : ViewModel() {

    var month by mutableStateOf(YearMonth.now())
        private set
    var dayCounts by mutableStateOf<Map<LocalDate, Int>>(emptyMap())
        private set
    var selectedDay by mutableStateOf<LocalDate?>(null)
    var dayRecords by mutableStateOf<List<SexRecordWithPartners>>(emptyList())
        private set

    init { load() }

    fun load() {
        viewModelScope.launch {
            dayCounts = monthCalendar(month)
            selectedDay?.let { loadDay(it) }
        }
    }

    /** 每次进入日历页刷新一次（tab 切回 / 从其他页返回），并同步当前月 */
    fun refreshOnEnter() {
        viewModelScope.launch {
            month = YearMonth.now()
            load()
        }
    }

    fun prevMonth() { month = month.minusMonths(1); load() }
    fun nextMonth() { month = month.plusMonths(1); load() }

    fun selectDay(date: LocalDate) {
        selectedDay = date
        viewModelScope.launch { loadDay(date) }
    }

    private suspend fun loadDay(date: LocalDate) {
        val zone = ZoneId.systemDefault()
        val from = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val until = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        dayRecords = getRecords.observeRangeOnce(from, until).sortedByDescending { it.record.startTime }
    }
}

/** 日历月视图 */
@Composable
fun CalendarScreen(
    onEditRecord: (Long) -> Unit,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    // 每次进入日历页刷新一次（需求：进入即刷新）
    androidx.compose.runtime.DisposableEffect(Unit) {
        viewModel.refreshOnEnter()
        onDispose { }
    }
    Column(modifier = Modifier.fillMaxSize().background(LocalIntimaPalette.current.bg)) {
        // 月份切换
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(Icons.Rounded.KeyboardArrowLeft, "上月", modifier = Modifier.size(28.dp).clickable { viewModel.prevMonth() })
            M3Text(
                "${viewModel.month.year} 年 ${viewModel.month.monthValue} 月",
                fontSize = 20.sp, fontWeight = FontWeight.Bold,
            )
            Icon(Icons.Rounded.KeyboardArrowRight, "下月", modifier = Modifier.size(28.dp).clickable { viewModel.nextMonth() })
        }

        MonthGrid(
            month = viewModel.month,
            dayCounts = viewModel.dayCounts,
            selected = viewModel.selectedDay,
            onSelect = viewModel::selectDay,
        )

        // 当日记录
        viewModel.selectedDay?.let { day ->
            M3Text(
                "${day.monthValue} 月 ${day.dayOfMonth} 日 · ${viewModel.dayRecords.size} 场",
                fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            androidx.compose.foundation.lazy.LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(viewModel.dayRecords.size) { index ->
                    val item = viewModel.dayRecords[index]
                    RecordRow(item = item, strongDisguise = false, onClick = { onEditRecord(item.record.id) })
                }
            }
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    dayCounts: Map<LocalDate, Int>,
    selected: LocalDate?,
    onSelect: (LocalDate) -> Unit,
) {
    val firstDay = month.atDay(1)
    val leading = firstDay.dayOfWeek.value % 7   // 周一=1 → 前导空格
    val daysInMonth = month.lengthOfMonth()
    val today = LocalDate.now()

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .background(LocalIntimaPalette.current.card, RoundedCornerShape(16.dp))
            .padding(12.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("一", "二", "三", "四", "五", "六", "日").forEach {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    M3Text(it, fontSize = 12.sp, color = LocalIntimaPalette.current.textHint)
                }
            }
        }
        // 前导 + 日期网格
        var day = 1 - leading
        while (day <= daysInMonth) {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                repeat(7) {
                    val d = day
                    day++
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (d in 1..daysInMonth) {
                            val date = month.atDay(d)
                            val count = dayCounts[date] ?: 0
                            val isToday = date == today
                            val isSelected = date == selected
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                isSelected -> top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.primary
                                                isToday -> top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.primary.copy(alpha = 0.08f)
                                                else -> Color.Transparent
                                            }
                                        )
                                        .clickable { onSelect(date) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    M3Text(
                                        "$d",
                                        fontSize = 13.sp,
                                        color = when {
                                            isSelected -> Color.White
                                            else -> LocalIntimaPalette.current.text
                                        },
                                        fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                                    )
                                }
                                // 圆点标记
                                if (count > 0) {
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 2.dp)
                                            .size(5.dp)
                                            .background(top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.primary, CircleShape),
                                    )
                                } else {
                                    Box(modifier = Modifier.padding(top = 2.dp).size(5.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
