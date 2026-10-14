package com.lyltc1.lindachu.data.backup

import android.content.Context
import android.net.Uri
import com.lyltc1.lindachu.data.local.ImageStorage
import com.lyltc1.lindachu.data.local.entity.CategoryEntity
import com.lyltc1.lindachu.data.sync.SYNC_VERSION
import com.lyltc1.lindachu.data.sync.SyncRecipe
import com.lyltc1.lindachu.data.sync.parseSyncRecipe
import com.lyltc1.lindachu.data.sync.toJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * 备份导出与导入（zip）。
 *
 * 走 SAF 系统文件选择器，不申请任何存储权限，规避 Android 11+ 分区存储限制。
 * 备份内容 = 一份 JSON（分类 + 全部菜谱含食材步骤）+ images/ 目录，
 * 与 GitHub 数据仓库使用同一套 [SyncRecipe] 结构，导入导出逻辑可以复用。
 */
class BackupManager(
    private val context: Context,
    private val imageStorage: ImageStorage
) {

    data class Payload(
        val categories: List<CategoryEntity> = emptyList(),
        val recipes: List<SyncRecipe> = emptyList()
    )

    suspend fun export(uri: Uri, payload: Payload): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val stream = context.contentResolver.openOutputStream(uri)
                ?: error("无法写入目标文件")
            ZipOutputStream(BufferedOutputStream(stream)).use { zip ->
                zip.putNextEntry(ZipEntry(FILE_DATA))
                zip.write(buildJson(payload).toByteArray())
                zip.closeEntry()

                val imagesRoot = imageStorage.imagesRoot()
                imagesRoot.walkTopDown().filter { it.isFile }.forEach { file ->
                    val entry = "$DIR_IMAGES/" +
                        file.relativeTo(imagesRoot).path.replace(File.separatorChar, '/')
                    zip.putNextEntry(ZipEntry(entry))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        }
    }

    suspend fun import(uri: Uri): Result<Payload> = withContext(Dispatchers.IO) {
        runCatching {
            var payload: Payload? = null
            val stream = context.contentResolver.openInputStream(uri)
                ?: error("无法读取备份文件")
            ZipInputStream(stream).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val bytes = zip.readBytes()
                        when {
                            entry.name == FILE_DATA -> payload = parsePayload(String(bytes))
                            entry.name.startsWith("$DIR_IMAGES/") ->
                                imageStorage.saveRaw(entry.name, bytes)
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
            payload ?: error("备份文件缺少 $FILE_DATA")
        }
    }

    private fun buildJson(payload: Payload): String {
        val categories = JSONArray().apply {
            payload.categories.forEach { category ->
                put(
                    JSONObject()
                        .put("id", category.id)
                        .put("name", category.name)
                        .put("sortOrder", category.sortOrder)
                        .put("colorHex", category.colorHex)
                )
            }
        }
        val recipes = JSONArray().apply {
            payload.recipes.forEach { recipe -> put(JSONObject(recipe.toJson())) }
        }
        return JSONObject()
            .put("version", SYNC_VERSION)
            .put("exportedAt", System.currentTimeMillis())
            .put("categories", categories)
            .put("recipes", recipes)
            .toString(2)
    }

    private fun parsePayload(raw: String): Payload {
        val json = JSONObject(raw)
        val categoriesArray = json.optJSONArray("categories") ?: JSONArray()
        val categories = buildList {
            repeat(categoriesArray.length()) { index ->
                val item = categoriesArray.getJSONObject(index)
                add(
                    CategoryEntity(
                        id = item.getString("id"),
                        name = item.optString("name", ""),
                        sortOrder = item.optInt("sortOrder", 0),
                        colorHex = item.optString("colorHex", "#FF7043")
                    )
                )
            }
        }

        val recipesArray = json.optJSONArray("recipes") ?: JSONArray()
        val recipes = buildList {
            repeat(recipesArray.length()) { index ->
                add(parseSyncRecipe(recipesArray.getJSONObject(index).toString()))
            }
        }

        return Payload(categories, recipes)
    }

    companion object {
        const val FILE_DATA = "lindachu-backup.json"
        const val DIR_IMAGES = "images"
    }
}
