package com.lyltc1.lindachu.ui.navigation

import androidx.compose.runtime.mutableStateListOf

/** 简易返回栈。根页面为 Home，栈深为 1 时不再响应返回 */
class NavState {

    private val backStack = mutableStateListOf<Screen>(Screen.Home)

    val stack: List<Screen> get() = backStack
    val current: Screen get() = backStack.last()
    val canPop: Boolean get() = backStack.size > 1

    fun push(screen: Screen) {
        backStack.add(screen)
    }

    fun pop(): Boolean {
        if (!canPop) return false
        backStack.removeAt(backStack.lastIndex)
        return true
    }
}
