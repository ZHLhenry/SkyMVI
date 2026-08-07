package com.sky.mvi.sample.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.sky.mvi.core.model.ArticleBean
import com.sky.mvi.mvi.compose.LaunchedIntent
import com.sky.mvi.mvi.compose.MviScreen
import com.sky.mvi.mvi.rememberMviEffectHandler
import com.sky.mvi.widget.refresh.RefreshListWidget
import com.sky.mvi.widget.state.PageStateLayout

/**
 * 首页文章列表路由：装配 ViewModel、副作用处理器与首屏加载 Intent。
 */
@Composable
fun HomeRoute(
    navController: NavHostController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val onEffect = rememberMviEffectHandler(navController)
    // 进入页面即触发首屏加载
    LaunchedIntent(viewModel, HomeIntent.Refresh)
    MviScreen(
        viewModel = viewModel,
        onEffect = onEffect
    ) { state, onIntent ->
        HomeContent(state = state, onIntent = onIntent)
    }
}

@Composable
private fun HomeContent(
    state: HomeState,
    onIntent: (HomeIntent) -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("SkyMVI 文章列表") }) }
    ) { padding ->
        PageStateLayout(
            pageState = state.pageState,
            onRetry = { onIntent(HomeIntent.Refresh) },
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            RefreshListWidget(
                items = state.datas,
                isRefreshing = state.isRefreshing,
                isLoadingMore = state.isLoadingMore,
                hasMore = state.curPage < state.pageCount,
                onRefresh = { onIntent(HomeIntent.Refresh) },
                onLoadMore = { onIntent(HomeIntent.LoadMore) },
                key = { it.id },
                itemContent = { article ->
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
private fun ArticleItem(article: ArticleBean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Text(article.title, style = MaterialTheme.typography.titleMedium, maxLines = 2)
        Spacer(Modifier.height(4.dp))
        Text(
            text = "${article.author.ifBlank { article.shareUser }} · ${article.niceDate} · ${article.chapterName}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
        Spacer(Modifier.height(12.dp))
        HorizontalDivider()
    }
}
