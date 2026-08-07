package com.sky.mvi.core.common

/**
 * 全局网络配置。IS_PROD 由 app 模块的 Application#onCreate 中按构建类型注入。
 */
object AppConfig {
    // 是否生产环境（由 app 模块 Application 中根据 BuildConfig.FLAVOR 注入）
    var IS_PROD: Boolean = false

    // http请求地址
    const val BASE_URL = "https://www.wanandroid.com/"

    // 缓存目录名
    const val CACHE_ID = "SkyMVI_Cache"

    // 超时时间（秒）
    const val DEFAULT_TIMEOUT = 20L
}
