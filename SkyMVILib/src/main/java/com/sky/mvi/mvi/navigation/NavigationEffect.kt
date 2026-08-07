package com.sky.mvi.mvi.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.sky.mvi.mvi.UiEffect
import com.sky.mvi.mvi.compose.CollectEffect
import kotlinx.coroutines.flow.Flow

/**
 * 可交由导航框架自动处理的副作用标记接口。
 *
 * 实现该接口的 [UiEffect] 会被 [HandleNavigationEffects] / [com.sky.mvi.mvi.rememberMviEffectHandler]
 * 自动转成对应的导航动作，从而把「页面跳转」从屏幕上的 `onNavigateXxx` 回调里解放出来，
 * 让 ViewModel 成为跳转逻辑的唯一来源。
 *
 * ```
 * sealed interface HomeEffect : UiEffect {
 *     data class NavigateDetail(val id: Int) : HomeEffect, NavigationEffect {
 *         override fun handle(controller: NavHostController) = controller.navigateTo("detail/$id")
 *     }
 * }
 * ```
 */
interface NavigationEffect : UiEffect {
    /** 执行该导航副作用，[controller] 为当前 NavHostController */
    fun handle(controller: NavHostController)
}

/**
 * 生命周期感知地收集 [UiEffect]，自动处理 [NavigationEffect]，其余交回 [onEffect]。
 *
 * 注意：[UiEffect] 由 Channel 驱动，只能被「一个」收集器消费。
 * 当屏幕已通过 [com.sky.mvi.mvi.compose.MviScreen] 收集副作用时，
 * 请勿再额外调用本方法，改用 [com.sky.mvi.mvi.rememberMviEffectHandler] 传入 MviScreen 的 onEffect。
 *
 * @param navController 当前导航控制器
 * @param effectFlow 副作用流（通常是 `viewModel.effect`）
 * @param onEffect 非导航类副作用的兜底处理
 */
@Composable
fun HandleNavigationEffects(
    navController: NavHostController,
    effectFlow: Flow<UiEffect>,
    onEffect: (UiEffect) -> Unit = {}
) {
    CollectEffect(flow = effectFlow) { effect ->
        if (effect is NavigationEffect) {
            effect.handle(navController)
        } else {
            onEffect(effect)
        }
    }
}
