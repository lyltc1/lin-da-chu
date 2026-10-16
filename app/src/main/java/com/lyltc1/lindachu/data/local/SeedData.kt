package com.lyltc1.lindachu.data.local

import com.lyltc1.lindachu.data.local.entity.CategoryEntity

/**
 * 内置数据：只保留分类，不再内置任何菜谱。
 *
 * 菜谱统一以 GitHub 仓库为唯一来源，App 里不再塞示例菜谱——
 * 否则内置版本会在同步时按 updatedAt 反过来覆盖用户在远端维护的版本。
 *
 * 分类仍然内置：同步只传输菜谱，不传输分类表，
 * 新设备拉回菜谱后需要有分类数据才能归类显示。
 *
 * 分类 id 用 c- 前缀，与用户自建条目（UUID）区分。
 */
object SeedData {

    /** 示例分类 id 前缀 */
    const val CATEGORY_PREFIX = "c-"

    /** 云端菜谱 id 前缀，沿用早年示例数据的命名 */
    const val DISH_PREFIX = "d-"

    /**
     * id 一旦定下就不要再改——菜谱里存的是 id，
     * 改了的话菜就找不到分类，会掉进「其他」。
     *
     * 所以要改的是显示名：「家常菜」改成「素菜」，id 仍是 c-home。
     * 后面加菜按类别选就行，肉菜和海鲜现在还是空的。
     */
    fun categories(): List<CategoryEntity> = listOf(
        CategoryEntity(id = "c-home", name = "素菜", sortOrder = 0, colorHex = "#43A047"),
        CategoryEntity(id = "c-meat", name = "肉菜", sortOrder = 1, colorHex = "#E5533D"),
        CategoryEntity(id = "c-seafood", name = "海鲜", sortOrder = 2, colorHex = "#039BE5"),
        CategoryEntity(id = "c-soup", name = "汤", sortOrder = 3, colorHex = "#29B6F6"),
        CategoryEntity(id = "c-rice", name = "主食", sortOrder = 4, colorHex = "#F9A825")
    )
}
