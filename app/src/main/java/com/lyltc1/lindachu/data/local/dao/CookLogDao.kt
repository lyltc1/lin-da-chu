package com.lyltc1.lindachu.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.lyltc1.lindachu.data.local.entity.CookLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CookLogDao {

    @Query("SELECT * FROM cook_log WHERE dishId = :dishId ORDER BY cookedAt DESC")
    fun observeByDish(dishId: String): Flow<List<CookLogEntity>>

    @Upsert
    suspend fun upsert(log: CookLogEntity)

    @Query("DELETE FROM cook_log WHERE dishId = :dishId")
    suspend fun deleteByDish(dishId: String)
}
