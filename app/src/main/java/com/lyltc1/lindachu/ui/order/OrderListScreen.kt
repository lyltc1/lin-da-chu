package com.lyltc1.lindachu.ui.order

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lyltc1.lindachu.data.local.entity.OrderStatus
import com.lyltc1.lindachu.data.repository.OrderWithItems
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderListScreen(
    viewModel: OrderListViewModel,
    onScanClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val orders by viewModel.orders.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    val tabs = listOf("全部", "待接单", "已接单", "已完成")
    val visible = when (selectedTab) {
        1 -> orders.filter { it.order.status == OrderStatus.PENDING.code }
        2 -> orders.filter { it.order.status == OrderStatus.ACCEPTED.code }
        3 -> orders.filter { it.order.status == OrderStatus.DONE.code }
        else -> orders
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("订单") },
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
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onScanClick) {
                Icon(Icons.Default.Add, contentDescription = "扫码接单")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            if (visible.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "还没有订单，点右下角扫码接单",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(visible, key = { it.order.id }) { item ->
                        OrderCard(
                            item = item,
                            onAdvance = { viewModel.advance(item.order) },
                            onDelete = { viewModel.delete(item.order.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderCard(
    item: OrderWithItems,
    onAdvance: () -> Unit,
    onDelete: () -> Unit
) {
    val status = OrderStatus.from(item.order.status)
    val containerColor = when (status) {
        OrderStatus.PENDING -> MaterialTheme.colorScheme.tertiaryContainer
        OrderStatus.ACCEPTED -> MaterialTheme.colorScheme.secondaryContainer
        OrderStatus.DONE -> MaterialTheme.colorScheme.surfaceVariant
        OrderStatus.CANCELLED -> MaterialTheme.colorScheme.errorContainer
    }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.order.tableNo.ifBlank { "未填桌号" },
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${item.order.menuTitle} · ${formatDateTime(item.order.createdAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(status.label, style = MaterialTheme.typography.labelLarge)
            }

            item.items.forEach { orderItem ->
                Text(
                    text = "· ${orderItem.dishTitle} × ${orderItem.count}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (item.order.note.isNotBlank()) {
                Text(
                    text = "备注：${item.order.note}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (status == OrderStatus.PENDING || status == OrderStatus.ACCEPTED) {
                    TextButton(onClick = onAdvance) {
                        Text(if (status == OrderStatus.PENDING) "接单" else "完成")
                    }
                }
                OutlinedButton(onClick = onDelete) { Text("删除") }
            }
        }
    }
}

private fun formatDateTime(millis: Long): String =
    SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(millis))
