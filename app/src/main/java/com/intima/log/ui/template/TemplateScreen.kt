package com.intima.log.ui.template

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text as M3Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.intima.log.data.db.Partner
import com.intima.log.data.db.SceneTemplate
import com.intima.log.data.repo.PartnerRepository
import com.intima.log.data.repo.TemplateRepository
import com.intima.log.domain.usecase.ApplyTemplateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.intima.log.ui.theme.LocalIntimaPalette

@HiltViewModel
class TemplateViewModel @Inject constructor(
    private val repository: TemplateRepository,
    private val partnerRepository: PartnerRepository,
    private val applyTemplate: ApplyTemplateUseCase,
) : ViewModel() {

    val templates: StateFlow<List<SceneTemplate>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var partners: List<Partner> = emptyList()
        private set

    init {
        viewModelScope.launch { partners = partnerRepository.listActive() }
    }

    fun save(name: String, partnerIds: List<Long>, rating: Int) {
        viewModelScope.launch {
            repository.save(
                SceneTemplate(
                    name = name.ifBlank { "模板" },
                    partnerIdsJson = ApplyTemplateUseCase.encodeIds(partnerIds),
                    rating = rating,
                )
            )
        }
    }

    fun delete(id: Long) = viewModelScope.launch { repository.delete(id) }
}

/** 场景模板管理页 */
@Composable
fun TemplateScreen(viewModel: TemplateViewModel = hiltViewModel()) {
    val templates by viewModel.templates.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(LocalIntimaPalette.current.bg)) {
        M3Text(
            "场景模板",
            fontSize = 28.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp),
        )
        if (templates.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(top = 80.dp), contentAlignment = Alignment.Center) {
                M3Text("保存常用组合，一键套用\n在记录页创建", color = LocalIntimaPalette.current.textHint, fontSize = 14.sp, lineHeight = 22.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(templates, key = { it.id }) { template ->
                    val names = ApplyTemplateUseCase.parseIds(template.partnerIdsJson)
                        .mapNotNull { id -> viewModel.partners.firstOrNull { it.id == id }?.nickname }
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth()
                            .background(LocalIntimaPalette.current.card, RoundedCornerShape(16.dp))
                            .padding(14.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                M3Text(template.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                M3Text(
                                    buildString {
                                        append(if (names.isEmpty()) "未指定参与者" else names.joinToString(" + "))
                                                                                if (template.rating > 0) append(" · ${"★".repeat(template.rating)}")
                                    },
                                    fontSize = 12.sp, color = LocalIntimaPalette.current.textHint,
                                )
                            }
                            M3Text(
                                "删除",
                                color = Color(0xFFE94634), fontSize = 13.sp,
                                modifier = Modifier.clickable { viewModel.delete(template.id) }.padding(6.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
