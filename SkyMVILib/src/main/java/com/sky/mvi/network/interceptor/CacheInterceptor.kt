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
        if (!NetworkUtil.isNetworkAvailable(app)) {
            val maxAge = 60 * 60
            response.newBuilder()
                .removeHeader("Pragma")
                .header("Cache-Control", "public, max-age=$maxAge")
                .build()
        } else {
            val maxStale = 60 * 60 * 24 * day // tolerate 4-weeks stale
            response.newBuilder()
                .removeHeader("Pragma")
                .header("Cache-Control", "public, only-if-cached, max-stale=$maxStale")
                .build()
        }
        return response
    }
}