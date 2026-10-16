package com.lyltc1.lindachu.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 一道菜。
 *
 * 时间统一用 epoch millis 存储，便于与 GitHub 同步时做增量比对；
 * 删除采用软删除（deletedAt），配合同步的墓碑机制避免误删扩散。
 */
@Entity(
    tableName = "dish",
    indices = [Index("categoryId"), Index("updatedAt"), Index("title")]
)
data class DishEntity(
    @PrimaryKey val id: String,
    val title: String,
    /** 封面图相对路径，如 images/<dishId>/cover.jpg */
    val coverPath: String? = null,
    val categoryId: String? = null,
    /** 逗号分隔的标签，如 "快手,家常" */
    val tags: String = "",
    /** 逗号分隔的别名，如 "手撕包菜,炒包菜"；只参与搜索，界面不展示 */
    val aliases: String = "",
    val durationMin: Int = 0,
    val notes: String = "",
    /** 对这道菜的整体评价，如 "简单清爽，色泽翠绿" */
    val review: String = "",
    /** 评分 0–5，0 表示未评分 */
    val rating: Int = 0,
    val cookCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    /** 非空表示已删除 */
    val deletedAt: Long? = null
)
