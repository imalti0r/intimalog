package com.intima.log.ui.partner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material3.Icon
import androidx.compose.material3.Text as M3Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.intima.log.data.db.Gender
import com.intima.log.data.db.Partner
import com.intima.log.data.repo.PartnerRepository
import com.intima.log.ui.components.PartnerAvatar
import com.intima.log.ui.theme.avatarColor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.intima.log.ui.theme.LocalIntimaPalette

@HiltViewModel
class PartnerListViewModel @Inject constructor(
    private val repository: PartnerRepository,
) : ViewModel() {
    /** 未删除的全部伴侣（含隐藏，UI 中隐藏的半透明展示） */
    val partners: StateFlow<List<Partner>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addPartner(nickname: String, emoji: String, gender: Gender, isRegular: Boolean) {
        viewModelScope.launch {
            repository.save(
                Partner(
                    nickname = nickname.ifBlank { "未命名" },
                    emoji = emoji,
                    gender = gender,
                    colorIndex = (partners.value.size) % 8,
                    isRegular = isRegular,
                )
            )
        }
    }

    /** 修改信息 */
    fun updatePartner(partner: Partner, nickname: String, emoji: String, gender: Gender, isRegular: Boolean) {
        viewModelScope.launch {
            repository.save(
                partner.copy(
                    nickname = nickname.ifBlank { partner.nickname },
                    emoji = emoji,
                    gender = gender,
                    isRegular = isRegular,
                )
            )
        }
    }

    /** 删除（软删除，可恢复） */
    fun delete(id: Long) = viewModelScope.launch { repository.archive(id) }

    /** 恢复 */
    fun restore(id: Long) = viewModelScope.launch { repository.unarchive(id) }

    /** 彻底删除：物理删除伴侣行，历史记录的关联改为匿名占位（"已删除"） */
    fun purge(id: Long) = viewModelScope.launch { repository.purge(id) }

    /** 隐藏 / 取消隐藏 */
    fun setHidden(id: Long, hidden: Boolean) = viewModelScope.launch {
        if (hidden) repository.hide(id) else repository.unhide(id)
    }

    /** 重名检查（忽略大小写与首尾空格；排除自身） */
    fun findDuplicate(nickname: String, excludeId: Long = 0): Partner? {
        val q = nickname.trim()
        if (q.isEmpty()) return null
        return partners.value.firstOrNull {
            it.id != excludeId && it.id != PartnerRepository.DELETED_PLACEHOLDER_ID &&
                it.nickname.trim().equals(q, ignoreCase = true)
        }
    }
}

/** 伴侣列表 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PartnerListScreen(
    onOpenCombinations: () -> Unit,
    viewModel: PartnerListViewModel = hiltViewModel(),
) {
    val partners by viewModel.partners.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Partner?>(null) }
    var menuFor by remember { mutableStateOf<Long?>(null) }
    var deleteConfirm by remember { mutableStateOf<Partner?>(null) }
    var purgeConfirm by remember { mutableStateOf<Partner?>(null) }

    val active = partners.filter { !it.archived && it.id != PartnerRepository.DELETED_PLACEHOLDER_ID }
    val archived = partners.filter { it.archived && it.id != PartnerRepository.DELETED_PLACEHOLDER_ID }

    Column(modifier = Modifier.fillMaxSize().background(LocalIntimaPalette.current.bg)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            M3Text("伴侣", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // 组合视图入口
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0x0D000000))
                        .clickable(onClick = onOpenCombinations),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.GridView, "组合视图", tint = LocalIntimaPalette.current.text, modifier = Modifier.size(20.dp))
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0x0D000000))
                        .clickable { showAdd = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Add, "新增", tint = LocalIntimaPalette.current.text, modifier = Modifier.size(22.dp))
                }
            }
        }

        if (active.isEmpty() && archived.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(top = 80.dp), contentAlignment = Alignment.Center) {
                M3Text("还没有添加伴侣\n点击右上角 + 新增", color = LocalIntimaPalette.current.textHint, fontSize = 14.sp, lineHeight = 22.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(active, key = { it.id }) { partner ->
                    PartnerCard(
                        partner = partner,
                        menuOpen = menuFor == partner.id,
                        onMenuToggle = { menuFor = if (menuFor == partner.id) null else partner.id },
                        onEdit = { editing = partner; menuFor = null },
                        onHide = { viewModel.setHidden(partner.id, true); menuFor = null },
                        onUnhide = { viewModel.setHidden(partner.id, false); menuFor = null },
                        onDelete = { deleteConfirm = partner; menuFor = null },
                    )
                }
                // 已删除区（无活跃伴侣时也显示）
                if (archived.isNotEmpty()) {
                    item {
                        M3Text(
                            "已删除（点击恢复）", fontSize = 13.sp,
                            color = LocalIntimaPalette.current.textHint,
                            modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                        )
                    }
                    items(archived, key = { "a${it.id}" }) { partner ->
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(LocalIntimaPalette.current.card.copy(alpha = 0.5f))
                                .combinedClickable(
                                    onClick = { viewModel.restore(partner.id) },
                                    onLongClick = { purgeConfirm = partner },
                                )
                                .padding(14.dp),                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            PartnerAvatar(partner = partner, size = 44.dp)
                            Column(Modifier.weight(1f)) {
                                M3Text(
                                    partner.nickname, fontSize = 16.sp,
                                    color = LocalIntimaPalette.current.textHint,
                                )
                                M3Text("点击恢复 · 长按彻底删除", fontSize = 12.sp, color = LocalIntimaPalette.current.textHint)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddPartnerDialog(
            onDismiss = { showAdd = false },
            checkDuplicate = { name -> viewModel.findDuplicate(name) },
            onConfirm = { name, emoji, gender, regular ->
                viewModel.addPartner(name, emoji, gender, regular)
                showAdd = false
            },
        )
    }

    editing?.let { partner ->
        AddPartnerDialog(
            initial = partner,
            onDismiss = { editing = null },
            checkDuplicate = { name -> viewModel.findDuplicate(name, excludeId = partner.id) },
            onConfirm = { name, emoji, gender, regular ->
                viewModel.updatePartner(partner, name, emoji, gender, regular)
                editing = null
            },
        )
    }

    deleteConfirm?.let { partner ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { deleteConfirm = null },
            title = { M3Text("删除「${partner.nickname}」？") },
            text = { M3Text("删除后不再出现在列表与记录选择器；历史记录保留，可在列表底部点击恢复。") },
            confirmButton = {
                M3Text(
                    "删除", color = Color(0xFFE5484D), fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { viewModel.delete(partner.id); deleteConfirm = null }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            },
            dismissButton = {
                M3Text(
                    "取消", color = LocalIntimaPalette.current.textHint,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { deleteConfirm = null }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            },
        )
    }

    purgeConfirm?.let { partner ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { purgeConfirm = null },
            title = { M3Text("彻底删除「${partner.nickname}」？") },
            text = { M3Text("此操作不可恢复。历史记录中将显示为「已删除」，统计数字保持不变。") },
            confirmButton = {
                M3Text(
                    "彻底删除", color = Color(0xFFE5484D), fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { viewModel.purge(partner.id); purgeConfirm = null }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            },
            dismissButton = {
                M3Text(
                    "取消", color = LocalIntimaPalette.current.textHint,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { purgeConfirm = null }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            },
        )
    }
}

/** 单个伴侣卡片 + 「···」操作菜单（编辑/隐藏/删除） */
@Composable
private fun PartnerCard(
    partner: Partner,
    menuOpen: Boolean,
    onMenuToggle: () -> Unit,
    onEdit: () -> Unit,
    onHide: () -> Unit,
    onUnhide: () -> Unit,
    onDelete: () -> Unit,
) {
    val palette = LocalIntimaPalette.current
    Box {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(palette.card.copy(alpha = if (partner.hidden) 0.45f else 1f))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PartnerAvatar(partner = partner, size = 44.dp)
            Column(Modifier.weight(1f)) {
                M3Text(
                    partner.nickname, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                    color = palette.text,
                )
                M3Text(
                    buildList {
                        if (partner.hidden) add("已隐藏")
                        add(partner.alias ?: if (partner.isRegular) "固定伴侣" else "")
                    }.filter { it.isNotBlank() }.joinToString(" · "),
                    fontSize = 12.sp, color = palette.textHint,
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onMenuToggle)
                    .padding(6.dp),
            ) {
                M3Text("⋯", fontSize = 18.sp, color = palette.textHint)
                // 菜单锚定在 ⋯ 按钮上，悬浮弹出（不挤压其它卡片）
                androidx.compose.material3.DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = onMenuToggle,
                ) {
                    androidx.compose.material3.DropdownMenuItem(
                        text = { M3Text("修改信息") },
                        onClick = { onMenuToggle(); onEdit() },
                    )
                    if (partner.hidden) {
                        androidx.compose.material3.DropdownMenuItem(
                            text = { M3Text("取消隐藏") },
                            onClick = { onMenuToggle(); onUnhide() },
                        )
                    } else {
                        androidx.compose.material3.DropdownMenuItem(
                            text = { M3Text("隐藏") },
                            onClick = { onMenuToggle(); onHide() },
                        )
                    }
                    androidx.compose.material3.DropdownMenuItem(
                        text = { M3Text("删除", color = Color(0xFFE5484D)) },
                        onClick = { onMenuToggle(); onDelete() },
                    )
                }
            }
        }
    }
}

@Composable
private fun PartnerMenuItem(text: String, onClick: () -> Unit, danger: Boolean = false) {
    M3Text(
        text,
        fontSize = 14.sp,
        color = if (danger) Color(0xFFE5484D) else LocalIntimaPalette.current.text,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
    )
}

@Composable
private fun AddPartnerDialog(
    initial: Partner? = null,
    onDismiss: () -> Unit,
    checkDuplicate: (String) -> Partner?,
    onConfirm: (String, String, Gender, Boolean) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.nickname ?: "") }
    var emoji by remember { mutableStateOf(initial?.emoji ?: "🙂") }
    var gender by remember { mutableStateOf(initial?.gender ?: Gender.UNDISCLOSED) }
    var regular by remember { mutableStateOf(initial?.isRegular ?: true) }
    var duplicateOf by remember { mutableStateOf<Partner?>(null) }

    val emojis = listOf("🙂", "😊", "😘", "🥰", "😎", "🤗", "🐱", "🌸", "⭐", "🌙", "🔥", "💫")

    // 重名时先弹确认
    duplicateOf?.let { dup ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { duplicateOf = null },
            title = { M3Text("已有同名伴侣") },
            text = { M3Text("列表中已存在「${dup.nickname}」，仍要保存一个同名的新伴侣吗？") },
            confirmButton = {
                M3Text(
                    "仍要保存", color = Color(0xFFE5484D), fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            duplicateOf = null
                            onConfirm(name, emoji, gender, regular)
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            },
            dismissButton = {
                M3Text(
                    "返回修改", color = LocalIntimaPalette.current.textHint,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { duplicateOf = null }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            },
        )
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(LocalIntimaPalette.current.card, RoundedCornerShape(24.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            M3Text(if (initial == null) "新增伴侣" else "修改信息", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            androidx.compose.material3.OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    duplicateOf = null
                },
                label = { M3Text("昵称（必填）") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                supportingText = {
                    // 实时重名提示（不阻塞输入）
                    if (initial == null && name.isNotBlank()) {
                        checkDuplicate(name)?.let { dup ->
                            M3Text(
                                "⚠ 已有同名伴侣「${dup.nickname}」，保存时会再次确认",
                                fontSize = 12.sp, color = Color(0xFFE9A23B),
                            )
                        }
                    }
                },
            )
            // Emoji 选择
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                emojis.take(8).forEach { e ->
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(if (emoji == e) avatarColor(1) else LocalIntimaPalette.current.chipBg, CircleShape)
                            .clickable { emoji = e },
                        contentAlignment = Alignment.Center,
                    ) { M3Text(e, fontSize = 18.sp) }
                }
            }
            // 性别
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(Gender.MALE to "男", Gender.FEMALE to "女", Gender.OTHER to "其他", Gender.UNDISCLOSED to "不透露").forEach { (g, label) ->
                    SelectablePill(label, gender == g) { gender = g }
                }
            }
            // 固定伴侣
            Row(verticalAlignment = Alignment.CenterVertically) {
                M3Text("固定伴侣", fontSize = 14.sp)
                Spacer(Modifier.weight(1f))
                top.yukonga.miuix.kmp.basic.Switch(checked = regular, onCheckedChange = { regular = it })
            }
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                M3Text(
                    "取消", fontSize = 15.sp, color = LocalIntimaPalette.current.textHint,
                    modifier = Modifier.clickable(onClick = onDismiss).padding(8.dp),
                )
                M3Text(
                    "保存", fontSize = 15.sp, color = if (name.isNotBlank()) Color(0xFF3482FF) else LocalIntimaPalette.current.textHint, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(enabled = name.isNotBlank()) {
                        // 重名校验：命中则弹确认，由用户决定是否仍要保存
                        val dup = checkDuplicate(name)
                        if (dup != null && initial == null) {
                            duplicateOf = dup
                        } else {
                            onConfirm(name, emoji, gender, regular)
                        }
                    }.padding(8.dp),
                )
            }
        }
    }
}

@Composable
fun SelectablePill(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(if (selected) Color(0xFF3482FF) else LocalIntimaPalette.current.chipBg, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        M3Text(text, color = if (selected) Color.White else LocalIntimaPalette.current.text, fontSize = 13.sp)
    }
}
