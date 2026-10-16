package com.lyltc1.lindachu.ui.order

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.lyltc1.lindachu.data.repository.MenuRepository
import com.lyltc1.lindachu.data.repository.OrderWithItems
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 点单汇总页。
 *
 * 排版刻意做成一张卡片：菜单名、时间、菜名与份数、合计，
 * 一眼能看全，直接截图发群里就行。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderSummaryScreen(
    viewModel: OrderSummaryViewModel,
    onShoppingList: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val item by viewModel.order.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("点单汇总") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        when (val data = item) {
            null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "这份点单已经不存在了",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> {
                Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = data.order.menuTitle.ifBlank { "未命名菜单" },
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Text(
                                text = formatDateTime(data.order.createdAt),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            HorizontalDivider()

                            data.items.forEach { orderItem ->
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = orderItem.dishTitle,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "× ${orderItem.count}",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }

                            HorizontalDivider()

                            Text(
                                text = "共 ${data.items.size} 道 · ${data.items.sumOf { it.count }} 份",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )

                            if (data.order.note.isNotBlank()) {
                                Text(
                                    text = "备注：${data.order.note}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Button(
                        onClick = onShoppingList,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        Text("买菜清单")
                    }
                }
            }
        }
    }
}

class OrderSummaryViewModel(
    menuRepository: MenuRepository,
    orderId: String
) : ViewModel() {

    val order: StateFlow<OrderWithItems?> = menuRepository.observeOrder(orderId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

class OrderSummaryViewModelFactory(
    private val menuRepository: MenuRepository,
    private val orderId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return OrderSummaryViewModel(menuRepository, orderId) as T
    }
}

private fun formatDateTime(millis: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(millis))
