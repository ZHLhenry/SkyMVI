package com.sky.mvi.network.manager

/**
 * @Class: NetState
 * @Author: Henry
 * @Date: 2026/08/03
 * @Description: 网络状态数据类，标识当前网络是否可用
 *
 * 注：声明为 data class 以获得结构化相等语义，使 StateFlow 能正确去重，
 * 避免网络状态未真正变化时的重复通知
 */

data class NetState(
    val isSuccess: Boolean = true
)
