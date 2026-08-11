package com.sky.mvi.sample.ui

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sky.mvi.core.common.FlowKeys
import com.sky.mvi.core.common.TokenExpiredEvent
import com.sky.mvi.flow.SkyFlow
import com.sky.mvi.sample.ui.detail.DetailRoute
import com.sky.mvi.sample.ui.home.HomeRoute
import com.sky.mvi.sample.ui.login.LoginRoute

/**
 * 应用根导航：home -> detail/{id}（默认首页为 Home）。
 *
 * 当拦截器检测到 Token 过期时，通过 SkyFlow 广播 [TokenExpiredEvent]，
 * AppRoot 订阅该事件后自动 popBackStack 并跳转到 LoginScreen。
 *
 * 页面跳转完全由 ViewModel 发出的 [com.sky.mvi.core.navigation.SkyNavigationEffect] 驱动，
 * 各 Route 通过 [com.sky.mvi.core.rememberSkyMviEffectHandler] 在 SkyMviScreen 的 onEffect 内自动处理。
 */
@Composable
fun AppRoot() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.Home.pattern) {
        composable(Routes.Home.pattern) {
            HomeRoute(navController = navController)
            // 全局订阅「登录失效」事件：收到后提示原因并清空回退栈、跳转登录页
            val lifecycleOwner = LocalLifecycleOwner.current
            val context = LocalContext.current
            LaunchedEffect(Unit) {
                SkyFlow.withStick<TokenExpiredEvent>(FlowKeys.TOKEN_EXPIRED)
                    .register(lifecycleOwner = lifecycleOwner) { event ->
                        // 把拦截器下发的过期原因 Toast 出来
                        Toast.makeText(context, event.reason, Toast.LENGTH_LONG).show()
                        // 清空回退栈，确保用户无法通过返回键回到已失效的页面
                        navController.popBackStack(Routes.Home.pattern, inclusive = true)
                        navController.navigate(Routes.Login.pattern) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
            }
        }
        composable(Routes.Login.pattern) {
            LoginRoute(navController = navController)
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
