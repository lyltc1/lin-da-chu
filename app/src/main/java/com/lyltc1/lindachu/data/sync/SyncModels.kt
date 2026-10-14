package com.lyltc1.lindachu.data.sync

import com.lyltc1.lindachu.data.local.entity.DishEntity
import com.lyltc1.lindachu.data.local.entity.IngredientEntity
import com.lyltc1.lindachu.data.local.entity.StepEntity
import com.lyltc1.lindachu.util.encodeStringList
import com.lyltc1.lindachu.util.parseStringList
import org.json.JSONArray
import org.json.JSONObject

/** 同步数据格式版本，格式变更时递增以便兼容处理 */
const val SYNC_VERSION = 1
const val FILE_INDEX = "index.json"

/** index.json 中每条菜谱的摘要，用于增量比对 */
data class DishEntry(
    val id: String,
    val title: String,
    val updatedAt: Long,
    val deleted: Boolean,
    /** 远端 recipes/<id>.json 的 blob sha，更新文件时需要带上 */
    val sha: String
)

/** 数据仓库根索引 */
data class SyncIndex(
    val version: Int = SYNC_VERSION,
    val updatedAt: Long = 0,
    val dishes: List<DishEntry> = emptyList()
)

/** recipes/<dishId>.json 的完整内容 */
data class SyncRecipe(
    val dish: DishEntity,
    val ingredients: List<IngredientEntity> = emptyList(),
    val steps: List<StepEntity> = emptyList()
) {
    /** 该菜谱引用的全部图片相对路径，用于同步时判断需要传输哪些文件 */
    fun imagePaths(): List<String> = buildList {
        dish.coverPath?.let { add(it) }
        steps.forEach { step -> addAll(parseStringList(step.imagePaths)) }
    }.distinct()
}

fun SyncIndex.toJson(): String = JSONObject()
    .put("version", version)
    .put("updatedAt", updatedAt)
    .put("dishes", JSONArray().apply { dishes.forEach { put(it.toJson()) } })
    .toString(2)

private fun DishEntry.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("title", title)
    .put("updatedAt", updatedAt)
    .put("deleted", deleted)
    .put("sha", sha)

fun parseSyncIndex(raw: String): SyncIndex {
    val json = JSONObject(raw)
    val array = json.optJSONArray("dishes") ?: JSONArray()
    val entries = buildList {
        repeat(array.length()) { index ->
            val item = array.getJSONObject(index)
            add(
                DishEntry(
                    id = item.getString("id"),
                    title = item.optString("title", ""),
                    updatedAt = item.optLong("updatedAt", 0L),
                    deleted = item.optBoolean("deleted", false),
                    sha = item.optString("sha", "")
                )
            )
        }
    }
    return SyncIndex(
        version = json.optInt("version", SYNC_VERSION),
        updatedAt = json.optLong("updatedAt", 0L),
        dishes = entries
    )
}

fun SyncRecipe.toJson(): String {
    val dish = this.dish
    val json = JSONObject()
        .put("id", dish.id)
        .put("title", dish.title)
        .put("tags", dish.tags)
        .put("difficulty", dish.difficulty)
        .put("durationMin", dish.durationMin)
        .put("servings", dish.servings)
        .put("notes", dish.notes)
        .put("rating", dish.rating)
        .put("cookCount", dish.cookCount)
        .put("createdAt", dish.createdAt)
        .put("updatedAt", dish.updatedAt)

    dish.coverPath?.let { json.put("coverPath", it) }
    dish.categoryId?.let { json.put("categoryId", it) }
    dish.deletedAt?.let { json.put("deletedAt", it) }

    json.put("ingredients", JSONArray().apply {
        ingredients.forEach { ingredient ->
            put(
                JSONObject()
                    .put("name", ingredient.name)
                    .put("amount", ingredient.amount)
                    .put("unit", ingredient.unit)
                    .put("group", ingredient.groupName)
            )
        }
    })

    json.put("steps", JSONArray().apply {
        steps.forEach { step ->
            val stepJson = JSONObject().put("text", step.text)
            stepJson.put("images", JSONArray().apply {
                parseStringList(step.imagePaths).forEach { put(it) }
            })
            step.timerSec?.let { stepJson.put("timerSec", it) }
            put(stepJson)
        }
    })

    return json.toString(2)
}

fun parseSyncRecipe(raw: String): SyncRecipe {
    val json = JSONObject(raw)
    val id = json.getString("id")

    val dish = DishEntity(
        id = id,
        title = json.optString("title", ""),
        coverPath = json.optString("coverPath", "").takeIf { it.isNotBlank() },
        categoryId = json.optString("categoryId", "").takeIf { it.isNotBlank() },
        tags = json.optString("tags", ""),
        difficulty = json.optInt("difficulty", 2),
        durationMin = json.optInt("durationMin", 0),
        servings = json.optInt("servings", 2),
        notes = json.optString("notes", ""),
        rating = json.optInt("rating", 0),
        cookCount = json.optInt("cookCount", 0),
        createdAt = json.optLong("createdAt", System.currentTimeMillis()),
        updatedAt = json.optLong("updatedAt", System.currentTimeMillis()),
        deletedAt = json.optLong("deletedAt", 0L).takeIf { it > 0L }
    )

    val ingredientsArray = json.optJSONArray("ingredients") ?: JSONArray()
    val ingredients = buildList {
        repeat(ingredientsArray.length()) { index ->
            val item = ingredientsArray.getJSONObject(index)
            add(
                IngredientEntity(
                    id = "$id-i$index",
                    dishId = id,
                    name = item.optString("name", ""),
                    amount = item.optString("amount", ""),
                    unit = item.optString("unit", ""),
                    groupName = item.optString("group", "主料"),
                    orderIndex = index
                )
            )
        }
    }

    val stepsArray = json.optJSONArray("steps") ?: JSONArray()
    val steps = buildList {
        repeat(stepsArray.length()) { index ->
            val item = stepsArray.getJSONObject(index)
            val images = parseImages(item.optJSONArray("images"))
            add(
                StepEntity(
                    id = "$id-s$index",
                    dishId = id,
                    orderIndex = index,
                    text = item.optString("text", ""),
                    imagePaths = encodeStringList(images),
                    timerSec = item.optLong("timerSec", 0L).takeIf { it > 0L }?.toInt()
                )
            )
        }
    }

    return SyncRecipe(dish, ingredients, steps)
}

private fun parseImages(array: JSONArray?): List<String> = buildList {
    if (array == null) return@buildList
    repeat(array.length()) { index -> add(array.getString(index)) }
}
