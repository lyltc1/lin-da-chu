package com.lyltc1.lindachu.data.repository

import androidx.room.withTransaction
import com.lyltc1.lindachu.data.local.AppDatabase
import com.lyltc1.lindachu.data.local.entity.DishEntity
import com.lyltc1.lindachu.data.local.entity.MenuEntity
import com.lyltc1.lindachu.data.local.entity.OrderEntity
import com.lyltc1.lindachu.data.local.entity.OrderItemEntity
import com.lyltc1.lindachu.data.local.entity.OrderStatus
import com.lyltc1.lindachu.data.order.OrderCodec
import com.lyltc1.lindachu.util.encodeStringList
import com.lyltc1.lindachu.util.parseStringList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.UUID

data class OrderWithItems(
    val order: OrderEntity,
    val items: List<OrderItemEntity>
)

/**
 * 菜单与订单的数据入口。
 *
 * 菜单是"发布快照"，只存菜谱 ID 列表；订单则冗余保存菜名——
 * 因为下单之后菜谱可能被改名或删掉，订单要留住朋友当时看到的名字，
 * 否则过几天回顾"谁点了什么"就对不上了。
 */
class MenuRepository(private val db: AppDatabase) {

    private val menuDao = db.menuDao()
    private val orderDao = db.orderDao()
    private val dishDao = db.dishDao()

    fun observeMenus(): Flow<List<MenuEntity>> = menuDao.observeAll()

    fun observeMenu(id: String): Flow<MenuEntity?> = menuDao.observeById(id)

    suspend fun getMenu(id: String): MenuEntity? = menuDao.getById(id)

    /** 保存菜单，dishIds 统一序列化为 JSON 数组，避免调用方各自拼字符串 */
    suspend fun saveMenu(
        title: String,
        greeting: String,
        dishIds: List<String>,
        existingId: String? = null
    ): String {
        val now = System.currentTimeMillis()
        val id = existingId ?: "m-${UUID.randomUUID().toString().take(8)}"
        val menu = MenuEntity(
            id = id,
            title = title.ifBlank { "未命名菜单" },
            dishIds = encodeStringList(dishIds),
            greeting = greeting,
            createdAt = menuDao.getById(id)?.createdAt ?: now,
            updatedAt = now
        )
        menuDao.upsert(menu)
        return id
    }

    suspend fun deleteMenu(id: String) = menuDao.delete(id)

    /** 菜单中的菜谱，保持菜单顺序；已被删除的菜自动跳过 */
    suspend fun getMenuDishes(menu: MenuEntity): List<DishEntity> =
        parseStringList(menu.dishIds)
            .mapNotNull { dishDao.getById(it) }
            .filter { it.deletedAt == null }

    fun observeOrders(): Flow<List<OrderWithItems>> =
        combine(orderDao.observeAll(), orderDao.observeAllItems()) { orders, items ->
            val grouped = items.groupBy { it.orderId }
            orders.map { OrderWithItems(it, grouped[it.id].orEmpty()) }
        }

    /** 把扫码得到的订单码落库 */
    suspend fun receiveOrder(payload: OrderCodec.Payload): Result<OrderEntity> {
        val menu = menuDao.getById(payload.menuId)
            ?: return Result.failure(IllegalArgumentException("本机没有这份菜单，请先发布菜单再接单"))
        val dishIds = parseStringList(menu.dishIds)
        val orderId = "o-${UUID.randomUUID().toString().take(8)}"

        val items = payload.picks.mapNotNull { pick ->
            val dish = dishIds.getOrNull(pick.index)?.let { dishDao.getById(it) }
                ?: return@mapNotNull null
            OrderItemEntity(
                id = UUID.randomUUID().toString(),
                orderId = orderId,
                dishId = dish.id,
                dishTitle = dish.title,
                count = pick.count.coerceIn(1, 99)
            )
        }
        if (items.isEmpty()) return Result.failure(IllegalArgumentException("订单里没有有效菜品"))

        val now = System.currentTimeMillis()
        val order = OrderEntity(
            id = orderId,
            menuId = menu.id,
            menuTitle = menu.title,
            tableNo = payload.tableNo,
            note = payload.note,
            createdAt = now,
            updatedAt = now
        )
        db.withTransaction {
            orderDao.insert(order)
            orderDao.insertItems(items)
        }
        return Result.success(order)
    }

    suspend fun updateOrderStatus(id: String, status: OrderStatus) {
        orderDao.updateStatus(id, status.code, System.currentTimeMillis())
    }

    suspend fun deleteOrder(id: String) {
        db.withTransaction {
            orderDao.deleteItems(id)
            orderDao.delete(id)
        }
    }
}
