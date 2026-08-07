package com.sky.mvi.core.common.net.interceptor

import com.sky.mvi.core.common.ErrorCode.ERROR_200
import com.sky.mvi.core.common.net.ApiResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/**
 * Token 校验拦截器：解析响应体，业务码为 [ERROR_200] 时表示登录态有效。
 *
 * 说明：原 SkyMVVM 在此处通过 SkyFlow 广播 token 事件，但 SkyFlow 需要额外
 * `enableSkyFlowLib(true)` 才可用。本示例保持轻量，仅做解析与打点，避免
 * 未在初始化阶段开启 SkyFlow 时触发运行时异常。
 *
 * 注意：直接传入 [ApiResponse]::class.java 会让泛型参数 `T` 被类型擦除，
 * 导致 KotlinJsonAdapterFactory 找不到 T 的 adapter 而抛
 * "No JsonAdapter for T (with no annotations)"。
 * 因此这里用 [Types.newParameterizedType] 显式声明泛型实参（这里只关心
 * errorCode/errorMsg，对 T 用 Any 占位即可）。
 */
class TokenOutInterceptor : Interceptor {

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter<ApiResponse<Any>>(
        Types.newParameterizedType(
            ApiResponse::class.java,
            Any::class.java
        )
    )

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        return if (response.body.contentType() != null) {
            val mediaType = response.body.contentType()
            val string = response.body.string()
            val responseBody = string.toResponseBody(mediaType)
            val apiResponse = adapter.fromJson(string)
            if (apiResponse?.errorCode == ERROR_200) {
                // token 校验通过，可在此触发登录态刷新 / 事件通知
            }
            response.newBuilder().body(responseBody).build()
        } else {
            response
        }
    }
}
