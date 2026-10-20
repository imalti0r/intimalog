package com.intima.log.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * 记录↔伴侣 多对多关联。
 * 同一场记录不允许重复添加同一伴侣（复合主键保证）。
 */
@Entity(
    tableName = "record_partner_cross_ref",
    primaryKeys = ["recordId", "partnerId"],
    foreignKeys = [
        ForeignKey(
            entity = SexRecord::class,
            parentColumns = ["id"],
            childColumns = ["recordId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Partner::class,
            parentColumns = ["id"],
            childColumns = ["partnerId"],
            onDelete = ForeignKey.RESTRICT   // 有引用的伴侣只允许软删除
        )
    ],
    indices = [Index("partnerId")]
)
data class RecordPartnerCrossRef(
    val recordId: Long,
    val partnerId: Long,
    val joinOrder: Int = 0,               // 参与顺序，0 起
    val isPrimary: Boolean = false,       // 主参与者，每场最多 1 个
)
