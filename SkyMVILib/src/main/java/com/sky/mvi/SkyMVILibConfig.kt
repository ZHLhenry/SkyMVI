package com.sky.mvi

import android.content.Context
import com.elvishew.xlog.LogConfiguration
import com.elvishew.xlog.printer.Printer
import okhttp3.Interceptor

/**
 * @Class: SkyMVILibConfig
 * @Author: Henry
 * @Date: 2026/08/03
 * @Description: SkyMVI库配置类，支持XLog日志、SkyFlow事件流、OkHttp日志、Compose严格模式等模块配置
 */

class SkyMVILibConfig private constructor(
    var context: Context,
    var xLogLibEnabled: Boolean,
    var skyFlowLibEnabled: Boolean,
    var xLogConfig: LogConfiguration?,
    var xLogPrinter: Array<out Printer>,
    var okHttpLogLibEnabled: Boolean,
    var okHttpLogConfig: Interceptor?,
    var strictModeEnabled: Boolean
) {

    class Builder(private val context: Context) {
        private var xLogLibEnabled = false
        private var skyFlowLibEnabled = false
        private var xLogConfig: LogConfiguration? = null
        private var xLogPrinter: Array<out Printer> = emptyArray()
        private var okHttpLogLibEnabled = false
        private var okHttpLogConfig: Interceptor? = null
        private var strictModeEnabled = false

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

        /**
         * 启用严格模式：Debug 期校验 MVI 用法（如 State 是否为 data class），
         * 发现问题时打印警告日志，便于早期暴露架构误用
         *
         * @param enableStrictMode 是否启用 默认false
         */
        fun enableStrictMode(enableStrictMode: Boolean = false): Builder {
            this.strictModeEnabled = enableStrictMode
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
                okHttpLogConfig = okHttpLogConfig,
                strictModeEnabled = strictModeEnabled
            )
        }
    }
}
