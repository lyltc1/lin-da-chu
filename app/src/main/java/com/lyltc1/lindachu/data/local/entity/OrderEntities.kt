package com.lyltc1.lindachu.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** 订单状态流转：待接单 → 已接单 → 已完成，或直接取消 */
enum class OrderStatus(val code: Int, val label: String) {
    PENDING(0, "待接单"),
    ACCEPTED(1, "已接单"),
    DONE(2, "已完成"),
    CANCELLED(3, "已取消");

    companion object {
        fun from(code: Int): OrderStatus = entries.firstOrNull { it.code == code } ?: PENDING
    }
}

@Entity(
    tableName = "orders",
    indices = [Index("createdAt"), Index("status")]
)
data class OrderEntity(
    @PrimaryKey val id: String,
    val menuId: String = "",
    val menuTitle: String = "",
    /** 桌号或称呼，朋友随手填 */
    val tableNo: String = "",
    val note: String = "",
    val status: Int = OrderStatus.PENDING.code,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "order_item",
    indices = [Index("orderId")]
)
data class OrderItemEntity(
    @PrimaryKey val id: String,
    val orderId: String,
    val dishId: String = "",
    /**
     * 冗余菜名。
     *
     * 菜单快照之后菜谱可能被改名或删除，订单要留住下单那一刻看到的名字，
     * 否则回顾"上次谁点了什么"时会对不上。
     */
    val dishTitle: String = "",
    val count: Int = 1,
    val note: String = ""
)
