package com.lyltc1.lindachu.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lyltc1.lindachu.data.local.entity.OrderEntity
import com.lyltc1.lindachu.data.local.entity.OrderItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {

    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE status = :status ORDER BY createdAt DESC")
    fun observeByStatus(status: Int): Flow<List<OrderEntity>>

    @Query("SELECT * FROM order_item WHERE orderId = :orderId")
    fun observeItems(orderId: String): Flow<List<OrderItemEntity>>

    /** 全量订单项（订单列表页一次性拼装，避免 N 次查询） */
    @Query("SELECT * FROM order_item ORDER BY orderId")
    fun observeAllItems(): Flow<List<OrderItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(order: OrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<OrderItemEntity>)

    @Query("UPDATE orders SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: Int, updatedAt: Long)

    @Query("DELETE FROM orders WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM order_item WHERE orderId = :orderId")
    suspend fun deleteItems(orderId: String)
}
