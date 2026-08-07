package com.sky.mvi.network.log

/**
 * @Class: LogProxy
 * @Author: Henry
 * @Date: 2025/2/23 10:24
 * @Description: 日志代理接口，定义e/w/i/d日志输出方法
 */

interface LogProxy {
    fun e(tag: String, msg: String)

    fun w(tag: String, msg: String)

    fun i(tag: String, msg: String)

    fun d(tag: String, msg: String)
}