package com.lyltc1.lindachu.data.local

import com.lyltc1.lindachu.data.local.entity.CategoryEntity
import com.lyltc1.lindachu.data.local.entity.DishEntity
import com.lyltc1.lindachu.data.local.entity.DishWithDetails
import com.lyltc1.lindachu.data.local.entity.IngredientEntity
import com.lyltc1.lindachu.data.local.entity.StepEntity

/** 首次安装的示例数据，便于立刻看到界面效果与使用方式 */
object SeedData {

    fun categories(): List<CategoryEntity> = listOf(
        CategoryEntity(id = "c-home", name = "家常菜", sortOrder = 0, colorHex = "#FF7043"),
        CategoryEntity(id = "c-soup", name = "汤羹", sortOrder = 1, colorHex = "#4CAF50"),
        CategoryEntity(id = "c-bake", name = "烘焙", sortOrder = 2, colorHex = "#FFB300"),
        CategoryEntity(id = "c-drink", name = "饮品", sortOrder = 3, colorHex = "#29B6F6")
    )

    fun dishes(): List<DishWithDetails> {
        val now = System.currentTimeMillis()

        val tomatoEgg = DishWithDetails(
            dish = DishEntity(
                id = "d-tomato-egg",
                title = "番茄炒蛋",
                categoryId = "c-home",
                tags = "快手,家常,下饭",
                difficulty = 1,
                durationMin = 10,
                servings = 2,
                notes = "番茄去皮口感更细腻，糖用来提鲜不是增甜。",
                rating = 5,
                cookCount = 12,
                createdAt = now,
                updatedAt = now
            ),
            ingredients = listOf(
                IngredientEntity("i-1", "d-tomato-egg", "番茄", "2", "个", "主料", 0),
                IngredientEntity("i-2", "d-tomato-egg", "鸡蛋", "3", "个", "主料", 1),
                IngredientEntity("i-3", "d-tomato-egg", "小葱", "1", "根", "辅料", 2),
                IngredientEntity("i-4", "d-tomato-egg", "盐", "2", "克", "调料", 3),
                IngredientEntity("i-5", "d-tomato-egg", "糖", "3", "克", "调料", 4)
            ),
            steps = listOf(
                StepEntity("s-1", "d-tomato-egg", 0, "番茄顶部划十字，热水烫 30 秒后去皮，切滚刀块。"),
                StepEntity("s-2", "d-tomato-egg", 1, "鸡蛋加少许盐打散，热油下锅炒至凝固后盛出。"),
                StepEntity("s-3", "d-tomato-egg", 2, "底油下番茄块，中火炒 3 分钟出汁，加糖和盐。", timerSec = 180),
                StepEntity("s-4", "d-tomato-egg", 3, "倒回鸡蛋翻炒均匀，撒葱花出锅。")
            )
        )

        val colaWings = DishWithDetails(
            dish = DishEntity(
                id = "d-cola-wings",
                title = "可乐鸡翅",
                categoryId = "c-home",
                tags = "宴客,小孩爱吃",
                difficulty = 2,
                durationMin = 30,
                servings = 3,
                notes = "收汁时要守着锅，糖分高容易糊。",
                rating = 4,
                cookCount = 5,
                createdAt = now,
                updatedAt = now
            ),
            ingredients = listOf(
                IngredientEntity("i-6", "d-cola-wings", "鸡中翅", "10", "个", "主料", 0),
                IngredientEntity("i-7", "d-cola-wings", "可乐", "330", "毫升", "主料", 1),
                IngredientEntity("i-8", "d-cola-wings", "姜", "3", "片", "辅料", 2),
                IngredientEntity("i-9", "d-cola-wings", "生抽", "15", "毫升", "调料", 3),
                IngredientEntity("i-10", "d-cola-wings", "料酒", "10", "毫升", "调料", 4)
            ),
            steps = listOf(
                StepEntity("s-5", "d-cola-wings", 0, "鸡翅两面划刀，冷水下锅加姜片料酒焯水，捞出冲净。"),
                StepEntity("s-6", "d-cola-wings", 1, "锅中少油，鸡翅煎至两面金黄。"),
                StepEntity("s-7", "d-cola-wings", 2, "倒入可乐没过鸡翅，加生抽，大火烧开转中小火炖 15 分钟。", timerSec = 900),
                StepEntity("s-8", "d-cola-wings", 3, "开盖大火收汁，不停翻动至汤汁浓稠裹住鸡翅。")
            )
        )

        return listOf(tomatoEgg, colaWings)
    }
}
