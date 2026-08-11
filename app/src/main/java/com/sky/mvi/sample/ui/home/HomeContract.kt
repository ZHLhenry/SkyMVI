package com.sky.mvi.sample.ui.home

import androidx.navigation.NavHostController
import com.sky.mvi.core.model.ArticleBean
import com.sky.mvi.core.SkyUiEffect
import com.sky.mvi.core.SkyUiIntent
import com.sky.mvi.core.SkyUiState
import com.sky.mvi.core.navigation.SkyNavigationEffect
import com.sky.mvi.core.navigation.skyNavigateTo
import com.sky.mvi.widget.toast.SkyToastEffect
import com.sky.mvi.sample.ui.Routes

/**
 * 首页文章列表 MVI 契约。
 *
 * 列表数据改由 Paging 3 驱动（[HomeViewModel.articles]），因此不再持有
 * `datas / curPage / pageCount / isRefreshing / isLoadingMore` 等手写分页字段，
 * 刷新与触底加载更多全部由 `SkyRefreshPagingLayout` + `LazyPagingItems` 接管。
 */
data object HomeState : SkyUiState

sealed interface HomeIntent : SkyUiIntent {
    data class ItemClick(val article: ArticleBean) : HomeIntent
}

sealed interface HomeEffect : SkyUiEffect {
    data class ShowToast(val msg: String) : HomeEffect, SkyToastEffect {
        override val message: String get() = msg
    }

    /** 跳转详情页，由 [com.sky.mvi.core.rememberSkyMviEffectHandler] 自动分发 */
    data class NavigateDetail(val id: Int) : HomeEffect, SkyNavigationEffect {
        override fun handle(controller: NavHostController) =
            controller.skyNavigateTo(Routes.Detail.build(id))
    }
}
