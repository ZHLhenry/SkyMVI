package com.sky.mvi.mvi.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sky.mvi.mvi.BaseMviViewModel
import com.sky.mvi.mvi.UiEffect
import com.sky.mvi.mvi.UiIntent
import com.sky.mvi.mvi.UiState

/**
 * @Class: MviScreen
 * @Author: Henry
 * @Date: 2026/08/03
 * @Description: MVI 页面骨架，一次性完成 State 订阅、Effect 消费与 Intent 分发的绑定
 */

/**
 * 生命周期感知地订阅页面状态。
 *
 * 相比 `collectAsState()`，本方法在页面不可见时自动停止收集，避免后台无谓重组。
 */
@Composable
fun <S : UiState, I : UiIntent, E : UiEffect> BaseMviViewModel<S, I, E>.collectState(): State<S> =
    uiState.collectAsStateWithLifecycle()

/**
 * MVI 页面骨架：把「订阅状态 + 消费副作用 + 分发意图」三件事收敛到一处。
 *
 * content 只依赖不可变的 [S] 与 `(I) -> Unit`，不持有 ViewModel，
 * 因此可以直接被 `@Preview` 复用，也便于做 UI 单元测试。
 *
 * ```
 * @Composable
 * fun HomeRoute(viewModel: HomeViewModel = hiltViewModel()) {
 *     MviScreen(
 *         viewModel = viewModel,
 *         onEffect = { effect ->
 *             when (effect) {
 *                 is HomeEffect.ShowToast -> toast(effect.msg)
 *             }
 *         }
 *     ) { state, onIntent ->
 *         HomeContent(state = state, onIntent = onIntent)
 *     }
 * }
 * ```
 *
 * @param viewModel MVI ViewModel
 * @param lifecycleState 消费副作用的最低生命周期状态
 * @param onEffect 一次性副作用处理
 * @param content 页面内容，接收当前状态与意图分发器
 */
@Composable
fun <S : UiState, I : UiIntent, E : UiEffect> MviScreen(
    viewModel: BaseMviViewModel<S, I, E>,
    lifecycleState: Lifecycle.State = Lifecycle.State.STARTED,
    onEffect: suspend (E) -> Unit = {},
    content: @Composable (state: S, onIntent: (I) -> Unit) -> Unit
) {
    val state by viewModel.collectState()

    CollectEffect(
        flow = viewModel.effect,
        lifecycleState = lifecycleState,
        collector = onEffect
    )

    // 分发器随 viewModel 缓存，避免每次重组生成新 lambda 导致子 Composable 无谓重组
    val onIntent = remember(viewModel) { viewModel::sendIntent }

    content(state, onIntent)
}
