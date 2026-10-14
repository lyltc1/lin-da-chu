package com.lyltc1.lindachu.ui.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lyltc1.lindachu.data.local.entity.DishEntity
import com.lyltc1.lindachu.data.menu.MenuPublisher
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
    /** 选中的菜谱 ID，顺序即菜单展示顺序，订单码按序号引用菜品 */
    val selected: List<String> = emptyList()
)

sealed interface PublishState {
    data object Idle : PublishState
    data object Publishing : PublishState
    data class Done(val url: String) : PublishState
    data class Failed(val message: String) : PublishState
}

class MenuDetailViewModel(
    private val menuRepository: MenuRepository,
    private val dishRepository: DishRepository,
    private val publisher: MenuPublisher,
    menuId: String?
) : ViewModel() {

    private val _draft = MutableStateFlow(MenuDraft())
    val draft: StateFlow<MenuDraft> = _draft.asStateFlow()

    private val _publishState = MutableStateFlow<PublishState>(PublishState.Idle)
    val publishState: StateFlow<PublishState> = _publishState.asStateFlow()

    val dishes: StateFlow<List<DishEntity>> = dishRepository.observeDishes()
        .map { list -> list.filter { it.deletedAt == null } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var savedId: String? = menuId

    init {
        val id = menuId
        if (id != null) {
            viewModelScope.launch {
                val menu = menuRepository.getMenu(id) ?: return@launch
                savedId = menu.id
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
     * 保存并发布。
     *
     * 先落库再推送：订单码只带 menuId 和菜品序号，接单端必须能在本机查到
     * 这份菜单才能还原菜名，所以"先本地、后远端"是硬顺序。
     */
    fun publish() {
        viewModelScope.launch {
            _publishState.value = PublishState.Publishing
            val draft = _draft.value
            val id = menuRepository.saveMenu(draft.title, draft.greeting, draft.selected, savedId)
            savedId = id
            val menu = menuRepository.getMenu(id) ?: run {
                _publishState.value = PublishState.Failed("菜单保存失败")
                return@launch
            }
            _publishState.value = publisher.publish(menu).fold(
                onSuccess = { PublishState.Done(it) },
                onFailure = { PublishState.Failed(it.message ?: "发布失败") }
            )
        }
    }
}

class MenuDetailViewModelFactory(
    private val menuRepository: MenuRepository,
    private val dishRepository: DishRepository,
    private val publisher: MenuPublisher,
    private val menuId: String?
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MenuDetailViewModel(menuRepository, dishRepository, publisher, menuId) as T
    }
}
