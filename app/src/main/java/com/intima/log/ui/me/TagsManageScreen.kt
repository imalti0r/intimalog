package com.intima.log.ui.me

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
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
import com.intima.log.data.db.IntimaDatabase
import com.intima.log.ui.theme.LocalIntimaPalette
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 自定义标签（体位 / 玩具共用）。存储：单表 tags(kind, name)。
 * kind: "position" | "toy"
 */
data class CustomTag(val id: Long, val name: String)

@HiltViewModel
class TagsViewModel @Inject constructor(
    private val db: IntimaDatabase,
) : ViewModel() {

    private val _positions = MutableStateFlow<List<CustomTag>>(emptyList())
    val positions = _positions.asStateFlow()

    private val _toys = MutableStateFlow<List<CustomTag>>(emptyList())
    val toys = _toys.asStateFlow()

    private val _customProtections = MutableStateFlow<List<CustomTag>>(emptyList())
    val customProtections = _customProtections.asStateFlow()

    private val _customPlaces = MutableStateFlow<List<CustomTag>>(emptyList())
    val customPlaces = _customPlaces.asStateFlow()

    init {
        viewModelScope.launch {
            db.tagDao().observeKind("position").collect { list ->
                _positions.value = list.map { CustomTag(it.id, it.name) }
            }
        }
        viewModelScope.launch {
            db.tagDao().observeKind("toy").collect { list ->
                _toys.value = list.map { CustomTag(it.id, it.name) }
            }
        }
        viewModelScope.launch {
            db.tagDao().observeKind("protection").collect { list ->
                _customProtections.value = list.map { CustomTag(it.id, it.name) }
            }
        }
        viewModelScope.launch {
            db.tagDao().observeKind("place").collect { list ->
                _customPlaces.value = list.map { CustomTag(it.id, it.name) }
            }
        }
    }

    fun add(kind: String, name: String) {
        val n = name.trim()
        if (n.isEmpty()) return
        viewModelScope.launch { db.tagDao().insertIgnore(kind, n) }
    }

    fun rename(kind: String, id: Long, newName: String) {
        val n = newName.trim()
        if (n.isEmpty()) return
        viewModelScope.launch { db.tagDao().rename(id, n) }
    }

    fun delete(kind: String, id: Long) = viewModelScope.launch { db.tagDao().delete(id) }
}

/** 「我的」页里的自定义标签管理页（体位 / 玩具共用 UI） */
@Composable
fun TagsManageScreen(
    title: String,
    kind: String,
    onBack: () -> Unit,
    viewModel: TagsViewModel = hiltViewModel(),
) {
    val list by when (kind) {
        "position" -> viewModel.positions
        "protection" -> viewModel.customProtections
        "place" -> viewModel.customPlaces
        else -> viewModel.toys
    }.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<CustomTag?>(null) }
    var deleteConfirm by remember { mutableStateOf<CustomTag?>(null) }
    val palette = LocalIntimaPalette.current

    Column(modifier = Modifier.fillMaxSize().background(palette.bg)) {
        // 顶部
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            M3Text(title, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = palette.text)
            androidx.compose.material3.IconButton(onClick = { showAdd = true }) {
                Icon(Icons.Rounded.Add, "新增", tint = palette.text, modifier = Modifier.size(26.dp))
            }
        }

        if (list.isEmpty()) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.fillMaxWidth().padding(top = 80.dp),
                contentAlignment = Alignment.Center,
            ) {
                M3Text("还没有条目\n点击右上角 + 新增", color = palette.textHint, fontSize = 14.sp, lineHeight = 22.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(list, key = { it.id }) { tag ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(palette.card)
                            .clickable { editing = tag }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        M3Text(tag.name, fontSize = 16.sp, color = palette.text, modifier = Modifier.weight(1f))
                        Icon(
                            Icons.Rounded.Edit, "编辑", tint = palette.textHint,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(16.dp))
                        Icon(
                            Icons.Rounded.Delete, "删除", tint = Color(0xFFE5484D),
                            modifier = Modifier
                                .size(20.dp)
                                .clickable { deleteConfirm = tag },
                        )
                    }
                }
            }
        }
    }

    if (showAdd) {
        TagEditDialog(title = "新增", initial = null, onDismiss = { showAdd = false }) { name ->
            viewModel.add(kind, name); showAdd = false
        }
    }
    editing?.let { tag ->
        TagEditDialog(title = "修改", initial = tag.name, onDismiss = { editing = null }) { name ->
            viewModel.rename(kind, tag.id, name); editing = null
        }
    }
    deleteConfirm?.let { tag ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { deleteConfirm = null },
            title = { M3Text("删除「${tag.name}」？") },
            text = { M3Text("历史记录中的引用保持不变，仅从待选列表移除。") },
            confirmButton = {
                M3Text(
                    "删除", color = Color(0xFFE5484D), fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { viewModel.delete(kind, tag.id); deleteConfirm = null }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            },
            dismissButton = {
                M3Text(
                    "取消", color = palette.textHint,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { deleteConfirm = null }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            },
        )
    }
}

@Composable
private fun TagEditDialog(
    title: String,
    initial: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf(initial ?: "") }
    val palette = LocalIntimaPalette.current
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.card, RoundedCornerShape(24.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            M3Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = palette.text)
            androidx.compose.material3.OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { M3Text("名称") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                M3Text(
                    "取消", color = palette.textHint,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
                Spacer(Modifier.width(8.dp))
                M3Text(
                    "保存", color = Color(0xFF3482FF), fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = name.isNotBlank()) { onConfirm(name) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }
}
