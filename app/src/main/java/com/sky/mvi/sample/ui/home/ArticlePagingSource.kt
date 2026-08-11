package com.sky.mvi.sample.ui.home

import com.sky.mvi.core.common.net.ApiService
import com.sky.mvi.core.common.paging.BasePagingSource
import com.sky.mvi.core.model.ArticleBean

/**
 * 首页文章列表的 Paging 3 数据源。
 *
 * 仅描述「数据从哪来、怎么判断到底」，分页机制（key 推进、刷新锚点、错误包装、空列表兜底）
 * 全部由 [BasePagingSource] 负责。
 *
 * - wanandroid 的 `article/list/{currentPage}/json` 路径参数是 0-based（list/0/json 即第 1 页）；
 * - 响应 `curPage` 是 1-based，`pageCount` 为总页数，二者由 [BasePage] 透出。
 *
 * @param api 示例接口
 * @param onError 加载失败时回传错误信息（用于 Toast 提示），可选
 */
class ArticlePagingSource(
    private val api: ApiService,
    onError: ((String) -> Unit)? = null
) : BasePagingSource<ArticleBean>(onError = onError) {

    override suspend fun loadPage(page: Int): PageResult<ArticleBean> =
        toPageResult(
            response = api.getEntryAndExitDataApi(currentPage = page),
            getList = { datas },
            reachedEnd = { curPage >= pageCount }
        )
}
