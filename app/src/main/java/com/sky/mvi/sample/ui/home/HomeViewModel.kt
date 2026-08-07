package com.sky.mvi.sample.ui.home

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.sky.mvi.core.common.net.ApiService
import com.sky.mvi.ext.apiRequest
import com.sky.mvi.mvi.BaseMviViewModel
import com.sky.mvi.mvi.PageState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * 首页列表 ViewModel：通过 SkyMVILib 的 [apiRequest] 扩展拉取 wanandroid 文章列表，
 * 将结果归约为 [HomeState]；下拉刷新与触底加载更多均走同一套 Intent 分发。
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val api: ApiService,
    @ApplicationContext private val context: Context
) : BaseMviViewModel<HomeState, HomeIntent, HomeEffect>() {

    override fun initialState() = HomeState()

    override fun handleIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.Refresh -> loadData(0, isRefresh = true)
            is HomeIntent.LoadMore -> loadMore()
            is HomeIntent.ItemClick -> sendEffect(HomeEffect.NavigateDetail(intent.article.id))
        }
    }

    private fun loadData(page: Int, isRefresh: Boolean) {
        // 刷新与加载更多互斥：任一状态进行中都不再发起新请求
        if (currentState.isRefreshing || currentState.isLoadingMore) return
        apiRequest(
            context = context,
            block = { api.getEntryAndExitDataApi(page) },
            onStart = {
                setState {
                    if (isRefresh) copy(isRefreshing = true)
                    else copy(pageState = PageState.Loading)
                }
            },
            success = { resp ->
                setState {
                    val list = if (isRefresh) resp.datas else datas + resp.datas
                    copy(
                        datas = list,
                        curPage = resp.curPage,
                        pageCount = resp.pageCount,
                        isRefreshing = false,
                        pageState = if (list.isEmpty()) {
                            PageState.Empty("暂无文章")
                        } else {
                            PageState.Success
                        }
                    )
                }
            },
            error = { e ->
                setState {
                    if (isRefresh) copy(isRefreshing = false)
                    else copy(pageState = PageState.error(e))
                }
                sendEffect(HomeEffect.ShowToast(e.errorMsg))
            }
        )
    }

    private fun loadMore() {
        // 加载更多与刷新互斥：任一状态进行中都不再触发下一页
        if (currentState.isLoadingMore || currentState.isRefreshing) return
        // wanandroid 协议：
        // - 路径参数 currentPage 是 0-based（list/0/json 表示第 1 页）
        // - 响应字段 curPage 是 1-based，第 N 页请求返回 curPage = N
        // 因此"下一页"对应的路径参数就是当前 resp.curPage，而"已到底"则是 curPage >= pageCount
        if (currentState.curPage >= currentState.pageCount) {
            sendEffect(HomeEffect.ShowToast("没有更多了"))
            return
        }
        val next = currentState.curPage
        setState { copy(isLoadingMore = true, loadMoreError = null) }
        apiRequest(
            context = context,
            block = { api.getEntryAndExitDataApi(next) },
            success = { resp ->
                setState {
                    // 兜底：服务端返回空数组说明数据被截断/被风控，标记到底避免死循环
                    val newList = if (resp.datas.isEmpty()) datas else datas + resp.datas
                    copy(
                        datas = newList,
                        curPage = resp.curPage,
                        pageCount = resp.pageCount,
                        isLoadingMore = false,
                        loadMoreError = null
                    )
                }
            },
            error = { e ->
                setState { copy(isLoadingMore = false, loadMoreError = e.errorMsg) }
                sendEffect(HomeEffect.ShowToast(e.errorMsg))
            }
        )
    }
}
