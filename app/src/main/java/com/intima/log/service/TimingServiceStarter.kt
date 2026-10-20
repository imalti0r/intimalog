package com.intima.log.service

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat

/** 兼容各 API 等级的前台服务启动入口 */
object TimingServiceStarter {
    fun start(context: Context) {
        runCatching {
            val intent = Intent(context, TimingService::class.java)
            if (Build.VERSION.SDK_INT >= 26) {
                ContextCompat.startForegroundService(context, intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
