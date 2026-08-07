package com.sky.mvi.network.log

/**
 * @Class: LogManager
 * @Author: Henry
 * @Date: 2025/2/23 10:24
 * @Description: 日志管理器，统一管理日志输出代理
 */

object LogManager {
    private var logProxy: LogProxy? = null

    fun logProxy(logProxy: LogProxy) {
        LogManager.logProxy = logProxy
    }

    fun e(tag: String, msg: String) {
        logProxy?.e(tag, msg)
    }

    fun w(tag: String, msg: String) {
        logProxy?.w(tag, msg)
    }

    fun i(tag: String, msg: String) {
        logProxy?.i(tag, msg)
    }

    fun d(tag: String, msg: String) {
        logProxy?.d(tag, msg)
    }
}