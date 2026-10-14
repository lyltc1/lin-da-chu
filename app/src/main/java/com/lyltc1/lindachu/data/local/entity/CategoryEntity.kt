package com.lyltc1.lindachu.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 菜谱分类，如家常菜、汤羹、烘焙 */
@Entity(tableName = "category")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val sortOrder: Int = 0,
    val colorHex: String = "#FF7043"
)
