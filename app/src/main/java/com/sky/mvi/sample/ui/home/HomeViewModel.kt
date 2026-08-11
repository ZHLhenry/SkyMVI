package com.sky.mvi.sample.ui.home

import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.sky.mvi.core.common.net.ApiService
import com.sky.mvi.core.model.ArticleBean
import com.sky.mvi.core.SkyBaseMviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject

/**
 * 首页列表 ViewModel：列表数据交给 Paging 3 驱动，ViewModel 仅负责
 * - 持有 [articles]（`Pager` + `ArticlePagingSource` 的 `PagingData` 流）；
 * - 通过 [errors] 把分页加载失败信息抛给 UI 做 Toast 提示。
 *
 * 下拉刷新（`LazyPagingItems.refresh()`）与上拉加载更多均由 `SkyRefreshPagingLayout`
 * 触发，不再走 Intent 分发。
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val api: ApiService
) : SkyBaseMviViewModel<HomeState, HomeIntent, HomeEffect>() {

    private val _errors = MutableSharedFlow<String>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    /** 分页加载失败的错误信息流，供 UI 转成 Toast */
    val errors: Flow<String> = _errors.asSharedFlow()

    val articles: Flow<PagingData<ArticleBean>> = Pager(
        config = PagingConfig(pageSize = 20, enablePlaceholders = false)
    ) {
        ArticlePagingSource(api) { _errors.tryEmit(it) }
    }.flow.cachedIn(viewModelScope)

    override fun initialState() = HomeState

    override fun handleIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.ItemClick -> sendEffect(HomeEffect.NavigateDetail(intent.article.id))
        }
    }
}
