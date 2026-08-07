package com.sky.mvi.mvi

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import com.sky.mvi.mvi.navigation.NavigationEffect
import com.sky.mvi.widget.toast.ToastEffect

/**
 * 返回一个副作用处理器，统一管理「导航 + Toast」两类最常见的副作用。
 *
 * 设计用于配合 [com.sky.mvi.mvi.compose.MviScreen] 的 `onEffect` 参数——
 * 它不会自行启动新的收集协程，从而避免与 MviScreen 内部的唯一 `CollectEffect`
 * 竞争消费 [UiEffect]（[UiEffect] 由 Channel 驱动，只能被一个订阅者消费）。
 *
 * ```
 * val navController = rememberNavController()
 * val onEffect = rememberMviEffectHandler(navController)
 * MviScreen(viewModel = vm, onEffect = onEffect) { state, onIntent -> ... }
 * ```
 *
 * @param navController 导航控制器，用于分发 [NavigationEffect]
 * @param onUnhandled 既非导航也非 Toast 的副作用兜底处理
 */
@Composable
fun rememberMviEffectHandler(
    navController: NavHostController,
    onUnhandled: (UiEffect) -> Unit = {}
): (UiEffect) -> Unit {
    val context = LocalContext.current
    return remember(navController, onUnhandled) {
        { effect ->
            when (effect) {
                is NavigationEffect -> effect.handle(navController)
                is ToastEffect -> Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                else -> onUnhandled(effect)
            }
        }
    }
}
