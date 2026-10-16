package com.lyltc1.lindachu.data.repository

import androidx.room.withTransaction
import com.lyltc1.lindachu.data.backup.BackupManager
import com.lyltc1.lindachu.data.local.AppDatabase
import com.lyltc1.lindachu.data.sync.SyncRecipe
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

    /** 某道菜的食材，买菜清单按份数折算时用 */
    suspend fun getIngredients(dishId: String): List<IngredientEntity> =
        ingredientDao.getByDish(dishId)

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

    /** 打包全部数据用于备份导出 */
    suspend fun exportPayload(): BackupManager.Payload {
        val dishes = dishDao.getAllForSync()
        val recipes = dishes.map { dish ->
            SyncRecipe(
                dish = dish,
                ingredients = ingredientDao.getByDish(dish.id),
                steps = stepDao.getByDish(dish.id)
            )
        }
        return BackupManager.Payload(
            categories = categoryDao.getAll(),
            recipes = recipes
        )
    }

    /**
     * 从备份恢复。
     *
     * 备份恢复属于"整体回滚"语义，直接按备份内容覆盖本地，
     * 不走增量合并，符合用户"回到某个时刻"的预期。
     */
    suspend fun restore(payload: BackupManager.Payload) {
        db.withTransaction {
            payload.categories.forEach { categoryDao.upsert(it) }
            payload.recipes.forEach { recipe ->
                dishDao.upsert(recipe.dish)
                ingredientDao.deleteByDish(recipe.dish.id)
                ingredientDao.upsertAll(recipe.ingredients)
                stepDao.deleteByDish(recipe.dish.id)
                stepDao.upsertAll(recipe.steps)
            }
        }
    }

    /**
     * 首次启动时写入内置分类，仅此而已。
     *
     * 菜谱一律以 GitHub 仓库为准，App 不再内置任何菜谱——
     * 否则内置版本会在同步时反过来覆盖用户在远端维护的版本。
     */
    suspend fun ensureSeeded() {
        // 内置分类以代码为准：既能补上后来新增的分类，
        // 也能把改名（家常菜 → 素菜）同步到已经装过的手机上。
        // 用户自建的分类 id 是 UUID，不在列表里，不会被碰到。
        categoryDao.upsertAll(com.lyltc1.lindachu.data.local.SeedData.categories())
    }

    fun newId(): String = UUID.randomUUID().toString()
}
