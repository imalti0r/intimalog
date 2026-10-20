package com.intima.log.ui.partner

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text as M3Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.intima.log.data.db.Partner
import com.intima.log.data.repo.PartnerRepository
import com.intima.log.domain.usecase.CombinationRank
import com.intima.log.domain.usecase.GetCombinationStatsUseCase
import com.intima.log.ui.components.AvatarGroup
import com.intima.log.ui.components.PartnerAvatar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.intima.log.ui.theme.LocalIntimaPalette

@HiltViewModel
class CombinationViewModel @Inject constructor(
    private val stats: GetCombinationStatsUseCase,
    private val partnerRepository: PartnerRepository,
    private val recordRepository: com.intima.log.data.repo.RecordRepository,
) : ViewModel() {

    var combinations by mutableStateOf<List<CombinationRank>>(emptyList())
        private set
    var partnerMap by mutableStateOf<Map<Long, Partner>>(emptyMap())
        private set

    init {
        viewModelScope.launch {
            partnerMap = partnerRepository.listActive().associateBy { it.id }
        }
        load()
    }

    fun load(partnerFilter: Long? = null) {
        viewModelScope.launch {
            combinations = if (partnerFilter != null) {
                stats.forPartner(partnerFilter, topN = 5)
            } else {
                stats()
            }
        }
    }

    suspend fun exactRecords(partnerIds: List<Long>): List<com.intima.log.data.db.SexRecordWithPartners> =
        recordRepository.recordsWithExactPartners(partnerIds)
}

/** 组合视图列表：所有出现过的搭档组合（≥2 人），按场次降序 */
@Composable
fun CombinationListScreen(
    onOpen: (List<Long>) -> Unit,
    viewModel: CombinationViewModel = hiltViewModel(),
) {
    Column(modifier = Modifier.fillMaxSize().background(LocalIntimaPalette.current.bg)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowBack, "返回",
                modifier = Modifier.size(24.dp).clickable { /* nav pop */ },
            )
            M3Text("组合视图", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        if (viewModel.combinations.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(top = 80.dp), contentAlignment = Alignment.Center) {
                M3Text("还没有多人组合记录", color = LocalIntimaPalette.current.textHint, fontSize = 14.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(viewModel.combinations, key = { it.partnerIds.joinToString(",") }) { combo ->
                    val partners = combo.partnerIds.mapNotNull { viewModel.partnerMap[it] }
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(LocalIntimaPalette.current.card)
                            .clickable { onOpen(combo.partnerIds) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AvatarGroup(partners = partners, size = 36.dp)
                        Column(Modifier.padding(start = 16.dp)) {
                            M3Text(
                                partners.joinToString(" × ") { it.nickname },
                                fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                            )
                            M3Text(
                                "共 ${combo.sessionCount} 场" +
                                    (combo.avgDurationSeconds?.let { " · 平均 ${it / 60} 分钟" } ?: ""),
                                fontSize = 12.sp, color = LocalIntimaPalette.current.textHint,
                            )
                        }
                    }
                }
            }
        }
    }
}
/** 组合详情：精确匹配该组合的记录时间线 */
@Composable
fun CombinationDetailScreen(
    partnerIds: List<Long>,
    viewModel: CombinationViewModel = hiltViewModel(),
) {
    var records by mutableStateOf<List<com.intima.log.data.db.SexRecordWithPartners>>(emptyList())
    var avgDuration by mutableStateOf<Long?>(null)
    var avgRating by mutableStateOf<Double?>(null)

    LaunchedEffect(partnerIds) {
        records = viewModel.exactRecords(partnerIds)
        val durations = records.mapNotNull { it.record.durationSeconds }
        avgDuration = durations.takeIf { it.isNotEmpty() }?.average()?.toLong()
        val ratings = records.map { it.record.rating }.filter { it > 0 }
        avgRating = ratings.takeIf { it.isNotEmpty() }?.average()
    }

    Column(modifier = Modifier.fillMaxSize().background(LocalIntimaPalette.current.bg)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回", modifier = Modifier.size(24.dp))
            Column {
                M3Text("组合详情", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                M3Text(
                    buildString {
                        append("${records.size} 场")
                        avgDuration?.let { append(" · 平均 ${it / 60} 分钟") }
                        avgRating?.let { append(" · 满意度 %.1f".format(it)) }
                    },
                    fontSize = 13.sp, color = LocalIntimaPalette.current.textHint,
                )
            }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(records, key = { it.record.id }) { item ->
                com.intima.log.ui.home.RecordRow(item = item, strongDisguise = false, onClick = {})
            }
        }
    }
}
