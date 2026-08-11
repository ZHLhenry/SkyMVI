package com.sky.mvi.core.common

/**
 * 登录失效事件：结构化对象，直接传给 SkyFlow 全局事件总线。
 * 定义在 core/common 层，确保拦截器（core）与应用层（app）都能引用，避免循环依赖。
 */
data class TokenExpiredEvent(
    val reason: String,
    val timestamp: Long = System.currentTimeMillis()
)
