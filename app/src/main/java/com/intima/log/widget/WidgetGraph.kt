package com.intima.log.widget

import android.content.Context
import com.intima.log.data.repo.ProfileRepository
import com.intima.log.data.repo.RecordRepository
import dagger.hilt.android.EntryPointAccessors
import java.time.LocalDate
import java.time.ZoneId

/** 非 Compose 上下文取 Hilt 依赖的入口 */
object WidgetGraph {
    @dagger.hilt.EntryPoint
    @dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
    interface WidgetEntryPoint {
        fun recordRepository(): RecordRepository
        fun profileRepository(): ProfileRepository
    }

    fun recordRepository(context: Context): RecordRepository =
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java
        ).recordRepository()

    fun profileRepository(context: Context): ProfileRepository =
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java
        ).profileRepository()

    fun dayStartMillis(): Long {
        val zone = ZoneId.systemDefault()
        return LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
    }
}
