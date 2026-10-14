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
import com.lyltc1.lindachu.ui.home.HomeScreen
import com.lyltc1.lindachu.ui.home.HomeViewModel
import com.lyltc1.lindachu.ui.home.HomeViewModelFactory

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
                factory = HomeViewModelFactory(container.dishRepository, appContext)
            )
            HomeScreen(
                viewModel = homeViewModel,
                onDishClick = { dishId -> navState.push(Screen.Detail(dishId)) }
            )
        }

        is Screen.Detail -> {
            val detailViewModel: DetailViewModel = viewModel(
                key = "detail_${screen.dishId}",
                factory = DetailViewModelFactory(container.dishRepository, screen.dishId, appContext)
            )
            DetailScreen(
                viewModel = detailViewModel,
                onBack = { navState.pop() }
            )
        }

        is Screen.Edit -> Unit // M3 接入编辑页
    }
}
