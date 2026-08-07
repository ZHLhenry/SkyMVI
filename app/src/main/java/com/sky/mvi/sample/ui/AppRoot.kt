package com.sky.mvi.sample.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sky.mvi.sample.ui.detail.DetailRoute
import com.sky.mvi.sample.ui.home.HomeRoute
import com.sky.mvi.sample.ui.login.LoginRoute

/**
 * 应用根导航：login -> home -> detail/{id}
 *
 * 页面跳转完全由 ViewModel 发出的 [com.sky.mvi.mvi.navigation.NavigationEffect] 驱动，
 * 各 Route 通过 [com.sky.mvi.mvi.rememberMviEffectHandler] 在 MviScreen 的 onEffect 内自动处理，
 * 不再需要手写 onLoginSuccess / onNavigateDetail 等回调。
 */
@Composable
fun AppRoot() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.Login.pattern) {
        composable(Routes.Login.pattern) {
            LoginRoute(navController = navController)
        }
        composable(Routes.Home.pattern) {
            HomeRoute(navController = navController)
        }
        composable(
            route = Routes.Detail.pattern,
            arguments = listOf(navArgument("id") { type = NavType.IntType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("id") ?: 0
            DetailRoute(articleId = id, navController = navController)
        }
    }
}
