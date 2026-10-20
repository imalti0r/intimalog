package com.intima.log.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text as M3Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.intima.log.data.db.SexRecordWithPartners
import com.intima.log.data.db.StatsBasis
import com.intima.log.data.repo.ProfileRepository
import com.intima.log.domain.usecase.DurationFormatter
import com.intima.log.domain.usecase.GetHomeMetricsUseCase
import com.intima.log.domain.usecase.GetRecordsUseCase
import com.intima.log.domain.usecase.SaveRecordWithPartnersUseCase
import com.intima.log.ui.lock.DisguiseHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import com.intima.log.ui.theme.LocalIntimaPalette

data class HomeUiState(
    val todaySessions: Int = 0,
    val todayPersonTimes: Int = 0,
    val weekSessions: Int = 0,
    val avgDuration: Long? = null,
    val basis: StatsBasis = StatsBasis.SESSION,
    val enableMultiPartner: Boolean = true,
    val title: String = "亲密记录",
    val records: List<SexRecordWithPartners> = emptyList(),
    val ongoingRecordId: Long? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getMetrics: GetHomeMetricsUseCase,
    private val getRecords: GetRecordsUseCase,
    private val saveRecord: SaveRecordWithPartnersUseCase,
    private val profileRepository: ProfileRepository,
    @dagger.hilt.android.qualifiers.ApplicationContext private val appContext: android.content.Context,
) : ViewModel() {
    private val metrics = MutableStateFlow(HomeUiState())

    val uiState: StateFlow<HomeUiState> = kotlinx.coroutines.flow.combine(
        metrics,
        profileRepository.profile,
        getRecords.observeAll(),
    ) { m, profile, records ->
        val zone = ZoneId.systemDefault()
        val dayStart = java.time.LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
        m.copy(
            basis = profile.statsBasis,
            enableMultiPartner = profile.enableMultiPartner,
            title = DisguiseHelper.homeTitle(profile),
            records = records.filter { it.record.startTime >= dayStart }
                .sortedByDescending { it.record.startTime },
            ongoingRecordId = records.firstOrNull { it.record.endTime == null }?.record?.id,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        refresh()
        // 数据变化后重算指标
        viewModelScope.launch {
            getRecords.observeAll().collect { refresh() }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val profile = profileRepository.profile.first()
            val m = getMetrics(profile.statsBasis)
            metrics.value = metrics.value.copy(
                todaySessions = m.todaySessions,
                todayPersonTimes = m.todayPersonTimes,
                weekSessions = m.weekSessions,
                avgDuration = m.avgDurationSeconds,
            )
        }
    }

    fun startOngoing() {
        viewModelScope.launch {
            saveRecord.startOngoing()
            // 计时进行中启动常驻通知（B5 修复）
            com.intima.log.service.TimingServiceStarter.start(appContext)
        }
    }

    fun finishOngoing() {
        viewModelScope.launch { saveRecord.finishOngoing() }
    }
}

/** 首页 */
@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit,
    onStartEdit: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalIntimaPalette.current.bg)
    ) {
        // 顶部标题
        M3Text(
            state.title,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 指标卡
            item {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        value = "${state.todaySessions}",
                        label = if (state.basis == StatsBasis.PERSON_TIME) "今日 · ${state.todayPersonTimes} 人次" else "今日场次",
                    )
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        value = "${state.weekSessions}",
                        label = "本周" + if (state.basis == StatsBasis.PERSON_TIME) "人次" else "次数",
                    )
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        value = state.avgDuration?.let { DurationFormatter.shortFormat(it) } ?: "—",
                        label = "平均时长",
                    )
                }
            }

            // 大圆按钮
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    BigRecordButton(
                        ongoing = state.ongoingRecordId != null,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (state.ongoingRecordId != null) viewModel.finishOngoing() else viewModel.startOngoing()
                        },
                        onQuickAdd = { onNavigate("record_edit?recordId=0") },
                    )
                }
            }

            // 今日时间轴
            item {
                M3Text(
                    "今日时间轴",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            if (state.records.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth()
                            .background(LocalIntimaPalette.current.card, RoundedCornerShape(16.dp))
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        M3Text("今天还没有记录", color = LocalIntimaPalette.current.textHint, fontSize = 14.sp)
                    }
                }
            } else {
                items(state.records, key = { it.record.id }) { item ->
                    RecordRow(
                        item = item,
                        strongDisguise = false,
                        onClick = { onStartEdit(item.record.id) },
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}
