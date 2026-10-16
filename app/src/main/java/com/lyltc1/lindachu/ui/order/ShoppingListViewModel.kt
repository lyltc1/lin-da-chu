package com.lyltc1.lindachu.ui.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lyltc1.lindachu.data.repository.DishRepository
import com.lyltc1.lindachu.data.repository.MenuRepository
import com.lyltc1.lindachu.data.repository.OrderWithItems
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

/**
 * 买菜清单里的一行：某个食材按所有点了它的菜折算后的总量。
 *
 * [total] 为 null 表示这笔用量算不出数字（比如写了「少许」「适量」），
 * 此时不编一个假数字出来——清单看起来精确其实不准，比写「适量」更坑。
 */
data class ShoppingItem(
    val key: String,
    val name: String,
    val unit: String,
    val groupName: String,
    val total: Double?,
    /** 每一项来自哪道菜、原本写了多少，方便对着菜谱核对 */
    val sources: List<String>
)

class ShoppingListViewModel(
    menuRepository: MenuRepository,
    private val dishRepository: DishRepository,
    orderId: String
) : ViewModel() {

    val items: StateFlow<List<ShoppingItem>> = menuRepository.observeOrder(orderId)
        .mapLatest { order -> if (order == null) emptyList() else build(order) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * 把点单里的每道菜按其份数展开成食材，再按「食材名 + 单位」合并。
     *
     * 同名不同单位会各占一行（3 瓣和 2 头没法相加），
     * 同一行里只要有一处用量算不出，整行就降级成「适量」。
     */
    private suspend fun build(order: OrderWithItems): List<ShoppingItem> {
        data class Bucket(
            val name: String,
            val unit: String,
            var groupName: String,
            var total: Double? = null,
            var vague: Boolean = false,
            val sources: MutableList<String> = mutableListOf()
        )

        val buckets = LinkedHashMap<String, Bucket>()
        for (item in order.items) {
            if (item.count <= 0) continue
            for (ing in dishRepository.getIngredients(item.dishId)) {
                val name = ing.name.trim()
                if (name.isEmpty()) continue
                val parsed = parseAmount(ing.amount)
                val unit = ing.unit.trim().ifBlank { parsed.unit }
                val bucket = buckets.getOrPut("$name|$unit") {
                    Bucket(name = name, unit = unit, groupName = ing.groupName)
                }
                if (bucket.groupName.isBlank()) bucket.groupName = ing.groupName

                if (parsed.value != null) {
                    bucket.total = (bucket.total ?: 0.0) + parsed.value * item.count
                } else {
                    bucket.vague = true
                }
                bucket.sources += buildString {
                    append(item.dishTitle).append(" ×").append(item.count)
                    append("（").append(ing.amount.trim().ifBlank { "适量" })
                    if (ing.unit.trim().isNotEmpty()) append(ing.unit.trim())
                    append("）")
                }
            }
        }

        return buckets.values
            .map { bucket ->
                ShoppingItem(
                    key = "${bucket.name}|${bucket.unit}",
                    name = bucket.name,
                    unit = bucket.unit,
                    groupName = bucket.groupName.ifBlank { "其他" },
                    total = if (bucket.vague) null else bucket.total,
                    sources = bucket.sources
                )
            }
            .sortedWith(compareBy({ groupRank(it.groupName) }, { it.name }))
    }

    /**
     * 用量写法不统一：有的是「3」配单独的单位字段，
     * 有的直接把一整串写进用量（「3 瓣（可选）」）。
     *
     * 这里取最前面的数字当数量，单位优先用单位字段，
     * 没有就从剩余文字里剥出来，并去掉「（可选）」这类备注。
     */
    private fun parseAmount(raw: String): ParsedAmount {
        val text = raw.trim()
        val match = AMOUNT_RE.find(text)
        val value = match?.groupValues?.get(1)?.toDoubleOrNull()
        val rest = if (match == null) text else text.substring(match.range.last + 1)
        return ParsedAmount(value, rest.replace(REMARK_RE, "").trim())
    }

    private data class ParsedAmount(val value: Double?, val unit: String)

    private companion object {
        val AMOUNT_RE = Regex("""^\s*(\d+(?:\.\d+)?)""")
        val REMARK_RE = Regex("""[（(][^）)]*[）)]""")
    }

    /** 主料 → 辅料 → 调料 → 其他 */
    private fun groupRank(name: String): Int = when {
        name.contains("主料") -> 0
        name.contains("辅") -> 1
        name.contains("调") -> 2
        else -> 3
    }
}

class ShoppingListViewModelFactory(
    private val menuRepository: MenuRepository,
    private val dishRepository: DishRepository,
    private val orderId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ShoppingListViewModel(menuRepository, dishRepository, orderId) as T
    }
}
