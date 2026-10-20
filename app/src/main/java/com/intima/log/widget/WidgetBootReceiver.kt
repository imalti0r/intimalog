package com.intima.log.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.launch

/** 重启后刷新小组件 */
class WidgetBootReceiver : BroadcastReceiver() {
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Main)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, IntimaWidgetReceiver::class.java))
            if (ids.isNotEmpty()) {
                scope.launch { IntimaWidget().updateAll(context) }
            }
        }
    }
}
