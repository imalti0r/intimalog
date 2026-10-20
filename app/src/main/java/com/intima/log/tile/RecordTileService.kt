package com.intima.log.tile

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.intima.log.data.repo.RecordRepository
import com.intima.log.widget.WidgetGraph
import com.intima.log.widget.IntimaWidget
import androidx.glance.appwidget.updateAll

/**
 * 快捷设置磁贴：一键开始/结束记录。
 * 状态：无进行中=inactive「开始记录」；进行中=active「结束记录」。
 */
class RecordTileService : TileService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private fun recordRepository(): RecordRepository = WidgetGraph.recordRepository(applicationContext)

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        scope.launch {
            val repo = recordRepository()
            val ongoing = repo.ongoingRecord()
            if (ongoing != null) {
                repo.finishOngoing()
            } else {
                repo.startOngoing()
            }
            IntimaWidget().updateAll(applicationContext)
            updateTile()
        }
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        scope.launch {
            val ongoing = recordRepository().ongoingRecord()
            tile.state = if (ongoing != null) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            tile.subtitle = if (ongoing != null) "计时进行中" else "开始记录"
            tile.updateTile()
        }
    }
}
