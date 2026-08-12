package com.sky.mvi.sample

import com.sky.mvi.SkyMVILib
import com.sky.mvi.SkyMVILibConfig
import com.sky.mvi.base.BaseApplication
import com.sky.mvi.core.common.AppConfig
import dagger.hilt.android.HiltAndroidApp

/**
 * 示例 Application：继承 SkyMVILib 的 [BaseApplication] 以复用网络监听与前后台观测，
 * 通过 [@HiltAndroidApp] 启用 Hilt 依赖注入。
 *
 * 注意：ViewModel 首次访问时会调用 [SkyMVILib.requireInit] 校验初始化状态，
 * 因此必须在 Application#onCreate 最前面完成 SkyMVILib.init()。
 */
@HiltAndroidApp
class SkyMviSampleApp : BaseApplication() {
    override fun onCreate() {
        // SkyMVILib 必须在 super.onCreate() 之前初始化，
        SkyMVILib.init(
            SkyMVILibConfig.Builder(this)
                // 同时开启 XLog 与 OkHttp 日志拦截器，让 OkHttp 走通的请求/响应
                // 能通过 XLog 落盘调试；任一缺失都会让 NetworkModule 中
                // AndroidLoggingInterceptor.build() 抛 UninitializedException
                .enableXLog(enableXLogLib = true)
                .enableOkHttpLogLib(enableOkHttpLogLib = true)
                // 启用 SkyFlow 全局事件总线（登录页示例演示广播「登录失效」事件）
                .enableSkyFlow(enableSkyFlowLib = true)
                .build()
        )
        super.onCreate()
        // 非生产环境开启 Chucker 调试面板
        AppConfig.IS_PROD = false
    }
}
