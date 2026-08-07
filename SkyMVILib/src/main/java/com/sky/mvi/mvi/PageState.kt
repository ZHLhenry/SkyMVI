package com.sky.mvi.mvi

import androidx.compose.runtime.Immutable
import com.sky.mvi.network.AppException

/**
 * @Class: PageState
 * @Author: Henry
 * @Date: 2026/08/03
 * @Description: 页面级 UI 状态密封类，描述首屏加载、成功、空数据、错误四种形态
 *
 * 与 [com.sky.mvi.network.state.ResultState] 的分工：
 * - ResultState 描述「一次请求的结果」，属于数据层语义；
 * - PageState 描述「页面此刻该渲染什么」，属于 UI 层语义，是 UiState 的一个字段。
 */

@Immutable
sealed interface PageState {

    /** 首屏加载中（骨架屏 / 转圈） */
    data object Loading : PageState

    /** 加载成功且有数据 */
    data object Success : PageState

    /** 请求成功但结果为空 */
    data class Empty(val message: String? = null) : PageState

    /** 加载失败 */
    data class Error(
        val message: String,
        val code: Int = -1,
        val throwable: Throwable? = null
    ) : PageState

    val isLoading: Boolean get() = this is Loading
    val isSuccess: Boolean get() = this is Success
    val isEmpty: Boolean get() = this is Empty
    val isError: Boolean get() = this is Error

    companion object {
        /**
         * 由 [AppException] 构造错误态
         */
        fun error(e: AppException): Error = Error(
            message = e.errorMsg,
            code = e.errCode,
            throwable = e.throwable
        )

        /**
         * 根据数据是否为空，自动返回 Success 或 Empty
         */
        fun of(isEmpty: Boolean, emptyMessage: String? = null): PageState =
            if (isEmpty) Empty(emptyMessage) else Success
    }
}

/**
 * 列表分页加载状态，配合下拉刷新 / 上拉加载使用
 */
@Immutable
data class LoadMoreState(
    /** 是否正在下拉刷新 */
    val isRefreshing: Boolean = false,
    /** 是否正在加载更多 */
    val isLoadingMore: Boolean = false,
    /** 是否还有下一页 */
    val hasMore: Boolean = true,
    /** 当前页码 */
    val page: Int = FIRST_PAGE
) {
    companion object {
        const val FIRST_PAGE = 1
    }

    /** 是否处于可继续加载更多的状态 */
    val canLoadMore: Boolean get() = hasMore && !isLoadingMore && !isRefreshing

    /** 重置为首页状态 */
    fun reset(): LoadMoreState = LoadMoreState()

    /** 一页加载完成后推进页码 */
    fun next(hasMore: Boolean): LoadMoreState = copy(
        isRefreshing = false,
        isLoadingMore = false,
        hasMore = hasMore,
        page = page + 1
    )
}
