package com.lyltc1.lindachu.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.lyltc1.lindachu.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM category ORDER BY sortOrder, name")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Upsert
    suspend fun upsert(category: CategoryEntity)

    @Upsert
    suspend fun upsertAll(categories: List<CategoryEntity>)

    @Query("DELETE FROM category WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT COUNT(*) FROM category")
    suspend fun count(): Int
}
