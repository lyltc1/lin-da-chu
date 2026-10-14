package com.lyltc1.lindachu.data.repository

import androidx.room.withTransaction
import com.lyltc1.lindachu.data.local.AppDatabase
import com.lyltc1.lindachu.data.local.entity.CategoryEntity
import com.lyltc1.lindachu.data.local.entity.DishEntity
import com.lyltc1.lindachu.data.local.entity.DishWithDetails
import com.lyltc1.lindachu.data.local.entity.IngredientEntity
import com.lyltc1.lindachu.data.local.entity.StepEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * 菜谱数据访问入口。
 *
 * UI 层只依赖本类，不直接接触 DAO，便于后续接入 GitHub 同步：
 * 同步逻辑只需在此处增加"写远端"的分支。
 */
class DishRepository(private val db: AppDatabase) {

    private val dishDao = db.dishDao()
    private val categoryDao = db.categoryDao()
    private val ingredientDao = db.ingredientDao()
    private val stepDao = db.stepDao()
    private val cookLogDao = db.cookLogDao()

    /** 列表数据源：搜索词优先，其次分类，最后全部 */
    fun observeDishes(categoryId: String? = null, query: String = ""): Flow<List<DishEntity>> =
        when {
            query.isNotBlank() -> dishDao.search(query.trim())
            categoryId != null -> dishDao.observeByCategory(categoryId)
            else -> dishDao.observeAll()
        }

    fun observeDish(id: String): Flow<DishWithDetails?> = dishDao.observeWithDetails(id)

    fun observeCategories(): Flow<List<CategoryEntity>> = categoryDao.observeAll()

    suspend fun saveDish(
        dish: DishEntity,
        ingredients: List<IngredientEntity>,
        steps: List<StepEntity>
    ) {
        db.withTransaction {
            dishDao.upsert(dish.copy(updatedAt = System.currentTimeMillis()))
            ingredientDao.deleteByDish(dish.id)
            ingredientDao.upsertAll(ingredients)
            stepDao.deleteByDish(dish.id)
            stepDao.upsertAll(steps)
        }
    }

    suspend fun saveCategory(category: CategoryEntity) = categoryDao.upsert(category)

    suspend fun softDeleteDish(id: String) {
        db.withTransaction {
            dishDao.softDelete(id)
            ingredientDao.deleteByDish(id)
            stepDao.deleteByDish(id)
            cookLogDao.deleteByDish(id)
        }
    }

    /** 清理超过保留期的墓碑，默认保留 90 天 */
    suspend fun purgeDeleted(keepMillis: Long = 90L * 24 * 60 * 60 * 1000) {
        dishDao.purgeDeleted(System.currentTimeMillis() - keepMillis)
    }

    suspend fun ensureSeeded() {
        if (categoryDao.count() > 0) return
        db.withTransaction {
            categoryDao.upsertAll(com.lyltc1.lindachu.data.local.SeedData.categories())
            SeedDishes.insertAll(this@DishRepository)
        }
    }

    fun newId(): String = UUID.randomUUID().toString()
}

private object SeedDishes {
    suspend fun insertAll(repo: DishRepository) {
        com.lyltc1.lindachu.data.local.SeedData.dishes().forEach { item ->
            repo.saveDish(item.dish, item.ingredients, item.steps)
        }
    }
}
