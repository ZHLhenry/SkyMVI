package com.sky.mvi.network.log
import com.sky.mvi.SkyMVILib
import com.sky.mvi.SkyMVILib.UninitializedException
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import javax.inject.Singleton

/**
 * @Class: AndroidLoggingInterceptor
 * @Author: Henry
 * @Date: 2025/2/23 10:22
 * @Description: OkHttp日志拦截器的Hilt依赖注入Module
 */

@InstallIn(SingletonComponent::class)
@Module
object AndroidLoggingInterceptor {
    @Singleton
    @Provides
    fun build(): Interceptor {
        SkyMVILib.requireInit()
        if (SkyMVILib.getConfig()?.okHttpLogLibEnabled == false) {
            throw UninitializedException(
                "Please add the \"enableOkHttpLogLib(true)\" attribute in the SkyMVILibConfig configuration."
            )
        }
        init()
        return SkyMVILib.getConfig()?.okHttpLogConfig!!
    }
}