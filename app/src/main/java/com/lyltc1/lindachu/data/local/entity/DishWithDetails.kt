package com.lyltc1.lindachu.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

/** 一道菜连同食材与步骤，供详情页使用 */
data class DishWithDetails(
    @Embedded val dish: DishEntity,
    @Relation(parentColumn = "id", entityColumn = "dishId")
    val ingredients: List<IngredientEntity>,
    @Relation(parentColumn = "id", entityColumn = "dishId")
    val steps: List<StepEntity>
)
