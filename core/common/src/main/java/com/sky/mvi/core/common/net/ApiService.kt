package com.sky.mvi.core.common.net

import com.sky.mvi.core.model.ArticleBean
import com.sky.mvi.core.model.ArticleResponseBean
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 示例接口： wanandroid 首页文章列表 / 置顶文章
 */
interface ApiService {

    /**
     * 首页文章列表
     *
     * [pageSize] 选 20：单页即可覆盖一屏可视化区域，避免首屏渲染后立刻触发
     * `RefreshListWidget` 的触底自动加载（首屏 5 条会让屏幕底出现大段空白 + loading
     * widget 一闪而过，给用户"列表缩短 / 翻页无效"的错觉）。
     */
    @GET("article/list/{currentPage}/json")
    suspend fun getEntryAndExitDataApi(
        @Path("currentPage") currentPage: Int = 0,
        @Query("page_size") pageSize: Int = 20
    ): ApiResponse<ArticleResponseBean>

    /**
     * 获取置顶文章集合数据
     */
    @GET("article/top/json")
    suspend fun getTopAritrilList(): ApiResponse<MutableList<ArticleBean>>
}
