package com.sky.mvi

import android.annotation.SuppressLint
import com.elvishew.xlog.LogConfiguration
import com.elvishew.xlog.LogLevel
import com.elvishew.xlog.XLog
import com.elvishew.xlog.interceptor.BlacklistTagsFilterInterceptor
import com.elvishew.xlog.printer.AndroidPrinter
import com.elvishew.xlog.printer.Printer
import com.sky.mvi.network.log.LoggingInterceptor

/**
 * @Class: SkyMVILib
 * @Author: Henry
 * @Date: 2026/08/03
 * @Description: SkyMVI库入口，负责初始化日志、网络拦截器等核心配置
 */

object SkyMVILib {
    private const val TAG = "SkyMVILib"

    @SuppressLint("StaticFieldLeak")
    private var config: SkyMVILibConfig? = null

    /**
     * 是否已初始化
     */
    val isInitialized: Boolean
        get() = config != null

    /**
     * 初始化SkyMVILib，需在 Application#onCreate 中调用
     * @param config 配置项
     */
    fun init(config: SkyMVILibConfig) {
        if (config.xLogLibEnabled) {
            val xLogConfig: LogConfiguration = config.xLogConfig ?: LogConfiguration.Builder()
                .logLevel(LogLevel.ALL)
                .disableBorder()
                .addInterceptor(BlacklistTagsFilterInterceptor())
                .build()
            val defaultPrinter: Printer = AndroidPrinter(true)
            val printers: Array<out Printer> =
                if (config.xLogPrinter.isNotEmpty()) config.xLogPrinter else arrayOf(defaultPrinter)
            XLog.init(xLogConfig, *printers)
        }
        if (config.okHttpLogLibEnabled && config.okHttpLogConfig == null) {
            config.okHttpLogConfig = LoggingInterceptor.Builder()
                .loggable(enableLog = true)
                .androidPlatform()
                .request()
                .requestTag(tag = "Request")
                .response()
                .responseTag(tag = "Response")
                .hideVerticalLine()
                .build()
        }
        this.config = config
    }

    /**
     * 获取当前配置
     */
    fun getConfig(): SkyMVILibConfig? = config

    /**
     * 检查库是否已初始化，未初始化则抛出异常
     * @throws UninitializedException 未初始化时抛出
     */
    internal fun requireInit() {
        if (!isInitialized) {
            throw UninitializedException(
                "Please first call SkyMVILib.init() in the Application to perform the initialization."
            )
        }
    }

    /**
     * SkyMVILib 未初始化异常
     */
    class UninitializedException(message: String) : RuntimeException(message)
}
