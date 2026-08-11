package com.sky.mvi.core.common.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.sky.mvi.network.BaseResponse

/**
 * 通用分页数据源基类（页码固定为 [Int]，0-based 起始页）。
 *
 * 把 Paging 3 的「分页机制」（key 推进、刷新锚点、错误包装、空列表兜底）与
 * 「具体数据来源」解耦：子类/调用方只需提供 [loadPage]，描述「给定页码如何取一页数据」。
 *
 * 适用于任何列表场景——无论 [Value] 是 [com.sky.mvi.core.model.ArticleBean] 还是
 * 其他业务实体，无论数据来自 `getEntryAndExitDataApi` 还是其它接口，都不再需要
 * 重复编写 `PagingSource` 的样板代码。
 *
 * 页码统一用 [Int]（绝大多数分页接口如此）。若确有非 [Int] 页码需求，直接继承
 * `PagingSource` 自行实现即可，不必为本库增加复杂度。
 *
 * @param Value 列表单项类型
 * @param startKey 首屏加载时的起始页码，默认 0
 * @param onError 加载失败时回传错误信息（用于 Toast 提示），可选
 */
abstract class BasePagingSource<Value : Any>(
    private val startKey: Int = 0,
    private val onError: ((String) -> Unit)? = null
) : PagingSource<Int, Value>() {

    /**
     * 单页加载结果。由 [loadPage] 返回，描述「本页数据」以及「是否还有下一页」。
     *
     * @param data 本页数据列表
     * @param reachedEnd 是否已到末尾（true 表示不再请求下一页）。
     *   空列表默认视为到底，避免服务端截断/风控导致死循环。
     */
    data class PageResult<Value : Any>(
        val data: List<Value>,
        val reachedEnd: Boolean = data.isEmpty()
    )

    /**
     * 拉取指定页码的一页数据。子类负责调用具体 API、解析响应、判断到底条件。
     *
     * 实现示例（wanandroid 文章列表）：
     * ```kotlin
     * override suspend fun loadPage(page: Int): PageResult<ArticleBean> {
     *     val resp = api.getEntryAndExitDataApi(currentPage = page)
     *     return if (resp.isSucces()) {
     *         PageResult(resp.data.datas, resp.data.curPage >= resp.data.pageCount)
     *     } else {
     *         error(resp.errorMsg)   // 抛异常会被基类统一包装为 LoadResult.Error
     *     }
     * }
     * ```
     */
    protected abstract suspend fun loadPage(page: Int): PageResult<Value>

    /** 由 [page] 推导上一页 key：默认首屏（[startKey]）返回 null，其余返回 [page]-1 */
    protected open fun prevKeyOf(page: Int): Int? = if (page == startKey) null else page - 1

    /** 由 [page] 推导下一页 key：已到底返回 null，否则 [page]+1 */
    protected open fun nextKeyOf(page: Int, reachedEnd: Boolean): Int? =
        if (reachedEnd) null else page + 1

    final override fun getRefreshKey(state: PagingState<Int, Value>): Int? {
        return state.anchorPosition?.let { anchor ->
            state.closestPageToPosition(anchor)?.let { page ->
                // 以当前锚点最接近的页为基准，向前后各退一页作为刷新锚点
                page.prevKey?.plus(1) ?: page.nextKey?.minus(1)
            }
        }
    }

    final override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Value> {
        val page = params.key ?: startKey
        return try {
            val result = loadPage(page)
            LoadResult.Page(
                data = result.data,
                prevKey = prevKeyOf(page),
                nextKey = nextKeyOf(page, result.reachedEnd)
            )
        } catch (e: Exception) {
            onError?.invoke(e.message ?: "加载失败")
            LoadResult.Error(e)
        }
    }

    /**
     * 针对 [BaseResponse] 统一响应体的便捷封装：把「成功取列表 + 到底判断」封装为 [PageResult]。
     *
     * 适用于接口返回 `ApiResponse<out BasePage>` 且其列表字段可经 [getList] 提取的场景。
     *
     * @param response 接口原始响应
     * @param getList 从响应 data 中析出本页列表
     * @param reachedEnd 额外的「到底」判断（如 `curPage >= pageCount`），默认仅按空列表兜底
     */
    protected inline fun <T> toPageResult(
        response: BaseResponse<T>,
        crossinline getList: T.() -> List<Value>,
        crossinline reachedEnd: T.() -> Boolean = { false }
    ): PageResult<Value> {
        if (!response.isSucces()) error(response.getResponseMsg())
        val data = response.getResponseData()
        val list = data.getList()
        return PageResult(list, reachedEnd(data) || list.isEmpty())
    }
}
