package com.sky.mvi.network.interceptor
import com.sky.mvi.SkyMVILib
import com.sky.mvi.base.BaseApplication.Companion.app
import com.sky.mvi.util.NetworkUtil
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.Response

/**
 * @Class: CacheInterceptor
 * @Author: Henry
 * @Date: 2025/2/23 10:20
 * @Description: OkHttp缓存拦截器，无网络时强制使用缓存，有网络时设置缓存策略
 */

class CacheInterceptor(var day: Int = 7) : Interceptor {
    private val TAG = "CacheInterceptor"

    init {
        SkyMVILib.requireInit()
    }
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        if (!NetworkUtil.isNetworkAvailable(app)) {
            request = request.newBuilder()
                .cacheControl(CacheControl.FORCE_CACHE)
                .build()
        }
        val response = chain.proceed(request)
        // 必须接收 newBuilder().build() 的结果再返回，否则缓存头不会生效
        return if (!NetworkUtil.isNetworkAvailable(app)) {
            // 离线：仅返回已缓存内容，并容忍 day 天的过期数据
            val maxStale = 60 * 60 * 24 * day
            response.newBuilder()
                .removeHeader("Pragma")
                .header("Cache-Control", "public, only-if-cached, max-stale=$maxStale")
                .build()
        } else {
            // 在线：缓存 1 小时
            val maxAge = 60 * 60
            response.newBuilder()
                .removeHeader("Pragma")
                .header("Cache-Control", "public, max-age=$maxAge")
                .build()
        }
    }
}