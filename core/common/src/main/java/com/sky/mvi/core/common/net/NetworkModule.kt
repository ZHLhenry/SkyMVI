package com.sky.mvi.core.common.net

import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.sky.mvi.base.BaseApplication
import com.sky.mvi.core.common.AppConfig
import com.sky.mvi.core.common.net.interceptor.HeadsInterceptor
import com.sky.mvi.core.common.net.interceptor.TokenOutInterceptor
import com.sky.mvi.network.BaseNetworkApi
import com.sky.mvi.network.dns.SkyDnsParser
import com.sky.mvi.network.interceptor.CacheInterceptor
import com.sky.mvi.network.log.AndroidLoggingInterceptor
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * 全局网络模块：基于 SkyMVILib 的 [BaseNetworkApi] 配置 OkHttp 与 Retrofit，
 * 并通过 Hilt 以单例形式对外提供 [ApiService]。
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule : BaseNetworkApi() {

    override fun setHttpClientBuilder(builder: OkHttpClient.Builder): OkHttpClient.Builder {
        builder.apply {
            /** 备用 DNS 解析器：VPN 环境下系统 DNS 失败时回退到公共 DNS */
            dns(SkyDnsParser())
            /** 超时时间：连接、读、写 */
            connectTimeout(AppConfig.DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            readTimeout(AppConfig.DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            writeTimeout(AppConfig.DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            /** 缓存：最大 10M */
            cache(Cache(File(BaseApplication.app.cacheDir, AppConfig.CACHE_ID), 10 * 1024 * 1024))
            /** 缓存拦截器，缓存 1 天 */
            addInterceptor(CacheInterceptor(1))
            /** 公共请求头（需放在日志拦截器之前，否则 Log 不显示 head 信息） */
            addInterceptor(HeadsInterceptor())
            /** token 过期拦截器 */
            addInterceptor(TokenOutInterceptor())
            /** Chucker 调试面板（仅非生产环境） */
            if (!AppConfig.IS_PROD) {
                addInterceptor(ChuckerInterceptor.Builder(BaseApplication.app).build())
            }
            /** 日志拦截器 */
            addInterceptor(AndroidLoggingInterceptor.build())
        }
        return builder
    }

    override fun setRetrofitBuilder(builder: Retrofit.Builder): Retrofit.Builder {
        return builder.apply {
            addConverterFactory(
                MoshiConverterFactory.create(
                    Moshi.Builder()
                        .addLast(KotlinJsonAdapterFactory())
                        .build()
                )
            )
        }
    }

    @Provides
    fun provideApiService(): ApiService {
        return getApi(ApiService::class.java, AppConfig.BASE_URL, false)
    }
}
