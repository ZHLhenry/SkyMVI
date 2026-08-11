package com.sky.mvi.core.navigation

import androidx.navigation.NavHostController

/**
 * @Class: NavHostControllerExt
 * @Author: Henry
 * @Date: 2026/08/03
 * @Description: Navigation 组件扩展函数，提供类型安全的路由 API
 */

/**
 * 返回上一级页面
 */
fun NavHostController.skyNavigateBack() {
    popBackStack()
}

/**
 * 导航到指定路由，并通过 singleTop 与 restoreState 优化回退栈复用
 */
fun NavHostController.skyNavigateTo(
    route: String,
    popUpToRoute: String? = null,
    inclusive: Boolean = false,
    launchSingleTop: Boolean = true,
    restoreState: Boolean = true
) {
    navigate(route) {
        this.launchSingleTop = launchSingleTop
        this.restoreState = restoreState
        popUpToRoute?.let {
            popUpTo(it) {
                this.inclusive = inclusive
            }
        }
    }
}
