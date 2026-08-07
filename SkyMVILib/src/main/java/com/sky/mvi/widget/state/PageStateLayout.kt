package com.sky.mvi.widget.state

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sky.mvi.mvi.PageState

/**
 * 页面状态容器：根据 [pageState] 自动在 加载 / 成功 / 空 / 错误 之间切换。
 *
 * 成功时渲染 [content]，其余状态渲染对应占位（均支持自定义与重试）。
 * 与 [com.sky.mvi.mvi.PageState] 一一对应，是「一个页面四种形态」的标准解法。
 *
 * ```
 * PageStateLayout(
 *     pageState = state.pageState,
 *     onRetry = { onIntent(HomeIntent.Refresh) }
 * ) {
 *     ArticleList(items = state.datas)
 * }
 * ```
 *
 * @param pageState 当前页面状态
 * @param onRetry 重试回调（空 / 错误占位按钮共用）
 * @param loading 加载态自定义内容，默认 [LoadingWidget]
 * @param empty 空态自定义内容，默认 [EmptyWidget]
 * @param error 错误态自定义内容，默认 [ErrorWidget]
 * @param content 成功态内容
 */
@Composable
fun PageStateLayout(
    pageState: PageState,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit = {},
    loading: @Composable () -> Unit = { LoadingWidget() },
    empty: @Composable () -> Unit = { EmptyWidget(onRetry = onRetry) },
    error: @Composable (message: String) -> Unit = { msg -> ErrorWidget(message = msg, onRetry = onRetry) },
    content: @Composable () -> Unit
) {
    when (pageState) {
        PageState.Loading -> Box(modifier = modifier) { loading() }
        is PageState.Empty -> Box(modifier = modifier) { empty() }
        is PageState.Error -> Box(modifier = modifier) { error(pageState.message) }
        PageState.Success -> Box(modifier = modifier) { content() }
    }
}
