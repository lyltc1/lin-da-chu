package com.lyltc1.lindachu.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.lyltc1.lindachu.data.local.entity.IngredientEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IngredientDao {

    @Query("SELECT * FROM ingredient WHERE dishId = :dishId ORDER BY orderIndex")
    fun observeByDish(dishId: String): Flow<List<IngredientEntity>>

    @Query("SELECT * FROM ingredient WHERE dishId = :dishId ORDER BY orderIndex")
    suspend fun getByDish(dishId: String): List<IngredientEntity>

    @Upsert
    suspend fun upsertAll(items: List<IngredientEntity>)

    @Query("DELETE FROM ingredient WHERE dishId = :dishId")
    suspend fun deleteByDish(dishId: String)
}
