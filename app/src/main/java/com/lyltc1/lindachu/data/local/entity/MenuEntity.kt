package com.lyltc1.lindachu.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 对外发布的菜单快照。
 *
 * 只记录"哪几道菜"。发布时按当时的菜谱内容生成静态页，之后改动菜谱
 * 不会自动改变已发布的页面——需要重新发布才会更新。这样朋友扫到的
 * 菜单内容是稳定的，不会在你改菜谱的瞬间悄悄变化。
 */
@Entity(tableName = "menu")
data class MenuEntity(
    @PrimaryKey val id: String,
    val title: String,
    /** 逗号分隔的菜谱 ID，顺序即菜单展示顺序 */
    val dishIds: String = "",
    /** 显示在菜单顶部的招呼语 */
    val greeting: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
