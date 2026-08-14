package com.sky.mvi

import android.content.Context
import com.elvishew.xlog.LogConfiguration
import com.elvishew.xlog.printer.Printer
import okhttp3.Interceptor

/**
 * @Class: SkyMVILibConfig
 * @Author: Henry
 * @Date: 2026/08/03
 * @Description: SkyMVI库配置类，支持XLog日志、SkyFlow事件流、OkHttp日志等模块配置
 */

class SkyMVILibConfig private constructor(
    var context: Context,
    var xLogLibEnabled: Boolean,
    var skyFlowLibEnabled: Boolean,
    // 使用 Any 擦除类型，避免未启用 XLog 时 JVM 加载 XLog 类导致 NoClassDefFoundError
    var xLogConfig: Any?,
    var xLogPrinter: Array<out Any>,
    var okHttpLogLibEnabled: Boolean,
    var okHttpLogConfig: Interceptor?
) {

    class Builder(private val context: Context) {
        private var xLogLibEnabled = false
        private var skyFlowLibEnabled = false
        private var xLogConfig: Any? = null
        private var xLogPrinter: Array<out Any> = emptyArray()
        private var okHttpLogLibEnabled = false
        private var okHttpLogConfig: Interceptor? = null

        /**
         * 启用 XLog 日志模块。
         *
         * 注意：请在业务模块中添加 XLog 依赖。
         * @param enableXLogLib 是否启用XLog 默认false
         * @param xLogConfig XLog 配置，传 null 则使用内置默认配置（LogLevel.ALL）
         * @param printers 自定义打印类，不传则默认使用AndroidPrinter
         *
         * @see <a href="https://github.com/elvishew/xLog/blob/master/README_ZH.md">XLog 文档</a>
         */
        fun enableXLog(
            enableXLogLib: Boolean = false,
            xLogConfig: LogConfiguration? = null,
            vararg printers: Printer
        ): Builder {
            this.xLogLibEnabled = enableXLogLib
            this.xLogConfig = xLogConfig
            this.xLogPrinter = if (printers.isNotEmpty()) printers else emptyArray()
            return this
        }

        /**
         * 启用 SkyFlow 事件总线模块
         *
         * @param enableSkyFlowLib 是否启用SkyFlow 默认false
         *
         * @see [com.sky.mvi.flow.SkyFlow]
         */
        fun enableSkyFlow(enableSkyFlowLib: Boolean = false): Builder {
            this.skyFlowLibEnabled = enableSkyFlowLib
            return this
        }

        /**
         * 启用 OkHttp 日志模块
         *
         * @param enableOkHttpLogLib 是否启用日志拦截器，默认 false
         * @param okHttpLogConfig 自定义日志拦截器配置，传 null 则使用内置默认配置
         */
        fun enableOkHttpLogLib(
            enableOkHttpLogLib: Boolean = false,
            okHttpLogConfig: Interceptor? = null
        ): Builder {
            this.okHttpLogLibEnabled = enableOkHttpLogLib
            this.okHttpLogConfig = okHttpLogConfig
            return this
        }

        fun build(): SkyMVILibConfig {
            return SkyMVILibConfig(
                context = context.applicationContext,
                xLogLibEnabled = xLogLibEnabled,
                xLogConfig = xLogConfig,
                xLogPrinter = xLogPrinter,
                skyFlowLibEnabled = skyFlowLibEnabled,
                okHttpLogLibEnabled = okHttpLogLibEnabled,
                okHttpLogConfig = okHttpLogConfig
            )
        }
    }
}
