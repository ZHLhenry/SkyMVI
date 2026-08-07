package com.sky.mvi.sample.ui.home

import androidx.navigation.NavHostController
import com.sky.mvi.core.model.ArticleBean
import com.sky.mvi.mvi.PageState
import com.sky.mvi.mvi.UiEffect
import com.sky.mvi.mvi.UiIntent
import com.sky.mvi.mvi.UiState
import com.sky.mvi.mvi.navigation.NavigationEffect
import com.sky.mvi.mvi.navigation.navigateTo
import com.sky.mvi.sample.ui.Routes
import com.sky.mvi.widget.toast.ToastEffect

/**
 * 首页文章列表 MVI 契约
 */
data class HomeState(
    val pageState: PageState = PageState.Loading,
    val datas: List<ArticleBean> = emptyList(),
    val curPage: Int = 0,
    val pageCount: Int = 0,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val loadMoreError: String? = null
) : UiState

sealed interface HomeIntent : UiIntent {
    data object Refresh : HomeIntent
    data object LoadMore : HomeIntent
    data class ItemClick(val article: ArticleBean) : HomeIntent
}

sealed interface HomeEffect : UiEffect {
    data class ShowToast(val msg: String) : HomeEffect, ToastEffect {
        override val message: String get() = msg
    }

    /** 跳转详情页，由 [com.sky.mvi.mvi.rememberMviEffectHandler] 自动分发 */
    data class NavigateDetail(val id: Int) : HomeEffect, NavigationEffect {
        override fun handle(controller: NavHostController) =
            controller.navigateTo(Routes.Detail.build(id))
    }
}
