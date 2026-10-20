package com.intima.log.ui.record

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.intima.log.data.db.Partner
import com.intima.log.data.db.Place
import com.intima.log.data.repo.PartnerRepository
import com.intima.log.data.repo.ProfileRepository
import com.intima.log.data.repo.RecordDraft
import com.intima.log.domain.usecase.SaveRecordWithPartnersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class RecordEditState(
    val recordId: Long = 0,
    val partners: List<Partner> = emptyList(),
    val selectedIds: Set<Long> = emptySet(),
    val multiPartner: Boolean = true,
    val ongoing: Boolean = false,
        val customProtections: Set<String> = emptySet(), // 自定义保护措施（管理页维护）
    val rating: Int = 0,
    val moodTag: String = "",                        // 情绪（单选）
    val place: Place = Place.NOT_RECORDED,          // 预置地点（单选）
    val customPlaces: Set<String> = emptySet(),      // 自定义地点（多选）
    val positions: List<String> = emptyList(),       // 自定义体位
    val toys: List<String> = emptyList(),            // 自定义玩具
    val knownPositions: List<String> = emptyList(),  // 体位管理页维护的标签
    val knownToys: List<String> = emptyList(),       // 玩具管理页维护的标签
    val knownCustomProtections: List<String> = emptyList(), // 自定义保护措施标签
    val knownCustomPlaces: List<String> = emptyList(),      // 自定义地点标签
    val note: String = "",
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val saved: Boolean = false,
) {
    val startTimeText: String
        get() = Instant.ofEpochMilli(startTime).atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))

    val elapsedText: String
        get() {
            val sec = ((endTime ?: System.currentTimeMillis()) - startTime) / 1000
            return "${sec / 60} 分钟"
        }
}

@HiltViewModel
class RecordEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val saveUseCase: SaveRecordWithPartnersUseCase,
    private val partnerRepository: PartnerRepository,
    private val recordRepository: com.intima.log.data.repo.RecordRepository,
    private val db: com.intima.log.data.db.IntimaDatabase,
    profileRepository: ProfileRepository,
) : ViewModel() {

    private val recordId: Long = savedStateHandle.get<Long>("recordId") ?: 0L

    var state by mutableStateOf(RecordEditState(recordId = recordId))
        private set

    init {
        viewModelScope.launch {
            partnerRepository.observeActive().collect { partners ->
                state = state.copy(partners = partners)
            }
        }
        viewModelScope.launch {
            val profile = profileRepository.profile.first()
            state = state.copy(multiPartner = profile.enableMultiPartner)
        }
        // 管理页维护的体位 / 玩具 / 自定义保护措施 / 自定义地点
        viewModelScope.launch {
            db.tagDao().observeKind("position").collect { list ->
                state = state.copy(knownPositions = list.map { it.name })
            }
        }
        viewModelScope.launch {
            db.tagDao().observeKind("toy").collect { list ->
                state = state.copy(knownToys = list.map { it.name })
            }
        }
        viewModelScope.launch {
            db.tagDao().observeKind("protection").collect { list ->
                state = state.copy(knownCustomProtections = list.map { it.name })
            }
        }
        viewModelScope.launch {
            db.tagDao().observeKind("place").collect { list ->
                state = state.copy(knownCustomPlaces = list.map { it.name })
            }
        }
        if (recordId != 0L) {
            viewModelScope.launch {
                val existing = recordRepository.byId(recordId) ?: return@launch
                state = state.copy(
                    selectedIds = existing.partners.map { it.id }.toSet(),
                    // 旧记录的预置枚举保护措施转成自定义名显示（预置项已从 UI 移除）
                    customProtections = existing.record.customProtections.split(",").filter { it.isNotBlank() }.toSet(),
                    rating = existing.record.rating,
                    moodTag = existing.record.moodTags,
                    // 旧记录的预置地点转成自定义名显示（预置项已从 UI 移除）
                    customPlaces = (
                        (if (existing.record.place != Place.NOT_RECORDED) listOf(existing.record.place.label()) else emptyList()) +
                        existing.record.customPlaces.split(",").filter { it.isNotBlank() }
                        ).distinct().toSet(),
                    positions = existing.record.positions.split(",").filter { it.isNotBlank() },
                    toys = existing.record.toys.split(",").filter { it.isNotBlank() },
                    note = existing.record.note,
                    startTime = existing.record.startTime,
                    endTime = existing.record.endTime,
                )
            }
        } else {
            // 从首页「开始」进入时可能已有进行中记录
            viewModelScope.launch {
                val ongoing = recordRepository.ongoingRecord()
                if (ongoing != null) {
                    state = state.copy(
                        recordId = ongoing.id,
                        startTime = ongoing.startTime,
                        ongoing = true,
                    )
                }
            }
        }
    }

    fun onSelectionChange(ids: Set<Long>) {
        state = state.copy(selectedIds = ids)
    }

    /** 勾选/取消一个受管标签；同时写库（首次使用自动入库），记录侧自由文本 */
    fun setMood(m: String) { state = state.copy(moodTag = m) }

    fun toggleCustomPlace(name: String) {
        state = state.copy(customPlaces = state.customPlaces.let { if (name in it) it - name else it + name })
    }

    fun toggleCustomProtection(name: String) {
        state = state.copy(customProtections = state.customProtections.let { if (name in it) it - name else it + name })
    }

    fun setRating(r: Int) { state = state.copy(rating = r) }

    fun togglePosition(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { db.tagDao().insertIgnore("position", name.trim()) }
        state = state.copy(
            positions = if (name in state.positions) state.positions - name else state.positions + name,
        )
    }
    fun toggleToy(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { db.tagDao().insertIgnore("toy", name.trim()) }
        state = state.copy(
            toys = if (name in state.toys) state.toys - name else state.toys + name,
        )
    }
    fun setNote(n: String) { state = state.copy(note = n) }

    /** 调整开始时间（仅未在计时中时可改） */
    fun setStart(hour: Int, minute: Int) {
        if (state.ongoing) return
        val zoned = Instant.ofEpochMilli(state.startTime).atZone(ZoneId.systemDefault())
        val newStart = zoned.withHour(hour).withMinute(minute).toInstant().toEpochMilli()
        state = state.copy(startTime = newStart)
    }

    /** 持续分钟数（基于开始时间推算 endTime） */
    fun setDurationMinutes(min: Int) {
        if (state.ongoing) return
        state = state.copy(endTime = state.startTime + min * 60_000L)
    }

    val durationMinutes: Int
        get() = state.endTime?.let { ((it - state.startTime) / 60_000L).toInt() } ?: 0

    /** 结束进行中的计时并以当前表单保存 */
    fun finishNow() {
        viewModelScope.launch {
            val end = System.currentTimeMillis()
            recordRepository.finishOngoing(end)
            state = state.copy(endTime = end, ongoing = false)
            save()
        }
    }

    fun save() {
        viewModelScope.launch {
            val ids = state.selectedIds.toList()
            saveUseCase(
                RecordDraft(
                    recordId = if (state.ongoing) state.recordId else if (recordId != 0L) recordId else 0,
                    partnerIds = ids,
                    primaryPartnerId = ids.firstOrNull(),
                    startTime = state.startTime,
                    endTime = state.endTime ?: if (state.ongoing) null else System.currentTimeMillis(),
                                        customProtections = state.customProtections.toList(),
                    rating = state.rating,
                    moodTag = state.moodTag,
                    customPlaces = state.customPlaces.toList(),
                    positions = state.positions,
                    toys = state.toys,
                    note = state.note,
                )
            )
            state = state.copy(saved = true)
        }
    }

    fun delete() {
        viewModelScope.launch {
            recordRepository.delete(state.recordId)
            state = state.copy(saved = true)
        }
    }
}
