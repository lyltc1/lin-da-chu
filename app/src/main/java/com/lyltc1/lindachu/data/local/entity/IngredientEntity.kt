package com.lyltc1.lindachu.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** 食材。groupName 用于分组展示：主料 / 辅料 / 调料 */
@Entity(
    tableName = "ingredient",
    indices = [Index("dishId")]
)
data class IngredientEntity(
    @PrimaryKey val id: String,
    val dishId: String,
    val name: String,
    val amount: String = "",
    val unit: String = "",
    val groupName: String = "主料",
    val orderIndex: Int = 0
)
