package com.lyltc1.lindachu.ui.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lyltc1.lindachu.data.repository.MenuRepository
import com.lyltc1.lindachu.data.repository.OrderWithItems
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 点单记录。
 *
 * 点单全部在本机产生，没有接单/完成之类的状态流转，
 * 也没有扫码入口——列表只是"之前点过什么"的存档。
 */
class OrderListViewModel(private val menuRepository: MenuRepository) : ViewModel() {

    val orders: StateFlow<List<OrderWithItems>> = menuRepository.observeOrders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun delete(id: String) {
        viewModelScope.launch { menuRepository.deleteOrder(id) }
    }
}

class OrderListViewModelFactory(
    private val menuRepository: MenuRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return OrderListViewModel(menuRepository) as T
    }
}
