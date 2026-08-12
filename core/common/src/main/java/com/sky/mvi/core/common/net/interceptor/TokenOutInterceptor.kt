package com.sky.mvi.core.common.net.interceptor

import com.sky.mvi.core.common.ErrorCode.ERROR_0
import com.sky.mvi.core.common.ErrorCode.TOKEN_EXPIRED
import com.sky.mvi.core.common.FlowKeys
import com.sky.mvi.core.common.TokenExpiredEvent
import com.sky.mvi.core.common.net.ApiResponse
import com.sky.mvi.flow.SkyFlow
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/**
 * Token 校验拦截器：解析响应体中的业务错误码。
 *
 * - [ERROR_0]（errorCode == 0）表示登录态有效，正常放行；
 * - [TOKEN_EXPIRED] 表示 Token 过期 / 登录态失效，通过 SkyFlow 全局事件总线广播
 *   [TokenExpiredEvent]，由应用层（如 AppRoot）订阅并跳转登录页。
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
            when (apiResponse?.errorCode) {
                ERROR_0 -> { /* token 有效，正常放行 */ }
                TOKEN_EXPIRED -> {
                    // 通过 SkyFlow 广播「登录失效」全局事件（fire-and-forget）
                    SkyFlow.withStick<TokenExpiredEvent>(FlowKeys.TOKEN_EXPIRED).post(
                        TokenExpiredEvent(reason = "Token 已过期（拦截器检测到 $TOKEN_EXPIRED）")
                    )
                }
            }
            response.newBuilder().body(responseBody).build()
        } else {
            response
        }
    }
}
