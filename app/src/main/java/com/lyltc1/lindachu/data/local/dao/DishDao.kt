package com.lyltc1.lindachu.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.lyltc1.lindachu.data.local.entity.DishEntity
import com.lyltc1.lindachu.data.local.entity.DishWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface DishDao {

    @Query("SELECT * FROM dish WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<DishEntity>>

    @Query(
        "SELECT * FROM dish WHERE deletedAt IS NULL AND categoryId = :categoryId " +
            "ORDER BY updatedAt DESC"
    )
    fun observeByCategory(categoryId: String): Flow<List<DishEntity>>

    @Transaction
    @Query("SELECT * FROM dish WHERE id = :id")
    fun observeWithDetails(id: String): Flow<DishWithDetails?>

    /**
     * 全字段模糊搜索：菜名、标签、备注、食材名、步骤正文。
     * 个人菜谱量级在数百条以内，LIKE 足够且对中文准确，无需引入分词方案。
     */
    @Query(
        "SELECT DISTINCT dish.* FROM dish " +
            "LEFT JOIN ingredient ON ingredient.dishId = dish.id " +
            "LEFT JOIN step ON step.dishId = dish.id " +
            "WHERE dish.deletedAt IS NULL AND (" +
            "dish.title LIKE '%' || :query || '%' " +
            "OR dish.tags LIKE '%' || :query || '%' " +
            "OR dish.notes LIKE '%' || :query || '%' " +
            "OR ingredient.name LIKE '%' || :query || '%' " +
            "OR step.text LIKE '%' || :query || '%') " +
            "ORDER BY dish.updatedAt DESC"
    )
    fun search(query: String): Flow<List<DishEntity>>

    @Upsert
    suspend fun upsert(dish: DishEntity)

    @Upsert
    suspend fun upsertAll(dishes: List<DishEntity>)

    @Query("UPDATE dish SET deletedAt = :at, updatedAt = :at WHERE id = :id")
    suspend fun softDelete(id: String, at: Long = System.currentTimeMillis())

    /** 清理超过保留期的墓碑，默认保留 90 天 */
    @Query("DELETE FROM dish WHERE deletedAt IS NOT NULL AND deletedAt < :before")
    suspend fun purgeDeleted(before: Long)

    @Query("SELECT COUNT(*) FROM dish WHERE deletedAt IS NULL")
    suspend fun countActive(): Int
}
