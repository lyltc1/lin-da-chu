package com.lyltc1.lindachu.ui.navigation

/** 应用内的页面。当前页面数少，先用轻量栈自行管理，后续可平滑切到 Navigation-Compose */
sealed interface Screen {
    data object Home : Screen
    data class Detail(val dishId: String) : Screen
    data class Edit(val dishId: String?) : Screen
}
