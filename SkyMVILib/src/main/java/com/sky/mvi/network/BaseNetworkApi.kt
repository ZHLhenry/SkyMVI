package com.sky.mvi.network
import com.sky.mvi.SkyMVILib
import com.sky.mvi.util.HttpsCerUtils
import okhttp3.OkHttpClient
import retrofit2.Retrofit

/**
 * @Class: BaseNetworkApi
 * @Author: Henry
 * @Date: 2025/2/23 10:30
 * @Description: 网络API基类，封装Retrofit和OkHttpClient创建，支持HTTPS忽略证书模式
 */

abstract class BaseNetworkApi {

    fun <T> getApi(serviceClass: Class<T>, baseUrl: String, type: Boolean): T {
        SkyMVILib.requireInit()
        //根据type判断使用哪个okHttpClient
        val okHttpClient = if (type) okHttpClientHttps else okHttpClient
        val retrofitBuilder = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
        return setRetrofitBuilder(retrofitBuilder).build().create(serviceClass)
    }

    /**
     * 实现重写父类的setHttpClientBuilder方法，
     * 在这里可以添加拦截器，可以对 OkHttpClient.Builder 做任意操作
     */
    abstract fun setHttpClientBuilder(builder: OkHttpClient.Builder): OkHttpClient.Builder

    /**
     * 实现重写父类的setRetrofitBuilder方法，
     * 在这里可以对Retrofit.Builder做任意操作，比如添加GSON解析器，Protocol
     */
    abstract fun setRetrofitBuilder(builder: Retrofit.Builder): Retrofit.Builder

    /**
     * 配置http
     */
    private val okHttpClient: OkHttpClient
        get() {
            var builder = OkHttpClient.Builder()
            builder = setHttpClientBuilder(builder)
            return builder.build()
        }

    /**
     * 配置https,忽略证书模式
     */
    private val okHttpClientHttps: OkHttpClient
        get() {
            return HttpsCerUtils.trustAllCertificateClient
        }
}