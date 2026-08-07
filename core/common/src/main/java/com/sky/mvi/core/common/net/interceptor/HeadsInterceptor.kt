package com.sky.mvi.core.common.net.interceptor

import okhttp3.Interceptor
import okhttp3.Response

/**
 * 公共请求头拦截器
 */
class HeadsInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val builder = chain.request().newBuilder()
        builder.addHeader("token", "123456789").build()
        return chain.proceed(builder.build())
    }
}
