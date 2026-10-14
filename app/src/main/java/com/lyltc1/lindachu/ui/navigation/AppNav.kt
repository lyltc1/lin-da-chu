package com.lyltc1.lindachu.ui.navigation

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lyltc1.lindachu.di.AppContainer
import com.lyltc1.lindachu.ui.detail.DetailScreen
import com.lyltc1.lindachu.ui.detail.DetailViewModel
import com.lyltc1.lindachu.ui.detail.DetailViewModelFactory
import com.lyltc1.lindachu.ui.edit.EditScreen
import com.lyltc1.lindachu.ui.edit.EditViewModel
import com.lyltc1.lindachu.ui.edit.EditViewModelFactory
import com.lyltc1.lindachu.ui.home.HomeScreen
import com.lyltc1.lindachu.ui.home.HomeViewModel
import com.lyltc1.lindachu.ui.home.HomeViewModelFactory
import com.lyltc1.lindachu.ui.menu.MenuDetailScreen
import com.lyltc1.lindachu.ui.menu.MenuDetailViewModel
import com.lyltc1.lindachu.ui.menu.MenuDetailViewModelFactory
import com.lyltc1.lindachu.ui.menu.MenuListScreen
import com.lyltc1.lindachu.ui.menu.MenuListViewModel
import com.lyltc1.lindachu.ui.menu.MenuListViewModelFactory
import com.lyltc1.lindachu.ui.order.OrderListScreen
import com.lyltc1.lindachu.ui.order.OrderListViewModel
import com.lyltc1.lindachu.ui.order.OrderListViewModelFactory
import com.lyltc1.lindachu.ui.order.ScanOrderScreen
import com.lyltc1.lindachu.ui.settings.SettingsScreen
import com.lyltc1.lindachu.ui.settings.SettingsViewModel
import com.lyltc1.lindachu.ui.settings.SettingsViewModelFactory

/** 页面容器：按返回栈渲染当前页面，并处理系统返回键 */
@Composable
fun AppNav(container: AppContainer, appContext: Context) {
    val navState = remember { NavState() }
    // 扫码页扫到的订单码，返回订单列表后消费
    var scannedOrder by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = navState.canPop) {
        navState.pop()
    }

    when (val screen = navState.current) {
        Screen.Home -> {
            val homeViewModel: HomeViewModel = viewModel(
                factory = HomeViewModelFactory(container.dishRepository, appContext)
            )
            HomeScreen(
                viewModel = homeViewModel,
                onDishClick = { dishId -> navState.push(Screen.Detail(dishId)) },
                onCreateClick = { navState.push(Screen.Edit(null)) },
                onSettingsClick = { navState.push(Screen.Settings) },
                onMenusClick = { navState.push(Screen.MenuList) }
            )
        }

        is Screen.Detail -> {
            val detailViewModel: DetailViewModel = viewModel(
                key = "detail_${screen.dishId}",
                factory = DetailViewModelFactory(container.dishRepository, screen.dishId, appContext)
            )
            DetailScreen(
                viewModel = detailViewModel,
                onBack = { navState.pop() },
                onEdit = { navState.push(Screen.Edit(screen.dishId)) }
            )
        }

        is Screen.Edit -> {
            val editViewModel: EditViewModel = viewModel(
                key = "edit_${screen.dishId}",
                factory = EditViewModelFactory(
                    repository = container.dishRepository,
                    imageStorage = container.imageStorage,
                    dishId = screen.dishId
                )
            )
            EditScreen(
                viewModel = editViewModel,
                onSaved = { navState.pop() },
                onBack = { navState.pop() }
            )
        }

        Screen.Settings -> {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModelFactory(
                    repository = container.dishRepository,
                    syncManager = container.syncManager,
                    backupManager = container.backupManager,
                    preferences = container.syncPreferences
                )
            )
            SettingsScreen(
                viewModel = settingsViewModel,
                onBack = { navState.pop() }
            )
        }

        Screen.MenuList -> {
            val menuListViewModel: MenuListViewModel = viewModel(
                factory = MenuListViewModelFactory(container.menuRepository)
            )
            MenuListScreen(
                viewModel = menuListViewModel,
                onMenuClick = { menuId -> navState.push(Screen.MenuDetail(menuId)) },
                onCreateClick = { navState.push(Screen.MenuDetail(null)) },
                onOrdersClick = { navState.push(Screen.Orders) },
                onBack = { navState.pop() }
            )
        }

        is Screen.MenuDetail -> {
            val menuDetailViewModel: MenuDetailViewModel = viewModel(
                key = "menu_${screen.menuId}",
                factory = MenuDetailViewModelFactory(
                    menuRepository = container.menuRepository,
                    dishRepository = container.dishRepository,
                    publisher = container.menuPublisher,
                    menuId = screen.menuId
                )
            )
            MenuDetailScreen(
                viewModel = menuDetailViewModel,
                imageStorage = container.imageStorage,
                onBack = { navState.pop() }
            )
        }

        Screen.Orders -> {
            val orderListViewModel: OrderListViewModel = viewModel(
                factory = OrderListViewModelFactory(container.menuRepository)
            )
            LaunchedEffect(Unit) {
                scannedOrder?.let {
                    orderListViewModel.receive(it)
                    scannedOrder = null
                }
            }
            OrderListScreen(
                viewModel = orderListViewModel,
                onScanClick = { navState.push(Screen.ScanOrder) },
                onBack = { navState.pop() }
            )
        }

        Screen.ScanOrder -> {
            ScanOrderScreen(
                onScanned = { text ->
                    scannedOrder = text
                    navState.pop()
                },
                onBack = { navState.pop() }
            )
        }
    }
}
