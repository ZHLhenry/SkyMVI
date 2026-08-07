package com.sky.mvi.mvi.navigation

import androidx.navigation.NavHostController

/**
 * 带 popUpTo 的导航封装，集中处理 singleTop 与出栈逻辑，避免在多处手写 `navigate { ... }` 样板。
 *
 * @param route 目标路由
 * @param popUpToRoute 出栈到该路由（是否含本身由 [inclusive] 决定），为 null 表示不出栈
 * @param inclusive 是否同时把 [popUpToRoute] 出栈
 * @param singleTop 是否以 singleTop 方式跳转（避免重复实例）
 */
fun NavHostController.navigateTo(
    route: String,
    popUpToRoute: String? = null,
    inclusive: Boolean = false,
    singleTop: Boolean = true
) {
    navigate(route) {
        popUpToRoute?.let { popUpTo(it) { this.inclusive = inclusive } }
        this.launchSingleTop = singleTop
    }
}

/** 返回上一页 */
fun NavHostController.navigateBack() {
    popBackStack()
}
