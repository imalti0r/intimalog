package com.intima.log.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text as M3Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.intima.log.data.db.Partner
import com.intima.log.data.db.Protection
import com.intima.log.data.db.StatsBasis
import com.intima.log.data.repo.PartnerRepository
import com.intima.log.data.repo.ProfileRepository
import com.intima.log.data.repo.RecordRepository
import com.intima.log.domain.usecase.GetCombinationStatsUseCase
import com.intima.log.domain.usecase.GetStreakUseCase
import com.intima.log.domain.usecase.StatsBasisCounter
import com.intima.log.ui.theme.avatarColor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs
import javax.inject.Inject
import com.intima.log.ui.theme.LocalIntimaPalette

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val recordRepository: RecordRepository,
    private val partnerRepository: PartnerRepository,
    private val profileRepository: ProfileRepository,
    private val combinationStats: GetCombinationStatsUseCase,
    private val streakUseCase: GetStreakUseCase,
    private val counter: StatsBasisCounter,
) : ViewModel() {

    var basis by mutableStateOf(StatsBasis.SESSION)
        private set
    var weekTrend by mutableStateOf<List<Int>>(emptyList())
        private set            // 近 7 天，随口径
    var avgDurationTrend by mutableStateOf<List<Long>>(emptyList())
        private set            // 近 7 天平均时长（恒按场次）
    var protectionSlices by mutableStateOf<List<PieSlice>>(emptyList())
        private set
    var multiPartnerSlices by mutableStateOf<List<PieSlice>>(emptyList())
        private set
    var partnerDistribution by mutableStateOf<List<Pair<String, Int>>>(emptyList())
        private set            // 按参与人次
    var positionStats by mutableStateOf<List<Pair<String, Int>>>(emptyList())
        private set            // 体位使用排行
    var toyStats by mutableStateOf<List<Pair<String, Int>>>(emptyList())
        private set            // 玩具使用排行
    var combinations by mutableStateOf<List<com.intima.log.domain.usecase.CombinationRank>>(emptyList())
        private set
    var streak by mutableStateOf(0)
        private set
    var heatmap by mutableStateOf<Map<LocalDate, Int>>(emptyMap())
        private set

    init {
        viewModelScope.launch {
            basis = profileRepository.profile.first().statsBasis
            load()
        }
    }

    fun chooseBasis(b: StatsBasis) {
        basis = b
        viewModelScope.launch {
            profileRepository.setStatsBasis(b)
            load()
        }
    }

    fun load() {
        viewModelScope.launch {
            val all = recordRepository.listAllWithPartners()
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)

            // 近 7 天趋势（随口径）
            weekTrend = (6 downTo 0).map { offset ->
                val date = today.minusDays(offset.toLong())
                val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
                val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
                all.filter { it.record.startTime in start until end }
                    .sumOf { counter.value(basis, it.record.participantCount) }
            }

            // 平均时长趋势（恒按场次）：近 7 天每天的平均时长
            avgDurationTrend = (6 downTo 0).map { offset ->
                val date = today.minusDays(offset.toLong())
                val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
                val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
                val durations = all.filter { it.record.startTime in start until end }
                    .mapNotNull { it.record.durationSeconds }
                if (durations.isEmpty()) 0 else durations.average().toLong() / 60
            }

            // 保护措施环形（恒按场次）。数据源：旧枚举列 protections + 新自定义列 customProtections，
            // 逐场拆分计数（多选一场计入多个类别）；两者都空才归入「未记录」
            val byProtection = LinkedHashMap<String, Int>()
            fun bump(name: String) = byProtection.merge(name, 1, Int::plus)
            all.forEach { r ->
                val enums = r.record.protections.split(",").filter { it.isNotBlank() }
                    .mapNotNull { n -> runCatching { Protection.valueOf(n) }.getOrNull() }
                val customs = r.record.customProtections.split(",").filter { it.isNotBlank() }
                if (enums.isEmpty() && customs.isEmpty()) {
                    bump(protectionDisplayName(r.record.protection))
                } else {
                    enums.forEach { bump(protectionDisplayName(it)) }
                    customs.forEach { bump(it) }
                }
            }
            protectionSlices = byProtection.map { (name, n) ->
                PieSlice(name, n, protectionColorByName(name))
            }

            // 多人占比环形（恒按场次）
            val single = all.count { it.record.participantCount <= 1 }
            val two = all.count { it.record.participantCount == 2 }
            val threePlus = all.count { it.record.participantCount >= 3 }
            multiPartnerSlices = listOf(
                PieSlice("单", single, Color(0xFF3482FF)),
                PieSlice("双", two, Color(0xFF03B585)),
                PieSlice("3+", threePlus, Color(0xFF7B5CFF)),
            )

            // 参与人次分布
            val partnerMap = partnerRepository.listActive().associateBy { it.id }
            val dist = all.flatMap { it.partners }
                .groupingBy { it.id }
                .eachCount()
            partnerDistribution = dist.entries
                .sortedByDescending { it.value }
                .take(8)
                .map { (id, n) -> (partnerMap[id]?.nickname ?: "已归档") to n }

            // 体位使用排行（按出现场次）
            positionStats = all.flatMap { it.record.positions.split(",") }
                .filter { it.isNotBlank() }
                .groupingBy { it }
                .eachCount()
                .entries.sortedByDescending { it.value }
                .take(8)
                .map { it.key to it.value }

            // 玩具使用排行
            toyStats = all.flatMap { it.record.toys.split(",") }
                .filter { it.isNotBlank() }
                .groupingBy { it }
                .eachCount()
                .entries.sortedByDescending { it.value }
                .take(8)
                .map { it.key to it.value }

            // 组合排行
            combinations = combinationStats(topN = 5)

            // Streak & 热力图
            streak = streakUseCase()
            heatmap = all.groupBy {
                Instant.ofEpochMilli(it.record.startTime).atZone(zone).toLocalDate()
            }.mapValues { it.value.size }
        }
    }

    private fun protectionColor(p: Protection): Color = when (p) {
        Protection.CONDOM -> Color(0xFF03B585)
        Protection.PILL_SHORT, Protection.PILL_LONG, Protection.PILL_URGENT -> Color(0xFF3482FF)
        Protection.NONE -> Color(0xFFE94634)
        Protection.OTHER -> Color(0xFF7B5CFF)
        Protection.NOT_RECORDED -> Color(0xFF8E8E93)
    }

    private fun protectionDisplayName(p: Protection): String = when (p) {
        Protection.NONE -> "无措施"
        Protection.CONDOM -> "安全套"
        Protection.PILL_SHORT -> "短效避孕药"
        Protection.PILL_LONG -> "长效避孕药"
        Protection.PILL_URGENT -> "紧急避孕药"
        Protection.OTHER -> "其他"
        Protection.NOT_RECORDED -> "未记录"
    }

    /** 自定义保护措施条目按名称取色（稳定散列到预设色板） */
    private fun protectionColorByName(name: String): Color {
        if (name == "未记录") return Color(0xFF8E8E93)
        if (name == "无措施") return Color(0xFFE94634)
        if (name == "安全套") return Color(0xFF03B585)
        val palette = listOf(
            Color(0xFF3482FF), Color(0xFF7B5CFF), Color(0xFFFF9500),
            Color(0xFF03B585), Color(0xFFE94634), Color(0xFF30B8C9),
            Color(0xFFE85A8A), Color(0xFF8E8E93),
        )
        return palette[abs(name.hashCode()) % palette.size]
    }
}

/** 统计页 */
@Composable
fun StatsScreen(viewModel: StatsViewModel = hiltViewModel()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalIntimaPalette.current.bg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            M3Text("统计", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            StatsBasisSwitcher(basis = viewModel.basis, onChange = viewModel::chooseBasis)
        }
        M3Text(
            "连续记录 ${viewModel.streak} 天",
            fontSize = 14.sp,
            color = LocalIntimaPalette.current.textSub,
        )

        CardBlock("次数趋势（${if (viewModel.basis == StatsBasis.SESSION) "场次" else "人次"}）") {
            LineChart(values = viewModel.weekTrend)
        }
        CardBlock("平均时长趋势（分钟 · 恒按场次）") {
            LineChart(
                values = viewModel.avgDurationTrend.map { it.toInt() },
                lineColor = Color(0xFF03B585),
            )
        }
        CardBlock("保护措施比例（按场次）") {
            DonutChart(slices = viewModel.protectionSlices)
        }
        CardBlock("多人场次占比（单 / 双 / 3+）") {
            DonutChart(slices = viewModel.multiPartnerSlices, centerText = "按场次")
        }
        CardBlock("参与者分布（按参与人次）") {
            if (viewModel.partnerDistribution.isEmpty()) M3Text("暂无数据", fontSize = 13.sp, color = LocalIntimaPalette.current.textHint)
            else HBarChart(rows = viewModel.partnerDistribution, barColor = Color(0xFF7B5CFF))
        }
        CardBlock("体位使用排行（按场次）") {
            if (viewModel.positionStats.isEmpty()) M3Text("暂无数据", fontSize = 13.sp, color = LocalIntimaPalette.current.textHint)
            else HBarChart(rows = viewModel.positionStats, barColor = Color(0xFF03B585))
        }
        CardBlock("玩具使用排行（按场次）") {
            if (viewModel.toyStats.isEmpty()) M3Text("暂无数据", fontSize = 13.sp, color = LocalIntimaPalette.current.textHint)
            else HBarChart(rows = viewModel.toyStats, barColor = Color(0xFFFF9500))
        }
        CardBlock("热力图（近一年 · 按场次）") {
            Heatmap(dayCounts = viewModel.heatmap)
        }
    }
}

@Composable
private fun CardBlock(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(LocalIntimaPalette.current.card, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        M3Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF444444))
        Box(Modifier.padding(top = 12.dp)) { content() }
    }
}
