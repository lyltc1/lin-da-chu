package com.lyltc1.lindachu.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** 做菜记录，用于统计"做过几次"与回顾 */
@Entity(
    tableName = "cook_log",
    indices = [Index("dishId")]
)
data class CookLogEntity(
    @PrimaryKey val id: String,
    val dishId: String,
    val cookedAt: Long = System.currentTimeMillis(),
    val note: String = "",
    val rating: Int = 0
)
