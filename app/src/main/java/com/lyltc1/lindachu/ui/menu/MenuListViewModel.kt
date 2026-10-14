package com.lyltc1.lindachu.ui.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lyltc1.lindachu.data.local.entity.MenuEntity
import com.lyltc1.lindachu.data.repository.MenuRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MenuListViewModel(private val menuRepository: MenuRepository) : ViewModel() {

    val menus: StateFlow<List<MenuEntity>> = menuRepository.observeMenus()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun delete(id: String) {
        viewModelScope.launch { menuRepository.deleteMenu(id) }
    }
}

class MenuListViewModelFactory(
    private val menuRepository: MenuRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MenuListViewModel(menuRepository) as T
    }
}
