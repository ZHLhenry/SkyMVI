package com.sky.mvi.sample.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.sky.mvi.core.model.ArticleBean
import com.sky.mvi.core.compose.SkyMviScreen
import com.sky.mvi.core.rememberSkyMviEffectHandler
import com.sky.widget.refresh.SkyRefreshPagingLayout
import com.sky.widget.stateLayout.SkyPageState
import com.sky.widget.stateLayout.SkyPageStateLayout

@Composable
fun HomeRoute(
    navController: NavHostController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val onEffect = rememberSkyMviEffectHandler(navController)
    val lazyPagingItems = viewModel.articles.collectAsLazyPagingItems()

    SkyMviScreen(viewModel = viewModel, onEffect = onEffect) { _, onIntent ->
        // 监听分页错误流，转成 Toast Effect
        LaunchedEffect(Unit) {
            viewModel.errors.collect { msg -> onEffect(HomeEffect.ShowToast(msg)) }
        }
        HomeContent(
            lazyPagingItems = lazyPagingItems,
            onIntent = onIntent,
            onShowToast = { onEffect(HomeEffect.ShowToast(it)) }
        )
    }
}

/** 将 Paging 的 refresh 加载状态映射为 SkyPageStateLayout 所需的状态 */
private fun LazyPagingItems<ArticleBean>.toSkyPageState(): SkyPageState {
    val refresh = loadState.refresh
    return when {
        refresh is LoadState.Loading && itemCount == 0 -> SkyPageState.Loading
        refresh is LoadState.Error -> SkyPageState.Error((refresh.error.message ?: "加载失败"))
        refresh is LoadState.NotLoading && itemCount == 0 -> SkyPageState.Empty("暂无文章")
        else -> SkyPageState.Success
    }
}

@Composable
private fun HomeContent(
    lazyPagingItems: LazyPagingItems<ArticleBean>,
    onIntent: (HomeIntent) -> Unit,
    onShowToast: (String) -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("SkyMVI 文章列表") }) }
    ) { padding ->
        SkyPageStateLayout(
            pageState = lazyPagingItems.toSkyPageState(),
            // 空/错重试：调用 paging 的 retry()，会重新触发 refresh 或 append
            onEmptyRetry = { lazyPagingItems.retry() },
            onErrorRetry = { lazyPagingItems.retry() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            SkyRefreshPagingLayout(
                lazyPagingItems = lazyPagingItems,
                itemKey = { it.id },
                noMoreDataText = "没有更多数据",
                secondFloorRate = 2f,
                onSecondFloor = {
                    onShowToast("已进入二楼")
                },
                content = { article ->
                    ArticleItem(
                        article = article,
                        onClick = { onIntent(HomeIntent.ItemClick(article)) }
                    )
                }
            )
        }
    }
}

@Composable
private fun ArticleItem(
    article: ArticleBean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Text(
            text = article.title,
            style = MaterialTheme.typography.titleMedium
        )
        article.desc.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
    }
}
