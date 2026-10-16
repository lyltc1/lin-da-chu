package com.lyltc1.lindachu.ui.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lyltc1.lindachu.data.local.entity.DishEntity
import com.lyltc1.lindachu.data.repository.MenuRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PickUiState(
    val menuTitle: String = "",
    val dishes: List<DishEntity> = emptyList(),
    /** 菜谱 id → 份数；没点的菜不在表里 */
    val counts: Map<String, Int> = emptyMap(),
    val note: String = "",
    val message: String? = null
) {
    /** 这一轮一共点了几份 */
    val totalCount: Int get() = counts.values.sum()
    /** 点了几种菜 */
    val dishCount: Int get() = counts.count { it.value > 0 }
}

/** 点菜页左侧索引的一项：一个分类，以及它下面的菜 */
data class CategorySection(
    val id: String,
    val name: String,
    val dishes: List<DishEntity>
) {
    /** 这个分类一共点了几份，左侧角标显示 */
    fun pickedCount(counts: Map<String, Int>): Int = dishes.sumOf { counts[it.id] ?: 0 }
}

/**
 * 本机点菜。
 *
 * 大家轮流在这台手机上加减想吃的菜，最后一次性生成一份点单，
 * 存好之后跳到汇总页，直接截图就能发到群里。全程不联网。
 */
class PickDishesViewModel(
    private val menuRepository: MenuRepository,
    private val menuId: String
) : ViewModel() {

    private val _counts = MutableStateFlow<Map<String, Int>>(emptyMap())
    private val _note = MutableStateFlow("")
    private val _message = MutableStateFlow<String?>(null)

    /** 生成成功后把点单 id 交给页面，用于跳转汇总页 */
    private val _savedOrderId = MutableStateFlow<String?>(null)
    val savedOrderId: StateFlow<String?> = _savedOrderId.asStateFlow()

    /**
     * 菜单标题 + 按分类分好组的菜。
     *
     * 用 observe 而不是打开时读一次：菜单里加减了菜、或者改了名字，
     * 回到点菜页要立刻看到最新的，不然会照着旧清单点。
     */
    private val grouped: Flow<Pair<String, List<CategorySection>>> =
        menuRepository.observeMenu(menuId)
            .mapLatest { menu ->
                if (menu == null) {
                    _message.value = "这份菜单已经不存在了"
                    "" to emptyList()
                } else {
                    val dishes = menuRepository.getMenuDishes(menu)
                    menu.title to groupByCategory(dishes, menuRepository.getCategoryNames())
                }
            }
            .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    val uiState: StateFlow<PickUiState> = combine(
        grouped, _counts, _note, _message
    ) { (title, sections), counts, note, message ->
        PickUiState(title, sections.flatMap { it.dishes }, counts, note, message)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PickUiState())

    /** 按分类分好组的菜，点菜页左侧索引和右侧分组都用它 */
    val sections: StateFlow<List<CategorySection>> = grouped
        .map { it.second }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * 按分类分组，顺序跟着分类表走（家常菜在前、汤在后）。
     *
     * 没填分类、或者分类已经被删掉的菜，统一归到「其他」放最后，
     * 这样菜单里混着老菜也不会漏掉任何一道。
     */
    private fun groupByCategory(
        dishes: List<DishEntity>,
        names: Map<String, String>
    ): List<CategorySection> {
        val result = mutableListOf<CategorySection>()
        names.forEach { (id, name) ->
            val list = dishes.filter { it.categoryId == id }
            if (list.isNotEmpty()) result += CategorySection(id, name, list)
        }
        val rest = dishes.filter {
            it.categoryId.isNullOrBlank() || !names.containsKey(it.categoryId)
        }
        if (rest.isNotEmpty()) result += CategorySection("", "其他", rest)
        return result
    }

    fun increment(dishId: String) = _counts.update { map ->
        val next = (map[dishId] ?: 0) + 1
        map + (dishId to next.coerceAtMost(99))
    }

    fun decrement(dishId: String) = _counts.update { map ->
        val next = (map[dishId] ?: 0) - 1
        if (next <= 0) map - dishId else map + (dishId to next)
    }

    fun onNoteChanged(value: String) = _note.update { value }

    fun submit() {
        if (_counts.value.isEmpty()) {
            _message.value = "还没点菜呢，先选几道吧"
            return
        }
        viewModelScope.launch {
            menuRepository.createLocalOrder(menuId, _note.value, _counts.value)
                .fold(
                    onSuccess = { _savedOrderId.value = it.id },
                    onFailure = { _message.value = it.message ?: "生成失败" }
                )
        }
    }

    fun consumeMessage() = _message.update { null }
    fun consumeSavedOrderId() = _savedOrderId.update { null }
}

class PickDishesViewModelFactory(
    private val menuRepository: MenuRepository,
    private val menuId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PickDishesViewModel(menuRepository, menuId) as T
    }
}
