package com.sky.mvi.widget.refresh

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sky.mvi.widget.state.EmptyWidget
import com.sky.mvi.widget.state.LoadingMoreWidget
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/**
 * 通用下拉刷新 + 触底加载更多列表。
 *
 * - [isRefreshing] 控制下拉刷新指示器；
 * - 列表滚到倒数第 2 项且 [hasMore] 为真时自动触发 [onLoadMore]；
 * - 数据为空且非刷新中时展示 [emptyContent]。
 *
 * 通常配合 [com.sky.mvi.widget.state.PageStateLayout] 使用：
 * 首屏 Loading / 错误 交给外层 PageStateLayout，Success 时再渲染本控件。
 *
 * @param items 列表数据
 * @param isRefreshing 是否正在下拉刷新
 * @param isLoadingMore 是否正在加载更多（底部进度条）
 * @param hasMore 是否还有下一页
 * @param onRefresh 下拉刷新回调
 * @param onLoadMore 触底加载更多回调
 * @param key 列表项稳定主键，便于 Compose 复用
 * @param emptyContent 空数据占位
 * @param itemContent 单条渲染
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> RefreshListWidget(
    items: List<T>,
    isRefreshing: Boolean,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
    key: ((item: T) -> Any)? = null,
    emptyContent: @Composable () -> Unit = { EmptyWidget() },
    itemContent: @Composable (item: T) -> Unit
) {
    val listState: LazyListState = rememberLazyListState()

    // 使用 rememberUpdatedState 包装普通参数，使 snapshotFlow 能读取到最新值
    val isRefreshingState = rememberUpdatedState(isRefreshing)
    val isLoadingMoreState = rememberUpdatedState(isLoadingMore)
    val hasMoreState = rememberUpdatedState(hasMore)

    // 触底自动加载更多：滚到倒数第 2 项且仍可加载时触发
    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            val total = info.totalItemsCount
            hasMoreState.value &&
                !isLoadingMoreState.value &&
                !isRefreshingState.value &&
                total > 0 &&
                last >= total - 2
        }
            .distinctUntilChanged()
            .filter { it }
            .collect { onLoadMore() }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (items.isEmpty() && !isRefreshing) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                emptyContent()
            }
        } else {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    items(
                        count = items.size,
                        key = key?.let { k -> { index -> k(items[index]) } }
                    ) { index ->
                        itemContent(items[index])
                    }
                    if (isLoadingMore) {
                        item {
                            LoadingMoreWidget(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
