package com.lyltc1.lindachu.data.sync

import androidx.room.withTransaction
import com.lyltc1.lindachu.data.local.AppDatabase
import com.lyltc1.lindachu.data.local.ImageStorage
import com.lyltc1.lindachu.data.local.entity.DishEntity
import com.lyltc1.lindachu.data.prefs.SyncPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface SyncResult {
    data class Success(val pulled: Int, val pushed: Int, val syncedAt: Long) : SyncResult
    data class Failure(val message: String) : SyncResult
}

/**
 * 与 GitHub 数据仓库双向同步菜谱。
 *
 * 设计要点：
 * - 索引先行：index.json 记录每条菜谱的 updatedAt 与文件 sha，据此判断谁更新，
 *   避免每次全量下载比对。
 * - 冲突策略：以 updatedAt 较新者为准（last-write-wins）。个人单人多设备场景下
 *   冲突概率极低，先不引入交互式合并。
 * - 墓碑传播：删除是软删，本地保留 deletedAt 并推给远端，防止"删了又被别的设备拉回来"。
 * - 图片去重：靠文件名（新建时生成 UUID）判断，本地有就不下载、远端有就不上传。
 */
class SyncManager(
    private val database: AppDatabase,
    private val imageStorage: ImageStorage,
    private val preferences: SyncPreferences
) {

    private val dishDao = database.dishDao()
    private val ingredientDao = database.ingredientDao()
    private val stepDao = database.stepDao()

    suspend fun sync(): SyncResult {
        val token = preferences.getToken()
        if (token.isBlank()) return SyncResult.Failure("尚未设置 GitHub 令牌")
        val client = GitHubContentClient(token, preferences.getOwner(), preferences.getRepo())

        return try {
            val remoteFile = client.read(FILE_INDEX)
            val remoteEntries = (remoteFile?.content?.let(::parseSyncIndex) ?: SyncIndex())
                .dishes.associateBy { it.id }.toMutableMap()

            // 1) 远端更新 → 落到本地
            var pulled = 0
            remoteEntries.values.toList().forEach { entry ->
                val local = dishDao.getById(entry.id)
                if (entry.updatedAt > (local?.updatedAt ?: 0L)) {
                    downloadRecipe(client, entry.id)
                    pulled++
                }
            }

            // 2) 本地更新 → 推到远端
            var pushed = 0
            dishDao.getAllForSync().forEach { dish ->
                val remote = remoteEntries[dish.id]
                if (remote == null || dish.updatedAt > remote.updatedAt) {
                    val sha = uploadRecipe(client, dish)
                    remoteEntries[dish.id] = DishEntry(
                        id = dish.id,
                        title = dish.title,
                        updatedAt = dish.updatedAt,
                        deleted = dish.deletedAt != null,
                        sha = sha
                    )
                    pushed++
                }
            }

            // 3) 回写索引并记录同步时间
            val now = System.currentTimeMillis()
            val index = SyncIndex(
                version = SYNC_VERSION,
                updatedAt = now,
                dishes = remoteEntries.values.sortedByDescending { it.updatedAt }
            )
            withContext(Dispatchers.IO) {
                client.writeText(
                    path = FILE_INDEX,
                    content = index.toJson(),
                    message = "chore: 同步菜谱索引",
                    existingSha = remoteFile?.sha
                )
            }
            preferences.setLastSyncAt(now)

            SyncResult.Success(pulled = pulled, pushed = pushed, syncedAt = now)
        } catch (e: Exception) {
            SyncResult.Failure(e.message ?: "同步失败")
        }
    }

    private suspend fun downloadRecipe(client: GitHubContentClient, dishId: String) {
        val file = client.read("$DIR_RECIPES/$dishId.json") ?: return
        val recipe = parseSyncRecipe(file.content)

        database.withTransaction {
            dishDao.upsert(recipe.dish)
            ingredientDao.deleteByDish(dishId)
            ingredientDao.upsertAll(recipe.ingredients)
            stepDao.deleteByDish(dishId)
            stepDao.upsertAll(recipe.steps)
        }

        recipe.imagePaths().forEach { relative ->
            val target = imageStorage.resolve(relative)
            if (target != null && !target.exists()) {
                val bytes = client.readBytes(relative)
                if (bytes != null) imageStorage.saveRaw(relative, bytes)
            }
        }
    }

    private suspend fun uploadRecipe(client: GitHubContentClient, dish: DishEntity): String {
        val ingredients = ingredientDao.getByDish(dish.id)
        val steps = stepDao.getByDish(dish.id)
        val recipe = SyncRecipe(dish, ingredients, steps)

        recipe.imagePaths().forEach { relative ->
            val file = imageStorage.resolve(relative) ?: return@forEach
            if (!file.exists()) return@forEach
            if (client.exists(relative)) return@forEach
            withContext(Dispatchers.IO) {
                client.writeBytes(
                    path = relative,
                    bytes = file.readBytes(),
                    message = "chore: 上传菜谱图片"
                )
            }
        }

        val existingSha = client.read("$DIR_RECIPES/${dish.id}.json")?.sha
        return client.writeText(
            path = "$DIR_RECIPES/${dish.id}.json",
            content = recipe.toJson(),
            message = "chore: 更新菜谱 ${dish.title}",
            existingSha = existingSha
        )
    }

    companion object {
        const val DIR_RECIPES = "recipes"
    }
}
