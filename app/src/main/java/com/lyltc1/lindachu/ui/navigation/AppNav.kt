package com.lyltc1.lindachu.ui.navigation

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.lyltc1.lindachu.ui.order.OrderSummaryScreen
import com.lyltc1.lindachu.ui.order.OrderSummaryViewModel
import com.lyltc1.lindachu.ui.order.OrderSummaryViewModelFactory
import com.lyltc1.lindachu.ui.order.PickDishesScreen
import com.lyltc1.lindachu.ui.order.PickDishesViewModel
import com.lyltc1.lindachu.ui.order.PickDishesViewModelFactory
import com.lyltc1.lindachu.ui.settings.SettingsScreen
import com.lyltc1.lindachu.ui.settings.SettingsViewModel
import com.lyltc1.lindachu.ui.settings.SettingsViewModelFactory
import com.lyltc1.lindachu.ui.order.ShoppingListScreen
import com.lyltc1.lindachu.ui.order.ShoppingListViewModel
import com.lyltc1.lindachu.ui.order.ShoppingListViewModelFactory

/** 页面容器：按返回栈渲染当前页面，并处理系统返回键 */
@Composable
fun AppNav(container: AppContainer, appContext: Context) {
    val navState = remember { NavState() }

    BackHandler(enabled = navState.canPop) {
        navState.pop()
    }

    when (val screen = navState.current) {
        Screen.Home -> {
            val homeViewModel: HomeViewModel = viewModel(
                factory = HomeViewModelFactory(
                    repository = container.dishRepository,
                    syncManager = container.syncManager,
                    preferences = container.syncPreferences,
                    appContext = appContext
                )
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
                    menuId = screen.menuId
                )
            )
            MenuDetailScreen(
                viewModel = menuDetailViewModel,
                imageStorage = container.imageStorage,
                onPickDishes = { menuId -> navState.push(Screen.PickDishes(menuId)) },
                onBack = { navState.pop() }
            )
        }

        is Screen.PickDishes -> {
            val pickViewModel: PickDishesViewModel = viewModel(
                key = "pick_${screen.menuId}",
                factory = PickDishesViewModelFactory(container.menuRepository, screen.menuId)
            )
            PickDishesScreen(
                viewModel = pickViewModel,
                onGenerated = { orderId -> navState.push(Screen.OrderSummary(orderId)) },
                onBack = { navState.pop() }
            )
        }

        is Screen.OrderSummary -> {
            val summaryViewModel: OrderSummaryViewModel = viewModel(
                key = "summary_${screen.orderId}",
                factory = OrderSummaryViewModelFactory(container.menuRepository, screen.orderId)
            )
            OrderSummaryScreen(
                viewModel = summaryViewModel,
                onShoppingList = { navState.push(Screen.ShoppingList(screen.orderId)) },
                onBack = { navState.pop() }
            )
        }

        is Screen.ShoppingList -> {
            val shoppingViewModel: ShoppingListViewModel = viewModel(
                key = "shopping_${screen.orderId}",
                factory = ShoppingListViewModelFactory(
                    menuRepository = container.menuRepository,
                    dishRepository = container.dishRepository,
                    orderId = screen.orderId
                )
            )
            ShoppingListScreen(
                viewModel = shoppingViewModel,
                onBack = { navState.pop() }
            )
        }

        Screen.Orders -> {
            val orderListViewModel: OrderListViewModel = viewModel(
                factory = OrderListViewModelFactory(container.menuRepository)
            )
            OrderListScreen(
                viewModel = orderListViewModel,
                onOrderClick = { orderId -> navState.push(Screen.OrderSummary(orderId)) },
                onBack = { navState.pop() }
            )
        }
    }
}
