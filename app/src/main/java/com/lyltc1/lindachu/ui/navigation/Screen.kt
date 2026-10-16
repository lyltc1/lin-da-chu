package com.lyltc1.lindachu.ui.navigation

/** 应用内的页面。当前页面数少，先用轻量栈自行管理，后续可平滑切到 Navigation-Compose */
sealed interface Screen {
    data object Home : Screen
    data class Detail(val dishId: String) : Screen
    data class Edit(val dishId: String?) : Screen
    data object Settings : Screen
    data object MenuList : Screen
    data class MenuDetail(val menuId: String?) : Screen
    /** 本机点菜：大家轮流在这台手机上加减想吃的菜 */
    data class PickDishes(val menuId: String) : Screen
    /** 点单汇总，排版成适合直接截图发出去的样子 */
    data class OrderSummary(val orderId: String) : Screen
    /** 买菜清单：把这份点单用到的食材按份数折算合并 */
    data class ShoppingList(val orderId: String) : Screen
    data object Orders : Screen
}
