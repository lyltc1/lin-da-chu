package com.lyltc1.lindachu.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.lyltc1.lindachu.data.local.dao.CategoryDao
import com.lyltc1.lindachu.data.local.dao.CookLogDao
import com.lyltc1.lindachu.data.local.dao.DishDao
import com.lyltc1.lindachu.data.local.dao.IngredientDao
import com.lyltc1.lindachu.data.local.dao.StepDao
import com.lyltc1.lindachu.data.local.entity.CategoryEntity
import com.lyltc1.lindachu.data.local.entity.CookLogEntity
import com.lyltc1.lindachu.data.local.entity.DishEntity
import com.lyltc1.lindachu.data.local.entity.IngredientEntity
import com.lyltc1.lindachu.data.local.entity.StepEntity

@Database(
    entities = [
        DishEntity::class,
        CategoryEntity::class,
        IngredientEntity::class,
        StepEntity::class,
        CookLogEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dishDao(): DishDao
    abstract fun categoryDao(): CategoryDao
    abstract fun ingredientDao(): IngredientDao
    abstract fun stepDao(): StepDao
    abstract fun cookLogDao(): CookLogDao
}
