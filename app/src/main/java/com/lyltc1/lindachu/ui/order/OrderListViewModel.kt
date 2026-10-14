package com.lyltc1.lindachu.ui.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lyltc1.lindachu.data.local.entity.OrderEntity
import com.lyltc1.lindachu.data.local.entity.OrderStatus
import com.lyltc1.lindachu.data.order.OrderCodec
import com.lyltc1.lindachu.data.repository.MenuRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OrderListUiState(
    val message: String? = null
)

class OrderListViewModel(private val menuRepository: MenuRepository) : ViewModel() {

    val orders: StateFlow<List<com.lyltc1.lindachu.data.repository.OrderWithItems>> =
        menuRepository.observeOrders()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiState = MutableStateFlow(OrderListUiState())
    val uiState: StateFlow<OrderListUiState> = _uiState.asStateFlow()

    /** 推进状态：待接单 → 已接单 → 已完成 */
    fun advance(order: OrderEntity) {
        val next = when (OrderStatus.from(order.status)) {
            OrderStatus.PENDING -> OrderStatus.ACCEPTED
            OrderStatus.ACCEPTED -> OrderStatus.DONE
            else -> null
        } ?: return
        viewModelScope.launch { menuRepository.updateOrderStatus(order.id, next) }
    }

    fun cancel(order: OrderEntity) {
        viewModelScope.launch {
            menuRepository.updateOrderStatus(order.id, OrderStatus.CANCELLED)
        }
    }

    fun delete(id: String) {
        viewModelScope.launch { menuRepository.deleteOrder(id) }
    }

    /** 录入扫到的订单码 */
    fun receive(text: String) {
        val payload = OrderCodec.decode(text)
        if (payload == null) {
            _uiState.update { it.copy(message = "这不是有效的订单码") }
            return
        }
        viewModelScope.launch {
            val result = menuRepository.receiveOrder(payload)
            _uiState.update {
                it.copy(
                    message = result.fold(
                        onSuccess = { order -> "已录入订单：${order.tableNo.ifBlank { "未填桌号" }}" },
                        onFailure = { error -> error.message ?: "录入失败" }
                    )
                )
            }
        }
    }

    fun consumeMessage() = _uiState.update { it.copy(message = null) }
}

class OrderListViewModelFactory(
    private val menuRepository: MenuRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return OrderListViewModel(menuRepository) as T
    }
}
