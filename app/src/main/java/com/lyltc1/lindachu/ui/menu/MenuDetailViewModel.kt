package com.lyltc1.lindachu.ui.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lyltc1.lindachu.data.local.entity.DishEntity
import com.lyltc1.lindachu.data.repository.DishRepository
import com.lyltc1.lindachu.data.repository.MenuRepository
import com.lyltc1.lindachu.util.parseStringList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MenuDraft(
    val title: String = "",
    val greeting: String = "",
    /** 选中的菜谱 ID，顺序即点菜页里的展示顺序 */
    val selected: List<String> = emptyList()
)

class MenuDetailViewModel(
    private val menuRepository: MenuRepository,
    dishRepository: DishRepository,
    menuId: String?
) : ViewModel() {

    private val _draft = MutableStateFlow(MenuDraft())
    val draft: StateFlow<MenuDraft> = _draft.asStateFlow()

    /** 已落库的菜单 id；新建时为空，保存后才有值 */
    private val _savedId = MutableStateFlow<String?>(menuId)
    val savedId: StateFlow<String?> = _savedId.asStateFlow()

    /** 保存成功后要跳去点菜，用一次性事件把菜单 id 交给页面 */
    private val _savedMenuId = MutableStateFlow<String?>(null)
    val savedMenuId: StateFlow<String?> = _savedMenuId.asStateFlow()

    val dishes: StateFlow<List<DishEntity>> = dishRepository.observeDishes()
        .map { list -> list.filter { it.deletedAt == null } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        val id = menuId
        if (id != null) {
            viewModelScope.launch {
                val menu = menuRepository.getMenu(id) ?: return@launch
                _savedId.value = menu.id
                _draft.update { current ->
                    current.copy(
                        title = menu.title,
                        greeting = menu.greeting,
                        selected = parseStringList(menu.dishIds)
                    )
                }
            }
        }
    }

    fun onTitleChanged(value: String) = _draft.update { it.copy(title = value) }
    fun onGreetingChanged(value: String) = _draft.update { it.copy(greeting = value) }

    fun toggleDish(dishId: String) {
        _draft.update { draft ->
            val next = if (draft.selected.contains(dishId)) {
                draft.selected - dishId
            } else {
                draft.selected + dishId
            }
            draft.copy(selected = next)
        }
    }

    /**
     * 保存菜单，并通知页面跳到点菜页。
     *
     * 点菜页要按这份菜单出菜，所以必须先落库拿到 id，
     * 已经保存过的菜单直接复用原 id 跳转。
     */
    fun saveAndPick() {
        viewModelScope.launch {
            val draft = _draft.value
            val id = menuRepository.saveMenu(
                draft.title,
                draft.greeting,
                draft.selected,
                _savedId.value
            )
            _savedId.value = id
            _savedMenuId.value = id
        }
    }

    fun consumeSavedMenuId() = _savedMenuId.update { null }
}

class MenuDetailViewModelFactory(
    private val menuRepository: MenuRepository,
    private val dishRepository: DishRepository,
    private val menuId: String?
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MenuDetailViewModel(menuRepository, dishRepository, menuId) as T
    }
}
