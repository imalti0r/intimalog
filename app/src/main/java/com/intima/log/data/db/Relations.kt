package com.intima.log.data.db

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

/** 记录 + 其全部参与者（按 joinOrder 排序） */
data class SexRecordWithPartners(
    @Embedded val record: SexRecord,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = RecordPartnerCrossRef::class,
            parentColumn = "recordId",
            entityColumn = "partnerId"
        )
    )
    val partners: List<Partner>
)

/** 伴侣 + 参与统计 */
data class PartnerWithStats(
    @Embedded val partner: Partner,
    val sessionCount: Int,
    val totalDurationSeconds: Long,
    val lastTime: Long?,
)
