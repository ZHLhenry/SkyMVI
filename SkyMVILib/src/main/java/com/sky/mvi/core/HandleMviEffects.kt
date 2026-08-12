package com.sky.mvi.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import com.sky.mvi.core.compose.SkyCollectEffect
import com.sky.mvi.core.navigation.SkyHandleNavigationEffects
import com.sky.mvi.core.navigation.SkyNavigationEffect
import com.sky.mvi.widget.toast.SkyHandleToastEffects
import com.sky.mvi.widget.toast.SkyToastEffect

/**
 * @Class: HandleMviEffects
 * @Author: Henry
 * @Date: 2026/08/03
 * @Description: 通用 MVI Effect 处理器，自动分发导航与 Toast 两类标准副作用
 *
 * 设计用于配合 [com.sky.mvi.core.compose.SkyMviScreen] 的 `onEffect` 参数——
 * 它不会自行启动新的收集协程，从而避免与 SkyMviScreen 内部的唯一 [SkyCollectEffect]
 * 竞争消费 [SkyUiEffect]（[SkyUiEffect] 由 Channel 驱动，只能被一个订阅者消费）。
 *
 * 处理顺序：先处理导航，再处理 Toast，最后交给调用方自定义兜底处理。
 * 如果实现了 [SkyNavigationEffect] 或 [SkyToastEffect]，会自动被对应的处理器消费，
 * 否则回调到 [onUnhandled]。
 *
 * ```
 * val navController = rememberNavController()
 * val onEffect = rememberSkyMviEffectHandler(navController)
 * SkyMviScreen(viewModel = vm, onEffect = onEffect) { state, onIntent -> ... }
 * ```
 */
@Composable
fun rememberSkyMviEffectHandler(
    navController: NavHostController,
    onUnhandled: (SkyUiEffect) -> Unit = {}
): (SkyUiEffect) -> Unit {
    val context = LocalContext.current
    return remember(navController, onUnhandled) {
        { effect ->
            when (effect) {
                is SkyNavigationEffect -> SkyHandleNavigationEffects(navController, effect)
                is SkyToastEffect -> SkyHandleToastEffects(context, effect)
                else -> onUnhandled(effect)
            }
        }
    }
}

/**
 * 带自定义 effect 收集的版本，适合需要在通用处理之外追加页面级回调的场景。
 * 本函数会自行启动 [SkyCollectEffect]，调用方无需再套一层 [SkyMviScreen]。
 */
@Composable
fun <S : SkyUiState, I : SkyUiIntent, E : SkyUiEffect> SkyHandleMviEffects(
    viewModel: SkyBaseMviViewModel<S, I, E>,
    navController: NavHostController,
    customHandler: (SkyUiEffect) -> Unit = {}
) {
    val context = LocalContext.current
    SkyCollectEffect(viewModel.effect) { effect ->
        when (effect) {
            is SkyNavigationEffect -> SkyHandleNavigationEffects(navController, effect)
            is SkyToastEffect -> SkyHandleToastEffects(context, effect)
            else -> customHandler(effect)
        }
    }
}
