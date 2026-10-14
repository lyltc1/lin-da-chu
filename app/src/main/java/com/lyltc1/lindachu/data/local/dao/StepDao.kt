package com.lyltc1.lindachu.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.lyltc1.lindachu.data.local.entity.StepEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StepDao {

    @Query("SELECT * FROM step WHERE dishId = :dishId ORDER BY orderIndex")
    fun observeByDish(dishId: String): Flow<List<StepEntity>>

    @Query("SELECT * FROM step WHERE dishId = :dishId ORDER BY orderIndex")
    suspend fun getByDish(dishId: String): List<StepEntity>

    @Upsert
    suspend fun upsertAll(items: List<StepEntity>)

    @Query("DELETE FROM step WHERE dishId = :dishId")
    suspend fun deleteByDish(dishId: String)
}
