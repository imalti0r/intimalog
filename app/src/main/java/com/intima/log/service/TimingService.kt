package com.intima.log.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.intima.log.R
import com.intima.log.data.repo.RecordRepository
import com.intima.log.ui.MainActivity
import com.intima.log.widget.IntimaWidget
import com.intima.log.widget.WidgetGraph
import dagger.hilt.android.AndroidEntryPoint
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 计时前台服务：进行中记录的常驻通知（每分钟更新一次时长），带「结束」按钮。
 */
@AndroidEntryPoint
class TimingService : Service() {

    @Inject lateinit var recordRepository: RecordRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var ticker: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                scope.launch {
                    recordRepository.finishOngoing()
                    IntimaWidget().updateAll(applicationContext)
                    stopSelf()
                }
                return START_NOT_STICKY
            }
            else -> {
                startForeground(NOTIFICATION_ID, buildNotification(0))
                ticker?.cancel()
                ticker = scope.launch {
                    while (isActive) {
                        delay(60_000)
                        val ongoing = recordRepository.ongoingRecord()
                        if (ongoing == null) {
                            stopSelf()
                            break
                        }
                        val minutes = (System.currentTimeMillis() - ongoing.startTime) / 60_000
                        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                        nm.notify(NOTIFICATION_ID, buildNotification(minutes))
                    }
                }
            }
        }
        return START_STICKY
    }
    private fun buildNotification(minutes: Long): Notification {
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stop = PendingIntent.getService(
            this, 1,
            Intent(this, TimingService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_tile_record)
            .setContentTitle("记录进行中")
            .setContentText("已计时 ${minutes} 分钟")
            .setContentIntent(open)
            .addAction(0, "结束", stop)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "计时", NotificationManager.IMPORTANCE_LOW)
        )
    }

    override fun onDestroy() {
        ticker?.cancel()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "timing"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.intima.log.STOP_TIMING"
    }
}
