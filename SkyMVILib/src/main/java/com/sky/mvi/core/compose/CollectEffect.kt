package com.sky.mvi.core.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.sky.mvi.core.SkyBaseMviViewModel
import com.sky.mvi.core.SkyUiEffect
import com.sky.mvi.core.SkyUiIntent
import com.sky.mvi.core.SkyUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * @Class: SkyCollectEffect
 * @Author: Henry
 * @Date: 2026/08/03
 * @Description: Compose 侧副作用收集器，在指定生命周期内安全消费一次性事件
 */

/**
 * 生命周期感知地收集一次性副作用。
 *
 * 页面进入后台时自动暂停收集，回到前台后恢复，期间的事件由 Channel 缓存不会丢失。
 *
 * ```
 * SkyCollectEffect(viewModel.effect) { effect ->
 *     when (effect) {
 *         is HomeEffect.ShowToast -> snackbarHostState.showSnackbar(effect.msg)
 *         is HomeEffect.NavigateDetail -> navController.navigate("detail/${effect.id}")
 *     }
 * }
 * ```
 *
 * @param flow 副作用流
 * @param lifecycleState 收集所处的最低生命周期状态，默认 STARTED（页面可见）
 * @param collector 事件处理逻辑
 */
@Composable
fun <T> SkyCollectEffect(
    flow: Flow<T>,
    lifecycleState: Lifecycle.State = Lifecycle.State.STARTED,
    collector: suspend (T) -> Unit
) {
    val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
    // key 使用 flow 与 lifecycleOwner，避免每次重组都重启收集协程
    LaunchedEffect(flow, lifecycleOwner, lifecycleState) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(lifecycleState) {
            flow.collect { collector(it) }
        }
    }
}

/**
 * 页面首次进入时发送一次初始化 Intent（如首屏加载）。
 *
 * 使用 `LaunchedEffect(Unit)` 保证仅在进入组合时触发一次，
 * 屏幕旋转导致 Activity 重建时，因 ViewModel 存活、State 保留，不会重复请求。
 *
 * ```
 * SkyLaunchedIntent(viewModel, HomeIntent.Refresh)
 * ```
 */
@Composable
fun <S : SkyUiState, I : SkyUiIntent, E : SkyUiEffect> SkyLaunchedIntent(
    viewModel: SkyBaseMviViewModel<S, I, E>,
    intent: I
) {
    LaunchedEffect(Unit) {
        viewModel.sendIntent(intent)
    }
}

/**
 * 返回一个可在任意 Compose 回调中直接调用的 Intent 分发器。
 *
 * 相比在 Composable 中直接持有 ViewModel，向下传递 `(I) -> Unit` 的分发器
 * 能让子 Composable 与 ViewModel 解耦，便于 Preview 与测试。
 *
 * ```
 * val dispatch = rememberSkyIntentDispatcher(viewModel)
 * HomeContent(state = state, onIntent = dispatch)
 * ```
 */
@Composable
fun <S : SkyUiState, I : SkyUiIntent, E : SkyUiEffect> rememberSkyIntentDispatcher(
    viewModel: SkyBaseMviViewModel<S, I, E>
): (I) -> Unit {
    val scope = rememberCoroutineScope()
    return { intent ->
        scope.launch { viewModel.sendIntent(intent) }
    }
}
