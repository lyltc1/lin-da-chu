package com.lyltc1.lindachu.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.lyltc1.lindachu.data.local.entity.MenuEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MenuDao {

    @Query("SELECT * FROM menu ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<MenuEntity>>

    @Query("SELECT * FROM menu WHERE id = :id")
    fun observeById(id: String): Flow<MenuEntity?>

    @Query("SELECT * FROM menu WHERE id = :id")
    suspend fun getById(id: String): MenuEntity?

    @Upsert
    suspend fun upsert(menu: MenuEntity)

    @Query("DELETE FROM menu WHERE id = :id")
    suspend fun delete(id: String)
}
