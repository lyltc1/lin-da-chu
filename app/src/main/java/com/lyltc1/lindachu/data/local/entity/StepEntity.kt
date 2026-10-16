package com.lyltc1.lindachu.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** 制作步骤。imagePaths 存 JSON 数组字符串，如 ["images/x/s1.jpg"] */
@Entity(
    tableName = "step",
    indices = [Index("dishId")]
)
data class StepEntity(
    @PrimaryKey val id: String,
    val dishId: String,
    val orderIndex: Int,
    val text: String,
    /** 步骤小结，如 "择菜""热油"，用作一眼扫读的小标题；为空则只展示正文 */
    val summary: String = "",
    val imagePaths: String = "[]",
    /** 该步骤建议计时秒数，为空表示不需计时 */
    val timerSec: Int? = null
)
